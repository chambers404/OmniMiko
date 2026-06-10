package com.omnimiko.agent.tools

import com.omnimiko.common.model.ToolResult
import com.omnimiko.common.model.ToolSpec
import kotlinx.serialization.json.JsonObject

/** Holds the set of tools available to the agent and dispatches calls by name. */
class ToolRegistry(tools: List<Tool> = emptyList()) {

    private val byName: MutableMap<String, Tool> =
        tools.associateBy { it.spec.name }.toMutableMap()

    fun register(tool: Tool) {
        byName[tool.spec.name] = tool
    }

    fun unregister(name: String) {
        byName.remove(name)
    }

    fun specs(): List<ToolSpec> = byName.values.map { it.spec }

    fun get(name: String): Tool? = byName[name]

    fun requiresApproval(name: String): Boolean = byName[name]?.spec?.requiresApproval == true

    suspend fun execute(callId: String, name: String, arguments: JsonObject): ToolResult {
        val tool = byName[name]
            ?: return ToolResult(
                toolCallId = callId,
                toolName = name,
                isError = true,
                content = "Unknown tool: '$name'. Available: ${byName.keys.joinToString()}",
            )
        return runCatching { tool.execute(callId, arguments) }.getOrElse { t ->
            if (t is kotlinx.coroutines.CancellationException) throw t
            ToolResult(callId, name, isError = true, content = "Tool threw: ${t.message}")
        }
    }
}
