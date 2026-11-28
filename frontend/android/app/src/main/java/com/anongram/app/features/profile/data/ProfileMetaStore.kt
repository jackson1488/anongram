package com.anongram.app.features.profile.data

import android.content.Context
import com.anongram.app.core.db.AnonGramEncryptedDb
import com.anongram.app.core.db.schema.profile.LocalUserProfile
import com.anongram.app.core.db.schema.profile.UserProfileDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Единое шифрованное хранилище метаданных профиля пользователя (`anongram.db` -> `user_profile`),
 * с автоматической миграцией старого `anongram_profile_meta` SharedPreferences без потерь.
 */
object ProfileMetaStore {

    private const val PREFS_NAME = "anongram_profile_meta"
    private val ioScope = CoroutineScope(Dispatchers.IO)

    private val _profileFlow = MutableStateFlow<LocalUserProfile?>(null)
    val profileFlow: StateFlow<LocalUserProfile?> = _profileFlow.asStateFlow()

    @Volatile
    private var appContext: Context? = null

    init {
        ioScope.launch {
            AnonGramEncryptedDb.unlockedFlow.collect { unlocked ->
                if (unlocked) {
                    appContext?.let { syncAndMigrate(it) }
                }
            }
        }
    }

    fun ensureLoaded(context: Context) {
        appContext = context.applicationContext
        if (_profileFlow.value == null) {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val dn = prefs.getString("display_name", null)
            val handle = prefs.getString("handle", null)
            val bio = prefs.getString("bio", null)
            val birthday = prefs.getString("birthday", null)
            if (dn != null || handle != null || bio != null || birthday != null) {
                _profileFlow.value = LocalUserProfile(
                    userId = "self",
                    displayName = dn.orEmpty(),
                    username = handle.orEmpty(),
                    bio = bio.orEmpty(),
                    birthday = birthday.orEmpty(),
                    avatarRef = null,
                    updatedAt = System.currentTimeMillis()
                )
            }
        }
        if (AnonGramEncryptedDb.isUnlocked()) {
            ioScope.launch { syncAndMigrate(context.applicationContext) }
        }
    }

    fun saveFields(
        context: Context,
        displayName: String? = null,
        username: String? = null,
        bio: String? = null,
        birthday: String? = null
    ) {
        ensureLoaded(context)
        val prev = _profileFlow.value ?: LocalUserProfile(
            userId = "self",
            displayName = "",
            username = "",
            bio = "",
            birthday = "",
            avatarRef = null,
            updatedAt = System.currentTimeMillis()
        )
        val next = prev.copy(
            displayName = displayName ?: prev.displayName,
            username = username ?: prev.username,
            bio = bio ?: prev.bio,
            birthday = birthday ?: prev.birthday,
            updatedAt = System.currentTimeMillis()
        )
        _profileFlow.value = next
        if (AnonGramEncryptedDb.isUnlocked()) {
            ioScope.launch { UserProfileDao.upsert(next) }
        } else {
            context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().apply {
                displayName?.let { putString("display_name", it) }
                username?.let { putString("handle", it) }
                bio?.let { putString("bio", it) }
                birthday?.let { putString("birthday", it) }
            }.apply()
        }
    }

    private fun syncAndMigrate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val fromDb = UserProfileDao.get("self")
        val legacyName = prefs.getString("display_name", null)
        val legacyHandle = prefs.getString("handle", null)
        val legacyBio = prefs.getString("bio", null)
        val legacyBday = prefs.getString("birthday", null)

        val hasLegacy = legacyName != null || legacyHandle != null || legacyBio != null || legacyBday != null
        if (fromDb == null && hasLegacy) {
            val migrated = LocalUserProfile(
                userId = "self",
                displayName = legacyName.orEmpty(),
                username = legacyHandle.orEmpty(),
                bio = legacyBio.orEmpty(),
                birthday = legacyBday.orEmpty(),
                avatarRef = null,
                updatedAt = System.currentTimeMillis()
            )
            UserProfileDao.upsert(migrated)
            prefs.edit()
                .remove("display_name")
                .remove("handle")
                .remove("bio")
                .remove("birthday")
                .apply()
            _profileFlow.value = migrated
        } else if (fromDb != null) {
            _profileFlow.value = fromDb
            if (hasLegacy) {
                prefs.edit()
                    .remove("display_name")
                    .remove("handle")
                    .remove("bio")
                    .remove("birthday")
                    .apply()
            }
        }
    }
}
