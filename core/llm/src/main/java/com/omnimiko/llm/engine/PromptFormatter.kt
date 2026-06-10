package com.omnimiko.llm.engine

import com.omnimiko.common.model.ChatMessage
import com.omnimiko.common.model.Role

/**
 * Renders a transcript into the single prompt string most local chat models
 * expect. Defaults to a ChatML-style template, which Gemma/Qwen/Llama-Instruct
 * GGUF builds and MediaPipe `.task` bundles all tolerate well. Swap [template]
 * when a model needs a different control-token convention.
 */
class PromptFormatter(private val template: ChatTemplate = ChatTemplate.ChatML) {

    fun format(messages: List<ChatMessage>): String = buildString {
        for (m in messages) {
            val tag = when (m.role) {
                Role.SYSTEM -> "system"
                Role.USER -> "user"
                Role.ASSISTANT -> "assistant"
                Role.TOOL -> "tool"
            }
            when (template) {
                ChatTemplate.ChatML -> {
                    append("<|im_start|>").append(tag).append('\n')
                    append(m.content)
                    if (m.toolCalls.isNotEmpty()) {
                        append('\n').append(renderToolCalls(m))
                    }
                    append("<|im_end|>\n")
                }
            }
        }
        // Prime the model for its reply.
        append("<|im_start|>assistant\n")
    }

    private fun renderToolCalls(m: ChatMessage): String =
        m.toolCalls.joinToString("\n") { call ->
            "<tool_call>{\"name\":\"${call.name}\",\"arguments\":${call.arguments}}</tool_call>"
        }

    enum class ChatTemplate { ChatML }
}
