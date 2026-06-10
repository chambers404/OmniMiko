package com.omnimiko.agent

import com.omnimiko.agent.tools.CalculatorTool
import com.omnimiko.common.model.ToolResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorToolTest {

    private val tool = CalculatorTool()

    private suspend fun eval(expr: String): ToolResult =
        tool.execute("c1", JsonObject(mapOf("expression" to JsonPrimitive(expr))))

    @Test
    fun `respects operator precedence`() = runTest {
        val r = eval("3 + 4 * 2")
        assertFalse(r.isError)
        assertEquals("11", r.content)
    }

    @Test
    fun `handles parentheses and unary minus`() = runTest {
        assertEquals("-14", eval("(3 + 4) * -2").content)
    }

    @Test
    fun `reports an error for malformed input`() = runTest {
        assertTrue(eval("3 +* 2").isError)
    }

    @Test
    fun `missing argument is an error not a crash`() = runTest {
        val r = tool.execute("c1", JsonObject(emptyMap()))
        assertTrue(r.isError)
    }
}
