package com.anongram.app.features.attach.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
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
import com.anongram.app.features.player.logic.AudioPlayerController
import kotlinx.coroutines.launch

@Composable
fun AttachSheetContent(
    recipientName: String = "",
    onDismiss: () -> Unit,
    onSend: (List<AttachmentDraft>) -> Unit,
    keyboardHeightPx: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val screenHeightPx = with(density) { screenHeightDp.toPx() }

    // 1. Вычисление начальной высоты окна: если была открыта клавиатура — её высота, но не ниже 50% (ТЗ, шаг 2)
    val initialFraction = remember {
        if (keyboardHeightPx > 0 && screenHeightPx > 0) {
            (keyboardHeightPx.toFloat() / screenHeightPx).coerceIn(0.50f, 1.0f)
        } else {
            0.55f
        }
    }

    // 2. Единая анимируемая высота окна через Animatable (ТЗ, шаг 1)
    val sheetFraction = remember { Animatable(0f) }
    var isClosing by remember { mutableStateOf(false) }

    fun dismissAnimated() {
        if (isClosing) return
        isClosing = true
        // Закрыл окно вложений: останавливаем воспроизведение (ТЗ, шаг 6)
        try {
            AudioPlayerController.getInstance(context).stop()
        } catch (_: Exception) {}

        scope.launch {
            sheetFraction.animateTo(0f, tween(durationMillis = 260, easing = FastOutLinearInEasing))
            onDismiss()
        }
    }

    // Плавный выезд снизу вверх при открытии (~300 мс, FastOutSlowInEasing)
    LaunchedEffect(Unit) {
        sheetFraction.animateTo(initialFraction, tween(durationMillis = 300, easing = FastOutSlowInEasing))
    }

    var selectedTab by remember { mutableStateOf(AttachTab.GALLERY) }
    var selectedAlbum by remember { mutableStateOf("Все медиа") }
    var isAlbumsSheetOpen by remember { mutableStateOf(false) }

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

    // Обработка кнопки "Назад"
    BackHandler {
        when {
            showDiscardDialog -> showDiscardDialog = false
            isAlbumsSheetOpen -> isAlbumsSheetOpen = false
            isCameraOpen -> isCameraOpen = false
            isEditorOpen -> isEditorOpen = false
            isFullPlayerOpen -> isFullPlayerOpen = false
            sheetFraction.value > 0.70f -> {
                scope.launch { sheetFraction.animateTo(0.55f, tween(250, easing = FastOutSlowInEasing)) }
            }
            totalSelectedCount > 0 -> {
                showDiscardDialog = true
            }
            else -> dismissAnimated()
        }
    }

    // 3. Прокрутка растит окно (NestedScrollConnection, ТЗ шаг 3):
    // - Листаешь вниз (палец вверх): окно растет плавно до 100%, затем скроллится список.
    // - Листаешь вверх (палец вниз): сначала список до начала, затем окно уменьшается; ниже 50% закрывается.
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // available.y < 0: палец вверх, скролл контента вниз
                if (available.y < 0f && sheetFraction.value < 1.0f && screenHeightPx > 0) {
                    val deltaFrac = -available.y / screenHeightPx
                    val nextFrac = (sheetFraction.value + deltaFrac).coerceAtMost(1.0f)
                    scope.launch { sheetFraction.snapTo(nextFrac) }
                    return Offset(0f, available.y) // поглощаем жест, список пока не скроллится
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // available.y > 0: список доскроллился до самого верха, остаток плавно тянет окно вниз
                if (available.y > 0f && screenHeightPx > 0) {
                    val deltaFrac = available.y / screenHeightPx
                    val nextFrac = (sheetFraction.value - deltaFrac).coerceIn(0f, 1f)
                    scope.launch { sheetFraction.snapTo(nextFrac) }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                if (sheetFraction.value < 0.50f || available.y > 1000f) {
                    if (totalSelectedCount > 0) {
                        scope.launch { sheetFraction.animateTo(0.55f, tween(200)) }
                        showDiscardDialog = true
                    } else {
                        dismissAnimated()
                    }
                }
                return super.onPostFling(consumed, available)
            }
        }
    }

    val fraction = sheetFraction.value
    val isFullscreen = fraction >= 0.95f
    val scrimAlpha = (fraction * 0.65f).coerceIn(0f, 0.65f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable { requestDismiss() }
    ) {
        // Поверхность шторки вложений (плавающее окно от 50% до 100% высоты)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraction)
                .align(Alignment.BottomCenter)
                .clip(
                    if (isFullscreen) RoundedCornerShape(0.dp)
                    else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .background(AttachSheetBackground)
                .clickable(enabled = false) {}
                .nestedScroll(nestedScrollConnection)
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        if (screenHeightPx > 0) {
                            val nextFrac = (sheetFraction.value - (delta / screenHeightPx)).coerceIn(0f, 1f)
                            scope.launch { sheetFraction.snapTo(nextFrac) }
                        }
                    },
                    onDragStopped = { velocity ->
                        // Если отпустили ниже 50% — окно уезжает вниз и закрывается (ТЗ, шаг 1)
                        if (sheetFraction.value < 0.50f || velocity > 1200f) {
                            if (totalSelectedCount > 0) {
                                scope.launch { sheetFraction.animateTo(0.55f, tween(200)) }
                                showDiscardDialog = true
                            } else {
                                dismissAnimated()
                            }
                        }
                        // Если отпустили от 50% до 100% — окно остаётся в этом положении без залипания к ступенькам!
                    }
                )
        ) {
            // 4. ВЕРХНЯЯ НАВИГАЦИЯ ТОЛЬКО В ПОЛНОМ ЭКРАНЕ (ТЗ, шаг 4):
            // Пока окно меньше 95%: только тонкая ручка. Кнопок назад/заголовок/поиск/меню нет.
            // Когда окно >= 95%: плавно появляются плавающие таблетки AttachTopBar.
            if (!isFullscreen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .width(32.dp)
                            .height(3.dp)
                            .clip(CircleShape)
                            .background(Color(0x38FFFFFF))
                    )
                }
            } else if (selectedTab != AttachTab.MUSIC && selectedTab != AttachTab.FILE) {
                AttachTopBar(
                    title = selectedTab.title,
                    onBack = {
                        if (totalSelectedCount > 0) {
                            showDiscardDialog = true
                        } else {
                            scope.launch {
                                sheetFraction.animateTo(0.55f, tween(250, easing = FastOutSlowInEasing))
                            }
                        }
                    },
                    centerContent = if (selectedTab == AttachTab.GALLERY) {
                        {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { isAlbumsSheetOpen = !isAlbumsSheetOpen },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedAlbum,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Альбомы",
                                    tint = AttachPurple
                                )
                            }
                        }
                    } else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }

            // Контентная область вкладок (скроллится под плавающими таблетками)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                                scope.launch {
                                    sheetFraction.animateTo(1.0f, tween(260, easing = FastOutSlowInEasing))
                                }
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
                            onBack = {
                                if (totalSelectedCount > 0) {
                                    showDiscardDialog = true
                                } else {
                                    scope.launch {
                                        sheetFraction.animateTo(0.55f, tween(250, easing = FastOutSlowInEasing))
                                    }
                                }
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
                                scope.launch {
                                    sheetFraction.animateTo(1.0f, tween(260, easing = FastOutSlowInEasing))
                                }
                            },
                            selectedContacts = selectedContacts,
                            onToggleSelectContact = { contact ->
                                if (selectedContacts.any { it.name == contact.name }) {
                                    selectedContacts.removeAll { it.name == contact.name }
                                } else {
                                    selectedContacts.add(contact)
                                }
                            },
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
                                scope.launch {
                                    sheetFraction.animateTo(1.0f, tween(260, easing = FastOutSlowInEasing))
                                }
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
                            onBack = {
                                if (totalSelectedCount > 0) {
                                    showDiscardDialog = true
                                } else {
                                    scope.launch {
                                        sheetFraction.animateTo(0.55f, tween(250, easing = FastOutSlowInEasing))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 4. Нижняя панель вкладок: плавно уезжает вниз при >= 95% или при наличии выбранных элементов
                AttachTabBar(
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    visible = !isFullscreen && totalSelectedCount == 0,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // 11. Плавающая панель ввода подписи при выборе (любой раздел, любая высота)
                AttachInputBar(
                    caption = captionText,
                    onCaptionChange = { captionText = it },
                    selectedCount = totalSelectedCount,
                    visible = totalSelectedCount > 0,
                    onSend = { sendAllSelected() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
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

        // Диалог подтверждения сброса выбора при выходе
        if (showDiscardDialog) {
            AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = {
                    Text(
                        text = "Сбросить выбор",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Закрыть вложения?",
                        color = Color(0xFFD1D5DB),
                        fontSize = 15.sp
                    )
                },
                dismissButton = {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text(
                            text = "Отмена",
                            color = Color(0xFFA78BFA),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            clearAllSelected()
                            dismissAnimated()
                        }
                    ) {
                        Text(
                            text = "Сбросить",
                            color = Color(0xFFEF4444),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                containerColor = Color(0xFF242424),
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}
