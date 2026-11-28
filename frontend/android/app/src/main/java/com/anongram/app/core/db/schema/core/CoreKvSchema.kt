package com.anongram.app.core.db.schema.core

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.anongram.app.core.db.AnonGramEncryptedDb
import com.anongram.app.core.db.DbSchemaModule

/**
 * Системная схема `secure_kv` и legacy `chats_meta`.
 */
object CoreKvSchema : DbSchemaModule {
    override val moduleId: String = "core_kv"

    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS secure_kv (
                k TEXT PRIMARY KEY NOT NULL,
                v TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}

/**
 * Потокобезопасный доступ к зашифрованной таблице `secure_kv` в `anongram.db`.
 */
object SecureKvDao {

    fun get(key: String): String? {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return null
        return runCatching {
            db.query("SELECT v FROM secure_kv WHERE k = ? LIMIT 1", arrayOf(key)).use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull()
    }

    fun put(key: String, value: String) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            val cv = ContentValues().apply {
                put("k", key)
                put("v", value)
                put("updated_at", System.currentTimeMillis())
            }
            db.insert("secure_kv", SQLiteDatabase.CONFLICT_REPLACE, cv)
        }
    }

    fun remove(key: String) {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            db.delete("secure_kv", "k = ?", arrayOf(key))
        }
    }

    fun getByPrefix(prefix: String): Map<String, String> {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return emptyMap()
        return runCatching {
            val result = linkedMapOf<String, String>()
            db.query(
                "SELECT k, v FROM secure_kv WHERE k LIKE ?",
                arrayOf("$prefix%")
            ).use { c ->
                while (c.moveToNext()) {
                    result[c.getString(0)] = c.getString(1)
                }
            }
            result
        }.getOrDefault(emptyMap())
    }
}
