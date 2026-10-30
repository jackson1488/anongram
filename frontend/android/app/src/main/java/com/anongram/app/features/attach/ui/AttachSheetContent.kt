package com.anongram.app.features.attach.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.attach.ui.components.PreviewOrderScreen
import android.app.Activity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitVerticalDragOrCancellation
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.channels.Channel
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateListOf
import com.anongram.app.features.attach.api.AttachmentDraft
import com.anongram.app.features.attach.ui.components.AttachInputBar
import com.anongram.app.features.attach.ui.components.AttachTabBar
import com.anongram.app.features.attach.ui.components.AttachTopBar
import com.anongram.app.features.attach.ui.tabs.ContactQuickCardSheet
import com.anongram.app.features.attach.ui.tabs.ContactsTab
import com.anongram.app.features.attach.ui.tabs.DeviceContact
import com.anongram.app.features.attach.ui.tabs.DeviceFileItem
import com.anongram.app.features.attach.ui.tabs.FilesGallerySubScreen
import com.anongram.app.features.attach.ui.tabs.FilesTab
import com.anongram.app.features.attach.ui.tabs.GalleryMedia
import com.anongram.app.features.attach.ui.tabs.GalleryTab
import com.anongram.app.features.attach.ui.tabs.LocationTab
import com.anongram.app.features.attach.ui.tabs.MusicTab
import com.anongram.app.features.attach.ui.theme.AttachPurple
import com.anongram.app.features.attach.ui.theme.AttachSheetBackground
import com.anongram.app.features.camera.api.CameraScreen
import com.anongram.app.features.mediaeditor.api.EditableMediaItem
import com.anongram.app.features.mediaeditor.api.MediaEditorScreen
import com.anongram.app.features.player.api.FullPlayerSheet
import com.anongram.app.features.player.api.Track
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.anongram.app.features.attach.ui.theme.AttachBorder
import com.anongram.app.features.attach.ui.theme.AttachCardDark
import com.anongram.app.features.player.logic.AudioPlayerController
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.mutableIntStateOf
import com.anongram.app.features.attach.ui.tabs.AlbumDropdownRowItem
import com.anongram.app.features.attach.ui.tabs.GalleryAlbum
import com.anongram.app.features.attach.ui.tabs.GalleryMediaViewer
import kotlinx.coroutines.launch

