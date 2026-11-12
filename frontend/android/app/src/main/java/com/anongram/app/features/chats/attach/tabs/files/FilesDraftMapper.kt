package com.anongram.app.features.chats.attach.tabs.files

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryMedia

internal data class FileMeta(val name: String, val size: Long)

internal fun getFileMetadataHelper(context: Context, uri: Uri): FileMeta {
    var name = "file"
    var size = 0L
    try {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
            if (c.moveToFirst()) {
                if (nameIndex >= 0) name = c.getString(nameIndex) ?: "file"
                if (sizeIndex >= 0) size = c.getLong(sizeIndex)
            }
        }
    } catch (_: Exception) {}
    return FileMeta(name, size)
}

internal fun buildDraftsFromPickedUris(
    context: Context,
    uris: List<Uri>,
    forceAsFile: Boolean = false
): List<AttachmentDraft> = uris.map { uri ->
    if (forceAsFile) {
        val meta = getFileMetadataHelper(context, uri)
        AttachmentDraft.File(uri = uri, name = meta.name, size = meta.size)
    } else {
        val mime = try {
            context.contentResolver.getType(uri) ?: ""
        } catch (_: Exception) {
            ""
        }
        when {
            mime.startsWith("video") -> AttachmentDraft.Media(uri = uri, isVideo = true)
            mime.startsWith("image") -> AttachmentDraft.Media(uri = uri, isVideo = false)
            else -> {
                val meta = getFileMetadataHelper(context, uri)
                AttachmentDraft.File(uri = uri, name = meta.name, size = meta.size)
            }
        }
    }
}

internal fun buildUncompressedGalleryDrafts(
    context: Context,
    pickedMedia: List<GalleryMedia>,
    caption: String
): List<AttachmentDraft.File> = pickedMedia.mapIndexed { index, media ->
    val meta = getFileMetadataHelper(context, media.uri)
    AttachmentDraft.File(
        uri = media.uri,
        name = meta.name,
        size = meta.size,
        caption = if (index == 0) caption else ""
    )
}
