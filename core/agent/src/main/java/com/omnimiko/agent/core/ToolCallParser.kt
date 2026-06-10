package com.omnimiko.agent.core

import com.omnimiko.common.model.ToolCall
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

/**
 * Extracts `<tool_call>{...}</tool_call>` blocks from model output and the
 * surrounding free text. Robust to minor model sloppiness: tolerates whitespace,
 * code fences around the JSON, and missing closing tags at end-of-stream.
 */
class ToolCallParser(private val json: Json = Json { ignoreUnknownKeys = true }) {

    data class Parsed(
        /** Assistant prose with tool-call blocks removed. */
        val text: String,
        val toolCalls: List<ToolCall>,
    )

    fun parse(raw: String): Parsed {
        val calls = mutableListOf<ToolCall>()
        val leftover = StringBuilder()
        var index = 0

        while (true) {
            val open = raw.indexOf(OPEN, index)
            if (open == -1) {
                leftover.append(raw, index, raw.length)
                break
            }
            leftover.append(raw, index, open)
            val bodyStart = open + OPEN.length
            val close = raw.indexOf(CLOSE, bodyStart)
            val bodyEnd = if (close == -1) raw.length else close
            val body = raw.substring(bodyStart, bodyEnd)
            parseOne(body)?.let { calls.add(it) }
            index = if (close == -1) raw.length else close + CLOSE.length
        }

        return Parsed(text = leftover.toString().trim(), toolCalls = calls)
    }

    private fun parseOne(body: String): ToolCall? {
        val cleaned = body.trim().removeSurrounding("```json", "```").removeSurrounding("```").trim()
        return runCatching {
            val obj = json.parseToJsonElement(cleaned).jsonObject
            val name = obj["name"]?.jsonPrimitive?.content
            val args: JsonObject = obj["arguments"]?.jsonObject ?: JsonObject(emptyMap())
            if (name.isNullOrBlank()) null
            else ToolCall(id = UUID.randomUUID().toString(), name = name, arguments = args)
        }.getOrNull()
    }

    private companion object {
        const val OPEN = "<tool_call>"
        const val CLOSE = "</tool_call>"
    }
}
