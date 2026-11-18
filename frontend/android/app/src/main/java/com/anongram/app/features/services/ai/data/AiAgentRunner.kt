package com.anongram.app.features.services.ai.data

import android.content.Context
import kotlinx.coroutines.ensureActive
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.coroutineContext

/**
 * Агентский цикл (OpenCode-style):
 * отправляет сообщения в OpenAI-совместимый API, перехватывает tool_calls,
 * выполняет web_search / web_fetch и возвращает результат модели до финального ответа.
 */
object AiAgentRunner {
    private const val MAX_TOOL_STEPS = 6

    suspend fun runCloudTurn(
        context: Context,
        provider: CloudProviderProfile,
        model: CloudModelInfo,
        systemPrompt: String,
        historyMessages: List<AiMessage>,
        userPrompt: String,
        attachments: List<AiAttachment>,
        temperature: Float,
        maxTokens: Int,
        webToolsEnabled: Boolean,
        isRussian: Boolean,
        onToolStep: (String) -> Unit,
        onStreamUpdate: (content: String, reasoning: String) -> Unit
    ) {
        val messages = JSONArray()
        if (systemPrompt.isNotBlank()) {
            val agentHint = if (webToolsEnabled) {
                if (isRussian) {
                    "\n\nДоступны инструменты web_search и web_fetch. Используй их, когда нужна актуальная информация из сети или содержимое ссылки."
                } else {
                    "\n\nYou have web_search and web_fetch tools available. Use them whenever up-to-date web information or URL content is needed."
                }
            } else ""
            messages.put(JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt + agentHint)
            })
        }

        // Добавляем недавнюю историю диалога (до 14 сообщений)
        historyMessages.takeLast(14).forEach { msg ->
            if (msg.isUser) {
                if (msg.attachments.isNotEmpty() && msg.content == userPrompt && msg.attachments == attachments) {
                    // текущее сообщение добавим ниже с полным Vision/файлами
                } else {
                    messages.put(JSONObject().apply {
                        put("role", "user")
                        put("content", msg.content)
                    })
                }
            } else if (msg.content.isNotBlank()) {
                messages.put(JSONObject().apply {
                    put("role", "assistant")
                    put("content", msg.content)
                })
            }
        }

        // Текущий запрос пользователя (с поддержкой фото base64 и текстовых файлов)
        messages.put(
            OpenAiCompatibleClient.buildUserMessageJson(
                context = context,
                text = userPrompt,
                attachments = attachments,
                supportsVision = model.supportsVision
            )
        )

        val toolsJson = if (webToolsEnabled && model.supportsTools) {
            AiAgentTools.toolDefinitionsJson()
        } else {
            null
        }

        val totalContent = StringBuilder()
        val totalReasoning = StringBuilder()

        for (step in 0 until MAX_TOOL_STEPS) {
            coroutineContext.ensureActive()
            val stepContentStart = totalContent.length
            val stepReasoningStart = totalReasoning.length

            val result = OpenAiCompatibleClient.streamChatCompletion(
                baseUrl = provider.baseUrl,
                apiKey = provider.apiKey,
                modelId = model.id,
                messages = messages,
                tools = toolsJson,
                temperature = temperature,
                maxTokens = maxTokens,
                onDelta = { cChunk, rChunk ->
                    if (cChunk.isNotEmpty()) totalContent.append(cChunk)
                    if (rChunk.isNotEmpty()) totalReasoning.append(rChunk)
                    onStreamUpdate(totalContent.toString(), totalReasoning.toString())
                }
            )

            if (result.toolCalls.isEmpty() || !webToolsEnabled) {
                break
            }

            // Откатываем промежуточный текст только если модель не оставила осмысленного ответа до tool_calls
            val assistantStepText = totalContent.substring(stepContentStart)
            if (assistantStepText.isNotBlank() && step < MAX_TOOL_STEPS - 1) {
                totalContent.append("\n\n")
                onStreamUpdate(totalContent.toString(), totalReasoning.toString())
            }

            val assistantMsg = JSONObject().apply {
                put("role", "assistant")
                if (result.content.isNotBlank()) {
                    put("content", result.content)
                } else {
                    put("content", JSONObject.NULL)
                }
                val tcArr = JSONArray()
                result.toolCalls.forEach { tc ->
                    tcArr.put(JSONObject().apply {
                        put("id", tc.id)
                        put("type", "function")
                        put("function", JSONObject().apply {
                            put("name", tc.name)
                            put("arguments", tc.argumentsJson)
                        })
                    })
                }
                put("tool_calls", tcArr)
            }
            messages.put(assistantMsg)

            result.toolCalls.forEach { tc ->
                coroutineContext.ensureActive()
                val label = formatToolStepLabel(tc.name, tc.argumentsJson, isRussian)
                onToolStep(label)
                val toolOutput = AiAgentTools.executeTool(tc.name, tc.argumentsJson)
                messages.put(JSONObject().apply {
                    put("role", "tool")
                    put("tool_call_id", tc.id)
                    put("name", tc.name)
                    put("content", toolOutput)
                })
            }

            if (totalReasoning.length > stepReasoningStart) {
                totalReasoning.append("\n")
            }
        }
    }

    private fun formatToolStepLabel(name: String, argsJson: String, isRussian: Boolean): String {
        val args = runCatching { JSONObject(argsJson) }.getOrNull()
        return when (name) {
            "web_search" -> {
                val q = args?.optString("query").orEmpty().take(48)
                if (isRussian) "🔍 Поиск: $q" else "🔍 Search: $q"
            }
            "web_fetch" -> {
                val url = args?.optString("url").orEmpty().take(52)
                if (isRussian) "🌐 Чтение: $url" else "🌐 Fetch: $url"
            }
            else -> if (isRussian) "⚙️ $name" else "⚙️ $name"
        }
    }
}
