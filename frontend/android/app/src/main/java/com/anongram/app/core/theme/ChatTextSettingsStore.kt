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
    private const val KEY_LINK_PREVIEWS = "link_previews_enabled"
    private const val KEY_IN_APP_BROWSER = "in_app_browser_enabled"

    private val _messageTextSizeSp = MutableStateFlow(16)
    val messageTextSizeSp: StateFlow<Int> = _messageTextSizeSp.asStateFlow()

    private val _registeredUserName = MutableStateFlow("Кайрат")
    val registeredUserName: StateFlow<String> = _registeredUserName.asStateFlow()

    private val _linkPreviewsEnabled = MutableStateFlow(true)
    val linkPreviewsEnabled: StateFlow<Boolean> = _linkPreviewsEnabled.asStateFlow()

    private val _inAppBrowserEnabled = MutableStateFlow(true)
    val inAppBrowserEnabled: StateFlow<Boolean> = _inAppBrowserEnabled.asStateFlow()

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
        _linkPreviewsEnabled.value = prefs.getBoolean(KEY_LINK_PREVIEWS, true)
        _inAppBrowserEnabled.value = prefs.getBoolean(KEY_IN_APP_BROWSER, true)
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

    fun setLinkPreviewsEnabled(context: Context, enabled: Boolean) {
        _linkPreviewsEnabled.value = enabled
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_LINK_PREVIEWS, enabled)
            .apply()
    }

    fun setInAppBrowserEnabled(context: Context, enabled: Boolean) {
        _inAppBrowserEnabled.value = enabled
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_IN_APP_BROWSER, enabled)
            .apply()
    }
}
