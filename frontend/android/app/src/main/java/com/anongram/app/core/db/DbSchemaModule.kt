package com.anongram.app.core.db

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Контракт модульной схемы для локальной зашифрованной базы `anongram.db`.
 *
 * Каждый функциональный модуль (чаты, сообщения, медиа-индекс, профиль, авторизация, настройки)
 * описывает только свои таблицы, индексы и миграции в `core/db/schema/<module>/`.
 */
interface DbSchemaModule {
    val moduleId: String
    fun onCreate(db: SupportSQLiteDatabase)
    fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
}
