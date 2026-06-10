package com.omnimiko.common.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Declarative description of a tool the model is allowed to call. Rendered into
 * the system prompt so even models without native function-calling can emit
 * structured tool calls (parsed by the orchestrator).
 */
@Serializable
data class ToolSpec(
    val name: String,
    val description: String,
    /** JSON-schema-ish parameter map: name -> ParameterSpec. */
    val parameters: List<ParameterSpec> = emptyList(),
    /** Tools that can mutate state/device should be gated behind user approval. */
    val requiresApproval: Boolean = false,
)

@Serializable
data class ParameterSpec(
    val name: String,
    val type: String, // "string" | "number" | "boolean" | "object" | "array"
    val description: String,
    val required: Boolean = true,
)

/** A concrete request from the model to run a tool with specific arguments. */
@Serializable
data class ToolCall(
    val id: String,
    val name: String,
    @SerialName("arguments")
    val arguments: JsonObject,
)

/** The outcome of executing a [ToolCall]. */
@Serializable
data class ToolResult(
    val toolCallId: String,
    val toolName: String,
    val isError: Boolean,
    val content: String,
)
