package com.anongram.web.core.i18n

import com.anongram.web.platform.WebStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object LanguageSettingsStore {
    private const val KEY_LANGUAGE = "anongram_selected_language_code"
    private const val KEY_SHOW_TRANSLATE_BUTTON = "anongram_show_translate_button"
    private const val KEY_TRANSLATE_ENTIRE_CHATS = "anongram_translate_entire_chats"
    private const val KEY_DO_NOT_TRANSLATE_LANG = "anongram_do_not_translate_lang"

    private val _language = MutableStateFlow(AppLanguage.RU)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val _showTranslateButton = MutableStateFlow(false)
    val showTranslateButton: StateFlow<Boolean> = _showTranslateButton.asStateFlow()

    private val _translateEntireChats = MutableStateFlow(false)
    val translateEntireChats: StateFlow<Boolean> = _translateEntireChats.asStateFlow()

    private val _doNotTranslateLanguage = MutableStateFlow("Русский")
    val doNotTranslateLanguage: StateFlow<String> = _doNotTranslateLanguage.asStateFlow()

    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        initialized = true
        val savedCode = WebStorage.getString(KEY_LANGUAGE, AppLanguage.RU.code) ?: AppLanguage.RU.code
        _language.value = AppLanguage.fromCode(savedCode)
        _showTranslateButton.value = WebStorage.getBoolean(KEY_SHOW_TRANSLATE_BUTTON, false)
        _translateEntireChats.value = WebStorage.getBoolean(KEY_TRANSLATE_ENTIRE_CHATS, false)
        _doNotTranslateLanguage.value = WebStorage.getString(KEY_DO_NOT_TRANSLATE_LANG, "Русский") ?: "Русский"
    }

    fun setLanguage(newLanguage: AppLanguage) {
        _language.value = newLanguage
        WebStorage.putString(KEY_LANGUAGE, newLanguage.code)
    }

    fun setShowTranslateButton(show: Boolean) {
        _showTranslateButton.value = show
        WebStorage.putBoolean(KEY_SHOW_TRANSLATE_BUTTON, show)
    }

    fun setTranslateEntireChats(translateEntire: Boolean) {
        _translateEntireChats.value = translateEntire
        WebStorage.putBoolean(KEY_TRANSLATE_ENTIRE_CHATS, translateEntire)
    }

    fun setDoNotTranslateLanguage(lang: String) {
        _doNotTranslateLanguage.value = lang
        WebStorage.putString(KEY_DO_NOT_TRANSLATE_LANG, lang)
    }
}
