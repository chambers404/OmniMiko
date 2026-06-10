package com.omnimiko.common.model

import kotlinx.serialization.Serializable

/** The author of a message in a conversation transcript. */
@Serializable
enum class Role {
    SYSTEM,
    USER,
    ASSISTANT,
    /** Output produced by a tool, fed back into the model on the next turn. */
    TOOL,
}

/**
 * A single turn in a conversation. [toolCalls] is populated on ASSISTANT turns
 * when the model decides to invoke tools; [toolCallId] links a TOOL turn back to
 * the call that produced it.
 */
@Serializable
data class ChatMessage(
    val id: String,
    val role: Role,
    val content: String,
    val toolCalls: List<ToolCall> = emptyList(),
    val toolCallId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
