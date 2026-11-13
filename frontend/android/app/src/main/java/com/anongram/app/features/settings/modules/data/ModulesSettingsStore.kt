package com.anongram.app.features.settings.modules.data

import android.content.Context
import com.anongram.app.core.assets.ModuleAssetManifest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Движок расшифровки голосовых сообщений (ГС).
 */
enum class VoiceSttEngine(val id: String) {
    WHISPER("whisper"),
    VOSK("vosk"),
    SYSTEM_GOOGLE("system_google");

    companion object {
        fun fromId(id: String?): VoiceSttEngine =
            entries.firstOrNull { it.id == id } ?: WHISPER
    }
}

data class ModulesSettingsState(
    val voiceEngine: VoiceSttEngine = VoiceSttEngine.WHISPER,
    val whisperModelId: String = ModuleAssetManifest.WHISPER_TINY_Q5.id,
    val whisperLanguage: String = "auto",
    val voskAssetId: String = ModuleAssetManifest.VOSK_RU.id,
    val systemLanguage: String = "ru-RU",
    // Настройки локального перевода сообщений
    val showTranslateButton: Boolean = true,
    val autoTranslateIncoming: Boolean = false,
    val targetTranslateLanguage: String = "auto"
)

/**
 * Хранилище настроек раздела «Модули» (`SharedPreferences` + `StateFlow`).
 */
object ModulesSettingsStore {
    private const val PREFS_NAME = "anongram_modules_settings"
    private const val KEY_VOICE_ENGINE = "key_voice_engine"
    private const val KEY_WHISPER_MODEL_ID = "key_whisper_model_id"
    private const val KEY_WHISPER_LANG = "key_whisper_lang"
    private const val KEY_VOSK_ASSET_ID = "key_vosk_asset_id"
    private const val KEY_SYSTEM_LANG = "key_system_lang"
    private const val KEY_SHOW_TRANSLATE_BUTTON = "key_show_translate_button"
    private const val KEY_AUTO_TRANSLATE_INCOMING = "key_auto_translate_incoming"
    private const val KEY_TARGET_TRANSLATE_LANG = "key_target_translate_lang"

    private val _state = MutableStateFlow(ModulesSettingsState())
    val state: StateFlow<ModulesSettingsState> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _state.value = ModulesSettingsState(
                voiceEngine = VoiceSttEngine.fromId(prefs.getString(KEY_VOICE_ENGINE, VoiceSttEngine.WHISPER.id)),
                whisperModelId = prefs.getString(KEY_WHISPER_MODEL_ID, ModuleAssetManifest.WHISPER_TINY_Q5.id)
                    ?: ModuleAssetManifest.WHISPER_TINY_Q5.id,
                whisperLanguage = prefs.getString(KEY_WHISPER_LANG, "auto") ?: "auto",
                voskAssetId = prefs.getString(KEY_VOSK_ASSET_ID, ModuleAssetManifest.VOSK_RU.id)
                    ?: ModuleAssetManifest.VOSK_RU.id,
                systemLanguage = prefs.getString(KEY_SYSTEM_LANG, "ru-RU") ?: "ru-RU",
                showTranslateButton = prefs.getBoolean(KEY_SHOW_TRANSLATE_BUTTON, true),
                autoTranslateIncoming = prefs.getBoolean(KEY_AUTO_TRANSLATE_INCOMING, false),
                targetTranslateLanguage = prefs.getString(KEY_TARGET_TRANSLATE_LANG, "auto") ?: "auto"
            )
            initialized = true
        }
    }

    fun setVoiceEngine(context: Context, engine: VoiceSttEngine) {
        ensureInitialized(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_VOICE_ENGINE, engine.id).apply()
        _state.value = _state.value.copy(voiceEngine = engine)
    }

    fun setWhisperModelId(context: Context, modelId: String) {
        ensureInitialized(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_WHISPER_MODEL_ID, modelId).apply()
        _state.value = _state.value.copy(whisperModelId = modelId)
    }

    fun setWhisperLanguage(context: Context, lang: String) {
        ensureInitialized(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_WHISPER_LANG, lang).apply()
        _state.value = _state.value.copy(whisperLanguage = lang)
    }

    fun setVoskAssetId(context: Context, assetId: String) {
        ensureInitialized(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_VOSK_ASSET_ID, assetId).apply()
        _state.value = _state.value.copy(voskAssetId = assetId)
    }

    fun setSystemLanguage(context: Context, langTag: String) {
        ensureInitialized(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_SYSTEM_LANG, langTag).apply()
        _state.value = _state.value.copy(systemLanguage = langTag)
    }

    fun setShowTranslateButton(context: Context, show: Boolean) {
        ensureInitialized(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SHOW_TRANSLATE_BUTTON, show).apply()
        _state.value = _state.value.copy(showTranslateButton = show)
    }

    fun setAutoTranslateIncoming(context: Context, auto: Boolean) {
        ensureInitialized(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_TRANSLATE_INCOMING, auto).apply()
        _state.value = _state.value.copy(autoTranslateIncoming = auto)
    }

    fun setTargetTranslateLanguage(context: Context, langCode: String) {
        ensureInitialized(context)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TARGET_TRANSLATE_LANG, langCode).apply()
        _state.value = _state.value.copy(targetTranslateLanguage = langCode)
    }
}
