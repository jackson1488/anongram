package com.anongram.app.core.i18n

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Глобальный реактивный стор настроек языка интерфейса AnonGram.
 * Сохраняет выбор пользователя в SharedPreferences и мгновенно оповещает
 * всех подписчиков через StateFlow.
 */
object LanguageSettingsStore {
    private const val PREFS_NAME = "anongram_language_settings"
    private const val KEY_LANGUAGE = "selected_language_code"

    private val _language = MutableStateFlow(AppLanguage.RU)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs.getString(KEY_LANGUAGE, AppLanguage.RU.code) ?: AppLanguage.RU.code
        _language.value = AppLanguage.fromCode(savedCode)
    }

    fun setLanguage(context: Context, newLanguage: AppLanguage) {
        _language.value = newLanguage
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, newLanguage.code)
            .apply()
    }
}
