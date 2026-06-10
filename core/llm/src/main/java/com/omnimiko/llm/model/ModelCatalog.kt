package com.omnimiko.llm.model

/**
 * A model the user can download and run on-device. URLs point to redistributable
 * quantized bundles; sizes are approximate and used to warn before large
 * downloads on metered connections.
 */
data class CatalogModel(
    val id: String,
    val displayName: String,
    val description: String,
    val downloadUrl: String,
    val fileName: String,
    val approxSizeBytes: Long,
    val format: ModelFormat,
    val contextTokens: Int,
    val supportsTools: Boolean,
    /** RAM headroom recommended to run this comfortably, in MB. */
    val recommendedRamMb: Int,
)

enum class ModelFormat {
    /** MediaPipe LLM Inference bundle. */
    MEDIAPIPE_TASK,
    /** llama.cpp GGUF (requires the GGUF backend). */
    GGUF,
}

/**
 * Default catalog. These are placeholders pointing at common community bundles;
 * the actual URLs should be configured per deployment (some require accepting a
 * license on Hugging Face). The app reads any user-added entries from settings.
 */
object ModelCatalog {
    val defaults: List<CatalogModel> = listOf(
        CatalogModel(
            id = "gemma2-2b-it-q8",
            displayName = "Gemma 2 · 2B Instruct (INT8)",
            description = "Fast, capable general assistant. Best balance for mid-range phones.",
            downloadUrl = "https://example.invalid/models/gemma2-2b-it-int8.task",
            fileName = "gemma2-2b-it-int8.task",
            approxSizeBytes = 2_700_000_000L,
            format = ModelFormat.MEDIAPIPE_TASK,
            contextTokens = 8192,
            supportsTools = true,
            recommendedRamMb = 6144,
        ),
        CatalogModel(
            id = "qwen2.5-1.5b-instruct-q4",
            displayName = "Qwen2.5 · 1.5B Instruct (Q4_K_M)",
            description = "Lightweight, strong tool-calling. Runs on 4 GB devices.",
            downloadUrl = "https://example.invalid/models/qwen2.5-1.5b-instruct-q4_k_m.gguf",
            fileName = "qwen2.5-1.5b-instruct-q4_k_m.gguf",
            approxSizeBytes = 1_100_000_000L,
            format = ModelFormat.GGUF,
            contextTokens = 32768,
            supportsTools = true,
            recommendedRamMb = 4096,
        ),
        CatalogModel(
            id = "phi-3.5-mini-instruct-q4",
            displayName = "Phi-3.5 · Mini Instruct (Q4)",
            description = "Reasoning-focused small model with a long context window.",
            downloadUrl = "https://example.invalid/models/phi-3.5-mini-instruct-q4.gguf",
            fileName = "phi-3.5-mini-instruct-q4.gguf",
            approxSizeBytes = 2_300_000_000L,
            format = ModelFormat.GGUF,
            contextTokens = 16384,
            supportsTools = true,
            recommendedRamMb = 6144,
        ),
    )
}
