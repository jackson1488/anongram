package com.anongram.app.features.chats.attach.tabs.gallery

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.camera.api.CameraScreen
import com.anongram.app.features.chats.attach.AttachTab
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import com.anongram.app.features.chats.attach.state.AttachSheetDragState
import com.anongram.app.features.chats.attach.state.AttachSheetState
import com.anongram.app.features.chats.mediaeditor.api.EditableMediaItem
import com.anongram.app.features.chats.mediaeditor.api.MediaEditorScreen

@Composable
internal fun GalleryOverlays(
    chatId: String,
    recipientName: String,
    context: Context,
    sheetState: AttachSheetState,
    dragState: AttachSheetDragState,
    onSend: (List<AttachmentDraft>) -> Unit,
    onSendAllSelected: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current

    // Выпадающее меню альбомов
    GalleryAlbumsDropdownOverlay(
        sheetState = sheetState,
        dragState = dragState
    )

    // Полноэкранный просмотр фото со свайпом влево-вправо
    if (sheetState.viewingMediaList != null) {
        GalleryMediaViewer(
            mediaList = sheetState.viewingMediaList!!,
            initialIndex = sheetState.viewingMediaIndex,
            selectedMedia = sheetState.selectedMedia,
            onToggleSelect = { media ->
                if (sheetState.selectedMedia.any { it.uri == media.uri }) {
                    sheetState.selectedMedia.removeAll { it.uri == media.uri }
                } else if (sheetState.selectedMedia.size < 99) {
                    sheetState.selectedMedia.add(media)
                }
            },
            caption = sheetState.captionText,
            onCaptionChange = { sheetState.captionText = it },
            onSend = onSendAllSelected,
            onClose = { sheetState.viewingMediaList = null },
            onEditMedia = { media ->
                sheetState.viewingMediaList = null
                sheetState.editorItems = listOf(
                    EditableMediaItem(
                        id = media.uri.toString(),
                        uri = media.effectiveUri,
                        isVideo = media.isVideo,
                        durationMs = media.durationMs
                    )
                )
                sheetState.isEditorOpen = true
            },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(100f)
        )
    }

    // Экран предпросмотра порядка и спойлера
    AnimatedVisibility(
        visible = sheetState.isPreviewOrderOpen,
        enter = slideInHorizontally(
            initialOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(280)),
        exit = slideOutHorizontally(
            targetOffsetX = { fullWidth -> fullWidth },
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(240)),
        modifier = Modifier
            .fillMaxSize()
            .zIndex(120f)
    ) {
        PreviewOrderScreen(
            initialMediaList = sheetState.selectedMedia.toList(),
            caption = sheetState.captionText,
            onCaptionChange = { sheetState.captionText = it },
            onClose = { sheetState.isPreviewOrderOpen = false },
            chatId = chatId,
            initialOffsetPx = dragState.offsetPx,
            onOffsetChange = { dragState.offsetPx = it },
            onDiscardSelection = {
                sheetState.selectedMedia.clear()
                sheetState.isPreviewOrderOpen = false
                dragState.dismissAnimated()
            },
            onDismissAll = {
                sheetState.selectedMedia.clear()
                sheetState.isPreviewOrderOpen = false
                dragState.dismissAnimated()
            },
            onUpdateOrderAndSpoilers = { newOrder ->
                sheetState.selectedMedia.clear()
                sheetState.selectedMedia.addAll(newOrder)
            },
            onOpenEditor = { list, idx ->
                if (list.isNotEmpty()) {
                    sheetState.editorItems = list.map { m ->
                        val resolved = sheetState.selectedMedia.firstOrNull { it.uri == m.uri } ?: m
                        EditableMediaItem(
                            id = resolved.uri.toString(),
                            uri = resolved.effectiveUri,
                            isVideo = resolved.isVideo,
                            durationMs = resolved.durationMs,
                            caption = sheetState.captionText,
                            coverUri = resolved.coverUri
                        )
                    }
                    sheetState.editorInitialIndex = idx.coerceIn(0, list.lastIndex)
                    sheetState.isEditorOpen = true
                }
            },
            onSend = { orderedMedia, isUncompressed, isUngrouped, isHd ->
                val drafts = buildGalleryMediaDrafts(
                    context = context,
                    mediaList = orderedMedia,
                    caption = sheetState.captionText,
                    uncompressed = isUncompressed,
                    ungrouped = isUngrouped,
                    hd = isHd
                )
                onSend(drafts)
                sheetState.isPreviewOrderOpen = false
                dragState.dismissAnimated()
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    // Окно Камеры: снятое фото или видео открывается в редакторе изолированно (строго 1 этот файл, без перелистывания галереи)
    if (sheetState.isCameraOpen) {
        CameraScreen(
            onResult = { media ->
                sheetState.isCameraOpen = false
                val galleryMedia = GalleryMedia(
                    uri = media.uri,
                    isVideo = media.isVideo,
                    durationMs = media.durationMs,
                    dateAdded = System.currentTimeMillis()
                )
                sheetState.capturedMediaList.add(0, galleryMedia)
                if (sheetState.selectedMedia.none { it.uri == media.uri }) {
                    sheetState.selectedMedia.add(0, galleryMedia)
                }
                sheetState.selectedTab = AttachTab.GALLERY
                sheetState.editorItems = listOf(
                    EditableMediaItem(
                        id = media.uri.toString(),
                        uri = media.uri,
                        isVideo = media.isVideo,
                        durationMs = media.durationMs,
                        caption = sheetState.captionText
                    )
                )
                sheetState.editorInitialIndex = 0
                sheetState.isEditorOpen = true
            },
            onClose = { sheetState.isCameraOpen = false },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(250f)
        )
    }

    // Редактор медиа
    if (sheetState.isEditorOpen) {
        MediaEditorScreen(
            items = sheetState.editorItems,
            initialIndex = sheetState.editorInitialIndex,
            recipientName = recipientName,
            selectedIds = sheetState.selectedMedia.map { it.uri.toString() },
            targetBoundsForId = { id -> sheetState.galleryTileBounds[id] },
            onToggleSelectItem = { item ->
                val existingIdx = sheetState.selectedMedia.indexOfFirst { it.uri.toString() == item.id }
                if (existingIdx >= 0) {
                    sheetState.selectedMedia.removeAt(existingIdx)
                } else if (sheetState.selectedMedia.size < 99) {
                    val origUri = runCatching { android.net.Uri.parse(item.id) }.getOrNull() ?: item.uri
                    sheetState.selectedMedia.add(
                        GalleryMedia(
                            uri = origUri,
                            isVideo = item.isVideo,
                            durationMs = item.durationMs,
                            dateAdded = System.currentTimeMillis(),
                            editedUri = if (item.uri != origUri) item.uri else null,
                            coverUri = item.coverUri
                        )
                    )
                }
            },
            onSend = { editedList ->
                onSend(buildEditedMediaDrafts(editedList))
                sheetState.isEditorOpen = false
                dragState.dismissAnimated()
            },
            onSaveOnly = { editedList ->
                editedList.forEach { edited ->
                    val origUriStr = edited.originalId.ifBlank { edited.uri.toString() }
                    val existingIdx = sheetState.selectedMedia.indexOfFirst {
                        it.uri.toString() == origUriStr || it.editedUri == edited.uri
                    }
                    if (existingIdx >= 0) {
                        val old = sheetState.selectedMedia[existingIdx]
                        sheetState.selectedMedia[existingIdx] = old.copy(
                            editedUri = edited.uri,
                            coverUri = edited.coverUri
                        )
                    } else {
                        val origUri = runCatching { android.net.Uri.parse(origUriStr) }.getOrNull() ?: edited.uri
                        sheetState.selectedMedia.add(
                            GalleryMedia(
                                uri = origUri,
                                isVideo = edited.isVideo,
                                dateAdded = System.currentTimeMillis(),
                                editedUri = edited.uri,
                                coverUri = edited.coverUri
                            )
                        )
                    }
                }
            },
            onAddMore = { sheetState.isEditorOpen = false },
            onBack = { sheetState.isEditorOpen = false },
            modifier = Modifier
                .fillMaxSize()
                .zIndex(150f)
        )
    }
}

@Composable
internal fun GalleryAlbumsDropdownOverlay(
    sheetState: AttachSheetState,
    dragState: AttachSheetDragState
) {
    val scheme = LocalAnonGramScheme.current
    if (sheetState.isAlbumsSheetOpen && sheetState.availableAlbums.isNotEmpty() && dragState.offsetPx <= dragState.totalHeightPx * 0.08f) {
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(50f)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { sheetState.isAlbumsSheetOpen = false }
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        start = 58.dp,
                        top = statusBarTop + 58.dp,
                        end = 24.dp,
                        bottom = 24.dp
                    )
                    .widthIn(min = 230.dp, max = 290.dp)
                    .wrapContentHeight()
                    .heightIn(max = 380.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .border(0.5.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* consume clicks inside menu */ }
                    .padding(vertical = 8.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    items(sheetState.availableAlbums, key = { it.name }) { album ->
                        AlbumDropdownRowItem(
                            album = album,
                            isSelected = (
                                album.name == sheetState.selectedAlbum ||
                                    ((album.name == ALL_MEDIA_ALBUM_ID || album.name == "Все медиа") &&
                                        (sheetState.selectedAlbum == ALL_MEDIA_ALBUM_ID || sheetState.selectedAlbum == "Все медиа" || sheetState.selectedAlbum == "Галерея"))
                                ),
                            onClick = {
                                sheetState.selectedAlbum = album.name
                                sheetState.isAlbumsSheetOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