@Composable
fun AttachSheetContent(
    chatId: String = "",
    recipientName: String = "",
    onDismiss: () -> Unit,
    onSend: (List<AttachmentDraft>) -> Unit,
    keyboardHeightPx: Int = 0,
    wasKeyboardOpen: Boolean = false,
    onRestoreKeyboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    BoxWithConstraints(modifier.fillMaxSize()) {
        val H = constraints.maxHeight.toFloat()
        val halfOffset = H * 0.5f   // шторка видна на 50%
        val openOffset = H * 0.45f  // открывается на 55%

        // 0 = полностью открыта, H = закрыта
        var offsetPx by remember(H) { mutableFloatStateOf(H) }
        val scope = rememberCoroutineScope()
        var anim by remember { mutableStateOf<Job?>(null) }
        var isClosing by remember { mutableStateOf(false) }

        // Выбранные элементы по всем вкладкам (ТЗ, шаг 11)
        val selectedMedia = remember { mutableStateListOf<GalleryMedia>() }
        val selectedFiles = remember { mutableStateListOf<DeviceFileItem>() }
        val selectedTracks = remember { mutableStateListOf<Track>() }
        val selectedContacts = remember { mutableStateListOf<DeviceContact>() }
        val capturedMediaList = remember { mutableStateListOf<GalleryMedia>() }
        var captionText by remember { mutableStateOf("") }
        var showDiscardDialog by remember { mutableStateOf(false) }

        val totalSelectedCount by remember {
            derivedStateOf { selectedMedia.size + selectedFiles.size + selectedTracks.size + selectedContacts.size }
        }

        var availableAlbums by remember { mutableStateOf<List<GalleryAlbum>>(emptyList()) }
        var viewingMediaList by remember { mutableStateOf<List<GalleryMedia>?>(null) }
        var viewingMediaIndex by remember { mutableIntStateOf(0) }

        val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
        val SmoothInOutEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

        fun animateTo(target: Float, ms: Int = 260, isFling: Boolean = false, end: () -> Unit = {}) {
            anim?.cancel()
            val easing = if (isFling) DecelerateEasing else SmoothInOutEasing
            anim = scope.launch {
                animate(
                    initialValue = offsetPx,
                    targetValue = target,
                    animationSpec = tween(ms, easing = easing)
                ) { v, _ -> offsetPx = v }
                end()
            }
        }

        fun forceDismiss() {
            if (isClosing) return
            isClosing = true
            try {
                AudioPlayerController.getInstance(context).stop()
            } catch (_: Exception) {}
            if (wasKeyboardOpen) {
                onRestoreKeyboard()
            }
            animateTo(H, 240, isFling = true) { currentOnDismiss() }
        }

        fun close() {
            if (totalSelectedCount > 0) {
                showDiscardDialog = true
                animateTo(halfOffset, 200, isFling = false)
                return
            }
            forceDismiss()
        }

        fun release(velocityY: Float) {
            anim?.cancel()
            // Плавное отпускание и скролл (плавучая шторка от 50% до 100%):
            // В диапазоне от 50% до 100% (0..halfOffset) шторка фиксируется ровно там, где остановился палец (хоть 56%, хоть 80%).
            if (offsetPx <= halfOffset) {
                offsetPx = offsetPx.coerceIn(0f, halfOffset)
                return
            }

            // Ниже 50% (палец потянул шторку вниз ниже половины экрана):
            if (velocityY > 2000f || offsetPx > halfOffset + 80f) {
                if (totalSelectedCount > 0) {
                    showDiscardDialog = true
                    animateTo(halfOffset, ms = 200, isFling = true)
                } else {
                    close()
                }
            } else {
                animateTo(halfOffset, ms = 200, isFling = false)
            }
        }

        LaunchedEffect(H) {
            if (wasKeyboardOpen && keyboardHeightPx > 0) {
                launch {
                    snapshotFlow { H - offsetPx }
                        .first { it >= keyboardHeightPx * 0.90f && it > 0 }
                    keyboardController?.hide()
                }
            }
            animateTo(openOffset, 300)
        }

        val isFull by remember { derivedStateOf { offsetPx <= 20f } }

        fun dismissAnimated() {
            if (isClosing) return
            if (totalSelectedCount > 0) {
                showDiscardDialog = true
                animateTo(halfOffset, 200, isFling = false)
                return
            }
            forceDismiss()
        }

    var selectedTab by remember { mutableStateOf(AttachTab.GALLERY) }
    var selectedAlbum by remember { mutableStateOf("Все медиа") }
    var isAlbumsSheetOpen by remember { mutableStateOf(false) }

    LaunchedEffect(offsetPx) {
        if (offsetPx > 10f && isAlbumsSheetOpen) {
            isAlbumsSheetOpen = false
        }
    }
    var quickCardContact by remember { mutableStateOf<DeviceContact?>(null) }
    val isQuickContactOpen by remember { derivedStateOf { quickCardContact != null } }

    fun sendAllSelected() {
        if (totalSelectedCount == 0) return
        val drafts = mutableListOf<AttachmentDraft>()

        selectedMedia.forEachIndexed { index, media ->
            drafts.add(
                AttachmentDraft.Media(
                    uri = media.uri,
                    isVideo = media.isVideo,
                    caption = if (index == selectedMedia.lastIndex) captionText else "",
                    hasSpoiler = media.hasSpoiler
                )
            )
        }

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
                    phones = contact.phones
                )
            )
        }

        onSend(drafts)
        dismissAnimated()
    }

    // Overlays
    var isCameraOpen by remember { mutableStateOf(false) }
    var isEditorOpen by remember { mutableStateOf(false) }
    var isFullPlayerOpen by remember { mutableStateOf(false) }
    var isPreviewOrderOpen by remember { mutableStateOf(false) }
    var isUncompressedGalleryOpen by remember { mutableStateOf(false) }
    val uncompressedGalleryPicked = remember { mutableStateListOf<GalleryMedia>() }
    var uncompressedGalleryCaption by remember { mutableStateOf("") }
    var isGalleryOptionsMenuOpen by remember { mutableStateOf(false) }
    var editorItems by remember { mutableStateOf<List<EditableMediaItem>>(emptyList()) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    var isAttachEmojiOpen by remember { mutableStateOf(false) }

    val systemChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data
            val uris = mutableListOf<Uri>()
            data?.clipData?.let { clip ->
                for (i in 0 until clip.itemCount) {
                    clip.getItemAt(i)?.uri?.let { uris.add(it) }
                }
            } ?: data?.data?.let { uri ->
                uris.add(uri)
            }
            if (uris.isNotEmpty()) {
                val drafts = uris.map { uri ->
                    val mime = try {
                        context.contentResolver.getType(uri) ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                    if (mime.startsWith("video")) {
                        AttachmentDraft.Media(uri = uri, isVideo = true)
                    } else if (mime.startsWith("image")) {
                        AttachmentDraft.Media(uri = uri, isVideo = false)
                    } else {
                        val meta = getFileMetadataHelper(context, uri)
                        AttachmentDraft.File(uri = uri, name = meta.name, size = meta.size)
                    }
                }
                onSend(drafts)
                dismissAnimated()
            }
        }
    }

    val systemPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val drafts = uris.map { uri ->
                val mime = try {
                    context.contentResolver.getType(uri) ?: ""
                } catch (_: Exception) {
                    ""
                }
                if (mime.startsWith("video")) {
                    AttachmentDraft.Media(uri = uri, isVideo = true)
                } else if (mime.startsWith("image")) {
                    AttachmentDraft.Media(uri = uri, isVideo = false)
                } else {
                    val meta = getFileMetadataHelper(context, uri)
                    AttachmentDraft.File(uri = uri, name = meta.name, size = meta.size)
                }
            }
            onSend(drafts)
            dismissAnimated()
        }
    }

    val uncompressedPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val drafts = uris.map { uri ->
                val meta = getFileMetadataHelper(context, uri)
                AttachmentDraft.File(uri = uri, name = meta.name, size = meta.size)
            }
            onSend(drafts)
            dismissAnimated()
        }
    }

    val systemGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (!uris.isNullOrEmpty()) {
            val drafts = uris.map { uri ->
                val mime = try {
                    context.contentResolver.getType(uri) ?: ""
                } catch (_: Exception) {
                    ""
                }
                AttachmentDraft.Media(uri = uri, isVideo = mime.startsWith("video"))
            }
            onSend(drafts)
            dismissAnimated()
        }
    }

    fun sendAsUncompressed() {
        if (selectedMedia.isEmpty()) return
        val drafts = selectedMedia.mapIndexed { index, media ->
            val meta = getFileMetadataHelper(context, media.uri)
            AttachmentDraft.File(
                uri = media.uri,
                name = meta.name,
                size = meta.size,
                caption = if (index == selectedMedia.lastIndex) captionText else ""
            )
        }
        onSend(drafts)
        dismissAnimated()
    }

    fun sendAsUngrouped() {
        if (selectedMedia.isEmpty()) return
        val drafts = selectedMedia.mapIndexed { index, media ->
            AttachmentDraft.Media(
                uri = media.uri,
                isVideo = media.isVideo,
                caption = if (index == selectedMedia.lastIndex) captionText else "",
                hasSpoiler = media.hasSpoiler,
                ungrouped = true
            )
        }
        onSend(drafts)
        dismissAnimated()
    }

    fun toggleSpoilerForSelectedMedia() {
        if (selectedMedia.isEmpty()) return
        val allHaveSpoiler = selectedMedia.all { it.hasSpoiler }
        val target = !allHaveSpoiler
        val updated = selectedMedia.map { it.copy(hasSpoiler = target) }
        selectedMedia.clear()
        selectedMedia.addAll(updated)
    }

    fun sendAsHd() {
        if (selectedMedia.isEmpty()) return
        val drafts = selectedMedia.mapIndexed { index, media ->
            AttachmentDraft.Media(
                uri = media.uri,
                isVideo = media.isVideo,
                caption = if (index == selectedMedia.lastIndex) captionText else "",
                hasSpoiler = media.hasSpoiler,
                hd = true
            )
        }
        onSend(drafts)
        dismissAnimated()
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

    fun requestDismiss() {
        if (totalSelectedCount > 0) {
            showDiscardDialog = true
        } else {
            dismissAnimated()
        }
    }

    // Обработка кнопки "Назад" (закрывает шторку с любой высоты, либо показывает диалог сброса если есть выбранные)
    BackHandler {
        when {
            isUncompressedGalleryOpen -> isUncompressedGalleryOpen = false
            isPreviewOrderOpen -> isPreviewOrderOpen = false
            viewingMediaList != null -> viewingMediaList = null
            showDiscardDialog -> showDiscardDialog = false
            isAttachEmojiOpen -> isAttachEmojiOpen = false
            quickCardContact != null -> quickCardContact = null
            isAlbumsSheetOpen -> isAlbumsSheetOpen = false
            isGalleryOptionsMenuOpen -> isGalleryOptionsMenuOpen = false
            isMoreMenuOpen -> isMoreMenuOpen = false
            isCameraOpen -> isCameraOpen = false
            isEditorOpen -> isEditorOpen = false
            isFullPlayerOpen -> isFullPlayerOpen = false
            else -> requestDismiss()
        }
    }

    val connection = remember(H) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (isAlbumsSheetOpen) isAlbumsSheetOpen = false
                val dy = available.y
                if (dy < 0f && offsetPx > 0f) {        // палец вверх: растёт шторка плавно до любой высоты
                    anim?.cancel()
                    val new = (offsetPx + dy).coerceAtLeast(0f)
                    val used = new - offsetPx
                    offsetPx = new
                    return Offset(0f, used)
                }
                return Offset.Zero
            }
            override fun onPostScroll(
                consumed: Offset, available: Offset, source: NestedScrollSource
            ): Offset {
                if (isAlbumsSheetOpen) isAlbumsSheetOpen = false
                val dy = available.y
                if (dy > 0f) {                         // палец вниз, список уже в начале
                    anim?.cancel()
                    val maxOffset = if (totalSelectedCount > 0) halfOffset + 120f else H
                    val new = (offsetPx + dy).coerceAtMost(maxOffset)
                    val used = new - offsetPx
                    offsetPx = new
                    return Offset(0f, used)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                // Не перехватываем флинг в диапазоне 50%..100%, чтобы шторка останавливалась в любом положении (56%, 80% и т.д.)
                return Velocity.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (offsetPx > halfOffset) {
                    release(available.y)
                }
                return Velocity.Zero
            }
        }
    }

    // Затемнение
    Box(
        Modifier.fillMaxSize()
            .graphicsLayer {
                alpha = ((H - offsetPx) / H * 0.70f).coerceIn(0f, 0.70f)
            }
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { requestDismiss() }
    )

    // Шторка: ВСЕГДА на всю высоту, двигается только translationY
    @OptIn(ExperimentalFoundationApi::class)
    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { translationY = offsetPx }
                .nestedScroll(connection)
        ) {
            Column(
                Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(AttachSheetBackground)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* поглощаем клики внутри шторки */ }
            ) {
                // В полноэкранном режиме учитываем верхний статус-бар
                val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
                val topHandlePad = if (isFull) statusBarTop else 0.dp
                if (topHandlePad > 0.dp) {
                    Spacer(modifier = Modifier.height(topHandlePad))
                }

                // В полураскрытом состоянии Галереи: тонкая полоска ручки (18.dp)
                // При выборе медиа (только когда шторка ниже 95%): сверху появляется компактная полоса выбора (44.dp)
                val isExpanded95 = offsetPx <= H * 0.05f
                val isGallery = (selectedTab == AttachTab.GALLERY)
                val isGalleryWithSelection = (isGallery && selectedMedia.isNotEmpty() && !isExpanded95)
                val handleHeight = if (isGalleryWithSelection) 44.dp else 18.dp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(handleHeight)
                        .background(AttachSheetBackground)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { d ->
                                val maxOffset = if (totalSelectedCount > 0) halfOffset + 120f else H
                                offsetPx = (offsetPx + d).coerceIn(0f, maxOffset)
                            },
                            onDragStarted = { anim?.cancel() },
                            onDragStopped = { v -> release(v) }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGalleryWithSelection) {
                        GallerySelectionBar(
                            title = getGallerySelectionTitle(selectedMedia),
                            hasMoreItems = selectedMedia.isNotEmpty(),
                            onOpenPreviewOrder = {
                                if (selectedMedia.isNotEmpty()) {
                                    isPreviewOrderOpen = true
                                }
                            },
                            onOptionsClick = { isGalleryOptionsMenuOpen = true },
                            onBack = null,
                            menuContent = {
                                GallerySelectionOptionsMenu(
                                    expanded = isGalleryOptionsMenuOpen,
                                    onDismissRequest = { isGalleryOptionsMenuOpen = false },
                                    canUngroup = selectedMedia.size >= 2,
                                    allHaveSpoiler = selectedMedia.all { it.hasSpoiler },
                                    onOpenPreview = {
                                        if (selectedMedia.isNotEmpty()) {
                                            isPreviewOrderOpen = true
                                        }
                                    },
                                    onSendUncompressed = { sendAsUncompressed() },
                                    onSendUngrouped = { sendAsUngrouped() },
                                    onToggleSpoiler = { toggleSpoilerForSelectedMedia() },
                                    onSendHd = { sendAsHd() }
                                )
                            }
                        )
                    } else {
                        // Тонкая полоска ручки: высота всегда фиксированная, сетка ниже не прыгает
                        val isThinHandle = (selectedTab == AttachTab.GALLERY || selectedTab == AttachTab.MUSIC)
                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    val fadeProgress = ((offsetPx - H * 0.03f) / (H * 0.05f)).coerceIn(0f, 1f)
                                    alpha = fadeProgress
                                }
                                .width(if (isThinHandle) 24.dp else 28.dp)
                                .height(if (isThinHandle) 1.5.dp else 2.5.dp)
                                .clip(CircleShape)
                                .background(if (isThinHandle) Color(0x33FFFFFF) else Color(0x4DFFFFFF))
                        )
                    }
                }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    AttachTab.GALLERY -> {
                        GalleryTab(
                            onLaunchCamera = { isCameraOpen = true },
                            onOpenEditor = { items ->
                                editorItems = items
                                isEditorOpen = true
                            },
                            selectedAlbum = selectedAlbum,
                            onSelectAlbum = { selectedAlbum = it },
                            isAlbumsSheetOpen = isAlbumsSheetOpen,
                            onCloseAlbumsSheet = { isAlbumsSheetOpen = false },
                            onOpenAlbumsSheet = { isAlbumsSheetOpen = true },
                            selectedMedia = selectedMedia,
                            onToggleSelectMedia = { media ->
                                isAlbumsSheetOpen = false
                                if (selectedMedia.any { it.uri == media.uri }) {
                                    selectedMedia.removeAll { it.uri == media.uri }
                                } else {
                                    if (selectedMedia.size < 99) {
                                        selectedMedia.add(media)
                                    }
                                }
                            },
                            caption = captionText,
                            onCaptionChange = { captionText = it },
                            onSend = { sendAllSelected() },
                            capturedMediaItems = capturedMediaList,
                            isFullscreen = isFull,
                            onOpenViewer = { list, idx ->
                                isAlbumsSheetOpen = false
                                viewingMediaList = list
                                viewingMediaIndex = idx
                            },
                            onAlbumsLoaded = { availableAlbums = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AttachTab.FILE -> {
                        FilesTab(
                            onSelectFile = { fileDraft ->
                                onSend(listOf(fileDraft))
                                dismissAnimated()
                            },
                            onExpandToFullscreen = {
                                animateTo(0f, 240)
                            },
                            selectedFiles = selectedFiles,
                            onToggleSelectFile = { file ->
                                if (selectedFiles.any { it.uri == file.uri }) {
                                    selectedFiles.removeAll { it.uri == file.uri }
                                } else {
                                    selectedFiles.add(file)
                                }
                            },
                            isFullscreen = isFull,
                            onBack = { requestDismiss() },
                            onSelectFiles = { drafts ->
                                onSend(drafts)
                                dismissAnimated()
                            },
                            onOpenUncompressedGallery = {
                                isUncompressedGalleryOpen = true
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AttachTab.LOCATION -> {
                        LocationTab(
                            onSendLocation = { locDraft ->
                                onSend(listOf(locDraft))
                                dismissAnimated()
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AttachTab.CONTACT -> {
                        ContactsTab(
                            onSelectContact = { contactDraft ->
                                onSend(listOf(contactDraft))
                                dismissAnimated()
                            },
                            onExpandToFullscreen = {
                                animateTo(0f, 240)
                            },
                            selectedContacts = selectedContacts,
                            onToggleSelectContact = { contact ->
                                if (selectedContacts.any { it.name == contact.name }) {
                                    selectedContacts.removeAll { it.name == contact.name }
                                } else {
                                    selectedContacts.add(contact)
                                }
                            },
                            isFullscreen = isFull,
                            onBack = { requestDismiss() },
                            onQuickCardVisibilityChange = {},
                            onOpenQuickCard = { quickCardContact = it },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    AttachTab.MUSIC -> {
                        MusicTab(
                            onSelectAudio = { audioDraft ->
                                onSend(listOf(audioDraft))
                                dismissAnimated()
                            },
                            onExpandPlayer = { isFullPlayerOpen = true },
                            onExpandToFullscreen = {
                                animateTo(0f, 240)
                            },
                            selectedTracks = selectedTracks,
                            onToggleSelectTrack = { track ->
                                if (selectedTracks.any { it.id == track.id }) {
                                    selectedTracks.removeAll { it.id == track.id }
                                } else {
                                    selectedTracks.add(track)
                                }
                            },
                            isFullscreen = isFull,
                            onBack = { requestDismiss() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

            }
        }

        // 4. Плавающая шапка для Галереи и Геопозиции (на 95%+ всегда показывает «Галерея ▾» и навигацию, никогда не «Выбрано»)
        val showTopBar = (selectedTab == AttachTab.LOCATION || selectedTab == AttachTab.GALLERY) && (offsetPx <= H * 0.05f)
        if (showTopBar) {
            AttachTopBar(
                title = if (selectedTab == AttachTab.GALLERY) (if (selectedAlbum == "Все медиа" || selectedAlbum == "Галерея" || selectedAlbum == "Все" || selectedAlbum.isBlank()) "Галерея" else selectedAlbum) else selectedTab.title,
                onBack = { requestDismiss() },
                hasActionMenu = (selectedTab == AttachTab.GALLERY),
                actionMenuContent = if (selectedTab == AttachTab.GALLERY) {
                    {
                        var isTopBarMenuOpen by remember { mutableStateOf(false) }
                        IconButton(onClick = { isTopBarMenuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "Меню", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = isTopBarMenuOpen,
                            onDismissRequest = { isTopBarMenuOpen = false },
                            modifier = Modifier
                                .background(Color(0xFF222226), RoundedCornerShape(16.dp))
                                .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                        ) {

                            if (selectedMedia.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Просмотреть сообщение", color = Color.White, fontSize = 14.5.sp)
                                            Spacer(modifier = Modifier.weight(1f))
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = Color(0xFF8E8E93),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    },
                                    onClick = {
                                        isTopBarMenuOpen = false
                                        isPreviewOrderOpen = true
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Outlined.Visibility, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Открыть в...", color = Color.White, fontSize = 14.5.sp) },
                                onClick = {
                                    isTopBarMenuOpen = false
                                    val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                        type = "*/*"
                                        addCategory(Intent.CATEGORY_OPENABLE)
                                        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                    }
                                    systemChooserLauncher.launch(Intent.createChooser(intent, "Открыть с помощью"))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            )
                            if (selectedMedia.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (selectedMedia.all { it.hasSpoiler }) "Отменить спойлер" else "Скрыть под спойлер",
                                            color = Color.White,
                                            fontSize = 14.5.sp
                                        )
                                    },
                                    onClick = {
                                        isTopBarMenuOpen = false
                                        toggleSpoilerForSelectedMedia()
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (selectedMedia.all { it.hasSpoiler }) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Отправить в HD-качестве", color = Color.White, fontSize = 14.5.sp) },
                                onClick = {
                                    isTopBarMenuOpen = false
                                    sendAsHd()
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.HighQuality, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            )
                        }
                    }
                } else null,
                centerContent = if (selectedTab == AttachTab.GALLERY) {
                    {
                        val displayAlbumTitle = if (selectedAlbum == "Все медиа" || selectedAlbum == "Галерея" || selectedAlbum == "Все" || selectedAlbum.isBlank()) "Галерея" else selectedAlbum
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isAlbumsSheetOpen = !isAlbumsSheetOpen }
                                .padding(horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayAlbumTitle,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Альбомы",
                                tint = AttachPurple,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                } else null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .safeStatusBarPadding()
                    .zIndex(10f)
                    .graphicsLayer {
                        val p = ((H * 0.05f - offsetPx) / (H * 0.05f)).coerceIn(0f, 1f)
                        alpha = p
                        translationY = -8.dp.toPx() * (1f - p)
                    }
            )
        }

        // 5. Нижняя панель вкладок и ввода (парящая над экраном, управляется через translationY)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .graphicsLayer {
                    translationY = -minOf(offsetPx, halfOffset)
                }
        ) {
            AttachTabBar(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                visible = true,
                modifier = Modifier.graphicsLayer {
                    val isCardOrSelectionOpen = totalSelectedCount > 0 || isQuickContactOpen
                    val topNavP = ((H * 0.08f - offsetPx) / (H * 0.08f)).coerceIn(0f, 1f)
                    alpha = if (isCardOrSelectionOpen) 0f else (1f - topNavP)
                    translationY = if (isCardOrSelectionOpen) 200f else (200f * topNavP)
                }
            )

            AttachInputBar(
                caption = captionText,
                onCaptionChange = { captionText = it },
                selectedCount = totalSelectedCount,
                visible = totalSelectedCount > 0,
                onSend = { sendAllSelected() },
                isEmojiOpen = isAttachEmojiOpen,
                onToggleEmoji = {
                    if (!isAttachEmojiOpen && offsetPx > 0f) {
                        animateTo(0f, 240)
                    }
                    isAttachEmojiOpen = !isAttachEmojiOpen
                },
                onCloseEmoji = { isAttachEmojiOpen = false },
                modifier = Modifier.graphicsLayer {
                    alpha = if (totalSelectedCount > 0) 1f else 0f
                    translationY = if (totalSelectedCount > 0) 0f else 200f
                }
            )
        }
    }
}

        // Выпадающее меню альбомов (адаптивное по высоте меню прямо под шапкой с высоким zIndex)
        if (isAlbumsSheetOpen && availableAlbums.isNotEmpty() && offsetPx <= H * 0.08f) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(50f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { isAlbumsSheetOpen = false }
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
                        .background(Color(0xFF212121))
                        .border(0.5.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
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
                        items(availableAlbums, key = { it.name }) { album ->
                            AlbumDropdownRowItem(
                                album = album,
                                isSelected = (album.name == selectedAlbum || (album.name == "Все медиа" && (selectedAlbum == "Все медиа" || selectedAlbum == "Галерея"))),
                                onClick = {
                                    selectedAlbum = album.name
                                    isAlbumsSheetOpen = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Полноэкранный просмотр фото со свайпом влево-вправо поверх всего экрана
        if (viewingMediaList != null) {
            GalleryMediaViewer(
                mediaList = viewingMediaList!!,
                initialIndex = viewingMediaIndex,
                selectedMedia = selectedMedia,
                onToggleSelect = { media ->
                    if (selectedMedia.any { it.uri == media.uri }) {
                        selectedMedia.removeAll { it.uri == media.uri }
                    } else {
                        if (selectedMedia.size < 99) {
                            selectedMedia.add(media)
                        }
                    }
                },
                caption = captionText,
                onCaptionChange = { captionText = it },
                onSend = { sendAllSelected() },
                onClose = { viewingMediaList = null },
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(100f)
            )
        }

        // Отдельный полноэкранный раздел «Галерея без сжатия» (Файлы -> Галерея) — не шторка, шапка строго на уровне остальных
        AnimatedVisibility(
            visible = isUncompressedGalleryOpen,
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
                onBack = { isUncompressedGalleryOpen = false },
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
                    isUncompressedGalleryOpen = false
                    dismissAnimated()
                },
                selectedMedia = uncompressedGalleryPicked,
                onToggleMedia = { media ->
                    val idx = uncompressedGalleryPicked.indexOfFirst { it.uri == media.uri }
                    if (idx >= 0) uncompressedGalleryPicked.removeAt(idx) else uncompressedGalleryPicked.add(media)
                },
                caption = uncompressedGalleryCaption,
                onCaptionChange = { uncompressedGalleryCaption = it },
                onSendSelected = {
                    val drafts = uncompressedGalleryPicked.mapIndexed { i, m ->
                        val meta = getFileMetadataHelper(context, m.uri)
                        AttachmentDraft.File(
                            uri = m.uri,
                            name = meta.name,
                            size = meta.size,
                            caption = if (i == 0) uncompressedGalleryCaption else ""
                        )
                    }
                    uncompressedGalleryPicked.clear()
                    uncompressedGalleryCaption = ""
                    isUncompressedGalleryOpen = false
                    onSend(drafts)
                    dismissAnimated()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Экран предпросмотра порядка и спойлера (ТЗ: Экран предпросмотра выбора сообщений)
        AnimatedVisibility(
            visible = isPreviewOrderOpen,
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
                initialMediaList = selectedMedia.toList(),
                caption = captionText,
                onCaptionChange = { captionText = it },
                onClose = { isPreviewOrderOpen = false },
                chatId = chatId,
                initialOffsetPx = offsetPx,
                onOffsetChange = { offsetPx = it },
                onDiscardSelection = {
                    selectedMedia.clear()
                    isPreviewOrderOpen = false
                    dismissAnimated()
                },
                onDismissAll = {
                    selectedMedia.clear()
                    isPreviewOrderOpen = false
                    dismissAnimated()
                },
                onUpdateOrderAndSpoilers = { newOrder ->
                    selectedMedia.clear()
                    selectedMedia.addAll(newOrder)
                },
                onSend = { orderedMedia, isUncompressed, isUngrouped, isHd ->
                    val drafts = mutableListOf<AttachmentDraft>()
                    orderedMedia.forEachIndexed { index, media ->
                        val isCaptionTarget = (index == orderedMedia.lastIndex)
                        if (isUncompressed) {
                            val meta = getFileMetadataHelper(context, media.uri)
                            drafts.add(
                                AttachmentDraft.File(
                                    uri = media.uri,
                                    name = meta.name,
                                    size = meta.size,
                                    caption = if (isCaptionTarget) captionText else ""
                                )
                            )
                        } else {
                            drafts.add(
                                AttachmentDraft.Media(
                                    uri = media.uri,
                                    isVideo = media.isVideo,
                                    caption = if (isCaptionTarget) captionText else "",
                                    hd = isHd,
                                    hasSpoiler = media.hasSpoiler,
                                    ungrouped = isUngrouped
                                )
                            )
                        }
                    }
                    onSend(drafts)
                    isPreviewOrderOpen = false
                    dismissAnimated()
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Окно Камеры (CameraX / Camera2, ТЗ шаг 5 и шаг 8)
        if (isCameraOpen) {
            CameraScreen(
                onResult = { media ->
                    isCameraOpen = false
                    // Снял фото/видео: попадает в выбор (в Галерею первым, уже с галочкой) и открывается на предпросмотр (ТЗ шаг 8)
                    val galleryMedia = GalleryMedia(
                        uri = media.uri,
                        isVideo = media.isVideo,
                        dateAdded = System.currentTimeMillis()
                    )
                    capturedMediaList.add(0, galleryMedia)
                    selectedMedia.add(0, galleryMedia)
                    selectedTab = AttachTab.GALLERY
                    editorItems = listOf(
                        EditableMediaItem(
                            uri = media.uri,
                            isVideo = media.isVideo
                        )
                    )
                    isEditorOpen = true
                },
                onClose = { isCameraOpen = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Редактор медиа (MediaEditorScreen)
        if (isEditorOpen) {
            MediaEditorScreen(
                items = editorItems,
                recipientName = recipientName,
                onSend = { editedList ->
                    val drafts = editedList.map { m ->
                        AttachmentDraft.Media(
                            uri = m.uri,
                            isVideo = m.isVideo,
                            caption = m.caption,
                            hd = m.hd,
                            ttl = m.ttl
                        )
                    }
                    onSend(drafts)
                    isEditorOpen = false
                    dismissAnimated()
                },
                onAddMore = {
                    isEditorOpen = false
                },
                onBack = {
                    isEditorOpen = false
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Нижнее окно полного плеера (ТЗ, шаг 7)
        if (isFullPlayerOpen) {
            FullPlayerSheet(
                onDismiss = { isFullPlayerOpen = false },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Диалог подтверждения сброса выбора при выходе (ТЗ: центрированный заголовок и текст, широкая красная кнопка Сбросить)
        if (showDiscardDialog) {
            Dialog(onDismissRequest = { showDiscardDialog = false }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.88f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF242424))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Сбросить выбор?",
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "Выбранные элементы не будут отправлены.",
                            color = Color(0xFFB0B0B0),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Кнопка Отмена
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(23.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .clickable { showDiscardDialog = false },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Отмена",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Кнопка Сбросить (широкая, красная, по центру)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(23.dp))
                                    .background(Color(0xFFDC2626))
                                    .clickable {
                                        showDiscardDialog = false
                                        clearAllSelected()
                                        forceDismiss()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Сбросить",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (quickCardContact != null) {
            ContactQuickCardSheet(
                contact = quickCardContact!!,
                onSend = { phoneToSend ->
                    onSend(
                        listOf(
                            AttachmentDraft.Contact(
                                name = quickCardContact!!.name,
                                phones = listOf(phoneToSend)
                            )
                        )
                    )
                    quickCardContact = null
                    dismissAnimated()
                },
                onDismiss = { quickCardContact = null }
            )
        }
    }
}

private data class FileMeta(val name: String, val size: Long)

private fun getFileMetadataHelper(context: Context, uri: Uri): FileMeta {
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

private fun getGallerySelectionTitle(items: List<GalleryMedia>): String {
    val count = items.size
    if (count == 0) return ""
    val hasVideo = items.any { it.isVideo }
    val hasPhoto = items.any { !it.isVideo }
    val isOnlyPhoto = hasPhoto && !hasVideo
    val isOnlyVideo = hasVideo && !hasPhoto

    return when {
        isOnlyPhoto -> {
            val mod10 = count % 10
            val mod100 = count % 100
            val word = when {
                mod100 in 11..19 -> "фотографий"
                mod10 == 1 -> "фотография"
                mod10 in 2..4 -> "фотографии"
                else -> "фотографий"
            }
            val verb = if (count == 1) "Выбрана" else "Выбрано"
            "$verb $count $word"
        }
        isOnlyVideo -> {
            val mod10 = count % 10
            val mod100 = count % 100
            val word = when {
                mod100 in 11..19 -> "видео"
                mod10 == 1 -> "видео"
                mod10 in 2..4 -> "видео"
                else -> "видео"
            }
            val verb = if (count == 1) "Выбрано" else "Выбрано"
            "$verb $count $word"
        }
        else -> {
            val mod10 = count % 10
            val mod100 = count % 100
            val word = when {
                mod100 in 11..19 -> "медиа"
                mod10 == 1 -> "медиа"
                mod10 in 2..4 -> "медиа"
                else -> "медиа"
            }
            "Выбрано $count $word"
        }
    }
}

@Composable
private fun GallerySelectionBar(
    title: String,
    hasMoreItems: Boolean,
    onOpenPreviewOrder: () -> Unit,
    onOptionsClick: () -> Unit,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    menuContent: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(18.dp))
                .clickable(enabled = hasMoreItems, onClick = onOpenPreviewOrder)
                .padding(vertical = 4.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (hasMoreItems) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFC084FC)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Предпросмотр",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onOptionsClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Опции",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            menuContent?.invoke()
        }
    }
}

@Composable
private fun GallerySelectionOptionsMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    allHaveSpoiler: Boolean,
    canUngroup: Boolean = true,
    onOpenPreview: () -> Unit,
    onSendUncompressed: () -> Unit,
    onSendUngrouped: () -> Unit,
    onToggleSpoiler: () -> Unit,
    onSendHd: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier
            .background(Color(0xFF222226), RoundedCornerShape(16.dp))
            .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
    ) {
        // 1. Просмотреть сообщение > (доступно всегда при выборе >= 1 фото)
        DropdownMenuItem(
            text = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Просмотреть сообщение", color = Color.White, fontSize = 14.5.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF8E8E93),
                        modifier = Modifier.size(16.dp)
                    )
                }
            },
            onClick = {
                onDismissRequest()
                onOpenPreview()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        // 2. Отправить без сжатия
        DropdownMenuItem(
            text = { Text("Отправить без сжатия", color = Color.White, fontSize = 14.5.sp) },
            onClick = {
                onDismissRequest()
                onSendUncompressed()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.InsertDriveFile,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        if (canUngroup) {
            // 3. Отправить без группировки
            DropdownMenuItem(
                text = { Text("Отправить без группировки", color = Color.White, fontSize = 14.5.sp) },
                onClick = {
                    onDismissRequest()
                    onSendUngrouped()
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.GridOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            )
        }

        // 4. Скрыть под спойлер / Отменить спойлер
        DropdownMenuItem(
            text = {
                Text(
                    text = if (allHaveSpoiler) "Отменить спойлер" else "Скрыть под спойлер",
                    color = Color.White,
                    fontSize = 14.5.sp
                )
            },
            onClick = {
                onDismissRequest()
                onToggleSpoiler()
            },
            leadingIcon = {
                Icon(
                    imageVector = if (allHaveSpoiler) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        )

        // 5. Отправить в HD-качестве
        DropdownMenuItem(
            text = { Text("Отправить в HD-качестве", color = Color.White, fontSize = 14.5.sp) },
            onClick = {
                onDismissRequest()
                onSendHd()
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}


