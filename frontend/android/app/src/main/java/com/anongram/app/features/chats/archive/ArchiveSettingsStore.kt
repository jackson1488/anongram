package com.anongram.app.features.chats.archive

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ArchiveSettingsState(
    val keepChatsArchived: Boolean = false,
    val autoArchiveUnknown: Boolean = false
)

/**
 * Реактивное хранилище настроек архива чатов (сохраняется в SharedPreferences).
 */
object ArchiveSettingsStore {
    private const val PREFS_NAME = "anongram_archive_settings"
    private const val KEY_KEEP_ARCHIVED = "key_keep_chats_archived"
    private const val KEY_AUTO_ARCHIVE = "key_auto_archive_unknown"

    private val _state = MutableStateFlow(ArchiveSettingsState())
    val state: StateFlow<ArchiveSettingsState> = _state.asStateFlow()

    @Volatile
    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            _state.value = ArchiveSettingsState(
                keepChatsArchived = prefs.getBoolean(KEY_KEEP_ARCHIVED, false),
                autoArchiveUnknown = prefs.getBoolean(KEY_AUTO_ARCHIVE, false)
            )
            initialized = true
        }
    }

    fun setKeepChatsArchived(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_KEEP_ARCHIVED, enabled)
            .apply()
        _state.update { it.copy(keepChatsArchived = enabled) }
    }

    fun setAutoArchiveUnknown(context: Context, enabled: Boolean) {
        ensureInitialized(context)
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_AUTO_ARCHIVE, enabled)
            .apply()
        _state.update { it.copy(autoArchiveUnknown = enabled) }
    }
}
