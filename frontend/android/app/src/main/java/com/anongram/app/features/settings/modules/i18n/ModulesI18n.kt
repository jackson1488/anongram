package com.anongram.app.features.settings.modules.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore

/**
 * Словарь локализации модуля «Модули» (Настройки -> Модули: ГС, перевод, менеджер ассетов,
 * предупреждение приватности для Google, а также строки AI-расшифровки в баббле ГС).
 */
data class ModulesI18nStrings(
    val isRussian: Boolean,
    val back: String,
    val modulesTitle: String,
    val modulesMenuSubtitle: String,
    // Подмодуль ГС (voice/)
    val voiceSectionTitle: String,
    val voiceSectionHint: String,
    val recommendedBadge: String,
    val notRecommendedBadge: String,
    val engineWhisperTitle: String,
    val engineWhisperDesc: String,
    val engineVoskTitle: String,
    val engineVoskDesc: String,
    val engineSystemTitle: String,
    val engineSystemDesc: String,
    // Предупреждение приватности для Google / Системного движка
    val googleWarningTitle: String,
    val googleWarningBody: String,
    val googleWarningKeepLocal: String,
    val googleWarningAcceptAnyway: String,
    // Языки и модели
    val recognitionLanguageLabel: String,
    val langAuto: String,
    val langRu: String,
    val langEn: String,
    val systemAvailableApi33: String,
    val systemUnavailableBelowApi33: String,
    // Кнопки и статусы загрузки
    val downloadBtn: String,
    val cancelBtn: String,
    val deleteBtn: String,
    val extractingLabel: String,
    val installedLabel: (String) -> String,
    val downloadingProgress: (Int) -> String,
    val downloadErrorRetry: String,
    val activeLabel: String,
    val selectModelHint: String,
    // Подмодуль перевода (translation/)
    val translationSectionTitle: String,
    val translationSectionDesc: String,
    val comingSoonBadge: String,
    // Память модулей
    val storageSectionTitle: String,
    val storageUsedLabel: (String) -> String,
    val clearAllModulesBtn: String,
    // Баббл ГС (AI-стриминг расшифровки)
    val voiceTranscribing: String,
    val voiceCopyTranscript: String,
    val voiceRetryTranscript: String,
    val voiceModelMissingTitle: String,
    val voiceDownloadModelAction: (Int) -> String,
    val voiceOpenModulesSettings: String,
    val voiceSystemUnsupportedError: String,
    val voiceCopiedToast: String
)

private val RuModulesStrings = ModulesI18nStrings(
    isRussian = true,
    back = "Назад",
    modulesTitle = "Модули",
    modulesMenuSubtitle = "Расшифровка ГС, локальный перевод, языковые пакеты",
    voiceSectionTitle = "Расшифровка голосовых (ГС)",
    voiceSectionHint = "Модели скачиваются с GitHub Releases и работают полностью офлайн",
    recommendedBadge = "Рекомендуется",
    notRecommendedBadge = "Не рекомендуется",
    engineWhisperTitle = "Whisper (Локальный ИИ)",
    engineWhisperDesc = "Открытый код • Одна модель понимает все языки и ставит пунктуацию",
    engineVoskTitle = "Vosk (Локальный поток)",
    engineVoskDesc = "Открытый код • Быстрые лёгкие модели по отдельным языкам",
    engineSystemTitle = "Системный / Google",
    engineSystemDesc = "Закрытый исходный код • Встроенная служба устройства без загрузки моделей",
    googleWarningTitle = "Внимание: закрытый исходный код",
    googleWarningBody = "Движок Google / Системный имеет закрытый исходный код: мы не можем провести его аудит и гарантировать, что аудиозаписи не отправляются на внешние серверы.\n\nДля максимальной анонимности в AnonGram рекомендуется использовать открытые локальные движки Whisper или Vosk.",
    googleWarningKeepLocal = "Оставить локальный",
    googleWarningAcceptAnyway = "Всё равно выбрать",
    recognitionLanguageLabel = "Язык распознавания",
    langAuto = "Авто",
    langRu = "Русский",
    langEn = "English",
    systemAvailableApi33 = "Системное распознавание аудиофайлов доступно на вашем устройстве (Android 13+)",
    systemUnavailableBelowApi33 = "На Android ниже 13 системный движок не принимает готовые файлы ГС. Выберите Whisper или Vosk.",
    downloadBtn = "Скачать",
    cancelBtn = "Отмена",
    deleteBtn = "Удалить",
    extractingLabel = "Распаковка…",
    installedLabel = { size -> "Установлено • $size" },
    downloadingProgress = { pct -> "Загрузка $pct%" },
    downloadErrorRetry = "Ошибка • Повторить",
    activeLabel = "Активна",
    selectModelHint = "Нажмите для выбора модели",
    translationSectionTitle = "Локальный перевод сообщений",
    translationSectionDesc = "Офлайн-пакеты перевода (EN ↔ RU и др.) с GitHub Releases без отправки переписки на серверы — скоро.",
    comingSoonBadge = "Скоро",
    storageSectionTitle = "Память модулей",
    storageUsedLabel = { size -> "Занято на устройстве: $size" },
    clearAllModulesBtn = "Удалить все скачанные модули",
    voiceTranscribing = "Расшифровка…",
    voiceCopyTranscript = "Копировать",
    voiceRetryTranscript = "Повторить",
    voiceModelMissingTitle = "Модель речи не скачана",
    voiceDownloadModelAction = { mb -> "Скачать (~$mb МБ)" },
    voiceOpenModulesSettings = "Настройки модулей",
    voiceSystemUnsupportedError = "Системный движок не поддерживается для файлов на Android < 13. Выберите Whisper или Vosk в Настройки → Модули.",
    voiceCopiedToast = "Текст расшифровки скопирован"
)

