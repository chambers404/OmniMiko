package com.omnimiko.data.conversation

import com.omnimiko.common.model.ChatMessage
import com.omnimiko.common.model.Role
import com.omnimiko.common.model.ToolCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Persists conversations and their messages, mapping between Room entities and
 * the domain [ChatMessage]. Tool calls are stored as JSON so a turn round-trips
 * exactly, letting a session resume mid-plan after the app is killed.
 */
class ConversationRepository(
    private val conversations: ConversationDao,
    private val messages: MessageDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    fun observeConversations(): Flow<List<Conversation>> =
        conversations.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeMessages(conversationId: String): Flow<List<ChatMessage>> =
        messages.observeForConversation(conversationId).map { list -> list.map { it.toDomain() } }

    suspend fun messagesOf(conversationId: String): List<ChatMessage> =
        messages.forConversation(conversationId).map { it.toDomain() }

    suspend fun createConversation(title: String, modelId: String?): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        conversations.upsert(
            ConversationEntity(id = id, title = title, createdAt = now, updatedAt = now, modelId = modelId),
        )
        return id
    }

    suspend fun appendMessage(conversationId: String, message: ChatMessage) {
        messages.insert(message.toEntity(conversationId))
        conversations.byId(conversationId)?.let {
            conversations.upsert(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun rename(conversationId: String, title: String) {
        conversations.byId(conversationId)?.let {
            conversations.upsert(it.copy(title = title, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun delete(conversationId: String) = conversations.delete(conversationId)

    // ---- mapping ----

    private fun ConversationEntity.toDomain() =
        Conversation(id = id, title = title, updatedAt = updatedAt, modelId = modelId)

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        role = Role.valueOf(role),
        content = content,
        toolCalls = if (toolCallsJson.isBlank()) emptyList()
        else json.decodeFromString<List<ToolCall>>(toolCallsJson),
        toolCallId = toolCallId,
        createdAt = createdAt,
    )

    private fun ChatMessage.toEntity(conversationId: String) = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role.name,
        content = content,
        toolCallsJson = if (toolCalls.isEmpty()) "" else json.encodeToString(toolCalls),
        toolCallId = toolCallId,
        createdAt = createdAt,
    )
}

/** Lightweight conversation summary for list screens. */
data class Conversation(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val modelId: String?,
)
