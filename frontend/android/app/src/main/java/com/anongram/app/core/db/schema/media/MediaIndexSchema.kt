package com.anongram.app.core.db.schema.media

import androidx.sqlite.db.SupportSQLiteDatabase
import com.anongram.app.core.db.DbSchemaModule

/**
 * Схема индексации зашифрованных медиафайлов по типам и чатам.
 * Сами файлы лежат в `filesDir/media_enc/<media_type>/<chat_id>/<file_id>.enc`.
 */
object MediaIndexSchema : DbSchemaModule {
    override val moduleId: String = "media_index"

    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS media_index (
                file_id TEXT PRIMARY KEY NOT NULL,
                chat_id TEXT NOT NULL,
                media_type TEXT NOT NULL,
                enc_path TEXT NOT NULL,
                mime_type TEXT NOT NULL DEFAULT 'application/octet-stream',
                size_bytes INTEGER NOT NULL DEFAULT 0,
                duration_ms INTEGER NOT NULL DEFAULT 0,
                key_nonce BLOB NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_media_chat_type ON media_index (chat_id, media_type, created_at DESC)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS idx_media_type ON media_index (media_type, created_at DESC)"
        )
    }
}
