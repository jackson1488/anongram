package com.anongram.app.core.theme

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Глобальный реактивный стор настроек размера текста сообщений (от 1 до 30 sp)
 * и имени зарегистрированного пользователя для превью и чатов.
 */
object ChatTextSettingsStore {
    private const val PREFS_NAME = "anongram_chat_text_settings"
    private const val KEY_TEXT_SIZE_SP = "message_text_size_sp"
    private const val KEY_USER_NAME = "registered_user_name"

    private val _messageTextSizeSp = MutableStateFlow(16)
    val messageTextSizeSp: StateFlow<Int> = _messageTextSizeSp.asStateFlow()

    private val _registeredUserName = MutableStateFlow("Кайрат")
    val registeredUserName: StateFlow<String> = _registeredUserName.asStateFlow()

    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedSize = prefs.getInt(KEY_TEXT_SIZE_SP, 16).coerceIn(1, 30)
        val savedName = prefs.getString(KEY_USER_NAME, null)
            ?: context.applicationContext
                .getSharedPreferences("anongram_auth", Context.MODE_PRIVATE)
                .getString("user_name", null)
            ?: "Кайрат"
        _messageTextSizeSp.value = savedSize
        _registeredUserName.value = savedName.ifBlank { "Кайрат" }
    }

    fun setMessageTextSizeSp(context: Context, sizeSp: Int) {
        val clamped = sizeSp.coerceIn(1, 30)
        _messageTextSizeSp.value = clamped
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_TEXT_SIZE_SP, clamped)
            .apply()
    }

    fun setRegisteredUserName(context: Context, name: String) {
        val clean = name.trim().ifBlank { "Кайрат" }
        _registeredUserName.value = clean
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_USER_NAME, clean)
            .apply()
    }
}
