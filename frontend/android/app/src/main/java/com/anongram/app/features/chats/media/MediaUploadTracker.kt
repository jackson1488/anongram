package com.anongram.app.features.chats.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.Job
import java.io.File
import java.util.concurrent.ConcurrentHashMap

enum class UploadPhase {
    COMPRESSING,
    UPLOADING
}

data class MediaUploadItemState(
    val progress: Float = 0f,
    val processedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val phase: UploadPhase = UploadPhase.COMPRESSING
)

/**
 * Реестр активных задач сжатия и выгрузки медиа.
 * Позволяет отображать индивидуальный прогресс (кольцо + МБ) на каждом фото/видео
 * и мгновенно отменять конкретный элемент или весь альбом по крестику ✕.
 */
internal object MediaUploadTracker {
    private val itemStates = mutableStateMapOf<String, MediaUploadItemState>()
    private val itemJobs = ConcurrentHashMap<String, Job>()
    private val itemCancelCallbacks = ConcurrentHashMap<String, () -> Unit>()
    private val itemTempFiles = ConcurrentHashMap<String, File>()

    private fun key(messageId: String, uri: String): String = "$messageId|$uri"

    fun register(
        messageId: String,
        uri: String,
        totalBytes: Long,
        job: Job,
        onCancel: () -> Unit
    ) {
        val k = key(messageId, uri)
        itemJobs[k] = job
        itemCancelCallbacks[k] = onCancel
        itemStates[k] = MediaUploadItemState(
            progress = 0f,
            processedBytes = 0L,
            totalBytes = totalBytes.coerceAtLeast(0L),
            phase = UploadPhase.COMPRESSING
        )
    }

    fun update(
        messageId: String,
        uri: String,
        progress: Float,
        processedBytes: Long,
        totalBytes: Long,
        phase: UploadPhase = UploadPhase.COMPRESSING
    ) {
        val k = key(messageId, uri)
        if (!itemJobs.containsKey(k)) return
        itemStates[k] = MediaUploadItemState(
            progress = progress.coerceIn(0f, 1f),
            processedBytes = processedBytes.coerceAtLeast(0L),
            totalBytes = totalBytes.coerceAtLeast(0L),
            phase = phase
        )
    }

    fun attachTempFile(messageId: String, uri: String, file: File) {
        itemTempFiles[key(messageId, uri)] = file
    }

    fun complete(messageId: String, uri: String) {
        val k = key(messageId, uri)
        itemStates.remove(k)
        itemJobs.remove(k)
        itemCancelCallbacks.remove(k)
        itemTempFiles.remove(k)
    }

    fun cancelItem(messageId: String, uri: String) {
        val k = key(messageId, uri)
        itemStates.remove(k)
        itemJobs.remove(k)?.cancel()
        itemTempFiles.remove(k)?.let { runCatching { if (it.exists()) it.delete() } }
        itemCancelCallbacks.remove(k)?.invoke()
    }

    fun cancelMessage(messageId: String) {
        val prefix = "$messageId|"
        val matchingKeys = itemJobs.keys.filter { it.startsWith(prefix) }
        matchingKeys.forEach { k ->
            itemStates.remove(k)
            itemJobs.remove(k)?.cancel()
            itemTempFiles.remove(k)?.let { runCatching { if (it.exists()) it.delete() } }
            itemCancelCallbacks.remove(k)
        }
    }

    @Composable
    fun rememberState(messageId: String, uri: String?): MediaUploadItemState? {
        if (messageId.isBlank() || uri.isNullOrBlank()) return null
        return itemStates[key(messageId, uri)]
    }
}
