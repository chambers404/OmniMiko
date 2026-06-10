package com.omnimiko.agent

import com.omnimiko.agent.core.ToolCallParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolCallParserTest {

    private val parser = ToolCallParser()

    @Test
    fun `extracts a single tool call and surrounding text`() {
        val raw = """
            Let me check the time first.
            <tool_call>{"name": "current_time", "arguments": {}}</tool_call>
        """.trimIndent()

        val parsed = parser.parse(raw)

        assertEquals(1, parsed.toolCalls.size)
        assertEquals("current_time", parsed.toolCalls.first().name)
        assertTrue(parsed.text.contains("check the time"))
        assertTrue("tool-call block stripped from prose", !parsed.text.contains("<tool_call>"))
    }

    @Test
    fun `parses arguments object`() {
        val raw = """<tool_call>{"name":"calculator","arguments":{"expression":"2 + 2"}}</tool_call>"""

        val call = parser.parse(raw).toolCalls.single()

        assertEquals("calculator", call.name)
        assertEquals("\"2 + 2\"", call.arguments["expression"].toString())
    }

    @Test
    fun `returns no calls for plain prose`() {
        val parsed = parser.parse("Here is your final answer, no tools needed.")
        assertTrue(parsed.toolCalls.isEmpty())
        assertEquals("Here is your final answer, no tools needed.", parsed.text)
    }

    @Test
    fun `tolerates a missing closing tag at end of stream`() {
        val raw = """Thinking…<tool_call>{"name":"current_time","arguments":{}}"""
        val parsed = parser.parse(raw)
        assertEquals(1, parsed.toolCalls.size)
        assertEquals("current_time", parsed.toolCalls.first().name)
    }
}
