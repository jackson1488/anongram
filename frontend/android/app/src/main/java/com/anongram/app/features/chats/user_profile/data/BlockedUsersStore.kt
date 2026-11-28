package com.anongram.app.features.chats.user_profile.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.anongram.app.core.db.AnonGramEncryptedDb
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Хранилище заблокированных пользователей (`anongram.db` -> таблица `contacts`, `is_blocked = 1`),
 * с автоматической миграцией из SharedPreferences без потерь.
 */
object BlockedUsersStore {
    private const val PREFS_NAME = "anongram_blocked_users"
    private const val KEY_IDS = "blocked_peer_ids"

    private val ioScope = CoroutineScope(Dispatchers.IO)
    private val _blockedIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedIds: StateFlow<Set<String>> = _blockedIds.asStateFlow()

    private var initialized = false
    @Volatile
    private var appContext: Context? = null

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
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fromPrefs = prefs.getStringSet(KEY_IDS, emptySet())?.toSet() ?: emptySet()
        if (fromPrefs.isNotEmpty()) {
            _blockedIds.value = fromPrefs
        }
        if (AnonGramEncryptedDb.isUnlocked()) {
            ioScope.launch { syncAndMigrateWithDb(context.applicationContext) }
        }
    }

    fun isBlocked(peerId: String): Boolean = peerId in _blockedIds.value

    fun block(context: Context, peerId: String) {
        ensureInitialized(context)
        val next = _blockedIds.value + peerId
        _blockedIds.value = next
        if (AnonGramEncryptedDb.isUnlocked()) {
            ioScope.launch { setBlockedInDb(peerId, true) }
        } else {
            savePrefs(context, next)
        }
    }

    fun unblock(context: Context, peerId: String) {
        ensureInitialized(context)
        val next = _blockedIds.value - peerId
        _blockedIds.value = next
        if (AnonGramEncryptedDb.isUnlocked()) {
            ioScope.launch { setBlockedInDb(peerId, false) }
        } else {
            savePrefs(context, next)
        }
    }

    fun toggle(context: Context, peerId: String): Boolean {
        ensureInitialized(context)
        val willBlock = peerId !in _blockedIds.value
        if (willBlock) block(context, peerId) else unblock(context, peerId)
        return willBlock
    }

    private fun setBlockedInDb(peerId: String, blocked: Boolean) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            val now = System.currentTimeMillis()
            val updated = db.compileStatement(
                "UPDATE contacts SET is_blocked = ?, updated_at = ? WHERE user_id = ?"
            ).apply {
                bindLong(1, if (blocked) 1L else 0L)
                bindLong(2, now)
                bindString(3, peerId)
            }.executeUpdateDelete()
            if (updated == 0 && blocked) {
                val cv = ContentValues().apply {
                    put("user_id", peerId)
                    put("is_blocked", 1)
                    put("updated_at", now)
                }
                db.insert("contacts", SQLiteDatabase.CONFLICT_REPLACE, cv)
            }
        }
    }

    private fun syncAndMigrateWithDb(context: Context) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val legacy = prefs.getStringSet(KEY_IDS, emptySet())?.toSet() ?: emptySet()
        if (legacy.isNotEmpty()) {
            legacy.forEach { setBlockedInDb(it, true) }
            prefs.edit().remove(KEY_IDS).apply()
        }
        val fromDb = runCatching {
            val set = linkedSetOf<String>()
            db.query("SELECT user_id FROM contacts WHERE is_blocked = 1", emptyArray()).use { c ->
                while (c.moveToNext()) set.add(c.getString(0))
            }
            set
        }.getOrNull() ?: return
        _blockedIds.value = fromDb
    }

    private fun savePrefs(context: Context, ids: Set<String>) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_IDS, ids)
            .apply()
    }
}
