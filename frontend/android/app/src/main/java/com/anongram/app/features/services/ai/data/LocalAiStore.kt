package com.anongram.app.features.services.ai.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.anongram.llama.LlamaNativeBridge
import com.anongram.llama.LlamaTokenListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class LocalAiState(
    val activeModelId: String = AiCatalog.presets[3].id,
    val installedModels: List<InstalledAiModel> = emptyList(),
    val downloadingModelId: String? = null,
    val downloadProgress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val downloadError: String? = null,
    val reasoningLevel: AiReasoningLevel = AiReasoningLevel.MEDIUM,
    val temperature: Float = 0.7f,
    val cpuThreads: Int = 4,
    val contextLength: Int = 2048,
    val webSearchEnabled: Boolean = false,
    val systemPromptPreset: AiSystemPromptPreset = AiSystemPromptPreset.DEFAULT,
    val customSystemPrompt: String = "",
    val sessions: List<AiChatSession> = emptyList(),
    val activeSessionId: String = "",
    val isGenerating: Boolean = false
) {
    val activeModelDisplayName: String
        get() = installedModels.firstOrNull { it.id == activeModelId }?.displayName
            ?: AiCatalog.presets.firstOrNull { it.id == activeModelId }?.displayName
            ?: activeModelId

    val activeSession: AiChatSession?
        get() = sessions.firstOrNull { it.id == activeSessionId }

    val totalInstalledBytes: Long
        get() = installedModels.sumOf { it.sizeBytes }

    fun effectiveSystemPrompt(isRussian: Boolean): String = when (systemPromptPreset) {
        AiSystemPromptPreset.DEFAULT -> if (isRussian) {
            "Ты — локальный ИИ-помощник AnonGram. Отвечай точно, понятно и уважай приватность пользователя."
        } else {
            "You are AnonGram Local AI assistant. Answer accurately, clearly, and respect user privacy."
        }
        AiSystemPromptPreset.CODER -> if (isRussian) {
            "Ты — опытный инженер-программист. Пиши чистый, безопасный и рабочий код, кратко объясняя ключевые решения."
        } else {
            "You are a senior software engineer. Write clean, safe, working code and briefly explain key decisions."
        }
        AiSystemPromptPreset.ANALYST -> if (isRussian) {
            "Ты — системный аналитик. Разбирай задачу по шагам, проверяй факты, риски и крайние случаи."
        } else {
            "You are a systems analyst. Break down problems step by step, verify facts, risks, and edge cases."
        }
        AiSystemPromptPreset.TRANSLATOR -> if (isRussian) {
            "Ты — точный переводчик. Переводи естественно, сохраняя смысл, терминологию и стиль оригинала."
        } else {
            "You are an accurate translator. Translate naturally while preserving meaning, terminology, and tone."
        }
        AiSystemPromptPreset.CONCISE -> if (isRussian) {
            "Отвечай максимально кратко и по делу, без воды и лишних вступлений."
        } else {
            "Answer as concisely and directly as possible without filler."
        }
        AiSystemPromptPreset.CUSTOM -> customSystemPrompt.trim()
    }
}

object LocalAiStore {
    private const val PREFS_NAME = "anongram_local_ai_prefs"
    private const val KEY_ACTIVE_MODEL = "active_model_id"
    private const val KEY_REASONING = "reasoning_level"
    private const val KEY_TEMP = "temperature"
    private const val KEY_THREADS = "cpu_threads"
    private const val KEY_CTX = "context_length"
    private const val KEY_WEB_SEARCH = "web_search"
    private const val KEY_PROMPT_PRESET = "prompt_preset"
    private const val KEY_CUSTOM_PROMPT = "custom_prompt"
    private const val KEY_SESSIONS_JSON = "sessions_json"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(LocalAiState())
    val state: StateFlow<LocalAiState> = _state.asStateFlow()

    @Volatile
    private var initialized = false
    private var downloadJob: Job? = null
    private var generationJob: Job? = null

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val reasoning = runCatching {
                AiReasoningLevel.valueOf(prefs.getString(KEY_REASONING, AiReasoningLevel.MEDIUM.name)!!)
            }.getOrDefault(AiReasoningLevel.MEDIUM)
            val preset = runCatching {
                AiSystemPromptPreset.valueOf(prefs.getString(KEY_PROMPT_PRESET, AiSystemPromptPreset.DEFAULT.name)!!)
            }.getOrDefault(AiSystemPromptPreset.DEFAULT)

