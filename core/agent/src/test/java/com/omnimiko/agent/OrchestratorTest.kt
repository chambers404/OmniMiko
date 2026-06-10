package com.omnimiko.agent

import com.omnimiko.agent.core.Orchestrator
import com.omnimiko.agent.tools.CalculatorTool
import com.omnimiko.agent.tools.ToolRegistry
import com.omnimiko.common.model.AgentEvent
import com.omnimiko.common.model.ChatMessage
import com.omnimiko.common.model.Role
import com.omnimiko.llm.engine.MockLlmEngine
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class OrchestratorTest {

    private fun user(text: String) = ChatMessage(UUID.randomUUID().toString(), Role.USER, text)

    @Test
    fun `completes immediately when the model answers without tools`() = runTest {
        val engine = MockLlmEngine(scriptedResponse = "The answer is 42.")
        val orchestrator = Orchestrator(engine, ToolRegistry())

        val events = orchestrator.run(listOf(user("hi"))).toList()

        val completed = events.filterIsInstance<AgentEvent.Completed>().single()
        assertEquals("The answer is 42.", completed.finalMessage.content)
    }

    @Test
    fun `drives a tool call then produces a final answer`() = runTest {
        // First turn emits a calculator call; the mock then answers on the next turn.
        val responses = ArrayDeque(
            listOf(
                """<tool_call>{"name":"calculator","arguments":{"expression":"21 * 2"}}</tool_call>""",
                "That equals 42.",
            ),
        )
        val engine = object : MockLlmEngine() {
            override fun generate(
                messages: List<ChatMessage>,
                params: com.omnimiko.llm.engine.GenerationParams,
            ) = kotlinx.coroutines.flow.flow { emit(responses.removeFirst()) }
        }
        val registry = ToolRegistry(listOf(CalculatorTool()))
        val orchestrator = Orchestrator(engine, registry)

        val events = orchestrator.run(listOf(user("what is 21 * 2"))).toList()

        val toolFinished = events.filterIsInstance<AgentEvent.ToolCallFinished>().single()
        assertEquals("42", toolFinished.result.content)
        assertTrue(events.any { it is AgentEvent.Completed })
    }
}
