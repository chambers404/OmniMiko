package com.omnimiko.agent.tools

import com.omnimiko.common.model.ToolResult
import com.omnimiko.common.model.ToolSpec
import kotlinx.serialization.json.JsonObject

/**
 * A capability the agent can invoke. Each tool declares a [spec] (rendered into
 * the system prompt) and an [execute] implementation. Tools must be safe to call
 * concurrently with cancellation and should never throw — return an error
 * [ToolResult] instead so the loop can recover.
 */
interface Tool {
    val spec: ToolSpec

    suspend fun execute(callId: String, arguments: JsonObject): ToolResult

    /** Convenience for building a successful result. */
    fun success(callId: String, content: String): ToolResult =
        ToolResult(toolCallId = callId, toolName = spec.name, isError = false, content = content)

    /** Convenience for building an error result. */
    fun failure(callId: String, message: String): ToolResult =
        ToolResult(toolCallId = callId, toolName = spec.name, isError = true, content = message)
}
