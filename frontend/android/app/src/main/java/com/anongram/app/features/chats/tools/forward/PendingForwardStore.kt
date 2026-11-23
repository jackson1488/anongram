package com.anongram.app.features.chats.tools.forward

import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.domain.ChatMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PendingForwardItem(
    val key: String,
    val resolvedMessage: ChatMessage
)

data class PendingForwardState(
    val sourceSession: IChatSession,
    val targetSessionId: String,
    val items: List<PendingForwardItem>,
    val excludedKeys: Set<String> = emptySet()
) {
    val activeItems: List<PendingForwardItem>
        get() = items.filterNot { it.key in excludedKeys }
}

/**
 * Состояние отложенной анонимной пересылки через встроенную плашку поля ввода.
 */
object PendingForwardStore {
    private val _state = MutableStateFlow<PendingForwardState?>(null)
    val state: StateFlow<PendingForwardState?> = _state.asStateFlow()

    fun start(
        sourceSession: IChatSession,
        targetSessionId: String,
        items: List<PendingForwardItem>
    ) {
        if (items.isEmpty()) return
        _state.value = PendingForwardState(
            sourceSession = sourceSession,
            targetSessionId = targetSessionId,
            items = items
        )
    }

    /** Переключает галочку включения отдельного сообщения/фото; если сняты все — отменяет. */
    fun toggleInclude(key: String): Boolean {
        val current = _state.value ?: return false
        val nextExcluded = if (key in current.excludedKeys) {
            current.excludedKeys - key
        } else {
            current.excludedKeys + key
        }
        val remaining = current.items.filterNot { it.key in nextExcluded }
        return if (remaining.isEmpty()) {
            _state.value = null
            true
        } else {
            _state.value = current.copy(excludedKeys = nextExcluded)
            false
        }
    }

    /** Удаляет конкретное сообщение/элемент из пересылки; возвращает true, если пересылка полностью отменена. */
    fun removeItem(key: String): Boolean {
        val current = _state.value ?: return false
        val nextItems = current.items.filterNot { it.key == key }
        val nextExcluded = current.excludedKeys - key
        val remaining = nextItems.filterNot { it.key in nextExcluded }
        return if (remaining.isEmpty()) {
            _state.value = null
            true
        } else {
            _state.value = current.copy(items = nextItems, excludedKeys = nextExcluded)
            false
        }
    }

    /** Удаляет одно фото из альбома при пересылке; если остаётся 1 — превращает в Photo, если 0 — удаляет элемент. */
    fun removeAlbumPhoto(key: String, photoIndex: Int): Boolean {
        val current = _state.value ?: return false
        val target = current.items.firstOrNull { it.key == key } ?: return false
        val album = target.resolvedMessage.attachment as? com.anongram.app.features.chats.messages.domain.MessageAttachment.MediaAlbum
            ?: return removeItem(key)
        val nextPhotos = album.items.filterIndexed { idx, _ -> idx != photoIndex }
        if (nextPhotos.isEmpty()) {
            return removeItem(key)
        }
        val nextAttachment = if (nextPhotos.size == 1) {
            val single = nextPhotos.first()
            com.anongram.app.features.chats.messages.domain.MessageAttachment.Photo(
                label = if (single.isVideo) "Video" else "Photo",
                colorSeed = 0,
                uri = single.uri,
                isVideo = single.isVideo
            )
        } else {
            album.copy(items = nextPhotos)
        }
        val updatedMsg = target.resolvedMessage.copy(attachment = nextAttachment)
        val nextItems = current.items.map {
            if (it.key == key) it.copy(resolvedMessage = updatedMsg) else it
        }
        _state.value = current.copy(items = nextItems)
        return false
    }

    /** Удаляет один трек из MusicGroup при пересылке; если остаётся 1 — превращает в Music, если 0 — удаляет элемент. */
    fun removeMusicGroupItem(key: String, trackIndex: Int): Boolean {
        val current = _state.value ?: return false
        val target = current.items.firstOrNull { it.key == key } ?: return false
        val group = target.resolvedMessage.attachment as? com.anongram.app.features.chats.messages.domain.MessageAttachment.MusicGroup
            ?: return removeItem(key)
        val nextTracks = group.items.filterIndexed { idx, _ -> idx != trackIndex }
        if (nextTracks.isEmpty()) return removeItem(key)
        val nextAttachment = if (nextTracks.size == 1) nextTracks.first() else group.copy(items = nextTracks)
        val updatedMsg = target.resolvedMessage.copy(attachment = nextAttachment)
        _state.value = current.copy(items = current.items.map {
            if (it.key == key) it.copy(resolvedMessage = updatedMsg) else it
        })
        return false
    }

    /** Удаляет один файл из FileGroup при пересылке; если остаётся 1 — превращает в File, если 0 — удаляет элемент. */
    fun removeFileGroupItem(key: String, fileIndex: Int): Boolean {
        val current = _state.value ?: return false
        val target = current.items.firstOrNull { it.key == key } ?: return false
        val group = target.resolvedMessage.attachment as? com.anongram.app.features.chats.messages.domain.MessageAttachment.FileGroup
            ?: return removeItem(key)
        val nextFiles = group.items.filterIndexed { idx, _ -> idx != fileIndex }
        if (nextFiles.isEmpty()) return removeItem(key)
        val nextAttachment = if (nextFiles.size == 1) nextFiles.first() else group.copy(items = nextFiles)
        val updatedMsg = target.resolvedMessage.copy(attachment = nextAttachment)
        _state.value = current.copy(items = current.items.map {
            if (it.key == key) it.copy(resolvedMessage = updatedMsg) else it
        })
        return false
    }

    fun clear() {
        _state.value = null
    }
}
