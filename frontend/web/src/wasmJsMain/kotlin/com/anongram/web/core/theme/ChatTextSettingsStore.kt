package com.anongram.web.core.theme

import com.anongram.web.platform.WebStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Глобальный реактивный стор настроек размера текста сообщений (от 1 до 30 sp)
 * и имени зарегистрированного пользователя для превью и чатов.
 */
object ChatTextSettingsStore {
    private const val KEY_TEXT_SIZE_SP = "anongram_message_text_size_sp"
    private const val KEY_USER_NAME = "anongram_registered_user_name"

    private val _messageTextSizeSp = MutableStateFlow(16)
    val messageTextSizeSp: StateFlow<Int> = _messageTextSizeSp.asStateFlow()

    private val _registeredUserName = MutableStateFlow("Кайрат")
    val registeredUserName: StateFlow<String> = _registeredUserName.asStateFlow()

    private var initialized = false

    fun ensureInitialized() {
        if (initialized) return
        initialized = true
        val savedSize = WebStorage.getString(KEY_TEXT_SIZE_SP)?.toIntOrNull()?.coerceIn(1, 30) ?: 16
        val savedName = WebStorage.getString(KEY_USER_NAME)
            ?: WebStorage.getString("anongram_auth_nickname")
            ?: "Кайрат"
        _messageTextSizeSp.value = savedSize
        _registeredUserName.value = savedName.ifBlank { "Кайрат" }
    }

    fun setMessageTextSizeSp(sizeSp: Int) {
        val clamped = sizeSp.coerceIn(1, 30)
        _messageTextSizeSp.value = clamped
        WebStorage.putString(KEY_TEXT_SIZE_SP, clamped.toString())
    }

    fun setRegisteredUserName(name: String) {
        val clean = name.trim().ifBlank { "Кайрат" }
        _registeredUserName.value = clean
        WebStorage.putString(KEY_USER_NAME, clean)
    }
}
