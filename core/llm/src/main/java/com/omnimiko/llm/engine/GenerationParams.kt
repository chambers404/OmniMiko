package com.omnimiko.llm.engine

/** Sampling/decoding parameters for a single generation request. */
data class GenerationParams(
    val maxTokens: Int = 1024,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.95f,
    /** Sequences that, when produced, stop generation (e.g. a tool-call sentinel). */
    val stopSequences: List<String> = emptyList(),
    val seed: Int = 0,
)
