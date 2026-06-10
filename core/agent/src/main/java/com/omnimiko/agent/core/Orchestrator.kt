package com.omnimiko.agent.core

import com.omnimiko.agent.prompt.SystemPromptBuilder
import com.omnimiko.agent.tools.ToolRegistry
import com.omnimiko.common.log.OmniLog
import com.omnimiko.common.model.AgentEvent
import com.omnimiko.common.model.AgentEvent.Phase
import com.omnimiko.common.model.ChatMessage
import com.omnimiko.common.model.Role
import com.omnimiko.common.model.ToolCall
import com.omnimiko.llm.engine.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import java.util.UUID

/**
 * The core agentic loop. Given a conversation, it:
 *   1. builds a system prompt advertising the available tools,
 *   2. streams an assistant turn from the [InferenceEngine],
 *   3. parses any tool calls, runs them (pausing for approval when required),
 *   4. feeds tool results back and repeats until the model answers or the
 *      iteration budget is exhausted.
 *
 * Progress is surfaced as a [Flow] of [AgentEvent] so the UI can render thinking,
 * tool activity, and approvals live — the same shape as a desktop coding agent.
 */
class Orchestrator(
    private val engine: InferenceEngine,
    private val tools: ToolRegistry,
    private val parser: ToolCallParser = ToolCallParser(),
) {

    /** Hook the UI implements to grant/deny a tool that requires approval. */
    fun interface ApprovalGate {
        suspend fun requestApproval(call: ToolCall, spec: com.omnimiko.common.model.ToolSpec): Boolean
    }

    /**
     * Run the agent over [history] (the full prior transcript, including the new
     * user message). Emits events until completion. The returned flow is cold;
     * cancelling its collection cancels generation and any in-flight tool.
     */
    fun run(
        history: List<ChatMessage>,
        config: AgentConfig = AgentConfig(),
        approvalGate: ApprovalGate? = null,
    ): Flow<AgentEvent> = channelFlow {
        if (!engine.isReady) {
            send(AgentEvent.Error("No model is loaded. Download and activate one in the Models tab."))
            return@channelFlow
        }

        val systemPrompt = SystemPromptBuilder.build(
            tools = tools.specs(),
            persona = config.persona ?: DEFAULT_PERSONA,
        )
        // Working transcript begins with the system prompt, then prior turns.
        val transcript = ArrayList<ChatMessage>(history.size + 1).apply {
            add(ChatMessage(id = UUID.randomUUID().toString(), role = Role.SYSTEM, content = systemPrompt))
            addAll(history.filter { it.role != Role.SYSTEM })
        }

        var iteration = 0
        while (iteration < config.maxIterations) {
            iteration++
            send(AgentEvent.Status(Phase.GENERATING, "Turn $iteration"))

            // Stream one assistant turn, surfacing token deltas as they arrive.
            val full = StringBuilder()
            engine.generate(transcript, config.generationParams)
                .collectInto(full) { delta -> trySend(AgentEvent.Token(delta)) }

            val parsed = parser.parse(full.toString())
            val assistantMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                role = Role.ASSISTANT,
                content = parsed.text,
                toolCalls = parsed.toolCalls,
            )
            transcript.add(assistantMessage)
            send(AgentEvent.AssistantMessage(assistantMessage))

            // No tool calls => the model has produced its final answer.
            if (parsed.toolCalls.isEmpty()) {
                send(AgentEvent.Status(Phase.DONE))
                send(AgentEvent.Completed(assistantMessage))
                return@channelFlow
            }

            // Execute each requested tool, threading results back into the transcript.
            for (call in parsed.toolCalls) {
                if (config.enforceApprovals && tools.requiresApproval(call.name)) {
                    val spec = tools.get(call.name)!!.spec
                    send(AgentEvent.ApprovalRequired(call, spec))
                    send(AgentEvent.Status(Phase.WAITING_APPROVAL, call.name))
                    val approved = approvalGate?.requestApproval(call, spec) ?: false
                    if (!approved) {
                        val denied = com.omnimiko.common.model.ToolResult(
                            toolCallId = call.id,
                            toolName = call.name,
                            isError = true,
                            content = "User denied permission to run '${call.name}'.",
                        )
                        transcript.add(denied.asMessage())
                        send(AgentEvent.ToolCallFinished(denied))
                        continue
                    }
                }

                send(AgentEvent.ToolCallStarted(call))
                send(AgentEvent.Status(Phase.CALLING_TOOL, call.name))
                val result = tools.execute(call.id, call.name, call.arguments)
                OmniLog.d(TAG, "tool ${call.name} -> error=${result.isError}")
                transcript.add(result.asMessage())
                send(AgentEvent.ToolCallFinished(result))
            }
        }

        send(
            AgentEvent.Error(
                "Reached the maximum of ${config.maxIterations} iterations without a final answer.",
            ),
        )
    }

    private fun com.omnimiko.common.model.ToolResult.asMessage() = ChatMessage(
        id = UUID.randomUUID().toString(),
        role = Role.TOOL,
        content = content,
        toolCallId = toolCallId,
    )

    private companion object {
        const val TAG = "Orchestrator"
        const val DEFAULT_PERSONA =
            "You are OmniMiko, a capable, direct on-device AI agent."
    }
}

/**
 * Collect a token flow into [sink] while forwarding each delta to [onDelta].
 * Engines emit token deltas (see [InferenceEngine.generate]); we concatenate
 * them into the full turn and stream each delta to the UI as it arrives.
 */
private suspend inline fun Flow<String>.collectInto(
    sink: StringBuilder,
    crossinline onDelta: (String) -> Unit,
) {
    collect { delta ->
        sink.append(delta)
        onDelta(delta)
    }
}
