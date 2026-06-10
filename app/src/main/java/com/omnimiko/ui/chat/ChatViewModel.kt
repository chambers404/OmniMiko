package com.omnimiko.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.omnimiko.agent.core.AgentConfig
import com.omnimiko.agent.core.Orchestrator
import com.omnimiko.common.model.AgentEvent
import com.omnimiko.common.model.ChatMessage
import com.omnimiko.common.model.Role
import com.omnimiko.common.model.ToolCall
import com.omnimiko.common.model.ToolSpec
import com.omnimiko.data.conversation.ConversationRepository
import com.omnimiko.di.AppContainer
import com.omnimiko.llm.model.ModelManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** A tool call awaiting the user's approval, with the gate to resolve it. */
data class PendingApproval(
    val call: ToolCall,
    val spec: ToolSpec,
    private val decision: CompletableDeferred<Boolean>,
) {
    fun approve() = decision.complete(true)
    fun deny() = decision.complete(false)
}

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    /** Text being streamed for the in-progress assistant turn. */
    val streamingText: String = "",
    val isRunning: Boolean = false,
    val statusLine: String = "",
    val activeModelName: String? = null,
    val pendingApproval: PendingApproval? = null,
    val error: String? = null,
)

class ChatViewModel(
    private val orchestrator: Orchestrator,
    private val conversations: ConversationRepository,
    private val modelManager: ModelManager,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var conversationId: String? = null
    private var runJob: Job? = null

    private val approvalGate = Orchestrator.ApprovalGate { call, spec ->
        val deferred = CompletableDeferred<Boolean>()
        _state.update { it.copy(pendingApproval = PendingApproval(call, spec, deferred)) }
        val approved = deferred.await()
        _state.update { it.copy(pendingApproval = null) }
        approved
    }

    init {
        viewModelScope.launch {
            modelManager.activeModel.collect { model ->
                _state.update { it.copy(activeModelName = model?.displayName) }
            }
        }
    }

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _state.value.isRunning) return

        val userMessage = ChatMessage(UUID.randomUUID().toString(), Role.USER, trimmed)
        val history = _state.value.messages + userMessage
        _state.update {
            it.copy(messages = history, isRunning = true, streamingText = "", error = null)
        }

        runJob = viewModelScope.launch {
            ensureConversation(trimmed)
            conversationId?.let { conversations.appendMessage(it, userMessage) }

            val config = AgentConfig()
            orchestrator.run(history, config, approvalGate).collect { event ->
                handleEvent(event)
            }
        }
    }

    fun stop() {
        runJob?.cancel()
        runJob = null
        _state.update { it.copy(isRunning = false, statusLine = "Stopped", streamingText = "") }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    private suspend fun handleEvent(event: AgentEvent) {
        when (event) {
            is AgentEvent.Token ->
                _state.update { it.copy(streamingText = it.streamingText + event.text) }

            is AgentEvent.Status ->
                _state.update { it.copy(statusLine = event.phase.name.lowercase() + " " + event.detail) }

            is AgentEvent.AssistantMessage -> {
                // Commit the streamed turn into the transcript (skip empty tool-only turns).
                if (event.message.content.isNotBlank()) {
                    appendAndPersist(event.message)
                }
                _state.update { it.copy(streamingText = "") }
            }

            is AgentEvent.ToolCallStarted ->
                _state.update { it.copy(statusLine = "calling ${event.call.name}…") }

            is AgentEvent.ToolCallFinished -> {
                val toolMsg = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = Role.TOOL,
                    content = event.result.content,
                    toolCallId = event.result.toolCallId,
                )
                appendAndPersist(toolMsg)
            }

            is AgentEvent.ApprovalRequired -> Unit // surfaced via approvalGate state

            is AgentEvent.Completed ->
                _state.update { it.copy(isRunning = false, statusLine = "done") }

            is AgentEvent.Error ->
                _state.update { it.copy(isRunning = false, error = event.message, statusLine = "") }
        }
    }

    private suspend fun appendAndPersist(message: ChatMessage) {
        _state.update { it.copy(messages = it.messages + message) }
        conversationId?.let { conversations.appendMessage(it, message) }
    }

    private suspend fun ensureConversation(firstPrompt: String) {
        if (conversationId == null) {
            val title = firstPrompt.take(40).ifBlank { "New chat" }
            conversationId = conversations.createConversation(title, _state.value.activeModelName)
        }
    }

    /** Factory wiring the ViewModel from the [AppContainer] without a DI framework. */
    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(
            orchestrator = container.orchestrator,
            conversations = container.conversationRepository,
            modelManager = container.modelManager,
        ) as T
    }
}
