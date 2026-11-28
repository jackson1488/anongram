package com.anongram.app.features.chats.media.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.LruCache
import com.anongram.app.core.db.AnonGramEncryptedDb
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class MediaVaultCategory(val folderName: String) {
    VOICE("voice"),
    MUSIC("music"),
    VIDEO_NOTES("video_notes"),
    VIDEO("video"),
    PHOTO("photo"),
    FILES("files")
}

data class IndexedMediaEntry(
    val fileId: String,
    val chatId: String,
    val category: MediaVaultCategory,
    val encPath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val durationMs: Long,
    val createdAt: Long
)

/**
 * Модульное шифрованное хранилище медиафайлов AnonGram.
 *
 * - Сортировка по папкам: `filesDir/media_enc/<category>/<chat_id>/<file_id>.enc`
 * - Потоковое шифрование (AES-256-CTR) без загрузки целых видео в RAM и без BLOB в SQLite
 * - Ключи/IV и метаданные хранятся в зашифрованной `anongram.db` (`media_index`)
 * - Миниатюры фото кэшируются в RAM (`LruCache`) для плавного скролла 120 FPS
 */
object MediaVaultManager {

    private const val ROOT_DIR = "media_enc"
    private const val TRANSFORMATION = "AES/CTR/NoPadding"
    private const val KEY_BYTES = 32
    private const val IV_BYTES = 16
    private const val BUFFER_SIZE = 64 * 1024

    private val thumbCache = object : LruCache<String, ByteArray>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }

    private fun sanitizeSegment(raw: String): String =
        raw.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(64).ifEmpty { "default" }

    fun targetDir(context: Context, category: MediaVaultCategory, chatId: String): File {
        val dir = File(
            File(File(context.applicationContext.filesDir, ROOT_DIR), category.folderName),
            sanitizeSegment(chatId)
        )
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun encryptAndStore(
        context: Context,
        chatId: String,
        category: MediaVaultCategory,
        source: InputStream,
        mimeType: String = "application/octet-stream",
        durationMs: Long = 0L,
        fileId: String = UUID.randomUUID().toString()
    ): IndexedMediaEntry? {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return null
        val outFile = File(targetDir(context, category, chatId), "$fileId.enc")
        val keyNonce = ByteArray(KEY_BYTES + IV_BYTES).also { SecureRandom().nextBytes(it) }
        val keySpec = SecretKeySpec(keyNonce, 0, KEY_BYTES, "AES")
        val ivSpec = IvParameterSpec(keyNonce, KEY_BYTES, IV_BYTES)

        var totalBytes = 0L
        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
            }
            CipherOutputStream(FileOutputStream(outFile), cipher).use { cos ->
                val buf = ByteArray(BUFFER_SIZE)
                while (true) {
                    val read = source.read(buf)
                    if (read <= 0) break
                    cos.write(buf, 0, read)
                    totalBytes += read
                }
            }

            val now = System.currentTimeMillis()
            val cv = ContentValues().apply {
                put("file_id", fileId)
                put("chat_id", chatId)
                put("media_type", category.folderName)
                put("enc_path", outFile.absolutePath)
                put("mime_type", mimeType)
                put("size_bytes", totalBytes)
                put("duration_ms", durationMs)
                put("key_nonce", keyNonce)
                put("created_at", now)
            }
            db.insert("media_index", SQLiteDatabase.CONFLICT_REPLACE, cv)

            IndexedMediaEntry(
                fileId = fileId,
                chatId = chatId,
                category = category,
                encPath = outFile.absolutePath,
                mimeType = mimeType,
                sizeBytes = totalBytes,
                durationMs = durationMs,
                createdAt = now
            )
        }.getOrNull()
    }

    fun openDecryptedStream(fileId: String): InputStream? {
        thumbCache.get(fileId)?.let { return ByteArrayInputStream(it) }
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return null
        var encPath: String? = null
        var keyNonce: ByteArray? = null
        db.query(
            "SELECT enc_path, key_nonce FROM media_index WHERE file_id = ? LIMIT 1",
            arrayOf(fileId)
        ).use { c ->
            if (c.moveToFirst()) {
                encPath = c.getString(0)
                keyNonce = c.getBlob(1)
            }
        }
        val path = encPath ?: return null
        val kn = keyNonce ?: return null
        if (kn.size < KEY_BYTES + IV_BYTES) return null
        val file = File(path)
        if (!file.exists()) return null

        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(kn, 0, KEY_BYTES, "AES"),
                IvParameterSpec(kn, KEY_BYTES, IV_BYTES)
            )
        }
        return CipherInputStream(FileInputStream(file), cipher)
    }

    fun decryptToBytes(fileId: String, cacheInRam: Boolean = true): ByteArray? {
        thumbCache.get(fileId)?.let { return it }
        val bytes = openDecryptedStream(fileId)?.use { it.readBytes() } ?: return null
        if (cacheInRam && bytes.size <= 4 * 1024 * 1024) {
            thumbCache.put(fileId, bytes)
        }
        return bytes
    }

    fun listByChatAndCategory(chatId: String, category: MediaVaultCategory): List<IndexedMediaEntry> {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return emptyList()
        return runCatching {
            val out = mutableListOf<IndexedMediaEntry>()
            db.query(
                "SELECT file_id, chat_id, media_type, enc_path, mime_type, size_bytes, duration_ms, created_at " +
                    "FROM media_index WHERE chat_id = ? AND media_type = ? ORDER BY created_at DESC",
                arrayOf(chatId, category.folderName)
            ).use { c ->
                while (c.moveToNext()) {
                    out += IndexedMediaEntry(
                        fileId = c.getString(0),
                        chatId = c.getString(1),
                        category = category,
                        encPath = c.getString(3),
                        mimeType = c.getString(4),
                        sizeBytes = c.getLong(5),
                        durationMs = c.getLong(6),
                        createdAt = c.getLong(7)
                    )
                }
            }
            out
        }.getOrDefault(emptyList())
    }

    fun getTotalBytesByCategory(): Map<MediaVaultCategory, Long> {
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return emptyMap()
        return runCatching {
            val map = linkedMapOf<MediaVaultCategory, Long>()
            db.query(
                "SELECT media_type, SUM(size_bytes) FROM media_index GROUP BY media_type",
                emptyArray()
            ).use { c ->
                while (c.moveToNext()) {
                    val cat = MediaVaultCategory.entries.firstOrNull { it.folderName == c.getString(0) }
                        ?: continue
                    map[cat] = c.getLong(1)
                }
            }
            map
        }.getOrDefault(emptyMap())
    }

    fun clearCategory(context: Context, category: MediaVaultCategory) {
        val dir = File(File(context.applicationContext.filesDir, ROOT_DIR), category.folderName)
        if (dir.exists()) dir.deleteRecursively()
        val db = AnonGramEncryptedDb.getDbOrNull() ?: return
        runCatching {
            db.delete("media_index", "media_type = ?", arrayOf(category.folderName))
        }
    }
}
