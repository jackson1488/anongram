package com.anongram.app.features.attach.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    // 1. Начальная высота окна: не менее 50%, и не меньше высоты клавиатуры
    val defaultInitial = remember(keyboardHeightPx, screenHeightPx) {
        if (screenHeightPx > 0 && keyboardHeightPx > 0) {
            val khFrac = keyboardHeightPx.toFloat() / screenHeightPx
            maxOf(0.50f, khFrac + 0.05f).coerceIn(0.50f, 0.85f)
        } else {
            0.50f
        }
    }

    val defaultInitialOffsetPx = remember(defaultInitial, screenHeightPx) {
        screenHeightPx * (1f - defaultInitial)
    }

    // 2. Положение шторки через Animatable (смещение в пикселях от 0 = fullscreen до screenHeightPx = закрыто)
    val sheetOffsetY = remember { Animatable(screenHeightPx) }
    var isClosing by remember { mutableStateOf(false) }

    // Порог 95% вычисляется через derivedStateOf: перекомпозиция происходит ТОЛЬКО при пересечении порога!
    val isFullscreen by remember {
        derivedStateOf { sheetOffsetY.value <= screenHeightPx * 0.05f }
    }

    fun animateToOffset(
        targetOffset: Float,
        duration: Int = 260,
        onEnd: () -> Unit = {}
    ) {
        scope.launch {
            sheetOffsetY.animateTo(
                targetValue = targetOffset,
                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
            )
            onEnd()
        }
    }

    fun dismissAnimated() {
        if (isClosing) return
        isClosing = true
        try {
            AudioPlayerController.getInstance(context).stop()
        } catch (_: Exception) {}

        // Если клавиатура была открыта до вызова шторки, начинаем показ клавиатуры под закрывающейся шторкой
        if (wasKeyboardOpen) {
            onRestoreKeyboard()
        }

        animateToOffset(screenHeightPx, duration = 240) {
            onDismiss()
        }
    }

    // Плавный выезд снизу вверх при открытии (~300 мс, FastOutSlowInEasing)
    LaunchedEffect(Unit) {
        if (wasKeyboardOpen && keyboardHeightPx > 0) {
            launch {
                // Системную клавиатуру скрываем ТОЛЬКО когда верх шторки перекрыл клавиатуру
                snapshotFlow { screenHeightPx - sheetOffsetY.value }
                    .first { it >= keyboardHeightPx * 0.90f && it > 0 }
                keyboardController?.hide()
            }
        }
        animateToOffset(defaultInitialOffsetPx, duration = 280)
    }

    val scrollChannel = remember { Channel<Float>(Channel.UNLIMITED) }
    LaunchedEffect(Unit) {
        for (delta in scrollChannel) {
            val current = sheetOffsetY.value
            val next = (current + delta).coerceIn(0f, screenHeightPx)
            sheetOffsetY.snapTo(next)
        }
    }

    var selectedTab by remember { mutableStateOf(AttachTab.GALLERY) }
    var selectedAlbum by remember { mutableStateOf("Все медиа") }
    var isAlbumsSheetOpen by remember { mutableStateOf(false) }
    var isQuickContactOpen by remember { mutableStateOf(false) }

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
            isAlbumsSheetOpen -> isAlbumsSheetOpen = false
            isMoreMenuOpen -> isMoreMenuOpen = false
            isCameraOpen -> isCameraOpen = false
            isEditorOpen -> isEditorOpen = false
            isFullPlayerOpen -> isFullPlayerOpen = false
            isFullscreen -> animateToOffset(defaultInitialOffsetPx, 240)
            else -> requestDismiss()
        }
    }

    // Плавный скролл шторки 1:1 вместе со списками через channel без создания корутин на каждое событие
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // Палец вверх (available.y < 0): тянем шторку вверх к 0f (fullscreen)
                if (available.y < 0f && sheetOffsetY.value > 0f) {
                    val current = sheetOffsetY.value
                    val newTarget = (current + available.y).coerceAtLeast(0f)
                    val consumedY = newTarget - current
                    scrollChannel.trySend(consumedY)
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Палец вниз (available.y > 0): когда список доскроллен до самого верха, шторка плавно уменьшается
                if (available.y > 0f) {
                    val current = sheetOffsetY.value
                    val newTarget = (current + available.y).coerceAtMost(screenHeightPx)
                    val consumedY = newTarget - current
                    scrollChannel.trySend(consumedY)
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // Если шторка ещё не на весь экран:
                if (sheetOffsetY.value > screenHeightPx * 0.05f) {
                    if (available.y < -800f) {
                        animateToOffset(0f, 240)
                        return available
                    } else if (available.y > 800f) {
                        if (sheetOffsetY.value < screenHeightPx * 0.40f) {
                            animateToOffset(defaultInitialOffsetPx, 240)
                        } else {
                            requestDismiss()
                        }
                        return available
                    } else {
                        if (sheetOffsetY.value > defaultInitialOffsetPx + screenHeightPx * 0.10f) {
                            requestDismiss()
                        } else if (sheetOffsetY.value < defaultInitialOffsetPx * 0.5f) {
                            animateToOffset(0f, 240)
                        } else {
                            animateToOffset(defaultInitialOffsetPx, 240)
                        }
                    }
                    return Velocity.Zero
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (available.y > 800f) {
                    if (sheetOffsetY.value < screenHeightPx * 0.40f) {
                        animateToOffset(defaultInitialOffsetPx, 240)
                    } else {
                        requestDismiss()
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Затемнение сзади шторки отдельным слоем через graphicsLayer alpha
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val progress = (1f - (sheetOffsetY.value / screenHeightPx)).coerceIn(0f, 1f)
                    alpha = progress * 0.70f
                }
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { requestDismiss() }
        )

        // Поверхность шторки вложений (всегда полная высота экрана, аппаратный сдвиг через graphicsLayer translationY)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .graphicsLayer {
                    translationY = sheetOffsetY.value
                }
                .clip(
                    if (isFullscreen) RoundedCornerShape(0.dp)
                    else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .background(AttachSheetBackground)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { /* поглощаем клики внутри шторки */ }
                .nestedScroll(nestedScrollConnection)
        ) {
            // 1. Контентная область вкладок на весь экран (фото и файлы скроллятся под плавающей шапкой)
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
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
                            isFullscreen = isFullscreen,
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
                                animateToOffset(0f, 240)
                            },
                            selectedFiles = selectedFiles,
                            onToggleSelectFile = { file ->
                                if (selectedFiles.any { it.uri == file.uri }) {
                                    selectedFiles.removeAll { it.uri == file.uri }
                                } else {
                                    selectedFiles.add(file)
                                }
                            },
                            isFullscreen = isFullscreen,
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
                                animateToOffset(0f, 240)
                            },
                            selectedContacts = selectedContacts,
                            onToggleSelectContact = { contact ->
                                if (selectedContacts.any { it.name == contact.name }) {
                                    selectedContacts.removeAll { it.name == contact.name }
                                } else {
                                    selectedContacts.add(contact)
                                }
                            },
                            isFullscreen = isFullscreen,
                            onBack = { requestDismiss() },
                            onQuickCardVisibilityChange = { isQuickContactOpen = it },
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
                                animateToOffset(0f, 240)
                            },
                            selectedTracks = selectedTracks,
                            onToggleSelectTrack = { track ->
                                if (selectedTracks.any { it.id == track.id }) {
                                    selectedTracks.removeAll { it.id == track.id }
                                } else {
                                    selectedTracks.add(track)
                                }
                            },
                            isFullscreen = isFullscreen,
                            onBack = { requestDismiss() },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }

            // 2. Вуаль-градиент за плавающей шапкой в полном экране ТОЛЬКО в Галерее
            val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
            val headerScrimHeight = statusBarTop + 68.dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(headerScrimHeight)
                    .graphicsLayer {
                        val isGallery = (selectedTab == AttachTab.GALLERY)
                        if (!isGallery) {
                            alpha = 0f
                        } else {
                            val appearProgress = (1f - (sheetOffsetY.value / (screenHeightPx * 0.10f))).coerceIn(0f, 1f)
                            alpha = appearProgress
                        }
                    }
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Black.copy(alpha = 0.85f),
                            0.65f to Color.Black.copy(alpha = 0.55f),
                            1.0f to Color.Transparent
                        )
                    )
            )

            // 3. Ручка перетаскивания (плавно исчезает при приближении к 95%)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(36.dp)
                    .graphicsLayer {
                        val fadeProgress = ((sheetOffsetY.value - screenHeightPx * 0.05f) / (screenHeightPx * 0.05f)).coerceIn(0f, 1f)
                        alpha = fadeProgress
                    }
                    .pointerInput(screenHeightPx) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val velocityTracker = VelocityTracker()
                            velocityTracker.addPosition(down.uptimeMillis, down.position)
                            var pointerId = down.id

                            while (true) {
                                val change = awaitVerticalDragOrCancellation(pointerId) ?: break
                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                val delta = change.positionChange().y
                                change.consume()
                                val next = (sheetOffsetY.value + delta).coerceIn(0f, screenHeightPx)
                                sheetOffsetY.snapTo(next)
                            }

                            val velocityY = velocityTracker.calculateVelocity().y
                            if (velocityY < -800f || sheetOffsetY.value <= screenHeightPx * 0.05f) {
                                animateToOffset(0f, 240)
                            } else if (velocityY > 800f) {
                                if (sheetOffsetY.value < screenHeightPx * 0.40f) {
                                    animateToOffset(defaultInitialOffsetPx, 240)
                                } else {
                                    requestDismiss()
                                }
                            } else {
                                if (sheetOffsetY.value > defaultInitialOffsetPx + screenHeightPx * 0.10f) {
                                    requestDismiss()
                                } else if (sheetOffsetY.value < defaultInitialOffsetPx * 0.5f) {
                                    animateToOffset(0f, 240)
                                } else {
                                    animateToOffset(defaultInitialOffsetPx, 240)
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 10.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(Color(0x40FFFFFF))
                )
            }

            // 4. Плавающая шапка (плавно появляется от 90% до 95%)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .safeStatusBarPadding()
                    .graphicsLayer {
                        val hasTopBar = (selectedTab == AttachTab.GALLERY || selectedTab == AttachTab.LOCATION)
                        if (!hasTopBar) {
                            alpha = 0f
                        } else {
                            val appearProgress = (1f - (sheetOffsetY.value / (screenHeightPx * 0.10f))).coerceIn(0f, 1f)
                            alpha = appearProgress
                        }
                    }
            ) {
                AttachTopBar(
                    title = selectedTab.title,
                    onBack = {
                        if (isFullscreen) {
                            animateToOffset(defaultInitialOffsetPx, 240)
                        } else {
                            requestDismiss()
                        }
                    },
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
                            DropdownMenuItem(
                                text = { Text("Системная галерея", color = Color.White, fontSize = 14.sp) },
                                onClick = {
                                    isMoreMenuOpen = false
                                    systemGalleryLauncher.launch("image/*")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.PhotoLibrary,
                                        contentDescription = null,
                                        tint = AttachPurple,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Файлы (без сжатия)", color = Color.White, fontSize = 14.sp) },
                                onClick = {
                                    isMoreMenuOpen = false
                                    if (selectedMedia.isNotEmpty()) {
                                        sendAsUncompressed()
                                    } else {
                                        uncompressedPickerLauncher.launch("*/*")
                                    }
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
                            if (selectedMedia.isNotEmpty()) {
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
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5. Нижняя панель вкладок (парящая над экраном, управляется через graphicsLayer)
            AttachTabBar(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                visible = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        val isCardOrSelectionOpen = totalSelectedCount > 0 || isQuickContactOpen
                        val isNearFullscreen = sheetOffsetY.value <= screenHeightPx * 0.05f
                        if (isCardOrSelectionOpen || isNearFullscreen) {
                            alpha = 0f
                            translationY = 300f
                        } else {
                            alpha = 1f
                            val dragDownOffset = maxOf(0f, sheetOffsetY.value - defaultInitialOffsetPx)
                            translationY = dragDownOffset
                        }
                    }
            )

            // 6. Плавающая панель ввода подписи при наличии выбранных элементов
            AttachInputBar(
                caption = captionText,
                onCaptionChange = { captionText = it },
                selectedCount = totalSelectedCount,
                visible = totalSelectedCount > 0,
                onSend = { sendAllSelected() },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .graphicsLayer {
                        val dragDownOffset = maxOf(0f, sheetOffsetY.value - defaultInitialOffsetPx)
                        translationY = dragDownOffset
                    }
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

