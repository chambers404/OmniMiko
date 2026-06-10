package com.omnimiko.agent.core

import com.omnimiko.llm.engine.GenerationParams

/** Tunables for an agent run. */
data class AgentConfig(
    /** Hard cap on reasoning/tool iterations to prevent runaway loops. */
    val maxIterations: Int = 8,
    /** Sampling parameters passed to the inference engine each turn. */
    val generationParams: GenerationParams = GenerationParams(
        maxTokens = 1024,
        temperature = 0.7f,
        // Stop as soon as a tool call closes so we can act without waiting for more tokens.
        stopSequences = listOf("</tool_call>"),
    ),
    /** When true, tools marked requiresApproval pause the loop for user consent. */
    val enforceApprovals: Boolean = true,
    /** Optional persona override for the system prompt. */
    val persona: String? = null,
)
