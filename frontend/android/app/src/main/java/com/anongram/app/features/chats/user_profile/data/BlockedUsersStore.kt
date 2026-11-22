package com.anongram.app.features.chats.user_profile.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object BlockedUsersStore {
    private const val PREFS_NAME = "anongram_blocked_users"
    private const val KEY_IDS = "blocked_peer_ids"

    private val _blockedIds = MutableStateFlow<Set<String>>(emptySet())
    val blockedIds: StateFlow<Set<String>> = _blockedIds.asStateFlow()

    private var initialized = false

    fun ensureInitialized(context: Context) {
        if (initialized) return
        initialized = true
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _blockedIds.value = prefs.getStringSet(KEY_IDS, emptySet())?.toSet() ?: emptySet()
    }

    fun isBlocked(peerId: String): Boolean = peerId in _blockedIds.value

    fun block(context: Context, peerId: String) {
        ensureInitialized(context)
        val next = _blockedIds.value + peerId
        _blockedIds.value = next
        save(context, next)
    }

    fun unblock(context: Context, peerId: String) {
        ensureInitialized(context)
        val next = _blockedIds.value - peerId
        _blockedIds.value = next
        save(context, next)
    }

    fun toggle(context: Context, peerId: String): Boolean {
        ensureInitialized(context)
        val willBlock = peerId !in _blockedIds.value
        if (willBlock) block(context, peerId) else unblock(context, peerId)
        return willBlock
    }

    private fun save(context: Context, ids: Set<String>) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(KEY_IDS, ids)
            .apply()
    }
}
