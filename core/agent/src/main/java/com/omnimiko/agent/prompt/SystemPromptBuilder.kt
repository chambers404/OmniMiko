package com.omnimiko.agent.prompt

import com.omnimiko.common.model.ToolSpec

/**
 * Builds the system prompt that turns a plain chat model into an agent: it
 * explains the ReAct-style loop, lists available tools as JSON schemas, and
 * specifies the exact `<tool_call>` syntax the orchestrator parses. This is what
 * lets models without native function-calling still drive tools reliably.
 */
object SystemPromptBuilder {

    fun build(tools: List<ToolSpec>, persona: String = DEFAULT_PERSONA): String = buildString {
        appendLine(persona)
        appendLine()
        appendLine("# How you operate")
        appendLine(
            """
            You run fully on the user's device. You solve tasks by thinking, then
            optionally calling tools, observing their results, and repeating until
            you can answer. Keep reasoning concise. Prefer tools over guessing for
            anything factual, computational, or involving files or the web.
            """.trimIndent(),
        )
        appendLine()
        appendLine("# Tool-calling protocol")
        appendLine(
            """
            To call a tool, emit a single line of the exact form:
            <tool_call>{"name": "<tool_name>", "arguments": { ... }}</tool_call>
            Emit nothing after a tool call on that turn — wait for the result,
            which arrives as a message with role "tool". You may call multiple
            tools across turns. When you have the final answer, reply normally
            with no tool call.
            """.trimIndent(),
        )
        appendLine()
        appendLine("# Available tools")
        if (tools.isEmpty()) {
            appendLine("(none)")
        } else {
            for (t in tools) appendLine(renderTool(t))
        }
    }

    private fun renderTool(t: ToolSpec): String = buildString {
        append("- ").append(t.name).append(": ").append(t.description)
        if (t.requiresApproval) append("  [requires user approval]")
        if (t.parameters.isNotEmpty()) {
            append("\n    params:")
            for (p in t.parameters) {
                append("\n      - ").append(p.name).append(" (").append(p.type)
                append(if (p.required) ", required" else ", optional").append("): ").append(p.description)
            }
        }
    }

    private const val DEFAULT_PERSONA =
        "You are OmniMiko, a capable, direct on-device AI agent. You are private by " +
            "design: nothing leaves the device unless a tool explicitly requires it."
}
