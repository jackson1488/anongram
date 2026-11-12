package com.anongram.app.features.chats.attach.tabs.gallery

import android.content.Context
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import com.anongram.app.features.chats.attach.tabs.files.getFileMetadataHelper
import com.anongram.app.features.mediaeditor.api.EditedMedia

internal fun buildGalleryMediaDrafts(
    context: Context,
    mediaList: List<GalleryMedia>,
    caption: String,
    uncompressed: Boolean = false,
    ungrouped: Boolean = false,
    hd: Boolean = false
): List<AttachmentDraft> {
    if (mediaList.isEmpty()) return emptyList()
    return mediaList.mapIndexed { index, media ->
        val itemCaption = if (index == mediaList.lastIndex) caption else ""
        if (uncompressed) {
            val meta = getFileMetadataHelper(context, media.uri)
            AttachmentDraft.File(
                uri = media.uri,
                name = meta.name,
                size = meta.size,
                caption = itemCaption
            )
        } else {
            AttachmentDraft.Media(
                uri = media.uri,
                isVideo = media.isVideo,
                caption = itemCaption,
                hd = hd,
                hasSpoiler = media.hasSpoiler,
                ungrouped = ungrouped
            )
        }
    }
}

internal fun buildEditedMediaDrafts(
    editedList: List<EditedMedia>
): List<AttachmentDraft.Media> = editedList.map { m ->
    AttachmentDraft.Media(
        uri = m.uri,
        isVideo = m.isVideo,
        caption = m.caption,
        hd = m.hd,
        ttl = m.ttl
    )
}

internal fun toggleSpoilerForMediaSelection(selectedMedia: MutableList<GalleryMedia>) {
    if (selectedMedia.isEmpty()) return
    val target = !selectedMedia.all { it.hasSpoiler }
    val updated = selectedMedia.map { it.copy(hasSpoiler = target) }
    selectedMedia.clear()
    selectedMedia.addAll(updated)
}
