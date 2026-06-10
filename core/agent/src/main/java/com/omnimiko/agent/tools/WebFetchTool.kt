package com.omnimiko.agent.tools

import com.omnimiko.common.model.ParameterSpec
import com.omnimiko.common.model.ToolResult
import com.omnimiko.common.model.ToolSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Fetches the text content of a URL. This is the one tool that leaves the device,
 * so it is gated behind approval by default and strips HTML to plain-ish text to
 * keep the model's context budget under control.
 */
class WebFetchTool(
    private val client: OkHttpClient = OkHttpClient(),
) : Tool {
    override val spec = ToolSpec(
        name = "web_fetch",
        description = "Fetch the readable text of a web page by URL (HTTP GET).",
        parameters = listOf(
            ParameterSpec("url", "string", "Absolute http(s) URL to fetch."),
        ),
        requiresApproval = true,
    )

    override suspend fun execute(callId: String, arguments: JsonObject): ToolResult =
        withContext(Dispatchers.IO) {
            val url = arguments["url"]?.jsonPrimitive?.content
                ?: return@withContext failure(callId, "Missing 'url'.")
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                return@withContext failure(callId, "URL must be http(s): $url")
            }
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@use failure(callId, "HTTP ${resp.code} for $url")
                    val raw = resp.body?.string().orEmpty()
                    val text = stripHtml(raw).trim()
                    success(callId, if (text.length > MAX) text.take(MAX) + "\n…[truncated]" else text)
                }
            }.getOrElse { failure(callId, "Fetch failed: ${it.message}") }
        }

    private fun stripHtml(html: String): String = html
        .replace(Regex("(?is)<(script|style)[^>]*>.*?</\\1>"), " ")
        .replace(Regex("(?s)<[^>]+>"), " ")
        .replace(Regex("&nbsp;"), " ")
        .replace(Regex("[ \\t]+"), " ")
        .replace(Regex("\\n\\s*\\n\\s*\\n+"), "\n\n")

    private companion object { const val MAX = 40_000 }
}
