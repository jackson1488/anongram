package com.anongram.app.features.services.ai.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

data class ToolCallRequest(
    val id: String,
    val name: String,
    val argumentsJson: String
)

data class StreamCompletionResult(
    val content: String,
    val reasoning: String,
    val toolCalls: List<ToolCallRequest>,
    val finishReason: String?
)

/**
 * Лёгкий OpenAI-совместимый HTTP/SSE клиент поверх стандартного HttpURLConnection.
 * Поддерживает стриминг текста, reasoning_content, tool_calls, /v1/models и Vision (image_url base64).
 */
object OpenAiCompatibleClient {

    suspend fun fetchModels(baseUrl: String, apiKey: String): Result<List<CloudModelInfo>> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanBase = baseUrl.trimEnd('/')
            val url = URL("$cleanBase/models")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/json")
                if (apiKey.isNotBlank()) {
                    setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
                }
            }
            try {
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else (conn.errorStream ?: conn.inputStream)
                val body = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                if (code !in 200..299) {
                    error("HTTP $code: ${body.take(240)}")
                }
                val root = JSONObject(body)
                val data = root.optJSONArray("data") ?: JSONArray()
                val list = mutableListOf<CloudModelInfo>()
                for (i in 0 until data.length()) {
                    val item = data.optJSONObject(i) ?: continue
                    val id = item.optString("id").takeIf { it.isNotBlank() } ?: continue
                    val name = item.optString("name").takeIf { it.isNotBlank() } ?: id
                    val lower = id.lowercase()
                    val likelyVision = lower.contains("gpt-4o") || lower.contains("gpt-5") ||
                        lower.contains("gpt-6") || lower.contains("gemini") ||
                        lower.contains("vision") || lower.contains("vl") ||
                        lower.contains("claude-3") || lower.contains("claude-4") ||
                        lower.contains("grok") || lower.contains("qwen")
                    list.add(
                        CloudModelInfo(
                            id = id,
                            displayName = name,
                            supportsVision = likelyVision,
                            supportsTools = true
                        )
                    )
                }
                list
            } finally {
                conn.disconnect()
            }
        }
    }

    suspend fun streamChatCompletion(
        baseUrl: String,
        apiKey: String,
        modelId: String,
        messages: JSONArray,
        tools: JSONArray?,
        temperature: Float,
        maxTokens: Int,
        onDelta: (contentChunk: String, reasoningChunk: String) -> Unit
    ): StreamCompletionResult = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trimEnd('/')
        val url = URL("$cleanBase/chat/completions")
        val payload = JSONObject().apply {
            put("model", modelId)
            put("messages", messages)
            put("stream", true)
            put("temperature", temperature.toDouble().coerceIn(0.0, 2.0))
            if (maxTokens > 0) {
                put("max_tokens", maxTokens)
            }
            if (tools != null && tools.length() > 0) {
                put("tools", tools)
                put("tool_choice", "auto")
            }
        }

        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 90_000
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "text/event-stream, application/json")
            if (apiKey.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer ${apiKey.trim()}")
            }
        }

        val contentBuilder = StringBuilder()
        val reasoningBuilder = StringBuilder()
        val toolAcc = linkedMapOf<Int, Triple<StringBuilder, StringBuilder, StringBuilder>>()
        var finishReason: String? = null

        try {
            conn.outputStream.use { os ->
                os.write(payload.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code !in 200..299) {
                val errBody = (conn.errorStream ?: conn.inputStream)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                error("HTTP $code: ${errBody.take(300)}")
            }

            val contentType = conn.contentType.orEmpty().lowercase()
            BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { reader ->
                if (contentType.contains("application/json") && !contentType.contains("event-stream")) {
                    val full = reader.readText()
                    val json = JSONObject(full)
                    val choices = json.optJSONArray("choices")
                    val first = choices?.optJSONObject(0)
                    finishReason = first?.optString("finish_reason")?.takeIf { it.isNotBlank() && it != "null" }
                    val msg = first?.optJSONObject("message")
                    val c = msg?.optString("content").orEmpty().takeIf { it != "null" }.orEmpty()
                    val r = msg?.optString("reasoning_content").orEmpty().takeIf { it != "null" }.orEmpty()
                    if (c.isNotEmpty() || r.isNotEmpty()) {
                        contentBuilder.append(c)
                        reasoningBuilder.append(r)
                        onDelta(c, r)
                    }
                    val tArr = msg?.optJSONArray("tool_calls")
                    if (tArr != null) {
                        for (i in 0 until tArr.length()) {
                            val tObj = tArr.optJSONObject(i) ?: continue
                            val fn = tObj.optJSONObject("function")
                            val entry = toolAcc.getOrPut(i) {
                                Triple(StringBuilder(), StringBuilder(), StringBuilder())
                            }
                            entry.first.append(tObj.optString("id"))
                            entry.second.append(fn?.optString("name").orEmpty())
                            entry.third.append(fn?.optString("arguments").orEmpty())
                        }
                    }
                } else {
                    while (true) {
                        coroutineContext.ensureActive()
                        val line = reader.readLine() ?: break
                        val trimmed = line.trim()
                        if (trimmed.isEmpty() || trimmed.startsWith(":")) continue
                        if (!trimmed.startsWith("data:")) continue
                        val data = trimmed.removePrefix("data:").trim()
                        if (data == "[DONE]") break

                        val chunk = runCatching { JSONObject(data) }.getOrNull() ?: continue
                        val choices = chunk.optJSONArray("choices") ?: continue
                        val choice = choices.optJSONObject(0) ?: continue
                        choice.optString("finish_reason").takeIf { it.isNotBlank() && it != "null" }?.let {
                            finishReason = it
                        }
                        val delta = choice.optJSONObject("delta") ?: continue

                        val rChunk = delta.optString("reasoning_content", "")
                            .takeIf { it.isNotEmpty() && it != "null" }
                            ?: delta.optString("reasoning", "").takeIf { it.isNotEmpty() && it != "null" }
                            ?: ""
                        val cChunk = delta.optString("content", "")
                            .takeIf { it.isNotEmpty() && it != "null" }
                            ?: ""

                        if (cChunk.isNotEmpty() || rChunk.isNotEmpty()) {
                            contentBuilder.append(cChunk)
                            reasoningBuilder.append(rChunk)
                            onDelta(cChunk, rChunk)
                        }

                        val tCalls = delta.optJSONArray("tool_calls")
                        if (tCalls != null) {
                            for (i in 0 until tCalls.length()) {
                                val tc = tCalls.optJSONObject(i) ?: continue
                                val idx = tc.optInt("index", i)
                                val entry = toolAcc.getOrPut(idx) {
                                    Triple(StringBuilder(), StringBuilder(), StringBuilder())
                                }
                                tc.optString("id").takeIf { it.isNotBlank() && it != "null" }?.let {
                                    if (entry.first.isEmpty()) entry.first.append(it)
                                }
                                val fn = tc.optJSONObject("function")
                                fn?.optString("name")?.takeIf { it.isNotBlank() && it != "null" }?.let {
                                    entry.second.append(it)
                                }
                                fn?.optString("arguments")?.takeIf { it.isNotEmpty() && it != "null" }?.let {
                                    entry.third.append(it)
                                }
                            }
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }

        val resolvedTools = toolAcc.entries.sortedBy { it.key }.mapNotNull { (idx, triple) ->
            val name = triple.second.toString().trim()
            if (name.isEmpty()) return@mapNotNull null
            val id = triple.first.toString().ifBlank { "call_$idx" }
            val args = triple.third.toString().ifBlank { "{}" }
            ToolCallRequest(id = id, name = name, argumentsJson = args)
        }

        StreamCompletionResult(
            content = contentBuilder.toString(),
            reasoning = reasoningBuilder.toString(),
            toolCalls = resolvedTools,
            finishReason = finishReason
        )
    }

    fun buildUserMessageJson(
        context: Context,
        text: String,
        attachments: List<AiAttachment>,
        supportsVision: Boolean
    ): JSONObject {
        val textFiles = attachments.filter { !it.isImage }
        val images = attachments.filter { it.isImage }

        val fullText = buildString {
            if (textFiles.isNotEmpty()) {
                textFiles.forEach { file ->
                    append("[Attached file: ${file.name}]\n")
                    val content = file.textContent
                    if (!content.isNullOrBlank()) {
                        append(content.take(16_000)).append("\n\n")
                    } else {
                        append("(Binary or non-text file)\n\n")
                    }
                }
            }
            append(text)
            if (images.isNotEmpty() && !supportsVision) {
                append("\n\n[Note: User attached ${images.size} image(s), but selected model may not support vision input.]")
            }
        }.trim()

        if (images.isEmpty() || !supportsVision) {
            return JSONObject().apply {
                put("role", "user")
                put("content", fullText)
            }
        }

        val parts = JSONArray()
        parts.put(JSONObject().apply {
            put("type", "text")
            put("text", fullText.ifBlank { "Analyze the attached image(s)." })
        })
        images.take(4).forEach { img ->
            val dataUrl = encodeImageAsDataUrl(context, img)
            if (dataUrl != null) {
                parts.put(JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().apply {
                        put("url", dataUrl)
                    })
                })
            }
        }
        return JSONObject().apply {
            put("role", "user")
            put("content", parts)
        }
    }

    private fun encodeImageAsDataUrl(context: Context, attachment: AiAttachment): String? {
        return runCatching {
            val rawBytes = context.contentResolver.openInputStream(attachment.uri)?.use { it.readBytes() }
                ?: return null
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, opts)
            val maxDim = 1280
            var sample = 1
            while (opts.outWidth / sample > maxDim || opts.outHeight / sample > maxDim) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOpts) ?: return null
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bmp.recycle()
            val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            "data:image/jpeg;base64,$b64"
        }.getOrNull()
    }
}
