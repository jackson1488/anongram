package com.anongram.app.core.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import com.anongram.app.core.db.schema.auth.AuthContactsSchema
import com.anongram.app.core.db.schema.chats.ChatsTableSchema
import com.anongram.app.core.db.schema.chats.MessagesTableSchema
import com.anongram.app.core.db.schema.core.CoreKvSchema
import com.anongram.app.core.db.schema.media.MediaIndexSchema
import com.anongram.app.core.db.schema.profile.ProfileSchema
import com.anongram.app.features.settings.security.data.PasscodeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * Локальная зашифрованная база данных AnonGram (`anongram.db`) на базе SQLCipher (AES-256).
 *
 * - Из коробки шифруется 256-битным ключом из Android Keystore ([KeystoreDbKeyProvider]).
 * - При установке код-пароля база перешифровывается на ключ от код-пароля через `PRAGMA rekey`.
 * - Таблицы и миграции подключаются модульно через [DbSchemaModule] из `core/db/schema/`.
 */
object AnonGramEncryptedDb {

    private const val DB_NAME = "anongram.db"
    private const val DB_VERSION = 2

    @Volatile
    private var libraryLoaded = false

    @Volatile
    private var activeHelper: SupportSQLiteOpenHelper? = null

    @Volatile
    private var activeKeyBytes: ByteArray? = null

    private val _unlockedFlow = MutableStateFlow(false)
    val unlockedFlow: StateFlow<Boolean> = _unlockedFlow.asStateFlow()

    private val schemaModules = mutableListOf<DbSchemaModule>(
        CoreKvSchema,
        ChatsTableSchema,
        MessagesTableSchema,
        MediaIndexSchema,
        ProfileSchema,
        AuthContactsSchema
    )

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
    fun getDbOrNull(): SupportSQLiteDatabase? =
        runCatching { activeHelper?.writableDatabase }.getOrNull()

    private fun deleteDbFiles(context: Context) {
        val appCtx = context.applicationContext
        val dbFile = appCtx.getDatabasePath(DB_NAME)
        runCatching { dbFile.delete() }
        listOf("-wal", "-shm", "-journal").forEach { suffix ->
            runCatching { File(dbFile.parentFile, DB_NAME + suffix).delete() }
        }
    }

    @Synchronized
    fun openWithKey(context: Context, keyBytes: ByteArray): SupportSQLiteDatabase {
        ensureNativeLoaded()
        activeHelper?.let { existing ->
            val existingDb = runCatching { existing.writableDatabase }.getOrNull()
            if (existingDb != null) return existingDb
            runCatching { existing.close() }
            activeHelper = null
        }

        val keyCopy = keyBytes.copyOf()
        activeKeyBytes = keyCopy

        val modulesSnapshot = schemaModules.toList()
        fun buildHelper(key: ByteArray): SupportSQLiteOpenHelper {
            val factory = SupportOpenHelperFactory(key)
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
                        modulesSnapshot.forEach {
                            it.onCreate(db)
                            it.onUpgrade(db, oldVersion, newVersion)
                        }
                    }
                })
                .build()
            return factory.create(config)
        }

        var helper = buildHelper(keyCopy)
        val writable = try {
            helper.writableDatabase
        } catch (t: Throwable) {
            runCatching { helper.close() }
            // Если старый файл anongram_secure.db/anongram.db зашифрован несовместимым ключом (code 26),
            // пересоздаём чистую зашифрованную anongram.db актуальным ключом без падения приложения.
            deleteDbFiles(context)
            helper = buildHelper(keyBytes.copyOf())
            helper.writableDatabase
        }
        activeHelper = helper
        // Гарантируем, что таблицы модулей, добавленных после первого создания, также существуют.
        modulesSnapshot.forEach { it.onCreate(writable) }
        _unlockedFlow.value = true
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
        _unlockedFlow.value = true
    }

    @Synchronized
    fun isUnlocked(): Boolean = activeHelper != null

    @Synchronized
    fun lock() {
        _unlockedFlow.value = false
        runCatching { activeHelper?.close() }
        activeHelper = null
        activeKeyBytes?.fill(0)
        activeKeyBytes = null
    }
}
