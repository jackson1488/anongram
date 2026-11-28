package com.anongram.app.core.db.schema.profile

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anongram.app.core.db.AnonGramEncryptedDb
import com.anongram.app.core.db.DbSchemaModule

/**
 * Схема профиля текущего пользователя: имя, username, биография, дата рождения, аватар.
 */
object ProfileSchema : DbSchemaModule {
    override val moduleId: String = "user_profile"

    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profile (
                user_id TEXT PRIMARY KEY NOT NULL,
                display_name TEXT NOT NULL DEFAULT '',
                username TEXT NOT NULL DEFAULT '',
                bio TEXT NOT NULL DEFAULT '',
                birthday TEXT NOT NULL DEFAULT '',
                avatar_ref TEXT,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

data class LocalUserProfile(
    val userId: String,
    val displayName: String,
    val username: String,
    val bio: String,
    val birthday: String,
    val avatarRef: String?,
    val updatedAt: Long
)

object UserProfileDao {
    fun get(userId: String = "self"): LocalUserProfile? {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return null
        return runCatching {
            db.query(
                "SELECT user_id, display_name, username, bio, birthday, avatar_ref, updated_at FROM user_profile WHERE user_id = ? LIMIT 1",
                arrayOf(userId)
            ).use { c ->
                if (!c.moveToFirst()) return@use null
                LocalUserProfile(
                    userId = c.getString(0),
                    displayName = c.getString(1),
                    username = c.getString(2),
                    bio = c.getString(3),
                    birthday = c.getString(4),
                    avatarRef = if (c.isNull(5)) null else c.getString(5),
                    updatedAt = c.getLong(6)
                )
            }
        }.getOrNull()
    }

    fun upsert(profile: LocalUserProfile) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            val cv = ContentValues().apply {
                put("user_id", profile.userId)
                put("display_name", profile.displayName)
                put("username", profile.username)
                put("bio", profile.bio)
                put("birthday", profile.birthday)
                if (profile.avatarRef != null) put("avatar_ref", profile.avatarRef) else putNull("avatar_ref")
                put("updated_at", profile.updatedAt)
            }
            db.insert("user_profile", SQLiteDatabase.CONFLICT_REPLACE, cv)
        }
    }
}
