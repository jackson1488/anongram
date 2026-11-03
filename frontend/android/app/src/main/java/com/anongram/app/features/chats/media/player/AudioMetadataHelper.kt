package com.anongram.app.features.chats.media.player

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.collection.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AudioMetadataHelper {

    private val artCache = LruCache<String, Bitmap>(30)

    data class TrackMetadata(
        val title: String,
        val artist: String,
        val durationMs: Long,
        val sizeLabel: String,
        val albumArt: Bitmap?
    )

    fun getCachedArt(key: String): Bitmap? = artCache.get(key)

    fun putCachedArt(key: String, bitmap: Bitmap) {
        artCache.put(key, bitmap)
    }

    suspend fun extractMetadata(context: Context, uri: Uri?, fallbackTitle: String, fallbackArtist: String): TrackMetadata =
        withContext(Dispatchers.IO) {
            var title = fallbackTitle
            var artist = fallbackArtist
            var durationMs = 0L
            var sizeLabel = "3.7 MB"
            var albumArt: Bitmap? = null

            val cacheKey = uri?.toString() ?: "${fallbackArtist}_$fallbackTitle"
            val cached = artCache.get(cacheKey)
            if (cached != null) {
                albumArt = cached
            }

            if (uri != null && uri != Uri.EMPTY) {
                try {
                    val retriever = MediaMetadataRetriever()
                    if (uri.scheme == "content" || uri.scheme == "file") {
                        retriever.setDataSource(context, uri)
                    } else if (uri.path?.let { File(it).exists() } == true) {
                        retriever.setDataSource(uri.path)
                    }

                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.let {
                        if (it.isNotBlank()) title = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.let {
                        if (it.isNotBlank()) artist = it
                    }
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.let {
                        durationMs = it.toLongOrNull() ?: 0L
                    }

                    if (albumArt == null) {
                        val artBytes = retriever.embeddedPicture
                        if (artBytes != null) {
                            val bmp = BitmapFactory.decodeByteArray(artBytes, 0, artBytes.size)
                            if (bmp != null) {
                                albumArt = bmp
                                artCache.put(cacheKey, bmp)
                            }
                        }
                    }

                    // Size
                    if (uri.scheme == "file" && uri.path != null) {
                        val bytes = File(uri.path!!).length()
                        if (bytes > 0) {
                            sizeLabel = formatFileSize(bytes)
                        }
                    } else if (uri.scheme == "content") {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            val bytes = pfd.statSize
                            if (bytes > 0) sizeLabel = formatFileSize(bytes)
                        }
                    }
                    retriever.release()
                } catch (_: Throwable) {}
            }

            if (albumArt == null) {
                albumArt = generateFallbackArt(title, artist)
                artCache.put(cacheKey, albumArt)
            }

            TrackMetadata(
                title = title,
                artist = artist,
                durationMs = durationMs,
                sizeLabel = sizeLabel,
                albumArt = albumArt
            )
        }

    fun formatFileSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes.toDouble() / (1024 * 1024))
            bytes >= 1024 -> String.format("%d KB", bytes / 1024)
            else -> "$bytes B"
        }
    }

    fun generateFallbackArt(title: String, artist: String): Bitmap {
        val size = 200
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val hash = (title + artist).hashCode()
        val c1 = 0xFF000000.toInt() or (hash and 0x00FFFFFF)
        val c2 = 0xFF14151B.toInt()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), c1, c2, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Monogram or musical symbol
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 72f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val letter = title.firstOrNull()?.uppercaseChar()?.toString() ?: "♪"
        val yPos = (size / 2 - (textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(letter, size / 2f, yPos, textPaint)

        return bmp
    }

    /**
     * Генерация круглой аватарки с градиентом и первой буквой имени для системных уведомлений (ГС).
     * Для исходящего ("Вы") — изумрудно-зеленый градиент и буква 'В'.
     * Для входящего (например "Алиса") — уникальный стильный градиент по хешу имени и первая буква контакта.
     */
    fun generateAvatarBitmap(name: String, isOutgoing: Boolean = false): Bitmap {
        val size = 200
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val (c1, c2) = if (isOutgoing) {
            0xFF00C853.toInt() to 0xFF1B5E20.toInt()
        } else {
            val hash = name.hashCode()
            (0xFF000000.toInt() or (hash and 0x00FFFFFF)) to 0xFF14151B.toInt()
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(0f, 0f, size.toFloat(), size.toFloat(), c1, c2, Shader.TileMode.CLAMP)
        }
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 76f
            textAlign = Paint.Align.CENTER
            isFakeBoldText = true
        }
        val letter = if (isOutgoing) "В" else (name.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "?")
        val yPos = (size / 2 - (textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(letter, size / 2f, yPos, textPaint)

        return bmp
    }

    fun parseDurationLabelToMs(label: String): Long {
        val parts = label.trim().split(":")
        if (parts.size == 2) {
            val min = parts[0].toLongOrNull() ?: 0L
            val sec = parts[1].toLongOrNull() ?: 0L
            val total = (min * 60 + sec) * 1000L
            if (total > 0) return total
        } else if (parts.size == 3) {
            val hr = parts[0].toLongOrNull() ?: 0L
            val min = parts[1].toLongOrNull() ?: 0L
            val sec = parts[2].toLongOrNull() ?: 0L
            val total = (hr * 3600 + min * 60 + sec) * 1000L
            if (total > 0) return total
        }
        return 180_000L
    }
}
