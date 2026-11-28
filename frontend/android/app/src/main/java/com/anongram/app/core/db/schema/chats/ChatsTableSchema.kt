package com.anongram.app.core.db.schema.chats

import androidx.sqlite.db.SupportSQLiteDatabase
import com.anongram.app.core.db.DbSchemaModule

/**
 * Схема чатов, черновиков и персональных настроек каждого чата (`per_chat_settings`).
 */
object ChatsTableSchema : DbSchemaModule {
    override val moduleId: String = "chats_tables"

    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chats (
                chat_id TEXT PRIMARY KEY NOT NULL,
                title TEXT NOT NULL,
                chat_type TEXT NOT NULL DEFAULT 'DIRECT',
                fingerprint TEXT NOT NULL DEFAULT '',
                avatar_ref TEXT,
                last_msg_preview TEXT NOT NULL DEFAULT '',
                last_msg_ts INTEGER NOT NULL DEFAULT 0,
                unread_count INTEGER NOT NULL DEFAULT 0,
                is_pinned INTEGER NOT NULL DEFAULT 0,
                is_archived INTEGER NOT NULL DEFAULT 0,
                muted_until_ms INTEGER,
                payload_json TEXT NOT NULL DEFAULT '{}',
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_chats_sort ON chats (is_archived, is_pinned DESC, last_msg_ts DESC)"
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS per_chat_settings (
                chat_id TEXT PRIMARY KEY NOT NULL,
                wallpaper_id TEXT,
                custom_theme_json TEXT,
                auto_delete_sec INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS chat_drafts (
                chat_id TEXT PRIMARY KEY NOT NULL,
                draft_text TEXT NOT NULL DEFAULT '',
                reply_to_id TEXT,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