private val EnModulesStrings = ModulesI18nStrings(
    isRussian = false,
    back = "Back",
    modulesTitle = "Modules",
    modulesMenuSubtitle = "Voice transcription, local translation, language packs",
    voiceSectionTitle = "Voice Message Transcription",
    voiceSectionHint = "Models are downloaded from GitHub Releases and run 100% offline",
    recommendedBadge = "Recommended",
    notRecommendedBadge = "Not recommended",
    engineWhisperTitle = "Whisper (Local AI)",
    engineWhisperDesc = "Open-source • Single multilingual model with punctuation",
    engineVoskTitle = "Vosk (Local Stream)",
    engineVoskDesc = "Open-source • Fast lightweight per-language models",
    engineSystemTitle = "System / Google",
    engineSystemDesc = "Closed-source • Built-in device service without extra downloads",
    googleWarningTitle = "Privacy warning: closed source",
    googleWarningBody = "Google / System Speech is closed-source: we cannot audit its code or guarantee that your voice audio is not sent to external servers.\n\nFor maximum anonymity in AnonGram, we recommend open-source offline engines Whisper or Vosk.",
    googleWarningKeepLocal = "Keep local",
    googleWarningAcceptAnyway = "Use anyway",
    recognitionLanguageLabel = "Recognition language",
    langAuto = "Auto",
    langRu = "Russian",
    langEn = "English",
    systemAvailableApi33 = "System audio file recognition is available on your device (Android 13+)",
    systemUnavailableBelowApi33 = "On Android below 13, the system engine cannot transcribe saved voice files. Please choose Whisper or Vosk.",
    downloadBtn = "Download",
    cancelBtn = "Cancel",
    deleteBtn = "Delete",
    extractingLabel = "Extracting…",
    installedLabel = { size -> "Installed • $size" },
    downloadingProgress = { pct -> "Downloading $pct%" },
    downloadErrorRetry = "Error • Retry",
    activeLabel = "Active",
    selectModelHint = "Tap to select model",
    translationSectionTitle = "Local Message Translation",
    translationSectionDesc = "Offline translation packs (EN ↔ RU, etc.) from GitHub Releases without sending chats to servers — coming soon.",
    comingSoonBadge = "Soon",
    storageSectionTitle = "Modules Storage",
    storageUsedLabel = { size -> "Used on device: $size" },
    clearAllModulesBtn = "Delete all downloaded modules",
    voiceTranscribing = "Transcribing…",
    voiceCopyTranscript = "Copy",
    voiceRetryTranscript = "Retry",
    voiceModelMissingTitle = "Speech model not downloaded",
    voiceDownloadModelAction = { mb -> "Download (~$mb MB)" },
    voiceOpenModulesSettings = "Module settings",
    voiceSystemUnsupportedError = "System speech engine is not supported for files on Android < 13. Select Whisper or Vosk in Settings → Modules.",
    voiceCopiedToast = "Transcript copied"
)

object ModulesI18n {
    val current: ModulesI18nStrings
        @Composable
        get() {
            val lang = LanguageSettingsStore.language.collectAsState().value
            return if (lang == AppLanguage.RU) RuModulesStrings else EnModulesStrings
        }

    fun getSync(isRussian: Boolean): ModulesI18nStrings =
        if (isRussian) RuModulesStrings else EnModulesStrings
}
