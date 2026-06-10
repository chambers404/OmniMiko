package com.omnimiko.llm.engine

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession.LlmInferenceSessionOptions
import com.omnimiko.common.log.OmniLog
import com.omnimiko.common.model.ChatMessage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import java.io.Closeable

/**
 * [InferenceEngine] backed by MediaPipe's LLM Inference API. Consumes `.task`
 * model bundles (Gemma, Phi, etc.) and runs CPU or GPU inference fully
 * on-device. A new session is created per generation so sampling params can
 * vary per request.
 *
 * Note: the MediaPipe artifact is resolved at build time; this class is written
 * against its public surface. On machines without the Android SDK it will not
 * compile, which is expected — it is the production backend.
 */
class MediaPipeLlmEngine(
    private val appContext: Context,
    private val formatter: PromptFormatter = PromptFormatter(),
) : InferenceEngine, Closeable {

    private var engine: LlmInference? = null
    private var currentPath: String? = null

    override val isReady: Boolean get() = engine != null
    override val loadedModelId: String? get() = currentPath

    override suspend fun load(modelPath: String, options: EngineOptions) {
        if (currentPath == modelPath && engine != null) return
        unload()
        OmniLog.i(TAG, "Loading MediaPipe model: $modelPath")
        val opts = LlmInferenceOptions.builder()
            .setModelPath(modelPath)
            .setMaxTokens(options.maxContextTokens)
            .apply { if (options.gpuLayers > 0) setPreferredBackend(LlmInference.Backend.GPU) }
            .build()
        engine = LlmInference.createFromOptions(appContext, opts)
        currentPath = modelPath
    }

    override suspend fun unload() {
        engine?.let {
            OmniLog.i(TAG, "Unloading MediaPipe model")
            runCatching { it.close() }
        }
        engine = null
        currentPath = null
    }

    override fun generate(messages: List<ChatMessage>, params: GenerationParams): Flow<String> =
        callbackFlow {
            val inference = engine
                ?: throw IllegalStateException("No model loaded; call load() first")

            val sessionOptions = LlmInferenceSessionOptions.builder()
                .setTopK(params.topK)
                .setTopP(params.topP)
                .setTemperature(params.temperature)
                .build()

            val session = LlmInferenceSession.createFromOptions(inference, sessionOptions)
            session.addQueryChunk(formatter.format(messages))

            session.generateResponseAsync { partial, done ->
                trySend(partial)
                if (done) close()
            }

            awaitClose { runCatching { session.close() } }
        }.flowOn(Dispatchers.Default)

    override fun close() {
        engine?.close()
        engine = null
    }

    private companion object {
        const val TAG = "MediaPipeLlmEngine"
    }
}
