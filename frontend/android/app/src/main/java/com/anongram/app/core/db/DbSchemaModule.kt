package com.anongram.app.core.db

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Контракт модульной схемы для локальной зашифрованной базы `anongram.db`.
 *
 * Каждый функциональный модуль (чаты, профиль, медиа-индекс, авторизация, настройки)
 * описывает только свои таблицы, индексы и миграции, не раздувая единый класс БД.
 */
interface DbSchemaModule {
    val moduleId: String
    fun onCreate(db: SupportSQLiteDatabase)
    fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}

/**
 * Базовый системный модуль для `secure_kv` и обратной совместимости `chats_meta`.
 */
object CoreKvDbModule : DbSchemaModule {
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
}
