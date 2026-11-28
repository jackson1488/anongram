package com.anongram.app.core.theme

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.anongram.app.core.db.AnonGramEncryptedDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Хранилище персональных тем для отдельных чатов (`chatId -> ThemeSpec`).
 *
 * Хранит данные в зашифрованной таблице `per_chat_settings` (`anongram.db`),
 * автоматически мигрируя старые записи из SharedPreferences без потерь.
 */
object PerChatThemeStore {
    private const val PREFS_NAME = "anongram_per_chat_themes"
    private var initialized = false
    @Volatile
    private var appContext: Context? = null

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val _themesFlow = MutableStateFlow<Map<String, ThemeSpec>>(emptyMap())
    val themesFlow: StateFlow<Map<String, ThemeSpec>> = _themesFlow.asStateFlow()

    init {
        ioScope.launch {
            AnonGramEncryptedDb.unlockedFlow.collect { unlocked ->
                if (unlocked) {
                    appContext?.let { syncAndMigrateWithDb(it) }
                }
            }
        }
    }

    fun ensureInitialized(context: Context) {
        appContext = context.applicationContext
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val fromPrefs = buildMap {
                prefs.all.forEach { (key, value) ->
                    val raw = value as? String ?: return@forEach
                    ThemeSpec.decodeOrNull(raw)?.let { spec -> put(key, spec) }
                }
            }
            if (fromPrefs.isNotEmpty()) {
                _themesFlow.value = fromPrefs
            }
            initialized = true
        }
        if (AnonGramEncryptedDb.isUnlocked()) {
            ioScope.launch { syncAndMigrateWithDb(context.applicationContext) }
        }
    }

    fun setChatTheme(context: Context, chatId: String, spec: ThemeSpec) {
        ensureInitialized(context)
        _themesFlow.update { it + (chatId to spec) }
        val encoded = spec.encode()
        if (AnonGramEncryptedDb.isUnlocked()) {
            ioScope.launch { upsertThemeToDb(chatId, encoded) }
        } else {
            context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(chatId, encoded)
                .apply()
        }
    }

    fun clearChatTheme(context: Context, chatId: String) {
        ensureInitialized(context)
        _themesFlow.update { it - chatId }
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(chatId)
            .apply()
        if (AnonGramEncryptedDb.isUnlocked()) {
            ioScope.launch {
                val db = AnonGramEncryptedDb.getDbOrNull() ?: return@launch
                runCatching {
                    db.execSQL(
                        "UPDATE per_chat_settings SET custom_theme_json = NULL, updated_at = ? WHERE chat_id = ?",
                        arrayOf(System.currentTimeMillis(), chatId)
                    )
                }
            }
        }
    }

    private fun upsertThemeToDb(chatId: String, encoded: String) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            val cv = ContentValues().apply {
                put("chat_id", chatId)
                put("custom_theme_json", encoded)
                put("updated_at", System.currentTimeMillis())
            }
            db.insert("per_chat_settings", SQLiteDatabase.CONFLICT_REPLACE, cv)
        }
    }

    private fun syncAndMigrateWithDb(context: Context) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacy = buildMap {
            prefs.all.forEach { (key, value) ->
                val raw = value as? String ?: return@forEach
                ThemeSpec.decodeOrNull(raw)?.let { spec -> put(key, spec) }
            }
        }
        if (legacy.isNotEmpty()) {
            legacy.forEach { (chatId, spec) -> upsertThemeToDb(chatId, spec.encode()) }
            prefs.edit().clear().apply()
        }
        val fromDb = runCatching {
            val map = linkedMapOf<String, ThemeSpec>()
            db.query(
                "SELECT chat_id, custom_theme_json FROM per_chat_settings WHERE custom_theme_json IS NOT NULL",
                emptyArray()
            ).use { c ->
                while (c.moveToNext()) {
                    val chatId = c.getString(0)
                    val raw = c.getString(1)
                    ThemeSpec.decodeOrNull(raw)?.let { map[chatId] = it }
                }
            }
            map
        }.getOrNull() ?: return
        _themesFlow.update { current -> fromDb + current }
    }
}
