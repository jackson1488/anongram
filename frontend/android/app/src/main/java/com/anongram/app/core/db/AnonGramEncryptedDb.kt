package com.anongram.app.core.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * Локальная зашифрованная база данных AnonGram на базе SQLCipher (AES-256).
 *
 * - Когда код-пароль установлен, ключ выводится в RAM через PBKDF2 и затирается при [lock].
 * - При установке или смене код-пароля база перешифровывается на лету через PRAGMA rekey.
 */
object AnonGramEncryptedDb {

    private const val DB_NAME = "anongram_secure.db"
    private const val DB_VERSION = 1

    @Volatile
    private var libraryLoaded = false

    @Volatile
    private var activeHelper: SupportSQLiteOpenHelper? = null

    @Volatile
    private var activeKeyBytes: ByteArray? = null

    private fun ensureNativeLoaded() {
        if (!libraryLoaded) {
            synchronized(this) {
                if (!libraryLoaded) {
                    System.loadLibrary("sqlcipher")
                    libraryLoaded = true
                }
            }
        }
    }

    @Synchronized
    fun openWithKey(context: Context, keyBytes: ByteArray): SupportSQLiteDatabase {
        ensureNativeLoaded()
        activeHelper?.let { existing ->
            return existing.writableDatabase
        }
        val keyCopy = keyBytes.copyOf()
        activeKeyBytes = keyCopy

        val factory = SupportOpenHelperFactory(keyCopy)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context.applicationContext)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(DB_VERSION) {
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
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS chats_meta (
                            chat_id TEXT PRIMARY KEY NOT NULL,
                            title TEXT NOT NULL,
                            is_muted INTEGER NOT NULL DEFAULT 0,
                            updated_at INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) = Unit
            })
            .build()

        val helper = factory.create(config)
        activeHelper = helper
        return helper.writableDatabase
    }

    @Synchronized
    fun rekey(context: Context, newKeyBytes: ByteArray) {
        ensureNativeLoaded()
        val hex = newKeyBytes.joinToString("") { "%02x".format(it) }
        val db = activeHelper?.writableDatabase ?: openWithKey(context, newKeyBytes)
        db.execSQL("PRAGMA rekey = \"x'$hex'\"")
        activeKeyBytes?.fill(0)
        activeKeyBytes = newKeyBytes.copyOf()
    }

    @Synchronized
    fun isUnlocked(): Boolean = activeHelper != null

    @Synchronized
    fun lock() {
        runCatching { activeHelper?.close() }
        activeHelper = null
        activeKeyBytes?.fill(0)
        activeKeyBytes = null
    }
}
