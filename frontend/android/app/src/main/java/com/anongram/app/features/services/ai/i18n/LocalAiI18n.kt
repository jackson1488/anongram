package com.anongram.app.features.services.ai.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.features.services.ai.data.AiReasoningLevel
import com.anongram.app.features.services.ai.data.AiSystemPromptPreset

data class LocalAiI18nStrings(
    val isRussian: Boolean,
    val title: String,
    val newChat: String,
    val searchChatsHint: String,
    val pinnedSection: String,
    val allChatsSection: String,
    val noChatsYet: String,
    val aiSettingsLink: String,
    val modelSelectorFallback: String,
    val switchModelTitle: String,
    val installedModelsHeader: String,
    val noModelInstalledHint: String,
    val openModulesToDownload: String,
    val reasoningLevelTitle: String,
    val reasoningOff: String,
    val reasoningLow: String,
    val reasoningMedium: String,
    val reasoningHigh: String,
    val temperatureTitle: String,
    val tempPrecise: String,
    val tempBalanced: String,
    val tempCreative: String,
    val systemPromptTitle: String,
    val systemPromptPlaceholder: String,
    val promptPresetDefault: String,
    val promptPresetCoder: String,
    val promptPresetAnalyst: String,
    val promptPresetTranslator: String,
    val promptPresetConcise: String,
    val promptPresetCustom: String,
    val webSearchTooltip: String,
    val webSearchEnabledBadge: String,
    val emptyHeroTitle: String,
    val emptyHeroSubtitle: String,
    val quickPrompt1: String,
    val quickPrompt2: String,
    val quickPrompt3: String,
    val inputPlaceholder: String,
    val attachCamera: String,
    val attachPhoto: String,
    val attachFiles: String,
    val attachWebSearch: String,
    val attachThinkDeeper: String,
    val thoughtBlockTitle: (Float) -> String,
    val thoughtEditBtn: String,
    val thoughtApplyRegen: String,
    val thoughtCancel: String,
    val copyBtn: String,
    val copiedToast: String,
    val regenerateBtn: String,
    val shareBtn: String,
    val editMessageBtn: String,
    val pinChat: String,
    val unpinChat: String,
    val deleteChat: String,
    val generatingLabel: String,
    val demoEngineNotice: (String) -> String,
    val aiSettingsTitle: String,
    val modulesTitle: String,
    val cpuThreadsTitle: String,
    val contextLengthTitle: String,
    val gateTitle: String,
    val gateSubtitle: String,
    val gateDownloadBtn: String,
    val gateImportBtn: String,
    val gateOpenModulesBtn: String,
    val searchingWebStatus: (String) -> String,
    val readingSourcesStatus: (Int) -> String,
    val sourcesLabel: String,
    val resetPromptBtn: String,
    val maxTokensTitle: String,
    val webSearchSettingsHint: String,
    val searchBlockTitle: (Int, Boolean) -> String
) {
    fun reasoningLabel(level: AiReasoningLevel): String = when (level) {
        AiReasoningLevel.OFF -> reasoningOff
        AiReasoningLevel.LOW -> reasoningLow
        AiReasoningLevel.MEDIUM -> reasoningMedium
        AiReasoningLevel.HIGH -> reasoningHigh
    }

    fun promptPresetLabel(preset: AiSystemPromptPreset): String = when (preset) {
        AiSystemPromptPreset.DEFAULT -> promptPresetDefault
        AiSystemPromptPreset.CODER -> promptPresetCoder
        AiSystemPromptPreset.ANALYST -> promptPresetAnalyst
        AiSystemPromptPreset.TRANSLATOR -> promptPresetTranslator
        AiSystemPromptPreset.CONCISE -> promptPresetConcise
        AiSystemPromptPreset.CUSTOM -> promptPresetCustom
    }
}

