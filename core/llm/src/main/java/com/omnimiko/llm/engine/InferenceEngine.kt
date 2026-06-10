package com.omnimiko.llm.engine

import com.omnimiko.common.model.ChatMessage
import kotlinx.coroutines.flow.Flow

/**
 * Backend-agnostic contract for on-device text generation. Implementations wrap
 * a concrete runtime (MediaPipe LLM Inference, llama.cpp via JNI, ONNX Runtime,
 * etc.). The orchestrator depends only on this interface, so swapping the
 * runtime never touches agent logic.
 */
interface InferenceEngine {

    /** Whether a model is currently loaded and ready to generate. */
    val isReady: Boolean

    /** Identifier of the loaded model, or null if none. */
    val loadedModelId: String?

    /** Load a model from an on-device file path. Idempotent for the same path. */
    suspend fun load(modelPath: String, options: EngineOptions = EngineOptions())

    /** Release native resources. Safe to call when nothing is loaded. */
    suspend fun unload()

    /**
     * Stream a completion for the given transcript. Emits token deltas; the
     * collector concatenates them. Cancellation of the collecting coroutine
     * must stop the underlying native generation.
     */
    fun generate(
        messages: List<ChatMessage>,
        params: GenerationParams = GenerationParams(),
    ): Flow<String>

    /** Approximate token count for budgeting context windows. */
    fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)
}

/** Engine-level options fixed at load time (as opposed to per-request params). */
data class EngineOptions(
    val maxContextTokens: Int = 4096,
    /** Number of GPU layers to offload, when the backend supports it. */
    val gpuLayers: Int = 0,
    val numThreads: Int = Runtime.getRuntime().availableProcessors().coerceAtMost(6),
)
