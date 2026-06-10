package com.omnimiko.data.conversation

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    /** Id of the model active when this conversation was last used, if any. */
    val modelId: String? = null,
)

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("conversationId")],
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String,
    val content: String,
    /** Serialized List<ToolCall> as JSON, empty when none. */
    val toolCallsJson: String,
    val toolCallId: String?,
    val createdAt: Long,
)

/** A durable fact the agent has learned and may recall in future sessions. */
@Entity(tableName = "memories", indices = [Index("key", unique = true)])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val key: String,
    val value: String,
    val createdAt: Long,
    val updatedAt: Long,
)