object LocalAiI18n {
    private val ru = LocalAiI18nStrings(
        isRussian = true,
        title = "Локальный ИИ",
        newChat = "Новый чат",
        searchChatsHint = "Поиск по чатам ИИ...",
        pinnedSection = "Закреплённые",
        allChatsSection = "История чатов",
        noChatsYet = "Сохранённых диалогов пока нет",
        aiSettingsLink = "Модели и настройки (llama.cpp)",
        modelSelectorFallback = "Выбрать модель",
        switchModelTitle = "Модели на устройстве",
        installedModelsHeader = "Доступные модели",
        noModelInstalledHint = "Модель .gguf ещё не загружена — можно скачать с Hugging Face в разделе «Модули».",
        openModulesToDownload = "Скачать модель в Модулях",
        reasoningLevelTitle = "Уровень рассуждений",
        reasoningOff = "Выкл",
        reasoningLow = "Низкий",
        reasoningMedium = "Средний",
        reasoningHigh = "Высокий",
        temperatureTitle = "Температура",
        tempPrecise = "Точный",
        tempBalanced = "Баланс",
        tempCreative = "Креатив",
        systemPromptTitle = "Системный промпт",
        systemPromptPlaceholder = "Задайте роль, стиль и правила для ИИ...",
        promptPresetDefault = "Универсал",
        promptPresetCoder = "Программист",
        promptPresetAnalyst = "Аналитик",
        promptPresetTranslator = "Переводчик",
        promptPresetConcise = "Кратко",
        promptPresetCustom = "Свой",
        webSearchTooltip = "Веб-поиск",
        webSearchEnabledBadge = "Веб-поиск вкл",
        emptyHeroTitle = "Локальный ИИ AnonGram",
        emptyHeroSubtitle = "Приватные ответы прямо на устройстве",
        quickPrompt1 = "Объясни сложную тему простыми словами",
        quickPrompt2 = "Проверь и оптимизируй код",
        quickPrompt3 = "Сделай краткую выжимку текста",
        inputPlaceholder = "Спросите ИИ...",
        attachCamera = "Камера",
        attachPhoto = "Фото",
        attachFiles = "Файлы",
        attachWebSearch = "Веб-поиск",
        attachThinkDeeper = "Размышлять глубже",
        thoughtBlockTitle = { sec -> if (sec > 0f) "Ход мыслей · ${"%.1f".format(sec)} с" else "Ход мыслей" },
        thoughtEditBtn = "Редактировать мысли",
        thoughtApplyRegen = "Перегенерировать с этими мыслями",
        thoughtCancel = "Отмена",
        copyBtn = "Копировать",
        copiedToast = "Скопировано",
        regenerateBtn = "Заново",
        shareBtn = "Поделиться",
        editMessageBtn = "Изменить",
        pinChat = "Закрепить",
        unpinChat = "Открепить",
        deleteChat = "Удалить",
        generatingLabel = "Генерирую ответ...",
        demoEngineNotice = { modelName ->
            "Модель «$modelName» готова в интерфейсе. Нативный инференс llama.cpp подключается в следующем шаге — сейчас показан предпросмотр мыслей, параметров, системного промпта и чтения прикреплённых файлов."
        },
        aiSettingsTitle = "Настройки ИИ",
        modulesTitle = "Модели (Модули)",
        cpuThreadsTitle = "Потоки CPU",
        contextLengthTitle = "Длина контекста",
        gateTitle = "Скачайте модель для работы ИИ",
        gateSubtitle = "Без загруженной GGUF-модели локальный ИИ не может отвечать. Выберите модель ниже или импортируйте свою.",
        gateDownloadBtn = "Скачать",
        gateImportBtn = "Импортировать .gguf",
        gateOpenModulesBtn = "Открыть Модули",
        searchingWebStatus = { q -> "Ищу в интернете: ${q.take(36)}${if (q.length > 36) "…" else ""}" },
        readingSourcesStatus = { n -> "Найдено источников: $n · осмысление..." },
        sourcesLabel = "Источники",
        resetPromptBtn = "Сбросить",
        maxTokensTitle = "Макс. длина ответа",
        webSearchSettingsHint = "Включено по умолчанию: ИИ сам решает, когда искать в интернете и открывать сайты.",
        searchBlockTitle = { count, active ->
            when {
                active -> "Поиск в интернете…"
                count > 0 -> "Поиск · $count истор."
                else -> "Поиск"
            }
        }
    )

