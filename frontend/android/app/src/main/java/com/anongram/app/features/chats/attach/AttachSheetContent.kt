package com.anongram.app.features.chats.attach

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.api.AttachmentDraft
import com.anongram.app.features.chats.attach.components.AttachDiscardDialog
import com.anongram.app.features.chats.attach.components.AttachHandleBar
import com.anongram.app.features.chats.attach.components.AttachInputBar
import com.anongram.app.features.chats.attach.components.AttachTabBar
import com.anongram.app.features.chats.attach.i18n.AttachI18n
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.attach.state.rememberAttachSheetDragState
import com.anongram.app.features.chats.attach.state.rememberAttachSheetState
import com.anongram.app.features.chats.attach.tabs.contacts.ContactQuickCardSheet
import com.anongram.app.features.chats.attach.tabs.contacts.ContactsTab
import com.anongram.app.features.chats.attach.tabs.files.FilesGallerySubScreen
import com.anongram.app.features.chats.attach.tabs.files.FilesTab
import com.anongram.app.features.chats.attach.tabs.files.buildDraftsFromPickedUris
import com.anongram.app.features.chats.attach.tabs.files.buildUncompressedGalleryDrafts
import com.anongram.app.features.chats.attach.tabs.files.getFileMetadataHelper
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryFloatingTopBar
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryOverlays
import com.anongram.app.features.chats.attach.tabs.gallery.GallerySelectionBar
import com.anongram.app.features.chats.attach.tabs.gallery.GallerySelectionOptionsMenu
import com.anongram.app.features.chats.attach.tabs.gallery.GalleryTab
import com.anongram.app.features.chats.attach.tabs.gallery.buildGalleryMediaDrafts
import com.anongram.app.features.chats.attach.tabs.gallery.getGallerySelectionTitle
import com.anongram.app.features.chats.attach.tabs.gallery.toggleSpoilerForMediaSelection
import com.anongram.app.features.chats.attach.tabs.location.LocationFloatingTopBar
import com.anongram.app.features.chats.attach.tabs.location.LocationTab
import com.anongram.app.features.chats.attach.tabs.music.MusicTab
import com.anongram.app.features.player.api.FullPlayerSheet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun AttachSheetContent(
    chatId: String = "",
    recipientName: String = "",
    initialTab: AttachTab = AttachTab.GALLERY,
    isSingleReplaceMode: Boolean = false,
    onSingleReplace: ((AttachmentDraft) -> Unit)? = null,
    onDismiss: () -> Unit,
    onSend: (List<AttachmentDraft>) -> Unit,
    keyboardHeightPx: Int = 0,
    wasKeyboardOpen: Boolean = false,
    onRestoreKeyboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val appLanguage by LanguageSettingsStore.language.collectAsState()
    val attachStrings = remember(appLanguage) { AttachI18n.forLanguage(appLanguage) }
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val H = constraints.maxHeight.toFloat()
        val sheetState = rememberAttachSheetState(initialTab)
        val dragState = rememberAttachSheetDragState(
            totalHeightPx = H,
            context = context,
            wasKeyboardOpen = wasKeyboardOpen,
            onRestoreKeyboard = onRestoreKeyboard,
            onDismiss = onDismiss,
            sheetState = sheetState
        )

        LaunchedEffect(H) {
            if (wasKeyboardOpen && keyboardHeightPx > 0) {
                launch {
                    snapshotFlow { H - dragState.offsetPx }
                        .first { it >= keyboardHeightPx * 0.90f && it > 0 }
                    keyboardController?.hide()
                }
            } else if (wasKeyboardOpen) {
                keyboardController?.hide()
            }
            dragState.animateTo(dragState.openOffset, 300)
        }

        LaunchedEffect(dragState.offsetPx) {
            if (dragState.offsetPx > 10f && sheetState.isAlbumsSheetOpen) {
                sheetState.isAlbumsSheetOpen = false
            }
        }

        fun sendAllSelected() {
            val drafts = sheetState.buildAllSelectedDrafts(context)
            if (drafts.isEmpty()) return
            if (isSingleReplaceMode && onSingleReplace != null) {
                drafts.firstOrNull()?.let { onSingleReplace(it) }
                dragState.forceDismiss()
                return
            }
            onSend(drafts)
            dragState.dismissAnimated()
        }

        val systemChooserLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val data = result.data
                val uris = buildList {
                    data?.clipData?.let { clip ->
                        for (i in 0 until clip.itemCount) {
                            clip.getItemAt(i)?.uri?.let { add(it) }
                        }
                    } ?: data?.data?.let { add(it) }
                }
                if (uris.isNotEmpty()) {
                    onSend(buildDraftsFromPickedUris(context, uris))
                    dragState.dismissAnimated()
                }
            }
        }

        BackHandler {
            when {
                sheetState.isUncompressedGalleryOpen -> sheetState.isUncompressedGalleryOpen = false
                sheetState.isPreviewOrderOpen -> sheetState.isPreviewOrderOpen = false
                sheetState.viewingMediaList != null -> sheetState.viewingMediaList = null
                sheetState.showDiscardDialog -> sheetState.showDiscardDialog = false
                sheetState.isAttachEmojiOpen -> sheetState.isAttachEmojiOpen = false
                sheetState.quickCardContact != null -> sheetState.quickCardContact = null
                sheetState.isAlbumsSheetOpen -> sheetState.isAlbumsSheetOpen = false
                sheetState.isGalleryOptionsMenuOpen -> sheetState.isGalleryOptionsMenuOpen = false
                sheetState.isMoreMenuOpen -> sheetState.isMoreMenuOpen = false
                sheetState.isCameraOpen -> sheetState.isCameraOpen = false
                sheetState.isEditorOpen -> sheetState.isEditorOpen = false
                sheetState.isFullPlayerOpen -> sheetState.isFullPlayerOpen = false
                else -> dragState.requestDismiss()
            }
        }

        // Затемнение фона
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = ((H - dragState.offsetPx) / H * 0.70f).coerceIn(0f, 0.70f)
                }
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { dragState.requestDismiss() }
        )

        @OptIn(ExperimentalFoundationApi::class)
        CompositionLocalProvider(
            LocalOverscrollConfiguration provides null,
            LocalAttachStrings provides attachStrings
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = dragState.offsetPx }
                    .then(
                        if (sheetState.selectedTab == AttachTab.LOCATION) Modifier
                        else Modifier.nestedScroll(dragState.nestedScrollConnection)
                    )
            ) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(scheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { /* поглощаем клики внутри шторки */ }
                ) {
                    val isExpanded95 = dragState.isExpanded95
                    val isGallery = (sheetState.selectedTab == AttachTab.GALLERY)
                    val isGalleryWithSelection = (isGallery && sheetState.selectedMedia.isNotEmpty() && !isExpanded95)
                    val targetHandleHeight = when {
                        isGalleryWithSelection -> 44.dp
                        dragState.isFull -> 0.dp
                        sheetState.selectedTab == AttachTab.LOCATION -> 28.dp
                        else -> 0.dp
                    }
                    val handleHeight by animateDpAsState(
                        targetValue = targetHandleHeight,
                        animationSpec = tween(durationMillis = 200),
                        label = "attachHandleHeight"
                    )

                    if (handleHeight > 0.dp) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(handleHeight)
                                .background(scheme.surface)
                                .draggable(
                                    orientation = Orientation.Vertical,
                                    state = rememberDraggableState { d ->
                                        val maxOffset = if (sheetState.totalSelectedCount > 0) dragState.halfOffset + 120f else H
                                        dragState.offsetPx = (dragState.offsetPx + d).coerceIn(0f, maxOffset)
                                    },
                                    onDragStarted = { dragState.animJob?.cancel() },
                                    onDragStopped = { v -> dragState.release(v) }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                isGalleryWithSelection -> {
                                    GallerySelectionBar(
                                        title = getGallerySelectionTitle(sheetState.selectedMedia, attachStrings),
                                        hasMoreItems = sheetState.selectedMedia.isNotEmpty(),
                                        onOpenPreviewOrder = {
                                            if (sheetState.selectedMedia.isNotEmpty()) {
                                                sheetState.isPreviewOrderOpen = true
                                            }
                                        },
                                        onOptionsClick = { sheetState.isGalleryOptionsMenuOpen = true },
                                        onBack = null,
                                        menuContent = {
                                            GallerySelectionOptionsMenu(
                                                expanded = sheetState.isGalleryOptionsMenuOpen,
                                                onDismissRequest = { sheetState.isGalleryOptionsMenuOpen = false },
                                                canUngroup = sheetState.selectedMedia.size >= 2,
                                                allHaveSpoiler = sheetState.selectedMedia.all { it.hasSpoiler },
                                                onOpenPreview = {
                                                    if (sheetState.selectedMedia.isNotEmpty()) {
                                                        sheetState.isPreviewOrderOpen = true
                                                    }
                                                },
                                                onSendUncompressed = {
                                                    val drafts = buildGalleryMediaDrafts(
                                                        context = context,
                                                        mediaList = sheetState.selectedMedia,
                                                        caption = sheetState.captionText,
                                                        uncompressed = true
                                                    )
                                                    if (drafts.isNotEmpty()) {
                                                        onSend(drafts)
                                                        dragState.dismissAnimated()
                                                    }
                                                },
                                                onSendUngrouped = {
                                                    val drafts = buildGalleryMediaDrafts(
                                                        context = context,
                                                        mediaList = sheetState.selectedMedia,
                                                        caption = sheetState.captionText,
                                                        ungrouped = true
                                                    )
                                                    if (drafts.isNotEmpty()) {
                                                        onSend(drafts)
                                                        dragState.dismissAnimated()
                                                    }
                                                },
                                                onToggleSpoiler = { toggleSpoilerForMediaSelection(sheetState.selectedMedia) },
                                                onSendHd = {
                                                    val drafts = buildGalleryMediaDrafts(
                                                        context = context,
                                                        mediaList = sheetState.selectedMedia,
                                                        caption = sheetState.captionText,
                                                        hd = true
                                                    )
                                                    if (drafts.isNotEmpty()) {
                                                        onSend(drafts)
                                                        dragState.dismissAnimated()
                                                    }
                                                }
                                            )
                                        }
                                    )
                                }
                                sheetState.selectedTab == AttachTab.LOCATION -> AttachHandleBar()
                                else -> {
                                    val isThinHandle = (sheetState.selectedTab == AttachTab.GALLERY || sheetState.selectedTab == AttachTab.MUSIC)
                                    Box(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                alpha = ((dragState.offsetPx - H * 0.03f) / (H * 0.05f)).coerceIn(0f, 1f)
                                            }
                                            .width(if (isThinHandle) 24.dp else 28.dp)
                                            .height(if (isThinHandle) 1.5.dp else 2.5.dp)
                                            .clip(CircleShape)
                                            .background(scheme.textMuted.copy(alpha = if (isThinHandle) 0.25f else 0.35f))
                                    )
                                }
                            }
                        }
                    }

                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        when (sheetState.selectedTab) {
                            AttachTab.GALLERY -> {
                                GalleryTab(
                                    onLaunchCamera = { sheetState.isCameraOpen = true },
                                    onOpenEditor = { items ->
                                        sheetState.editorItems = items
                                        sheetState.isEditorOpen = true
                                    },
                                    selectedAlbum = sheetState.selectedAlbum,
                                    onSelectAlbum = { sheetState.selectedAlbum = it },
                                    isAlbumsSheetOpen = sheetState.isAlbumsSheetOpen,
                                    onCloseAlbumsSheet = { sheetState.isAlbumsSheetOpen = false },
                                    onOpenAlbumsSheet = { sheetState.isAlbumsSheetOpen = true },
                                    selectedMedia = sheetState.selectedMedia,
                                    onToggleSelectMedia = { media ->
                                        sheetState.isAlbumsSheetOpen = false
                                        if (isSingleReplaceMode) {
                                            val already = sheetState.selectedMedia.any { it.uri == media.uri }
                                            sheetState.selectedMedia.clear()
                                            if (!already) sheetState.selectedMedia.add(media)
                                        } else {
                                            if (sheetState.selectedMedia.any { it.uri == media.uri }) {
                                                sheetState.selectedMedia.removeAll { it.uri == media.uri }
                                            } else if (sheetState.selectedMedia.size < 99) {
                                                sheetState.selectedMedia.add(media)
                                            }
                                        }
                                    },
                                    caption = sheetState.captionText,
                                    onCaptionChange = { sheetState.captionText = it },
                                    onSend = { sendAllSelected() },
                                    capturedMediaItems = sheetState.capturedMediaList,
                                    isFullscreen = dragState.isFull,
                                    onOpenViewer = { list, idx ->
                                        sheetState.isAlbumsSheetOpen = false
                                        sheetState.viewingMediaList = list
                                        sheetState.viewingMediaIndex = idx
                                    },
                                    onAlbumsLoaded = { sheetState.availableAlbums = it },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            AttachTab.FILE -> {
                                FilesTab(
                                    onSelectFile = { fileDraft ->
                                        if (isSingleReplaceMode && onSingleReplace != null) {
                                            onSingleReplace(fileDraft)
                                            dragState.forceDismiss()
                                        } else {
                                            onSend(listOf(fileDraft))
                                            dragState.dismissAnimated()
                                        }
                                    },
                                    onExpandToFullscreen = { dragState.animateTo(0f, 240) },
                                    selectedFiles = sheetState.selectedFiles,
                                    onToggleSelectFile = { file ->
                                        if (isSingleReplaceMode) {
                                            val already = sheetState.selectedFiles.any { it.uri == file.uri }
                                            sheetState.selectedFiles.clear()
                                            if (!already) sheetState.selectedFiles.add(file)
                                        } else {
                                            if (sheetState.selectedFiles.any { it.uri == file.uri }) {
                                                sheetState.selectedFiles.removeAll { it.uri == file.uri }
                                            } else {
                                                sheetState.selectedFiles.add(file)
                                            }
                                        }
                                    },
                                    isFullscreen = dragState.isFull,
                                    onBack = { dragState.requestDismiss() },
                                    onSelectFiles = { drafts ->
                                        if (isSingleReplaceMode && onSingleReplace != null) {
                                            drafts.firstOrNull()?.let { onSingleReplace(it) }
                                            dragState.forceDismiss()
                                        } else {
                                            onSend(drafts)
                                            dragState.dismissAnimated()
                                        }
                                    },
                                    onOpenUncompressedGallery = { sheetState.isUncompressedGalleryOpen = true },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            AttachTab.LOCATION -> {
                                val sheetOffsetDp = with(LocalDensity.current) { dragState.offsetPx.coerceAtLeast(0f).toDp() }
                                LocationTab(
                                    onSendLocation = { locDraft ->
                                        onSend(listOf(locDraft))
                                        dragState.dismissAnimated()
                                    },
                                    searchQuery = sheetState.locationSearchQuery,
                                    isFullscreen = dragState.isFull || dragState.isLocationHeaderVisible,
                                    sheetOffsetDp = sheetOffsetDp,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            AttachTab.CONTACT -> {
                                ContactsTab(
                                    onSelectContact = { contactDraft ->
                                        onSend(listOf(contactDraft))
                                        dragState.dismissAnimated()
                                    },
                                    onExpandToFullscreen = { dragState.animateTo(0f, 240) },
                                    selectedContacts = sheetState.selectedContacts,
                                    onToggleSelectContact = { contact ->
                                        if (sheetState.selectedContacts.any { it.name == contact.name }) {
                                            sheetState.selectedContacts.removeAll { it.name == contact.name }
                                        } else {
                                            sheetState.selectedContacts.add(contact)
                                        }
                                    },
                                    isFullscreen = dragState.isFull,
                                    onBack = { dragState.requestDismiss() },
                                    onQuickCardVisibilityChange = {},
                                    onOpenQuickCard = { sheetState.quickCardContact = it },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            AttachTab.MUSIC -> {
                                MusicTab(
                                    onSelectAudio = { audioDraft ->
                                        if (isSingleReplaceMode && onSingleReplace != null) {
                                            onSingleReplace(audioDraft)
                                            dragState.forceDismiss()
                                        } else {
                                            onSend(listOf(audioDraft))
                                            dragState.dismissAnimated()
                                        }
                                    },
                                    onExpandPlayer = { sheetState.isFullPlayerOpen = true },
                                    onExpandToFullscreen = { dragState.animateTo(0f, 240) },
                                    selectedTracks = sheetState.selectedTracks,
                                    onToggleSelectTrack = { track ->
                                        if (isSingleReplaceMode) {
                                            val already = sheetState.selectedTracks.any { it.id == track.id }
                                            sheetState.selectedTracks.clear()
                                            if (!already) sheetState.selectedTracks.add(track)
                                        } else {
                                            if (sheetState.selectedTracks.any { it.id == track.id }) {
                                                sheetState.selectedTracks.removeAll { it.id == track.id }
                                            } else {
                                                sheetState.selectedTracks.add(track)
                                            }
                                        }
                                    },
                                    isFullscreen = dragState.isFull,
                                    onBack = { dragState.requestDismiss() },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                val showThinHandleOverlay = !(sheetState.selectedTab == AttachTab.GALLERY && sheetState.selectedMedia.isNotEmpty() && !dragState.isExpanded95) &&
                    sheetState.selectedTab != AttachTab.LOCATION
                if (showThinHandleOverlay) {
                    val isThinHandle = (sheetState.selectedTab == AttachTab.GALLERY || sheetState.selectedTab == AttachTab.MUSIC)
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .height(18.dp)
                            .graphicsLayer {
                                alpha = ((dragState.offsetPx - H * 0.03f) / (H * 0.05f)).coerceIn(0f, 1f)
                            }
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { d ->
                                    val maxOffset = if (sheetState.totalSelectedCount > 0) dragState.halfOffset + 120f else H
                                    dragState.offsetPx = (dragState.offsetPx + d).coerceIn(0f, maxOffset)
                                },
                                onDragStarted = { dragState.animJob?.cancel() },
                                onDragStopped = { v -> dragState.release(v) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(if (isThinHandle) 24.dp else 28.dp)
                                .height(if (isThinHandle) 1.5.dp else 2.5.dp)
                                .clip(CircleShape)
                                .background(scheme.textMuted.copy(alpha = if (isThinHandle) 0.25f else 0.35f))
                        )
                    }
                }

                // Плавающая шапка для Галереи (с 95%) и Геопозиции (с 80%)
                if (sheetState.selectedTab == AttachTab.GALLERY && dragState.isExpanded95) {
                    val alpha = ((H * 0.05f - dragState.offsetPx) / (H * 0.05f)).coerceIn(0f, 1f)
                    GalleryFloatingTopBar(
                        selectedAlbum = sheetState.selectedAlbum,
                        selectedMedia = sheetState.selectedMedia,
                        attachStrings = attachStrings,
                        topBarAlpha = alpha,
                        isAlbumsSheetOpen = sheetState.isAlbumsSheetOpen,
                        onToggleAlbumsSheet = { sheetState.isAlbumsSheetOpen = !sheetState.isAlbumsSheetOpen },
                        onBack = { dragState.requestDismiss() },
                        onOpenPreviewOrder = { sheetState.isPreviewOrderOpen = true },
                        onLaunchSystemChooser = { systemChooserLauncher.launch(it) },
                        onToggleSpoiler = { toggleSpoilerForMediaSelection(sheetState.selectedMedia) },
                        onSendHd = {
                            val drafts = buildGalleryMediaDrafts(
                                context = context,
                                mediaList = sheetState.selectedMedia,
                                caption = sheetState.captionText,
                                hd = true
                            )
                            if (drafts.isNotEmpty()) {
                                onSend(drafts)
                                dragState.dismissAnimated()
                            }
                        }
                    )
                } else if (sheetState.selectedTab == AttachTab.LOCATION && dragState.isLocationHeaderVisible) {
                    val alpha = ((H * 0.20f - dragState.offsetPx) / (H * 0.12f)).coerceIn(0f, 1f)
                    LocationFloatingTopBar(
                        attachStrings = attachStrings,
                        searchQuery = sheetState.locationSearchQuery,
                        isSearchActive = sheetState.isLocationSearchActive,
                        topBarAlpha = alpha,
                        onBack = { dragState.requestDismiss() },
                        onSearchQueryChange = { sheetState.locationSearchQuery = it },
                        onToggleSearch = {
                            sheetState.isLocationSearchActive = !sheetState.isLocationSearchActive
                            if (!sheetState.isLocationSearchActive) sheetState.locationSearchQuery = ""
                        }
                    )
                }

                // Нижняя парящая панель вкладок и ввода
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .imePadding()
                        .graphicsLayer {
                            translationY = -minOf(dragState.offsetPx, dragState.halfOffset)
                        }
                ) {
                    AttachTabBar(
                        selectedTab = sheetState.selectedTab,
                        onSelectTab = { newTab ->
                            val wasFullLocation = (sheetState.selectedTab == AttachTab.LOCATION && dragState.offsetPx <= H * 0.08f)
                            sheetState.selectedTab = newTab
                            if (wasFullLocation && newTab != AttachTab.LOCATION) {
                                dragState.animateTo(dragState.halfOffset, 260)
                            }
                        },
                        visible = !isSingleReplaceMode,
                        modifier = Modifier.graphicsLayer {
                            val isCardOrSelectionOpen = sheetState.totalSelectedCount > 0 || sheetState.isQuickContactOpen
                            val topNavP = if (sheetState.selectedTab == AttachTab.LOCATION) 0f else ((H * 0.08f - dragState.offsetPx) / (H * 0.08f)).coerceIn(0f, 1f)
                            alpha = if (isCardOrSelectionOpen) 0f else (1f - topNavP)
                            translationY = if (isCardOrSelectionOpen) 200f else (200f * topNavP)
                        }
                    )

                    AttachInputBar(
                        caption = sheetState.captionText,
                        onCaptionChange = { sheetState.captionText = it },
                        selectedCount = sheetState.totalSelectedCount,
                        visible = sheetState.totalSelectedCount > 0,
                        onSend = { sendAllSelected() },
                        isEmojiOpen = sheetState.isAttachEmojiOpen,
                        onToggleEmoji = {
                            if (!sheetState.isAttachEmojiOpen && dragState.offsetPx > 0f) {
                                dragState.animateTo(0f, 240)
                            }
                            sheetState.isAttachEmojiOpen = !sheetState.isAttachEmojiOpen
                        },
                        onCloseEmoji = { sheetState.isAttachEmojiOpen = false },
                        modifier = Modifier.graphicsLayer {
                            alpha = if (sheetState.totalSelectedCount > 0) 1f else 0f
                            translationY = if (sheetState.totalSelectedCount > 0) 0f else 200f
                        }
                    )
                }
            }
        }

        // Оверлеи Галереи
        GalleryOverlays(
            chatId = chatId,
            recipientName = recipientName,
            context = context,
            sheetState = sheetState,
            dragState = dragState,
            onSend = onSend,
            onSendAllSelected = { sendAllSelected() }
        )

        // Полноэкранный раздел «Галерея без сжатия» (Файлы -> Галерея)
        AnimatedVisibility(
            visible = sheetState.isUncompressedGalleryOpen,
            enter = slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(260)),
            exit = slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(220)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(115f)
        ) {
            FilesGallerySubScreen(
                isFullscreen = true,
                onBack = { sheetState.isUncompressedGalleryOpen = false },
                onSelectMediaAsFile = { media ->
                    val meta = getFileMetadataHelper(context, media.uri)
                    onSend(
                        listOf(
                            AttachmentDraft.File(
                                uri = media.uri,
                                name = meta.name,
                                size = meta.size
                            )
                        )
                    )
                    sheetState.isUncompressedGalleryOpen = false
                    dragState.dismissAnimated()
                },
                selectedMedia = sheetState.uncompressedGalleryPicked,
                onToggleMedia = { media ->
                    val idx = sheetState.uncompressedGalleryPicked.indexOfFirst { it.uri == media.uri }
                    if (idx >= 0) sheetState.uncompressedGalleryPicked.removeAt(idx) else sheetState.uncompressedGalleryPicked.add(media)
                },
                caption = sheetState.uncompressedGalleryCaption,
                onCaptionChange = { sheetState.uncompressedGalleryCaption = it },
                onSendSelected = {
                    val drafts = buildUncompressedGalleryDrafts(
                        context = context,
                        pickedMedia = sheetState.uncompressedGalleryPicked,
                        caption = sheetState.uncompressedGalleryCaption
                    )
                    sheetState.uncompressedGalleryPicked.clear()
                    sheetState.uncompressedGalleryCaption = ""
                    sheetState.isUncompressedGalleryOpen = false
                    onSend(drafts)
                    dragState.dismissAnimated()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (sheetState.isFullPlayerOpen) {
            FullPlayerSheet(
                onDismiss = { sheetState.isFullPlayerOpen = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (sheetState.showDiscardDialog) {
            AttachDiscardDialog(
                onDismiss = { sheetState.showDiscardDialog = false },
                onConfirmDiscard = {
                    sheetState.showDiscardDialog = false
                    sheetState.clearAllSelected()
                    dragState.forceDismiss()
                }
            )
        }

        if (sheetState.quickCardContact != null) {
            ContactQuickCardSheet(
                contact = sheetState.quickCardContact!!,
                onSelectContact = { contactDraft ->
                    onSend(listOf(contactDraft))
                    sheetState.quickCardContact = null
                    dragState.dismissAnimated()
                },
                onDismiss = { sheetState.quickCardContact = null }
            )
        }
    }
}
