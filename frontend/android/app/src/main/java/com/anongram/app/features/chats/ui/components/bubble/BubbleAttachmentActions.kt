package com.anongram.app.features.chats.ui.components.bubble

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Открытие файла (.pdf, .apk, .doc, .zip и т.д.) в системном приложении просмотра/установки Android.
 */
internal fun openFileInSystemViewer(context: Context, file: MessageAttachment.File) {
    val ext = file.name.substringAfterLast('.', "").lowercase()
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext)
        ?: when (file.kind) {
            FileKind.PDF -> "application/pdf"
            FileKind.APK -> "application/vnd.android.package-archive"
            FileKind.ARCHIVE -> "application/zip"
            FileKind.DOC -> "application/msword"
            FileKind.IMAGE -> "image/*"
            else -> "*/*"
        }

    runCatching {
        val targetUri: Uri? = if (!file.uri.isNullOrBlank()) {
            Uri.parse(file.uri)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AnonGram")
            }
            val resolver = context.contentResolver
            val inserted = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            if (inserted != null) {
                resolver.openOutputStream(inserted)?.use { out ->
                    out.write("AnonGram Document: ${file.name} (${file.sizeLabel})".toByteArray())
                }
            }
            inserted
        } else {
            null
        }

        if (targetUri != null) {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(targetUri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(viewIntent, file.name).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } else {
            Toast.makeText(context, "Открытие файла: ${file.name}", Toast.LENGTH_SHORT).show()
        }
    }.onFailure {
        Toast.makeText(context, "Нет приложения для открытия ${file.name}", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Сохранение обычного файла в системную папку «Загрузки» (MediaStore.Downloads).
 */
internal suspend fun saveFileToDeviceDownloads(context: Context, file: MessageAttachment.File): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val ext = file.name.substringAfterLast('.', "").lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, file.name)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AnonGram")
                }
                val destUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                resolver.openOutputStream(destUri)?.use { out ->
                    val srcUri = file.uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                    val copied = if (srcUri != null) {
                        resolver.openInputStream(srcUri)?.use { input -> input.copyTo(out) }
                    } else null
                    if (copied == null) {
                        out.write("AnonGram File: ${file.name} (${file.sizeLabel})".toByteArray())
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

/**
 * Сохранение музыкального трека в системную папку «Музыка» (MediaStore.Audio.Media / DIRECTORY_MUSIC).
 */
internal suspend fun saveMusicToDeviceMusic(context: Context, music: MessageAttachment.Music): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val safeFileName = "${music.artist} - ${music.title}.mp3".replace(Regex("[\\\\/:*?\"<>|]"), "_")
            val resolver = context.contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Audio.Media.DISPLAY_NAME, safeFileName)
                    put(MediaStore.Audio.Media.TITLE, music.title)
                    put(MediaStore.Audio.Media.ARTIST, music.artist)
                    put(MediaStore.Audio.Media.MIME_TYPE, "audio/mpeg")
                    put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/AnonGram")
                }
                val destUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@withContext false
                resolver.openOutputStream(destUri)?.use { out ->
                    val srcUri = music.uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
                    val copied = if (srcUri != null) {
                        resolver.openInputStream(srcUri)?.use { input -> input.copyTo(out) }
                    } else null
                    if (copied == null) {
                        out.write("ID3 AnonGram Audio Track: $safeFileName".toByteArray())
                    }
                }
            }
            true
        }.getOrDefault(false)
    }

internal fun shareTextOrFileFallback(context: Context, label: String) {
    runCatching {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, label)
        }
        val chooser = Intent.createChooser(shareIntent, "Поделиться").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

/**
 * Подсветка совпадающего кусочка слова/подстроки в тексте пузыря сообщения.
 */
internal fun buildHighlightedText(
    text: String,
    query: String,
    highlightColor: Color
): AnnotatedString {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return AnnotatedString(text)
    val lowerText = text.lowercase()
    val lowerQuery = trimmed.lowercase()
    var currentIndex = 0
    var matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
    if (matchIndex < 0) return AnnotatedString(text)

    val builder = AnnotatedString.Builder()
    while (matchIndex >= 0) {
        if (matchIndex > currentIndex) {
            builder.append(text.substring(currentIndex, matchIndex))
        }
        val endMatch = matchIndex + lowerQuery.length
        builder.pushStyle(
            SpanStyle(
                background = highlightColor.copy(alpha = 0.40f),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        )
        builder.append(text.substring(matchIndex, endMatch))
        builder.pop()
        currentIndex = endMatch
        matchIndex = lowerText.indexOf(lowerQuery, currentIndex)
    }
    if (currentIndex < text.length) {
        builder.append(text.substring(currentIndex))
    }
    return builder.toAnnotatedString()
}