    private val en = LocalAiI18nStrings(
        isRussian = false,
        title = "Local AI",
        newChat = "New chat",
        searchChatsHint = "Search AI chats...",
        pinnedSection = "Pinned",
        allChatsSection = "Chat history",
        noChatsYet = "No saved conversations yet",
        aiSettingsLink = "Models & settings (llama.cpp)",
        modelSelectorFallback = "Select model",
        switchModelTitle = "On-device models",
        installedModelsHeader = "Available models",
        noModelInstalledHint = "No .gguf model downloaded yet — download from Hugging Face in Modules.",
        openModulesToDownload = "Download model in Modules",
        reasoningLevelTitle = "Reasoning level",
        reasoningOff = "Off",
        reasoningLow = "Low",
        reasoningMedium = "Medium",
        reasoningHigh = "High",
        temperatureTitle = "Temperature",
        tempPrecise = "Precise",
        tempBalanced = "Balanced",
        tempCreative = "Creative",
        systemPromptTitle = "System prompt",
        systemPromptPlaceholder = "Define role, style and rules for AI...",
        promptPresetDefault = "Assistant",
        promptPresetCoder = "Coder",
        promptPresetAnalyst = "Analyst",
        promptPresetTranslator = "Translator",
        promptPresetConcise = "Concise",
        promptPresetCustom = "Custom",
        webSearchTooltip = "Web search",
        webSearchEnabledBadge = "Web search on",
        emptyHeroTitle = "AnonGram Local AI",
        emptyHeroSubtitle = "Private on-device intelligence",
        quickPrompt1 = "Explain a complex topic simply",
        quickPrompt2 = "Review and optimize code",
        quickPrompt3 = "Summarize key points",
        inputPlaceholder = "Ask AI...",
        attachCamera = "Camera",
        attachPhoto = "Photo",
        attachFiles = "Files",
        attachWebSearch = "Web search",
        attachThinkDeeper = "Think deeper",
        thoughtBlockTitle = { sec -> if (sec > 0f) "Reasoning · ${"%.1f".format(sec)}s" else "Reasoning" },
        thoughtEditBtn = "Edit thoughts",
        thoughtApplyRegen = "Regenerate with edited thoughts",
        thoughtCancel = "Cancel",
        copyBtn = "Copy",
        copiedToast = "Copied",
        regenerateBtn = "Regenerate",
        shareBtn = "Share",
        editMessageBtn = "Edit",
        pinChat = "Pin",
        unpinChat = "Unpin",
        deleteChat = "Delete",
        generatingLabel = "Generating...",
        demoEngineNotice = { modelName ->
            "Model \"$modelName\" is active in UI. Native llama.cpp inference hooks in next — currently previewing reasoning blocks, parameters, system prompt, and attached file context."
        },
        aiSettingsTitle = "AI Settings",
        modulesTitle = "Models (Modules)",
        cpuThreadsTitle = "CPU threads",
        contextLengthTitle = "Context length",
        gateTitle = "Download a model to start",
        gateSubtitle = "Local AI requires at least one GGUF model on device. Download one below or import your own.",
        gateDownloadBtn = "Download",
        gateImportBtn = "Import .gguf",
        gateOpenModulesBtn = "Open Modules",
        searchingWebStatus = { q -> "Searching web: ${q.take(36)}${if (q.length > 36) "…" else ""}" },
        readingSourcesStatus = { n -> "Found $n sources · thinking..." },
        sourcesLabel = "Sources",
        resetPromptBtn = "Reset",
        maxTokensTitle = "Max response length",
        webSearchSettingsHint = "Enabled by default: AI automatically decides when to search the web and read pages.",
        searchBlockTitle = { count, active ->
            when {
                active -> "Searching web…"
                count > 0 -> "Search · $count sources"
                else -> "Search"
            }
        }
    )

    val current: LocalAiI18nStrings
        @Composable
        get() {
            val lang by LanguageSettingsStore.language.collectAsState()
            return if (lang == AppLanguage.RU) ru else en
        }
}
