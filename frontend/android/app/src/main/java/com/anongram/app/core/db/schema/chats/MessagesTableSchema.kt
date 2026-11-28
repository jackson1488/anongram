package com.anongram.app.core.db.schema.chats

import androidx.sqlite.db.SupportSQLiteDatabase
import com.anongram.app.core.db.DbSchemaModule

/**
 * Схема текстовых сообщений и FTS5-индекса (отделена от тяжёлых медиафайлов).
 * Связь с медиа выполняется по `media_id` -> [MediaIndexSchema].
 */
object MessagesTableSchema : DbSchemaModule {
    override val moduleId: String = "messages_tables"

    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS messages (
                message_id TEXT PRIMARY KEY NOT NULL,
                chat_id TEXT NOT NULL,
                sender_id TEXT NOT NULL,
                body TEXT NOT NULL DEFAULT '',
                media_type TEXT,
                media_id TEXT,
                reply_to_id TEXT,
                status INTEGER NOT NULL DEFAULT 0,
                payload_json TEXT NOT NULL DEFAULT '{}',
                created_at INTEGER NOT NULL,
                edited_at INTEGER,
                is_deleted INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        // Быстрый индекс для мгновенного (1-3 мс) открытия последних 50-60 сообщений чата
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_messages_chat_time ON messages (chat_id, created_at DESC)"
        )
        // Индекс полнотекстового поиска по расшифрованной локальной БД
        db.execSQL(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS messages_fts USING fts5(
                message_id UNINDEXED,
                chat_id UNINDEXED,
                body,
                tokenize='unicode61'
            )
            """.trimIndent()
        )
    }
}