            val savedSessions = parseSessions(prefs.getString(KEY_SESSIONS_JSON, null))
            val initialSession = savedSessions.firstOrNull() ?: AiChatSession(
                id = UUID.randomUUID().toString(),
                title = ""
            )
            val sessions = if (savedSessions.isEmpty()) listOf(initialSession) else savedSessions

            _state.update {
                it.copy(
                    activeModelId = prefs.getString(KEY_ACTIVE_MODEL, it.activeModelId) ?: it.activeModelId,
                    reasoningLevel = reasoning,
                    temperature = prefs.getFloat(KEY_TEMP, 0.7f).coerceIn(0f, 1.5f),
                    cpuThreads = prefs.getInt(KEY_THREADS, 4).coerceIn(1, 8),
                    contextLength = prefs.getInt(KEY_CTX, 2048).coerceIn(512, 8192),
                    webSearchEnabled = prefs.getBoolean(KEY_WEB_SEARCH, false),
                    systemPromptPreset = preset,
                    customSystemPrompt = prefs.getString(KEY_CUSTOM_PROMPT, "") ?: "",
                    sessions = sessions,
                    activeSessionId = initialSession.id
                )
            }
            refreshInstalledModels(context)
            initialized = true
        }
    }

    private fun modelsDir(context: Context): File =
        File(context.applicationContext.filesDir, "ai_models").apply { mkdirs() }

    fun refreshInstalledModels(context: Context) {
        val dir = modelsDir(context)
        val files = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".gguf", ignoreCase = true) }
            ?: emptyList()
        val installed = files.map { file ->
            val preset = AiCatalog.presets.firstOrNull { it.filename.equals(file.name, ignoreCase = true) }
            InstalledAiModel(
                id = preset?.id ?: "file_${file.name}",
                displayName = preset?.displayName ?: file.name.removeSuffix(".gguf"),
                filename = file.name,
                sizeBytes = file.length(),
                repoId = preset?.repoId,
                supportsThinking = preset?.supportsThinking ?: file.name.contains("qwen3", ignoreCase = true) ||
                    file.name.contains("r1", ignoreCase = true),
                supportsVision = preset?.supportsVision ?: file.name.contains("vlm", ignoreCase = true)
            )
        }.sortedBy { it.displayName }

        _state.update { curr ->
            val nextActive = if (installed.any { it.id == curr.activeModelId }) {
                curr.activeModelId
            } else {
                installed.firstOrNull()?.id ?: curr.activeModelId
            }
            curr.copy(installedModels = installed, activeModelId = nextActive)
        }
    }

    fun selectModel(context: Context, modelId: String) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_ACTIVE_MODEL, modelId).apply()
        _state.update { it.copy(activeModelId = modelId) }
    }

    fun downloadPreset(context: Context, preset: AiModelPreset) {
        startDownload(
            context = context,
            id = preset.id,
            filename = preset.filename,
            url = preset.downloadUrl
        )
    }

    fun downloadHfFile(context: Context, repoId: String, hfFile: HfModelFile) {
        val id = "hf_${repoId}_${hfFile.filename}"
        startDownload(
            context = context,
            id = id,
            filename = hfFile.filename,
            url = hfFile.downloadUrl
        )
    }

    private fun startDownload(context: Context, id: String, filename: String, url: String) {
        ensureInitialized(context)
        downloadJob?.cancel()
        val destFile = File(modelsDir(context), filename)
        _state.update {
            it.copy(
                downloadingModelId = id,
                downloadProgress = 0f,
                downloadedBytes = 0L,
                totalBytes = 0L,
                downloadError = null
            )
        }
        downloadJob = scope.launch {
            val res = HuggingFaceClient.downloadFile(url, destFile) { down, total ->
                val p = if (total > 0L) (down.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
                _state.update {
                    it.copy(downloadProgress = p, downloadedBytes = down, totalBytes = total)
                }
            }
            res.onSuccess {
                refreshInstalledModels(context)
                val matchedId = _state.value.installedModels
                    .firstOrNull { it.filename.equals(filename, ignoreCase = true) }?.id ?: id
                selectModel(context, matchedId)
                _state.update { it.copy(downloadingModelId = null, downloadProgress = 0f, downloadError = null) }
            }.onFailure { err ->
                if (err !is java.util.concurrent.CancellationException) {
                    _state.update {
                        it.copy(downloadingModelId = null, downloadProgress = 0f, downloadError = err.message ?: "Error")
                    }
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _state.update { it.copy(downloadingModelId = null, downloadProgress = 0f) }
    }

    fun deleteInstalledModel(context: Context, model: InstalledAiModel) {
        ensureInitialized(context)
        File(modelsDir(context), model.filename).delete()
        refreshInstalledModels(context)
    }

    fun importLocalGguf(context: Context, uri: Uri) {
        ensureInitialized(context)
        scope.launch(Dispatchers.IO) {
            val name = queryDisplayName(context, uri)
                ?.takeIf { it.endsWith(".gguf", ignoreCase = true) }
                ?: "imported_${System.currentTimeMillis()}.gguf"
            val dest = File(modelsDir(context), name)
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    dest.outputStream().use { out -> input.copyTo(out) }
                }
            }.onSuccess {
                refreshInstalledModels(context)
                val imported = _state.value.installedModels
                    .firstOrNull { it.filename.equals(name, ignoreCase = true) }
                if (imported != null) {
                    selectModel(context, imported.id)
                }
            }
        }
    }

    fun setReasoningLevel(context: Context, level: AiReasoningLevel) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_REASONING, level.name).apply()
        _state.update { it.copy(reasoningLevel = level) }
    }

    fun setTemperature(context: Context, temp: Float) {
        ensureInitialized(context)
        val clamped = temp.coerceIn(0f, 1.5f)
        prefs(context).edit().putFloat(KEY_TEMP, clamped).apply()
        _state.update { it.copy(temperature = clamped) }
    }

    fun setCpuThreads(context: Context, threads: Int) {
        ensureInitialized(context)
        val clamped = threads.coerceIn(1, 8)
        prefs(context).edit().putInt(KEY_THREADS, clamped).apply()
        _state.update { it.copy(cpuThreads = clamped) }
    }

    fun setContextLength(context: Context, ctxLen: Int) {
        ensureInitialized(context)
        val clamped = ctxLen.coerceIn(512, 8192)
        prefs(context).edit().putInt(KEY_CTX, clamped).apply()
        _state.update { it.copy(contextLength = clamped) }
    }

    fun setWebSearchEnabled(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        prefs(context).edit().putBoolean(KEY_WEB_SEARCH, enabled).apply()
        _state.update { it.copy(webSearchEnabled = enabled) }
    }

    fun setSystemPromptPreset(context: Context, preset: AiSystemPromptPreset) {
        ensureInitialized(context)
        prefs(context).edit().putString(KEY_PROMPT_PRESET, preset.name).apply()
        _state.update { it.copy(systemPromptPreset = preset) }
    }

    fun setCustomSystemPrompt(context: Context, text: String) {
        ensureInitialized(context)
        prefs(context).edit()
            .putString(KEY_PROMPT_PRESET, AiSystemPromptPreset.CUSTOM.name)
            .putString(KEY_CUSTOM_PROMPT, text)
            .apply()
        _state.update {
            it.copy(systemPromptPreset = AiSystemPromptPreset.CUSTOM, customSystemPrompt = text)
        }
    }

    fun newChat(context: Context) {
        ensureInitialized(context)
        val current = _state.value.activeSession
        if (current != null && current.messages.isEmpty()) return
        val fresh = AiChatSession(id = UUID.randomUUID().toString(), title = "")
        _state.update { st ->
            st.copy(sessions = listOf(fresh) + st.sessions, activeSessionId = fresh.id)
        }
        persistSessions(context)
    }

    fun selectChat(sessionId: String) {
        _state.update { it.copy(activeSessionId = sessionId) }
    }

    fun togglePinChat(context: Context, sessionId: String) {
        ensureInitialized(context)
        _state.update { st ->
            st.copy(
                sessions = st.sessions.map { s ->
                    if (s.id == sessionId) s.copy(isPinned = !s.isPinned) else s
                }
            )
        }
        persistSessions(context)
    }

    fun deleteChat(context: Context, sessionId: String) {
        ensureInitialized(context)
        _state.update { st ->
            val remaining = st.sessions.filterNot { it.id == sessionId }
            val nextList = if (remaining.isEmpty()) {
                listOf(AiChatSession(id = UUID.randomUUID().toString(), title = ""))
            } else remaining
            val nextActive = if (st.activeSessionId == sessionId) nextList.first().id else st.activeSessionId
            st.copy(sessions = nextList, activeSessionId = nextActive)
        }
        persistSessions(context)
    }

    fun sendMessage(
        context: Context,
        prompt: String,
        attachments: List<AiAttachment>,
        isRussian: Boolean,
        overrideReasoning: AiReasoningLevel? = null
    ) {
        ensureInitialized(context)
        val cleanPrompt = prompt.trim()
        if (cleanPrompt.isEmpty() && attachments.isEmpty()) return

        val activeId = _state.value.activeSessionId
        val userMsg = AiMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            content = cleanPrompt,
            attachments = attachments
        )
        val assistantMsgId = UUID.randomUUID().toString()
        val assistantPlaceholder = AiMessage(
            id = assistantMsgId,
            isUser = false,
            content = ""
        )

        _state.update { st ->
            st.copy(
                isGenerating = true,
                sessions = st.sessions.map { s ->
                    if (s.id != activeId) s
                    else {
                        val nextTitle = if (s.title.isBlank()) {
                            (cleanPrompt.ifBlank { attachments.firstOrNull()?.name ?: "AI" }).take(36)
                        } else s.title
                        s.copy(
                            title = nextTitle,
                            updatedAtMs = System.currentTimeMillis(),
                            messages = s.messages + userMsg + assistantPlaceholder
                        )
                    }
                }
            )
        }

        launchAssistantResponse(
            context = context,
            sessionId = activeId,
            assistantMsgId = assistantMsgId,
            userPrompt = cleanPrompt,
            attachments = attachments,
            isRussian = isRussian,
            reasoning = overrideReasoning ?: _state.value.reasoningLevel,
            customThinking = null
        )
    }

    fun regenerateMessage(context: Context, assistantMsgId: String, isRussian: Boolean) {
        ensureInitialized(context)
        val session = _state.value.activeSession ?: return
        val idx = session.messages.indexOfFirst { it.id == assistantMsgId }
        if (idx <= 0) return
        val prevUser = session.messages.subList(0, idx).lastOrNull { it.isUser } ?: return

        _state.update { st ->
            st.copy(
                isGenerating = true,
                sessions = st.sessions.map { s ->
                    if (s.id != session.id) s
                    else s.copy(
                        messages = s.messages.map { m ->
                            if (m.id == assistantMsgId) m.copy(content = "", thinking = "", thinkingDurationSec = 0f)
                            else m
                        }
                    )
                }
            )
        }

        launchAssistantResponse(
            context = context,
            sessionId = session.id,
            assistantMsgId = assistantMsgId,
            userPrompt = prevUser.content,
            attachments = prevUser.attachments,
            isRussian = isRussian,
            reasoning = _state.value.reasoningLevel,
            customThinking = null
        )
    }

    fun editThinkingAndRegenerate(
        context: Context,
        assistantMsgId: String,
        newThinking: String,
        isRussian: Boolean
    ) {
        ensureInitialized(context)
        val session = _state.value.activeSession ?: return
        val idx = session.messages.indexOfFirst { it.id == assistantMsgId }
        if (idx <= 0) return
        val prevUser = session.messages.subList(0, idx).lastOrNull { it.isUser } ?: return

        _state.update { st ->
            st.copy(
                isGenerating = true,
                sessions = st.sessions.map { s ->
                    if (s.id != session.id) s
                    else s.copy(
                        messages = s.messages.map { m ->
                            if (m.id == assistantMsgId) m.copy(content = "", thinking = newThinking)
                            else m
                        }
                    )
                }
            )
        }

        launchAssistantResponse(
            context = context,
            sessionId = session.id,
            assistantMsgId = assistantMsgId,
            userPrompt = prevUser.content,
            attachments = prevUser.attachments,
            isRussian = isRussian,
            reasoning = _state.value.reasoningLevel,
            customThinking = newThinking
        )
    }

    fun editUserMessageAndRegenerate(
        context: Context,
        userMsgId: String,
        newText: String,
        isRussian: Boolean
    ) {
        ensureInitialized(context)
        val session = _state.value.activeSession ?: return
        val idx = session.messages.indexOfFirst { it.id == userMsgId }
        if (idx < 0) return
        val oldMsg = session.messages[idx]
        val assistantMsgId = UUID.randomUUID().toString()
        val updatedMessages = session.messages.take(idx) +
            oldMsg.copy(content = newText.trim()) +
            AiMessage(id = assistantMsgId, isUser = false, content = "")

        _state.update { st ->
            st.copy(
                isGenerating = true,
                sessions = st.sessions.map { s ->
                    if (s.id != session.id) s else s.copy(messages = updatedMessages)
                }
            )
        }

        launchAssistantResponse(
            context = context,
            sessionId = session.id,
            assistantMsgId = assistantMsgId,
            userPrompt = newText.trim(),
            attachments = oldMsg.attachments,
            isRussian = isRussian,
            reasoning = _state.value.reasoningLevel,
            customThinking = null
        )
    }

    fun stopGeneration(context: Context) {
        LlamaNativeBridge.nativeRequestStop()
        generationJob?.cancel()
        generationJob = null
        _state.update { it.copy(isGenerating = false) }
        persistSessions(context)
    }

    private fun parseThinkingAndAnswer(raw: String): Pair<String, String> {
        val startIdx = raw.indexOf("<think>")
        if (startIdx == -1) {
            return "" to raw.trimStart()
        }
        val afterStart = raw.substring(startIdx + "<think>".length)
        val endIdx = afterStart.indexOf("</think>")
        return if (endIdx == -1) {
            afterStart.trim() to ""
        } else {
            val thought = afterStart.substring(0, endIdx).trim()
            val answer = afterStart.substring(endIdx + "</think>".length).trimStart()
            thought to answer
        }
    }

    private fun launchAssistantResponse(
        context: Context,
        sessionId: String,
        assistantMsgId: String,
        userPrompt: String,
        attachments: List<AiAttachment>,
        isRussian: Boolean,
        reasoning: AiReasoningLevel,
        customThinking: String?
    ) {
        generationJob?.cancel()
        generationJob = scope.launch {
            val st = _state.value
            val installed = st.installedModels.firstOrNull { it.id == st.activeModelId }
                ?: st.installedModels.firstOrNull()

            if (installed == null) {
                val msg = if (isRussian) {
                    "Нет скачанной GGUF-модели. Откройте «Модули» и загрузите модель."
                } else {
                    "No GGUF model installed. Open Modules to download a model."
                }
                updateAssistantMessage(sessionId, assistantMsgId) { it.copy(content = msg) }
                _state.update { it.copy(isGenerating = false) }
                persistSessions(context)
                return@launch
            }

            val modelFile = File(modelsDir(context), installed.filename)
            if (!modelFile.exists()) {
                refreshInstalledModels(context)
                val msg = if (isRussian) "Файл модели не найден: ${installed.filename}" else "Model file not found: ${installed.filename}"
                updateAssistantMessage(sessionId, assistantMsgId) { it.copy(content = msg) }
                _state.update { it.copy(isGenerating = false) }
                persistSessions(context)
                return@launch
            }

            if (!LlamaNativeBridge.isAvailable) {
                val msg = if (isRussian) "Нативный движок llama.cpp недоступен на этом устройстве." else "Native llama.cpp engine is unavailable on this device."
                updateAssistantMessage(sessionId, assistantMsgId) { it.copy(content = msg) }
                _state.update { it.copy(isGenerating = false) }
                persistSessions(context)
                return@launch
            }

            val baseSys = st.effectiveSystemPrompt(isRussian)
            val sysBuilder = StringBuilder(baseSys)
            if (installed.supportsThinking) {
                when {
                    customThinking != null -> {
                        if (sysBuilder.isNotEmpty()) sysBuilder.append("\n\n")
                        sysBuilder.append("Use the following reasoning steps to form your final answer:\n$customThinking")
                    }
                    reasoning == AiReasoningLevel.OFF -> {
                        if (sysBuilder.isNotEmpty()) sysBuilder.append("\n")
                        sysBuilder.append("/no_think")
                    }
                    reasoning == AiReasoningLevel.HIGH -> {
                        if (sysBuilder.isNotEmpty()) sysBuilder.append("\n")
                        sysBuilder.append("Think deeply and step by step inside <think>...</think> before answering.")
                    }
                    else -> Unit
                }
            } else if (customThinking != null) {
                if (sysBuilder.isNotEmpty()) sysBuilder.append("\n\n")
                sysBuilder.append("Follow these reasoning steps:\n$customThinking")
            }

            val sessionMsgs = st.sessions.firstOrNull { it.id == sessionId }?.messages.orEmpty()
            val historyUntilPrompt = sessionMsgs
                .takeWhile { it.id != assistantMsgId }
                .filter { it.content.isNotBlank() }

            val rolesList = mutableListOf<String>()
            val contentsList = mutableListOf<String>()
            if (sysBuilder.isNotEmpty()) {
                rolesList.add("system")
                contentsList.add(sysBuilder.toString())
            }
            historyUntilPrompt.takeLast(10).forEachIndexed { index, msg ->
                val isLastUser = !msg.isUser.not() && index == historyUntilPrompt.takeLast(10).lastIndex
                val text = buildString {
                    append(msg.content)
                    if (msg.isUser && msg.attachments.isNotEmpty()) {
                        msg.attachments.forEach { att ->
                            if (!att.textContent.isNullOrBlank()) {
                                append("\n\n[File: ${att.name}]\n")
                                append(att.textContent.take(2000))
                            } else if (att.isImage) {
                                append("\n[Attached image: ${att.name}]")
                            }
                        }
                    } else if (isLastUser && attachments.isNotEmpty()) {
                        attachments.forEach { att ->
                            if (!att.textContent.isNullOrBlank()) {
                                append("\n\n[File: ${att.name}]\n")
                                append(att.textContent.take(2000))
                            }
                        }
                    }
                }
                rolesList.add(if (msg.isUser) "user" else "assistant")
                contentsList.add(text)
            }
            if (rolesList.none { it == "user" }) {
                rolesList.add("user")
                contentsList.add(userPrompt)
            }

            val startNs = System.nanoTime()
            try {
                val resultCode = withContext(Dispatchers.IO) {
                    val loaded = LlamaNativeBridge.nativeLoadModel(
                        modelPath = modelFile.absolutePath,
                        nCtx = st.contextLength,
                        nThreads = st.cpuThreads
                    )
                    if (!loaded) return@withContext -100

                    val rawSb = StringBuilder()
                    var lastUiUpdateMs = 0L
                    val maxTokens = when (reasoning) {
                        AiReasoningLevel.OFF -> 512
                        AiReasoningLevel.LOW -> 640
                        AiReasoningLevel.MEDIUM -> 1024
                        AiReasoningLevel.HIGH -> 1536
                    }

                    val code = LlamaNativeBridge.nativeGenerate(
                        roles = rolesList.toTypedArray(),
                        contents = contentsList.toTypedArray(),
                        temperature = st.temperature,
                        maxTokens = maxTokens,
                        listener = LlamaTokenListener { token ->
                            rawSb.append(token)
                            val nowMs = System.currentTimeMillis()
                            if (lastUiUpdateMs == 0L || nowMs - lastUiUpdateMs >= 40L) {
                                lastUiUpdateMs = nowMs
                                val (parsedThink, parsedAnswer) = parseThinkingAndAnswer(rawSb.toString())
                                val elapsedSec = ((System.nanoTime() - startNs) / 1_000_000_000f)
                                updateAssistantMessage(sessionId, assistantMsgId) { m ->
                                    m.copy(
                                        thinking = customThinking ?: parsedThink,
                                        thinkingDurationSec = if (parsedThink.isNotEmpty() || customThinking != null) elapsedSec else 0f,
                                        content = parsedAnswer
                                    )
                                }
                            }
                        }
                    )

                    val (finalThink, finalAnswer) = parseThinkingAndAnswer(rawSb.toString())
                    val totalSec = ((System.nanoTime() - startNs) / 1_000_000_000f)
                    updateAssistantMessage(sessionId, assistantMsgId) { m ->
                        m.copy(
                            thinking = customThinking ?: finalThink,
                            thinkingDurationSec = if (finalThink.isNotEmpty() || customThinking != null) totalSec else 0f,
                            content = finalAnswer.ifBlank { m.content }
                        )
                    }
                    code
                }

                if (resultCode < 0) {
                    val errMsg = if (isRussian) {
                        "Не удалось запустить модель (${installed.displayName}, код $resultCode). Попробуйте перезагрузить модель или уменьшить длину контекста."
                    } else {
                        "Failed to run model (${installed.displayName}, code $resultCode). Try reloading or lowering context length."
                    }
                    updateAssistantMessage(sessionId, assistantMsgId) { m ->
                        if (m.content.isBlank()) m.copy(content = errMsg) else m
                    }
                }
            } finally {
                _state.update { it.copy(isGenerating = false) }
                persistSessions(context)
            }
        }
    }

    private fun updateAssistantMessage(
        sessionId: String,
        messageId: String,
        transform: (AiMessage) -> AiMessage
    ) {
        _state.update { st ->
            st.copy(
                sessions = st.sessions.map { s ->
                    if (s.id != sessionId) s
                    else s.copy(messages = s.messages.map { m -> if (m.id == messageId) transform(m) else m })
                }
            )
        }
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun queryDisplayName(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    fun readAttachmentFromUri(context: Context, uri: Uri, isImageHint: Boolean): AiAttachment {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: if (isImageHint) "image/*" else "application/octet-stream"
        val isImage = isImageHint || mime.startsWith("image/")
        val name = queryDisplayName(context, uri) ?: uri.lastPathSegment ?: "file"
        val textContent = if (!isImage && isReadableText(name, mime)) {
            runCatching {
                resolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                    val buf = CharArray(8192)
                    val read = reader.read(buf)
                    if (read > 0) String(buf, 0, read) else null
                }
            }.getOrNull()
        } else null

        return AiAttachment(
            id = UUID.randomUUID().toString(),
            name = name,
            uriString = uri.toString(),
            isImage = isImage,
            mimeType = mime,
            textContent = textContent
        )
    }

    private fun isReadableText(name: String, mime: String): Boolean {
        if (mime.startsWith("text/")) return true
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in setOf(
            "txt", "md", "json", "xml", "csv", "log", "kt", "java", "py", "js", "ts",
            "c", "cpp", "h", "hpp", "rs", "go", "sh", "yaml", "yml", "toml", "ini", "html", "css"
        )
    }

    private fun persistSessions(context: Context) {
        val arr = JSONArray()
        _state.value.sessions.take(30).forEach { s ->
            val sObj = JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("isPinned", s.isPinned)
                put("updatedAtMs", s.updatedAtMs)
                val msgsArr = JSONArray()
                s.messages.takeLast(60).forEach { m ->
                    msgsArr.put(
                        JSONObject().apply {
                            put("id", m.id)
                            put("isUser", m.isUser)
                            put("content", m.content)
                            put("thinking", m.thinking)
                            put("thinkingDurationSec", m.thinkingDurationSec.toDouble())
                            put("timestampMs", m.timestampMs)
                        }
                    )
                }
                put("messages", msgsArr)
            }
            arr.put(sObj)
        }
        prefs(context).edit().putString(KEY_SESSIONS_JSON, arr.toString()).apply()
    }

    private fun parseSessions(raw: String?): List<AiChatSession> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val sObj = arr.getJSONObject(i)
                    val msgsArr = sObj.optJSONArray("messages") ?: JSONArray()
                    val msgs = buildList {
                        for (j in 0 until msgsArr.length()) {
                            val mObj = msgsArr.getJSONObject(j)
                            add(
                                AiMessage(
                                    id = mObj.getString("id"),
                                    isUser = mObj.getBoolean("isUser"),
                                    content = mObj.optString("content", ""),
                                    thinking = mObj.optString("thinking", ""),
                                    thinkingDurationSec = mObj.optDouble("thinkingDurationSec", 0.0).toFloat(),
                                    timestampMs = mObj.optLong("timestampMs", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                    add(
                        AiChatSession(
                            id = sObj.getString("id"),
                            title = sObj.optString("title", ""),
                            isPinned = sObj.optBoolean("isPinned", false),
                            updatedAtMs = sObj.optLong("updatedAtMs", System.currentTimeMillis()),
                            messages = msgs
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
