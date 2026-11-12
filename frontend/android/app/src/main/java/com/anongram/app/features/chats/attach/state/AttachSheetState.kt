package com.anongram.app.features.chats.attach.state

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.anongram.app.features.chats.attach.AttachTab
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import com.anongram.app.features.chats.attach.tabs.files.DeviceFileItem
import com.anongram.app.features.chats.attach.tabs.gallery.ALL_MEDIA_ALBUM_ID
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryAlbum
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryMedia
import com.anongram.app.features.chats.attach.tabs.gallery.buildGalleryMediaDrafts
import com.anongram.app.features.contacts.common.Contact
import com.anongram.app.features.mediaeditor.api.EditableMediaItem
import com.anongram.app.features.player.api.Track

@Stable
internal class AttachSheetState(
    initialTab: AttachTab
) {
    val selectedMedia: SnapshotStateList<GalleryMedia> = mutableStateListOf()
    val selectedFiles: SnapshotStateList<DeviceFileItem> = mutableStateListOf()
    val selectedTracks: SnapshotStateList<Track> = mutableStateListOf()
    val selectedContacts: SnapshotStateList<Contact> = mutableStateListOf()
    val capturedMediaList: SnapshotStateList<GalleryMedia> = mutableStateListOf()
    val uncompressedGalleryPicked: SnapshotStateList<GalleryMedia> = mutableStateListOf()

    var captionText by mutableStateOf("")
    var showDiscardDialog by mutableStateOf(false)
    var locationSearchQuery by mutableStateOf("")
    var isLocationSearchActive by mutableStateOf(false)

    var selectedTab by mutableStateOf(initialTab)
    var selectedAlbum by mutableStateOf(ALL_MEDIA_ALBUM_ID)
    var isAlbumsSheetOpen by mutableStateOf(false)
    var availableAlbums by mutableStateOf<List<GalleryAlbum>>(emptyList())

    var viewingMediaList by mutableStateOf<List<GalleryMedia>?>(null)
    var viewingMediaIndex by mutableIntStateOf(0)

    var quickCardContact by mutableStateOf<Contact?>(null)
    val isQuickContactOpen by derivedStateOf { quickCardContact != null }

    var isCameraOpen by mutableStateOf(false)
    var isEditorOpen by mutableStateOf(false)
    var isFullPlayerOpen by mutableStateOf(false)
    var isPreviewOrderOpen by mutableStateOf(false)
    var isUncompressedGalleryOpen by mutableStateOf(false)
    var uncompressedGalleryCaption by mutableStateOf("")
    var isGalleryOptionsMenuOpen by mutableStateOf(false)
    var isMoreMenuOpen by mutableStateOf(false)
    var isAttachEmojiOpen by mutableStateOf(false)
    var editorItems by mutableStateOf<List<EditableMediaItem>>(emptyList())

    val totalSelectedCount by derivedStateOf {
        selectedMedia.size + selectedFiles.size + selectedTracks.size + selectedContacts.size
    }

    fun buildAllSelectedDrafts(context: Context): List<AttachmentDraft> {
        if (totalSelectedCount == 0) return emptyList()
        val drafts = mutableListOf<AttachmentDraft>()

        drafts.addAll(
            buildGalleryMediaDrafts(
                context = context,
                mediaList = selectedMedia,
                caption = captionText
            )
        )

        selectedFiles.forEachIndexed { index, file ->
            drafts.add(
                AttachmentDraft.File(
                    uri = file.uri,
                    name = file.name,
                    size = file.size,
                    caption = if (selectedMedia.isEmpty() && index == selectedFiles.lastIndex) captionText else ""
                )
            )
        }

        selectedTracks.forEachIndexed { index, track ->
            drafts.add(
                AttachmentDraft.Audio(
                    uri = track.uri,
                    title = track.title,
                    artist = track.artist,
                    durationMs = track.durationMs,
                    caption = if (selectedMedia.isEmpty() && selectedFiles.isEmpty() && index == selectedTracks.lastIndex) captionText else ""
                )
            )
        }

        selectedContacts.forEach { contact ->
            drafts.add(
                AttachmentDraft.Contact(
                    name = contact.name,
                    fingerprint = contact.fingerprint,
                    isSasVerified = contact.isSasVerified
                )
            )
        }

        return drafts
    }

    fun clearAllSelected() {
        selectedMedia.clear()
        selectedFiles.clear()
        selectedTracks.clear()
        selectedContacts.clear()
        uncompressedGalleryPicked.clear()
        uncompressedGalleryCaption = ""
        captionText = ""
        isAttachEmojiOpen = false
    }
}

@Composable
internal fun rememberAttachSheetState(initialTab: AttachTab): AttachSheetState =
    remember(initialTab) { AttachSheetState(initialTab) }
