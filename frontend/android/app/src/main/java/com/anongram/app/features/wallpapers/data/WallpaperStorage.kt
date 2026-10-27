package com.anongram.app.features.wallpapers.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.anongram.app.features.wallpapers.model.Wallpaper
import com.anongram.app.features.wallpapers.model.WallpaperSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Локальное хранилище обоев: файлы пользовательских фото + SQLite база данных.
 */
class WallpaperStorage(
    private val context: Context,
    private val metadataCleaner: MetadataCleaner = SafeMetadataCleaner()
) {
    private val dbHelper = DatabaseHelper(context)
    private val wallpapersDir: File by lazy {
        File(context.filesDir, "wallpapers").apply { mkdirs() }
    }

    /**
     * Получить запись обоев для чата [chatId] (или общих, если chatId == null).
     */
    suspend fun getWallpaper(chatId: String?): Wallpaper? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val selection: String
        val selectionArgs: Array<String>?
        if (chatId == null) {
            selection = "chat_id IS NULL"
            selectionArgs = null
        } else {
            selection = "chat_id = ?"
            selectionArgs = arrayOf(chatId)
        }

        db.query(
            "wallpapers",
            null,
            selection,
            selectionArgs,
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                cursorToWallpaper(cursor)
            } else {
                null
            }
        }
    }

    /**
     * Получить все сохранённые записи обоев.
     */
    suspend fun getAll(): List<Wallpaper> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Wallpaper>()
        val db = dbHelper.readableDatabase
        db.query("wallpapers", null, null, null, null, null, null).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToWallpaper(cursor))
            }
        }
        list
    }

    /**
     * Сохранить или обновить обои.
     */
    suspend fun saveWallpaper(wallpaper: Wallpaper): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", wallpaper.id)
            put("chat_id", wallpaper.chatId)
            when (val src = wallpaper.source) {
                is WallpaperSource.Theme -> {
                    put("source_type", "theme")
                    put("source_value", src.themeId)
                }
                is WallpaperSource.Photo -> {
                    put("source_type", "photo")
                    put("source_value", src.path)
                }
                is WallpaperSource.None -> {
                    put("source_type", "none")
                    putNull("source_value")
                }
            }
            put("dim", wallpaper.dim)
            put("blur", if (wallpaper.blur) 1 else 0)
            put("motion", if (wallpaper.motion) 1 else 0)
        }

        db.insertWithOnConflict("wallpapers", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Удалить персональные обои чата.
     */
    suspend fun deleteForChat(chatId: String): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("wallpapers", "chat_id = ?", arrayOf(chatId))
    }

    /**
     * Сохранить фото в приватную папку приложения с очисткой метаданных.
     * @return Абсолютный путь к очищенному сохранённому файлу.
     */
    suspend fun importPhoto(inputStream: InputStream): String = withContext(Dispatchers.IO) {
        val tempFile = File(wallpapersDir, "temp_${UUID.randomUUID()}.jpg")
        val finalFile = File(wallpapersDir, "wp_${UUID.randomUUID()}.jpg")

        tempFile.outputStream().use { out ->
            inputStream.copyTo(out)
        }

        // Очищаем метаданные перед финальным сохранением
        metadataCleaner.clean(tempFile, finalFile)
        tempFile.delete()

        finalFile.absolutePath
    }

    private fun cursorToWallpaper(cursor: Cursor): Wallpaper {
        val id = cursor.getString(cursor.getColumnIndexOrThrow("id"))
        val chatId = if (cursor.isNull(cursor.getColumnIndexOrThrow("chat_id"))) {
            null
        } else {
            cursor.getString(cursor.getColumnIndexOrThrow("chat_id"))
        }
        val sourceType = cursor.getString(cursor.getColumnIndexOrThrow("source_type"))
        val sourceValue = if (cursor.isNull(cursor.getColumnIndexOrThrow("source_value"))) {
            null
        } else {
            cursor.getString(cursor.getColumnIndexOrThrow("source_value"))
        }
        val dim = cursor.getInt(cursor.getColumnIndexOrThrow("dim"))
        val blur = cursor.getInt(cursor.getColumnIndexOrThrow("blur")) == 1
        val motion = cursor.getInt(cursor.getColumnIndexOrThrow("motion")) == 1

        val source = when (sourceType) {
            "theme" -> WallpaperSource.Theme(sourceValue ?: "crypto_mesh")
            "photo" -> WallpaperSource.Photo(sourceValue ?: "")
            "none" -> WallpaperSource.None
            else -> WallpaperSource.Theme("crypto_mesh")
        }

        return Wallpaper(
            id = id,
            chatId = chatId,
            source = source,
            dim = dim,
            blur = blur,
            motion = motion
        )
    }

    private class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "wallpapers.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS wallpapers (
                    id TEXT PRIMARY KEY,
                    chat_id TEXT UNIQUE,
                    source_type TEXT NOT NULL,
                    source_value TEXT,
                    dim INTEGER NOT NULL DEFAULT 0,
                    blur INTEGER NOT NULL DEFAULT 0,
                    motion INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent()
            )
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS wallpapers")
            onCreate(db)
        }
    }
}
