package com.anongram.app.features.chats.viewer

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.anongram.app.features.chats.attach.tabs.gallery.loadHdBitmap
import com.anongram.app.features.chats.attach.tabs.gallery.loadMediaThumbnail
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.FileKind
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.viewer.i18n.ViewerI18nStrings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

internal data class ViewerMediaEntry(
    val message: ChatMessage,
    val uri: String,
    val isVideo: Boolean,
    val itemIndexInMessage: Int,
    val albumPositionInGroup: Int,
    val albumUrisInMessage: List<String>,
    val albumVideoFlagsInMessage: List<Boolean>
)

private fun isUncompressedImageFile(file: MessageAttachment.File): Boolean {
    if (file.uri.isNullOrBlank()) return false
    if (file.kind == FileKind.IMAGE) return true
    val ext = file.name.substringAfterLast('.', "").lowercase()
    return ext in setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp")
}

private fun isUncompressedVideoFile(file: MessageAttachment.File): Boolean {
    if (file.uri.isNullOrBlank()) return false
    if (file.kind == FileKind.VIDEO) return true
    val ext = file.name.substringAfterLast('.', "").lowercase()
    return ext in setOf("mp4", "mov", "mkv", "webm", "3gp", "avi", "m4v")
}

internal fun buildViewerEntries(
    allChatMessages: List<ChatMessage>,
    message: ChatMessage,
    overrideSingleUri: String?,
    isUncompressedMode: Boolean
): List<ViewerMediaEntry> {
    if (isUncompressedMode) {
        val currentMsg = allChatMessages.firstOrNull { it.id == message.id } ?: message
        return when (val att = currentMsg.attachment) {
            is MessageAttachment.FileGroup -> {
                val mediaItems = att.items.mapIndexedNotNull { fileIdx, fileItem ->
                    when {
                        isUncompressedImageFile(fileItem) -> Triple(fileIdx, fileItem.uri!!, false)
                        isUncompressedVideoFile(fileItem) -> Triple(fileIdx, fileItem.uri!!, true)
                        else -> null
                    }
                }
                val allGroupUris = mediaItems.map { it.second }
                val allGroupFlags = mediaItems.map { it.third }
                mediaItems.mapIndexed { posInGroup, (fileIdx, uriStr, isVid) ->
                    ViewerMediaEntry(
                        message = currentMsg,
                        uri = uriStr,
                        isVideo = isVid,
                        itemIndexInMessage = fileIdx,
                        albumPositionInGroup = posInGroup,
                        albumUrisInMessage = allGroupUris,
                        albumVideoFlagsInMessage = allGroupFlags
                    )
                }
            }
            is MessageAttachment.File -> {
                val u = overrideSingleUri ?: att.uri
                if (!u.isNullOrBlank()) {
                    val isVid = isUncompressedVideoFile(att)
                    listOf(
                        ViewerMediaEntry(
                            message = currentMsg,
                            uri = u,
                            isVideo = isVid,
                            itemIndexInMessage = 0,
                            albumPositionInGroup = 0,
                            albumUrisInMessage = listOf(u),
                            albumVideoFlagsInMessage = listOf(isVid)
                        )
                    )
                } else {
                    emptyList()
                }
            }
            else -> {
                if (!overrideSingleUri.isNullOrBlank()) {
                    listOf(
                        ViewerMediaEntry(
                            message = currentMsg,
                            uri = overrideSingleUri,
                            isVideo = false,
                            itemIndexInMessage = 0,
                            albumPositionInGroup = 0,
                            albumUrisInMessage = listOf(overrideSingleUri),
                            albumVideoFlagsInMessage = listOf(false)
                        )
                    )
                } else {
                    emptyList()
                }
            }
        }
    } else {
        val sourceMessages = if (allChatMessages.any { it.id == message.id }) {
            allChatMessages
        } else {
            allChatMessages + message
        }
        return buildList {
            for (msg in sourceMessages) {
                when (val att = msg.attachment) {
                    is MessageAttachment.Photo -> {
                        val u = att.uri
                        if (!u.isNullOrBlank()) {
                            add(
                                ViewerMediaEntry(
                                    message = msg,
                                    uri = u,
                                    isVideo = att.isVideo,
                                    itemIndexInMessage = 0,
                                    albumPositionInGroup = 0,
                                    albumUrisInMessage = listOf(u),
                                    albumVideoFlagsInMessage = listOf(att.isVideo)
                                )
                            )
                        }
                    }
                    is MessageAttachment.MediaAlbum -> {
                        val albumUris = att.items.map { it.uri }
                        val albumFlags = att.items.map { it.isVideo }
                        att.items.forEachIndexed { idx, item ->
                            add(
                                ViewerMediaEntry(
                                    message = msg,
                                    uri = item.uri,
                                    isVideo = item.isVideo,
                                    itemIndexInMessage = idx,
                                    albumPositionInGroup = idx,
                                    albumUrisInMessage = albumUris,
                                    albumVideoFlagsInMessage = albumFlags
                                )
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }
}

internal fun formatViewerSubtitleDate(epochMs: Long, strings: ViewerI18nStrings): String {
    val zone = ZoneId.systemDefault()
    val zdt = Instant.ofEpochMilli(epochMs).atZone(zone)
    val msgDate = zdt.toLocalDate()
    val today = Instant.ofEpochMilli(System.currentTimeMillis()).atZone(zone).toLocalDate()
    val locale = if (strings.isRussian) Locale.forLanguageTag("ru") else Locale.ENGLISH
    val timeStr = zdt.format(DateTimeFormatter.ofPattern("HH:mm", locale))
    val daysDiff = ChronoUnit.DAYS.between(msgDate, today)

    return when {
        daysDiff == 0L -> strings.todayAt(timeStr)
        daysDiff == 1L -> strings.yesterdayAt(timeStr)
        daysDiff in 2L..6L -> {
            val dayOfWeek = zdt.format(DateTimeFormatter.ofPattern("EEEE", locale)).lowercase(locale)
            strings.weekdayOrDateAt(dayOfWeek, timeStr)
        }
        msgDate.year == today.year -> {
            val datePart = zdt.format(DateTimeFormatter.ofPattern("d MMMM", locale))
            strings.weekdayOrDateAt(datePart, timeStr)
        }
        else -> {
            val datePart = zdt.format(DateTimeFormatter.ofPattern("d MMMM yyyy", locale))
            strings.weekdayOrDateAt(datePart, timeStr)
        }
    }
}

/**
 * Сохранение фото или видео в галерею устройства через MediaStore.
 */
internal suspend fun saveMediaToDeviceGallery(
    context: Context,
    uriString: String,
    isVideo: Boolean
): Boolean = withContext(Dispatchers.IO) {
    if (!isVideo) {
        return@withContext savePhotoToDeviceGallery(context, uriString)
    }
    runCatching {
        val parsedUri = Uri.parse(uriString)
        val filename = "AnonGram_${System.currentTimeMillis()}.mp4"
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, filename)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/AnonGram")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val destUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            ?: return@withContext false
        resolver.openInputStream(parsedUri)?.use { input ->
            resolver.openOutputStream(destUri)?.use { output ->
                input.copyTo(output)
            } ?: return@withContext false
        } ?: return@withContext false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Video.Media.IS_PENDING, 0)
            resolver.update(destUri, values, null, null)
        }
        true
    }.getOrDefault(false)
}

/**
 * Реальное сохранение фотографии в галерею устройства через MediaStore.
 */
internal suspend fun savePhotoToDeviceGallery(context: Context, uriString: String): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val parsedUri = Uri.parse(uriString)
            val bitmap = loadHdBitmap(context, parsedUri, 2560) ?: loadMediaThumbnail(context, parsedUri)
                ?: return@withContext false

            val filename = "AnonGram_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/AnonGram")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
            }

            val resolver = context.contentResolver
            val destUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext false

            resolver.openOutputStream(destUri)?.use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(destUri, values, null, null)
            }
            true
        }.getOrDefault(false)
    }

/**
 * Системное окно «Поделиться» (Intent.ACTION_SEND) для отправки медиа в любое приложение.
 */
internal fun sharePhotoSystemChooser(
    context: Context,
    uriString: String,
    isVideo: Boolean = false,
    chooserTitle: String = "Поделиться"
) {
    runCatching {
        val parsedUri = Uri.parse(uriString)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (isVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, parsedUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

/**
 * Открытие фото или видео в системном приложении (галерея / видеоплеер) через Intent.ACTION_VIEW.
 */
internal fun openMediaInSystemApp(
    context: Context,
    uriString: String,
    isVideo: Boolean,
    chooserTitle: String
): Boolean {
    return runCatching {
        val parsedUri = Uri.parse(uriString)
        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(parsedUri, if (isVideo) "video/*" else "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(viewIntent, chooserTitle).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        true
    }.getOrDefault(false)
}
