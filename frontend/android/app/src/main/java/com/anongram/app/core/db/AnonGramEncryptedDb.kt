package com.anongram.app.core.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.anongram.app.features.settings.security.data.PasscodeStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Локальная зашифрованная база данных AnonGram (`anongram.db`) на базе SQLCipher (AES-256).
 *
 * - Из коробки шифруется 256-битным ключом из Android Keystore ([KeystoreDbKeyProvider]).
 * - При установке код-пароля база перешифровывается на ключ от код-пароля через `PRAGMA rekey`.
 * - Таблицы и миграции подключаются модульно через [DbSchemaModule].
 */
object AnonGramEncryptedDb {

    private const val LEGACY_DB_NAME = "anongram_secure.db"
    private const val DB_NAME = "anongram.db"
    private const val DB_VERSION = 1

    @Volatile
    private var libraryLoaded = false

    @Volatile
    private var activeHelper: SupportSQLiteOpenHelper? = null

    @Volatile
    private var activeKeyBytes: ByteArray? = null

    private val schemaModules = mutableListOf<DbSchemaModule>(CoreKvDbModule)

    @Synchronized
    fun registerModule(module: DbSchemaModule) {
        if (schemaModules.none { it.moduleId == module.moduleId }) {
            schemaModules.add(module)
            // Если база уже открыта на момент ленивой регистрации, гарантируем создание таблиц модуля.
            activeHelper?.writableDatabase?.let { db ->
                module.onCreate(db)
            }
        }
    }

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

    private fun migrateLegacyDbFileIfNeeded(context: Context) {
        val appCtx = context.applicationContext
        val newDb = appCtx.getDatabasePath(DB_NAME)
        val oldDb = appCtx.getDatabasePath(LEGACY_DB_NAME)
        if (!newDb.exists() && oldDb.exists()) {
            newDb.parentFile?.mkdirs()
            if (oldDb.renameTo(newDb)) {
                listOf("-wal", "-shm", "-journal").forEach { suffix ->
                    val oldAux = File(oldDb.parentFile, LEGACY_DB_NAME + suffix)
                    val newAux = File(newDb.parentFile, DB_NAME + suffix)
                    if (oldAux.exists() && !newAux.exists()) {
                        oldAux.renameTo(newAux)
                    }
                }
            }
        }
    }

    @Synchronized
    fun ensureOpenWithoutPasscode(context: Context): SupportSQLiteDatabase? {
        activeHelper?.let { return it.writableDatabase }
        if (PasscodeStore.isPasscodeEnabled(context)) return null
        val key = KeystoreDbKeyProvider.getOrCreateKeyBytes(context)
        return try {
            openWithKey(context, key)
        } finally {
            key.fill(0)
        }
    }

    @Synchronized
    fun getDbOrNull(): SupportSQLiteDatabase? = activeHelper?.writableDatabase

    @Synchronized
    fun openWithKey(context: Context, keyBytes: ByteArray): SupportSQLiteDatabase {
        ensureNativeLoaded()
        activeHelper?.let { existing ->
            return existing.writableDatabase
        }
        migrateLegacyDbFileIfNeeded(context)

        val keyCopy = keyBytes.copyOf()
        activeKeyBytes = keyCopy

        val modulesSnapshot = schemaModules.toList()
        val factory = SupportOpenHelperFactory(keyCopy)
        val config = SupportSQLiteOpenHelper.Configuration.builder(context.applicationContext)
            .name(DB_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(DB_VERSION) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    modulesSnapshot.forEach { it.onCreate(db) }
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    modulesSnapshot.forEach { it.onUpgrade(db, oldVersion, newVersion) }
                }
            })
            .build()

        val helper = factory.create(config)
        activeHelper = helper
        val writable = helper.writableDatabase
        // Гарантируем, что таблицы модулей, добавленных после первого создания, также существуют.
        modulesSnapshot.forEach { it.onCreate(writable) }
        return writable
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
