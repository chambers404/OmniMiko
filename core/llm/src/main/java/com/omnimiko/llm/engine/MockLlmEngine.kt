package com.omnimiko.llm.engine

import com.omnimiko.common.model.ChatMessage
import com.omnimiko.common.model.Role
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * A deterministic, dependency-free engine used for UI development, previews, and
 * unit tests of the agent loop without a real model on device. It echoes a
 * canned response and can be scripted to emit a tool call so the orchestrator
 * path is exercisable end-to-end.
 */
open class MockLlmEngine(
    /** When set, the next generation emits exactly this text (e.g. a tool call). */
    var scriptedResponse: String? = null,
    private val tokenDelayMs: Long = 12L,
) : InferenceEngine {

    override val isReady: Boolean = true
    override val loadedModelId: String? = "mock://echo"

    override suspend fun load(modelPath: String, options: EngineOptions) = Unit
    override suspend fun unload() = Unit

    override fun generate(messages: List<ChatMessage>, params: GenerationParams): Flow<String> = flow {
        val response = scriptedResponse ?: defaultReply(messages)
        scriptedResponse = null
        // Stream word-by-word to mimic incremental decoding.
        for (chunk in response.chunkedWords()) {
            delay(tokenDelayMs)
            emit(chunk)
        }
    }

    private fun defaultReply(messages: List<ChatMessage>): String {
        val lastUser = messages.lastOrNull { it.role == Role.USER }?.content?.trim().orEmpty()
        return "I'm OmniMiko running on a mock engine. You said: \"$lastUser\". " +
            "Load a real on-device model from the Models tab to get genuine responses."
    }

    private fun String.chunkedWords(): List<String> =
        split(" ").mapIndexed { i, w -> if (i == 0) w else " $w" }
}
