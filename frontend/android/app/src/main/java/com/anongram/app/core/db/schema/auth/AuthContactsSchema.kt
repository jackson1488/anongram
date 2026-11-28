package com.anongram.app.core.db.schema.auth

import androidx.sqlite.db.SupportSQLiteDatabase
import com.anongram.app.core.db.DbSchemaModule

/**
 * Схема сессии и контактов / чёрного списка.
 */
object AuthContactsSchema : DbSchemaModule {
    override val moduleId: String = "auth_contacts"

    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS auth_session (
                key TEXT PRIMARY KEY NOT NULL,
                value TEXT NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS contacts (
                user_id TEXT PRIMARY KEY NOT NULL,
                display_name TEXT NOT NULL DEFAULT '',
                username TEXT NOT NULL DEFAULT '',
                bio TEXT NOT NULL DEFAULT '',
                avatar_ref TEXT,
                public_key TEXT NOT NULL DEFAULT '',
                is_blocked INTEGER NOT NULL DEFAULT 0,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_contacts_blocked ON contacts (is_blocked, display_name)"
        )
    }
}
