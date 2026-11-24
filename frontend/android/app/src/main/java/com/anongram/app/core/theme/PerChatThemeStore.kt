package com.anongram.app.core.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Хранилище персональных тем для отдельных чатов (`chatId -> ThemeSpec`).
 * Позволяет менять тему конкретного разговора, не затрагивая остальные экраны приложения.
 */
object PerChatThemeStore {
    private const val PREFS_NAME = "anongram_per_chat_themes"
    private var initialized = false

    private val _themesFlow = MutableStateFlow<Map<String, ThemeSpec>>(emptyMap())
    val themesFlow: StateFlow<Map<String, ThemeSpec>> = _themesFlow.asStateFlow()

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val loaded = buildMap {
                prefs.all.forEach { (key, value) ->
                    val raw = value as? String ?: return@forEach
                    ThemeSpec.decodeOrNull(raw)?.let { spec -> put(key, spec) }
                }
            }
            _themesFlow.value = loaded
            initialized = true
        }
    }

    fun setChatTheme(context: Context, chatId: String, spec: ThemeSpec) {
        ensureInitialized(context)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(chatId, spec.encode())
            .apply()
        _themesFlow.update { it + (chatId to spec) }
    }

    fun clearChatTheme(context: Context, chatId: String) {
        ensureInitialized(context)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(chatId)
            .apply()
        _themesFlow.update { it - chatId }
    }
}
