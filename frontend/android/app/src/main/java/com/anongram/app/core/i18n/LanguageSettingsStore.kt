package com.anongram.app.core.i18n

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Глобальный реактивный стор настроек языка интерфейса и перевода AnonGram.
 * Сохраняет выбор пользователя в SharedPreferences и мгновенно оповещает
 * всех подписчиков через StateFlow.
 */
object LanguageSettingsStore {
    private const val PREFS_NAME = "anongram_language_settings"
    private const val KEY_LANGUAGE = "selected_language_code"
    private const val KEY_SHOW_TRANSLATE_BUTTON = "show_translate_button"
    private const val KEY_TRANSLATE_ENTIRE_CHATS = "translate_entire_chats"
    private const val KEY_DO_NOT_TRANSLATE_LANG = "do_not_translate_lang"
    private const val KEY_CUSTOM_LANG_ACTIVE = "custom_lang_active"

    private val _language = MutableStateFlow(AppLanguage.RU)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _showTranslateButton = MutableStateFlow(false)
    val showTranslateButton: StateFlow<Boolean> = _showTranslateButton.asStateFlow()

    private val _translateEntireChats = MutableStateFlow(false)
    val translateEntireChats: StateFlow<Boolean> = _translateEntireChats.asStateFlow()

    private val _doNotTranslateLanguage = MutableStateFlow("Русский")
    val doNotTranslateLanguage: StateFlow<String> = _doNotTranslateLanguage.asStateFlow()

    private val _isCustomLanguageActive = MutableStateFlow(false)
    val isCustomLanguageActive: StateFlow<Boolean> = _isCustomLanguageActive.asStateFlow()

    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE, AppLanguage.RU.code) ?: AppLanguage.RU.code
        _language.value = AppLanguage.fromCode(savedCode)
        _showTranslateButton.value = prefs.getBoolean(KEY_SHOW_TRANSLATE_BUTTON, false)
        _translateEntireChats.value = prefs.getBoolean(KEY_TRANSLATE_ENTIRE_CHATS, false)
        _doNotTranslateLanguage.value = prefs.getString(KEY_DO_NOT_TRANSLATE_LANG, "Русский") ?: "Русский"
        _isCustomLanguageActive.value = prefs.getBoolean(KEY_CUSTOM_LANG_ACTIVE, false)
    }

    fun setLanguage(context: Context, newLanguage: AppLanguage) {
        _language.value = newLanguage
        _isCustomLanguageActive.value = false
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, newLanguage.code)
            .putBoolean(KEY_CUSTOM_LANG_ACTIVE, false)
            .apply()
    }

    fun setShowTranslateButton(context: Context, show: Boolean) {
        _showTranslateButton.value = show
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_SHOW_TRANSLATE_BUTTON, show)
            .apply()
    }

    fun setTranslateEntireChats(context: Context, translateEntire: Boolean) {
        _translateEntireChats.value = translateEntire
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_TRANSLATE_ENTIRE_CHATS, translateEntire)
            .apply()
    }

    fun setDoNotTranslateLanguage(context: Context, lang: String) {
        _doNotTranslateLanguage.value = lang
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_DO_NOT_TRANSLATE_LANG, lang)
            .apply()
    }

    fun setCustomLanguageActive(context: Context, active: Boolean) {
        _isCustomLanguageActive.value = active
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CUSTOM_LANG_ACTIVE, active)
            .apply()
    }
}
