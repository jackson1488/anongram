package com.anongram.app.core.assets

/**
 * Тип внешнего модуля, загружаемого с GitHub Releases без раздувания APK.
 */
enum class ModuleCategory {
    VOICE_WHISPER,
    VOICE_VOSK,
    TRANSLATION
}

/**
 * Описание внешнего пакета (модели речи или языкового пакета перевода).
 */
data class ModuleAssetSpec(
    val id: String,
    val category: ModuleCategory,
    val languageCode: String,
    val titleRu: String,
    val titleEn: String,
    val subtitleRu: String,
    val subtitleEn: String,
    val approxSizeMb: Int,
    val fileName: String,
    val isZipArchive: Boolean,
    val downloadUrl: String,
    val sha256: String = ""
)

/**
 * Состояние загрузки / установки конкретного внешнего модуля.
 */
sealed interface AssetDownloadState {
    data object NotInstalled : AssetDownloadState
    data class Downloading(val progress: Float, val downloadedBytes: Long, val totalBytes: Long) : AssetDownloadState
    data object Extracting : AssetDownloadState
    data class Installed(val sizeBytes: Long) : AssetDownloadState
    data class Error(val message: String) : AssetDownloadState
}

/**
 * Каталог внешних ассетов AnonGram, размещаемых на GitHub Releases.
 * При обновлении версий моделей достаточно обновить URL/хеш здесь.
 */
object ModuleAssetManifest {
    private const val ASSETS_RELEASE_BASE =
        "https://github.com/jackson1488/anongram-assets/releases/download/models-v1"

    val WHISPER_TINY_Q5 = ModuleAssetSpec(
        id = "whisper_tiny_q5",
        category = ModuleCategory.VOICE_WHISPER,
        languageCode = "multi",
        titleRu = "Whisper Tiny (q5_1)",
        titleEn = "Whisper Tiny (q5_1)",
        subtitleRu = "Быстрая мультиязычная модель • ~32 МБ",
        subtitleEn = "Fast multilingual model • ~32 MB",
        approxSizeMb = 32,
        fileName = "ggml-tiny-q5_1.bin",
        isZipArchive = false,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny-q5_1.bin"
    )

    val WHISPER_BASE_Q5 = ModuleAssetSpec(
        id = "whisper_base_q5",
        category = ModuleCategory.VOICE_WHISPER,
        languageCode = "multi",
        titleRu = "Whisper Base (q5_1)",
        titleEn = "Whisper Base (q5_1)",
        subtitleRu = "Повышенная точность и пунктуация • ~60 МБ",
        subtitleEn = "Higher accuracy & punctuation • ~60 MB",
        approxSizeMb = 60,
        fileName = "ggml-base-q5_1.bin",
        isZipArchive = false,
        downloadUrl = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-base-q5_1.bin"
    )

    val VOSK_RU = ModuleAssetSpec(
        id = "vosk_ru",
        category = ModuleCategory.VOICE_VOSK,
        languageCode = "ru",
        titleRu = "Русский (Vosk Small)",
        titleEn = "Russian (Vosk Small)",
        subtitleRu = "Быстрая потоковая модель RU • ~45 МБ",
        subtitleEn = "Fast streaming RU model • ~45 MB",
        approxSizeMb = 45,
        fileName = "vosk-model-small-ru-0.22.zip",
        isZipArchive = true,
        downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-ru-0.22.zip"
    )

    val VOSK_EN = ModuleAssetSpec(
        id = "vosk_en",
        category = ModuleCategory.VOICE_VOSK,
        languageCode = "en",
        titleRu = "English (Vosk Small)",
        titleEn = "English (Vosk Small)",
        subtitleRu = "Быстрая потоковая модель EN • ~40 МБ",
        subtitleEn = "Fast streaming EN model • ~40 MB",
        approxSizeMb = 40,
        fileName = "vosk-model-small-en-us-0.15.zip",
        isZipArchive = true,
        downloadUrl = "https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip"
    )

    val TRANSLATE_EN_RU = ModuleAssetSpec(
        id = "translate_en_ru",
        category = ModuleCategory.TRANSLATION,
        languageCode = "en-ru",
        titleRu = "Английский ↔ Русский",
        titleEn = "English ↔ Russian",
        subtitleRu = "Офлайн-пакет перевода • ~32 МБ",
        subtitleEn = "Offline translation pack • ~32 MB",
        approxSizeMb = 32,
        fileName = "anongram-translate-en-ru.zip",
        isZipArchive = true,
        downloadUrl = "$ASSETS_RELEASE_BASE/anongram-translate-en-ru.zip"
    )

    val allVoiceWhisper: List<ModuleAssetSpec> = listOf(WHISPER_TINY_Q5, WHISPER_BASE_Q5)
    val allVoiceVosk: List<ModuleAssetSpec> = listOf(VOSK_RU, VOSK_EN)
    val allTranslation: List<ModuleAssetSpec> = listOf(TRANSLATE_EN_RU)
    val all: List<ModuleAssetSpec> = allVoiceWhisper + allVoiceVosk + allTranslation

    fun findById(id: String): ModuleAssetSpec? = all.firstOrNull { it.id == id }
}
