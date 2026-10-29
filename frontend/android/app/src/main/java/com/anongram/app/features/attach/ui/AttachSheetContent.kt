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
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import com.anongram.app.core.ui.safeStatusBarPadding
import android.app.Activity
import android.content.Intent
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
import kotlinx.coroutines.launch

@Composable
fun AttachSheetContent(
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
        fun close() = animateTo(H, 240, isFling = true) { currentOnDismiss() }
        fun release(velocityY: Float) {
            anim?.cancel()
            // velocityY < 0 = свайп вверх, velocityY > 0 = свайп вниз
            if (velocityY < -350f && offsetPx > 0f) {
                // Резкий свайп вверх: аккуратно и плавно до конца 100 едет без фризов
                animateTo(0f, ms = 260, isFling = true)
            } else if (velocityY > 350f) {
                // Резкий свайп вниз:
                if (offsetPx < halfOffset - 20f) {
                    // Из полноэкранного режима вниз ВСЕГДА возвращается на 50%!
                    animateTo(halfOffset, ms = 260, isFling = true)
                } else {
                    // С 50% закрывает с мягкой анимацией
                    close()
                }
            } else {
                // Плавный свайп пальцем (свободное перемещение и мягкая доводка):
                when {
                    offsetPx < halfOffset * 0.5f -> {
                        // Провели вверх дальше половины пути — плавно доводим до 100%
                        animateTo(0f, ms = 240, isFling = false)
                    }
                    offsetPx <= halfOffset + H * 0.06f -> {
                        // Около 50% — ровно фиксируем на 50%
                        animateTo(halfOffset, ms = 200, isFling = false)
                    }
                    else -> {
                        // Оттянули вниз ниже половины — закрываем
                        close()
                    }
                }
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
            isClosing = true
            try {
                AudioPlayerController.getInstance(context).stop()
            } catch (_: Exception) {}

            if (wasKeyboardOpen) {
                onRestoreKeyboard()
            }
            close()
        }

    var selectedTab by remember { mutableStateOf(AttachTab.GALLERY) }
    var selectedAlbum by remember { mutableStateOf("Все медиа") }
    var isAlbumsSheetOpen by remember { mutableStateOf(false) }
    var quickCardContact by remember { mutableStateOf<DeviceContact?>(null) }
    val isQuickContactOpen by remember { derivedStateOf { quickCardContact != null } }

    // Выбранные элементы по всем вкладкам (ТЗ, шаг 11)
    val selectedMedia = remember { mutableStateListOf<GalleryMedia>() }
    val selectedFiles = remember { mutableStateListOf<DeviceFileItem>() }
    val selectedTracks = remember { mutableStateListOf<Track>() }
    val selectedContacts = remember { mutableStateListOf<DeviceContact>() }
    val capturedMediaList = remember { mutableStateListOf<GalleryMedia>() }
    var captionText by remember { mutableStateOf("") }

    val totalSelectedCount = selectedMedia.size + selectedFiles.size + selectedTracks.size + selectedContacts.size

    fun sendAllSelected() {
        if (totalSelectedCount == 0) return
        val drafts = mutableListOf<AttachmentDraft>()

        selectedMedia.forEachIndexed { index, media ->
            drafts.add(
                AttachmentDraft.Media(
                    uri = media.uri,
                    isVideo = media.isVideo,
                    caption = if (index == 0) captionText else ""
                )
            )
        }

        selectedFiles.forEachIndexed { index, file ->
            drafts.add(
                AttachmentDraft.File(
                    uri = file.uri,
                    name = file.name,
                    size = file.size,
                    caption = if (index == 0) captionText else ""
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
                    caption = if (index == 0) captionText else ""
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
    var editorItems by remember { mutableStateOf<List<EditableMediaItem>>(emptyList()) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var isMoreMenuOpen by remember { mutableStateOf(false) }

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
                caption = if (index == 0) captionText else ""
            )
        }
        onSend(drafts)
        dismissAnimated()
    }

    fun sendAsHd() {
        if (selectedMedia.isEmpty()) return
        val drafts = selectedMedia.mapIndexed { index, media ->
            AttachmentDraft.Media(
                uri = media.uri,
                isVideo = media.isVideo,
                caption = if (index == 0) captionText else "",
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
        captionText = ""
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
            showDiscardDialog -> showDiscardDialog = false
            quickCardContact != null -> quickCardContact = null
            isAlbumsSheetOpen -> isAlbumsSheetOpen = false
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
                val dy = available.y
                if (dy < 0f && offsetPx > 0f) {        // палец вверх: растёт шторка
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
                val dy = available.y
                if (dy > 0f) {                         // палец вниз, список уже в начале
                    anim?.cancel()
                    val new = (offsetPx + dy).coerceAtMost(H)
                    val used = new - offsetPx
                    offsetPx = new
                    return Offset(0f, used)
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (offsetPx > 0f) {
                    release(available.y)
                    return available
                }
                return Velocity.Zero
            }
            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 0f) {
                    release(available.y)
                    return available
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
                // ручка: тянуть можно за неё
                Box(
                    Modifier.fillMaxWidth().height(16.dp)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { d ->
                                offsetPx = (offsetPx + d).coerceIn(0f, H)
                            },
                            onDragStarted = { anim?.cancel() },
                            onDragStopped = { v -> release(v) }
                        )
                        .graphicsLayer {
                            val fadeProgress = ((offsetPx - H * 0.03f) / (H * 0.05f)).coerceIn(0f, 1f)
                            alpha = fadeProgress
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val isGallery = (selectedTab == AttachTab.GALLERY)
                    Box(
                        modifier = Modifier
                            .width(if (isGallery) 24.dp else 28.dp)
                            .height(if (isGallery) 1.5.dp else 2.5.dp)
                            .clip(CircleShape)
                            .background(if (isGallery) Color(0x33FFFFFF) else Color(0x4DFFFFFF))
                    )
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

        // 2. Вуаль-градиент за плавающей шапкой в полном экране ТОЛЬКО в Галерее
        val showGalleryScrim = (selectedTab == AttachTab.GALLERY)
        if (showGalleryScrim) {
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val headerScrimHeight = statusBarTop + 68.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(headerScrimHeight)
                    .zIndex(9f)
                    .graphicsLayer {
                        val p = ((H * 0.08f - offsetPx) / (H * 0.08f)).coerceIn(0f, 1f)
                        alpha = p
                    }
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.85f),
                            0.65f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Transparent
                        )
                    )
            )
        }

        // 4. Плавающая шапка
        val showTopBar = (selectedTab == AttachTab.GALLERY || selectedTab == AttachTab.LOCATION)
        if (showTopBar) {
            AttachTopBar(
                title = selectedTab.title,
                onBack = { requestDismiss() },
                hasActionMenu = (selectedTab == AttachTab.GALLERY),
                onActionClick = { isMoreMenuOpen = true },
                actionMenuContent = {
                    DropdownMenu(
                        expanded = isMoreMenuOpen,
                        onDismissRequest = { isMoreMenuOpen = false },
                        modifier = Modifier
                            .background(AttachCardDark)
                            .border(1.dp, AttachBorder, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Показать списком", color = Color.White, fontSize = 14.sp) },
                            onClick = { isMoreMenuOpen = false },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = AttachPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Открыть в...", color = Color.White, fontSize = 14.sp) },
                            onClick = {
                                isMoreMenuOpen = false
                                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                    type = "*/*"
                                    addCategory(Intent.CATEGORY_OPENABLE)
                                    putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                                }
                                systemChooserLauncher.launch(Intent.createChooser(intent, "Открыть с помощью"))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Folder,
                                    contentDescription = null,
                                    tint = AttachPurple,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        if (selectedMedia.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Отправить без сжатия", color = Color.White, fontSize = 14.sp) },
                                onClick = {
                                    isMoreMenuOpen = false
                                    sendAsUncompressed()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.InsertDriveFile,
                                        contentDescription = null,
                                        tint = AttachPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Отправить в HD качестве", color = Color.White, fontSize = 14.sp) },
                                onClick = {
                                    isMoreMenuOpen = false
                                    sendAsHd()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = null,
                                        tint = AttachPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text("Файлы (без сжатия)", color = Color.White, fontSize = 14.sp) },
                                onClick = {
                                    isMoreMenuOpen = false
                                    uncompressedPickerLauncher.launch("image/*")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.InsertDriveFile,
                                        contentDescription = null,
                                        tint = AttachPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                },
                centerContent = if (selectedTab == AttachTab.GALLERY) {
                    {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable { isAlbumsSheetOpen = !isAlbumsSheetOpen }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedAlbum,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                modifier = Modifier.weight(1f)
                            )
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
                        val p = ((H * 0.08f - offsetPx) / (H * 0.08f)).coerceIn(0f, 1f)
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
                modifier = Modifier.graphicsLayer {
                    alpha = if (totalSelectedCount > 0) 1f else 0f
                    translationY = if (totalSelectedCount > 0) 0f else 200f
                }
            )
        }
    }
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
                                        dismissAnimated()
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

