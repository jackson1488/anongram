package com.anongram.app.features.attach.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.outlined.GridOff
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.attach.ui.tabs.GalleryMedia
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.attach.ui.tabs.loadHdBitmap
import com.anongram.app.features.attach.ui.theme.AttachPurple
import com.anongram.app.features.wallpapers.ui.WallpaperBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Экран предпросмотра и изменения порядка медиа (настоящий Telegram-style мини-чат):
 * - Сквозь полупрозрачное затемнение вверху видна шапка чата и сообщения (как в галерее)
 * - Шторка открывается на уровне initialOffsetPx (50%..95%) и плавно свайпается вверх и вниз
 * - Альбомы группируются бабблами до 20 фото (Telegram standard) с фиолетовой рамкой и разделителями
 * - Сквозная нумерация бейджей (1), (2)...
 * - Вертикальный скролл LazyColumn для любого количества фото (36, 99 и т.д.)
 * - В самом низу списка: плашка «Перетащите для изменения порядка»
 * - Drag-and-Drop: зажатие -> уменьшение карточки -> перемещение -> swap строго при onDragEnd
 * - Фиксированный снизу AttachInputBar
 */
@Composable
fun PreviewOrderScreen(
    initialMediaList: List<GalleryMedia>,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onClose: () -> Unit,
    onSend: (List<GalleryMedia>, Boolean, Boolean, Boolean) -> Unit,
    onUpdateOrderAndSpoilers: (List<GalleryMedia>) -> Unit = {},
    onDiscardSelection: () -> Unit = {},
    onDismissAll: () -> Unit = {},
    chatId: String = "",
    initialOffsetPx: Float = 0f,
    onOffsetChange: (Float) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val orderedList = remember { mutableStateListOf<GalleryMedia>().apply { addAll(initialMediaList) } }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isUncompressed by remember { mutableStateOf(false) }
    var isUngrouped by remember { mutableStateOf(false) }
    var isHd by remember { mutableStateOf(false) }
    var viewingPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }

    fun handleClose() {
        onUpdateOrderAndSpoilers(orderedList.toList())
        onClose()
    }

    // Системный жест "Назад" плавно возвращает в галерею
    BackHandler { handleClose() }

    val allHaveSpoiler = orderedList.isNotEmpty() && orderedList.all { it.hasSpoiler }

    fun toggleSpoilerForAll() {
        val target = !allHaveSpoiler
        val updated = orderedList.map { it.copy(hasSpoiler = target) }
        orderedList.clear()
        orderedList.addAll(updated)
        onUpdateOrderAndSpoilers(orderedList.toList())
    }

    fun swapItems(fromIdx: Int, toIdx: Int) {
        if (fromIdx in orderedList.indices && toIdx in orderedList.indices && fromIdx != toIdx) {
            val item = orderedList.removeAt(fromIdx)
            orderedList.add(toIdx, item)
            onUpdateOrderAndSpoilers(orderedList.toList())
        }
    }

    var isEmojiPanelOpen by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val H = constraints.maxHeight.toFloat()
        val halfOffset = H * 0.44f
        var previewOffsetPx by remember(H) { mutableFloatStateOf(initialOffsetPx.coerceIn(0f, halfOffset)) }

        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y < 0f && previewOffsetPx > 0f) {
                        val newOffset = (previewOffsetPx + available.y).coerceAtLeast(0f)
                        val consumed = previewOffsetPx - newOffset
                        previewOffsetPx = newOffset
                        onOffsetChange(newOffset)
                        return Offset(0f, -consumed)
                    }
                    return Offset.Zero
                }

                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (available.y > 0f) {
                        previewOffsetPx = (previewOffsetPx + available.y).coerceAtLeast(0f)
                        onOffsetChange(previewOffsetPx)
                        return Offset(0f, available.y)
                    }
                    return Offset.Zero
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    return Velocity.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (previewOffsetPx > halfOffset + 80f || available.y > 2500f) {
                        showDiscardConfirmDialog = true
                    } else if (available.y < -2500f) {
                        previewOffsetPx = 0f
                        onOffsetChange(0f)
                    } else {
                        previewOffsetPx = previewOffsetPx.coerceIn(0f, halfOffset)
                        onOffsetChange(previewOffsetPx)
                    }
                    return Velocity.Zero
                }
            }
        }

        // 1. Полупрозрачное затемнение над шторкой (сквозь него видна настоящая шапка чата и сообщения)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = ((H - previewOffsetPx) / H * 0.65f).coerceIn(0f, 0.70f)
                }
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    showDiscardConfirmDialog = true
                }
        )

        // 2. Шторка предпросмотра (сдвинута на previewOffsetPx, скругление сверху)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = previewOffsetPx }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                // Живые обои чата внутри шторки предпросмотра с темным тинтом (как в Telegram)
                if (chatId.isNotBlank()) {
                    WallpaperBackground(
                        chatId = chatId,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF131317).copy(alpha = 0.50f))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF161618))
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(nestedScrollConnection)
                ) {
                    // Тонкая ручка шторки сверху, за которую можно тянуть
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    previewOffsetPx = (previewOffsetPx + delta).coerceAtLeast(0f)
                                    onOffsetChange(previewOffsetPx)
                                },
                                onDragStopped = { velocity ->
                                    if (velocity > 2500f || previewOffsetPx > halfOffset + 80f) {
                                        showDiscardConfirmDialog = true
                                    } else if (velocity < -2500f) {
                                        previewOffsetPx = 0f
                                        onOffsetChange(0f)
                                    } else {
                                        previewOffsetPx = previewOffsetPx.coerceIn(0f, halfOffset)
                                        onOffsetChange(previewOffsetPx)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(2.5.dp)
                                .clip(CircleShape)
                                .background(Color(0x4DFFFFFF))
                        )
                    }

                    // Верхняя шапка шторки: "< Предпросмотр" и кнопка "⋮" справа
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                            .draggable(
                                orientation = Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    previewOffsetPx = (previewOffsetPx + delta).coerceAtLeast(0f)
                                    onOffsetChange(previewOffsetPx)
                                },
                                onDragStopped = { velocity ->
                                    if (velocity > 2500f || previewOffsetPx > halfOffset + 80f) {
                                        showDiscardConfirmDialog = true
                                    } else if (velocity < -2500f) {
                                        previewOffsetPx = 0f
                                        onOffsetChange(0f)
                                    } else {
                                        previewOffsetPx = previewOffsetPx.coerceIn(0f, halfOffset)
                                        onOffsetChange(previewOffsetPx)
                                    }
                                }
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кнопка "< Предпросмотр"
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { handleClose() }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Предпросмотр",
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Кнопка "⋮" с выпадающим меню (строго одна иконка справа)
                        Box {
                            IconButton(onClick = { isMenuOpen = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "Опции",
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = isMenuOpen,
                                onDismissRequest = { isMenuOpen = false },
                                modifier = Modifier
                                    .background(Color(0xFF222226), RoundedCornerShape(16.dp))
                                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isUncompressed) "Отменить сжатие" else "Отправить без сжатия",
                                            color = Color.White,
                                            fontSize = 14.5.sp
                                        )
                                    },
                                    onClick = {
                                        isUncompressed = !isUncompressed
                                        isMenuOpen = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.InsertDriveFile,
                                            contentDescription = null,
                                            tint = if (isUncompressed) AttachPurple else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isUngrouped) "Отменить группировку" else "Отправить без группировки",
                                            color = Color.White,
                                            fontSize = 14.5.sp
                                        )
                                    },
                                    onClick = {
                                        isUngrouped = !isUngrouped
                                        isMenuOpen = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.GridOff,
                                            contentDescription = null,
                                            tint = if (isUngrouped) AttachPurple else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (allHaveSpoiler) "Отменить спойлер" else "Скрыть под спойлер",
                                            color = Color.White,
                                            fontSize = 14.5.sp
                                        )
                                    },
                                    onClick = {
                                        toggleSpoilerForAll()
                                        isMenuOpen = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (allHaveSpoiler) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                            contentDescription = null,
                                            tint = if (allHaveSpoiler) AttachPurple else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = if (isHd) "Стандартное качество" else "Отправить в HD-качестве",
                                            color = Color.White,
                                            fontSize = 14.5.sp
                                        )
                                    },
                                    onClick = {
                                        isHd = !isHd
                                        isMenuOpen = false
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = null,
                                            tint = if (isHd) AttachPurple else Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Скроллируемая область с альбомами сообщений (как в Telegram: до 10 медиа в одном альбоме)
                    val listState = rememberLazyListState()
                    val albumChunks = remember(orderedList.toList()) { orderedList.chunked(10) }
                    var draggingChunkIdx by remember { mutableIntStateOf(-1) }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        itemsIndexed(albumChunks, key = { chunkIdx, _ -> "chunk_$chunkIdx" }) { chunkIdx, chunk ->
                            val startOrderNumber = chunkIdx * 10 + 1
                            val startGlobalIndex = chunkIdx * 10
                            val bubbleZ = if (draggingChunkIdx == chunkIdx) 50f else 0f

                            if (chunk.size == 1) {
                                SinglePhotoBubble(
                                    media = chunk[0],
                                    orderNumber = startOrderNumber,
                                    globalIndex = startGlobalIndex,
                                    onPhotoClick = { viewingPhotoUri = it },
                                    isAnyDragging = draggingChunkIdx == chunkIdx,
                                    onDragStateChange = { dragging -> draggingChunkIdx = if (dragging) chunkIdx else -1 },
                                    onSwap = { fromIdx, offset, itemsInRow ->
                                        val delta = computeSwapDelta(offset, itemsInRow)
                                        if (delta != 0) {
                                            val target = (fromIdx + delta).coerceIn(0, orderedList.size - 1)
                                            if (target != fromIdx) swapItems(fromIdx, target)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().zIndex(bubbleZ)
                                )
                            } else {
                                AlbumBubble(
                                    mediaItems = chunk,
                                    startOrderNumber = startOrderNumber,
                                    startGlobalIndex = startGlobalIndex,
                                    onPhotoClick = { viewingPhotoUri = it },
                                    isAnyDragging = draggingChunkIdx == chunkIdx,
                                    onDragStateChange = { dragging -> draggingChunkIdx = if (dragging) chunkIdx else -1 },
                                    onSwap = { fromIdx, offset, itemsInRow ->
                                        val delta = computeSwapDelta(offset, itemsInRow)
                                        if (delta != 0) {
                                            val target = (fromIdx + delta).coerceIn(0, orderedList.size - 1)
                                            if (target != fromIdx) swapItems(fromIdx, target)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().zIndex(bubbleZ)
                                )
                            }
                        }

                        // Плашка "Перетащите для изменения порядка" (строго в самом низу списка, как на скриншоте 2)
                        item(key = "drag_hint_pill") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0x992E2E34))
                                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Перетащите для изменения порядка",
                                        color = Color(0xFFE2E2E2),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Отступ для нижней плавающей панели ввода
                        item(key = "bottom_input_spacer") {
                            Spacer(modifier = Modifier.height(84.dp))
                        }
                    }
                }
            }
        }

        // 3. Парящий фиксированный островок ввода подписи внизу
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            AttachInputBar(
                caption = caption,
                onCaptionChange = onCaptionChange,
                selectedCount = orderedList.size,
                visible = true,
                onSend = {
                    onSend(orderedList.toList(), isUncompressed, isUngrouped, isHd)
                },
                isEmojiOpen = isEmojiPanelOpen,
                onToggleEmoji = { isEmojiPanelOpen = !isEmojiPanelOpen },
                onCloseEmoji = { isEmojiPanelOpen = false },
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Диалог "Сбросить выбор?" при свайпе шторки вниз
        if (showDiscardConfirmDialog) {
            AlertDialog(
                onDismissRequest = {
                    showDiscardConfirmDialog = false
                    previewOffsetPx = previewOffsetPx.coerceIn(0f, halfOffset)
                    onOffsetChange(previewOffsetPx)
                },
                title = {
                    Text(
                        text = "Сбросить выбор?",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Text(
                        text = "Вы действительно хотите сбросить выбор фотографий?",
                        color = Color(0xFFCCCCCC),
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDiscardConfirmDialog = false
                            onDiscardSelection()
                            onDismissAll()
                        }
                    ) {
                        Text("Сбросить", color = Color(0xFFFF5252), fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDiscardConfirmDialog = false
                            previewOffsetPx = previewOffsetPx.coerceIn(0f, halfOffset)
                            onOffsetChange(previewOffsetPx)
                        }
                    ) {
                        Text("Отмена", color = Color.White)
                    }
                },
                containerColor = Color(0xFF222226),
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Полноэкранный просмотр фото по одиночному клику
        if (viewingPhotoUri != null) {
            Dialog(
                onDismissRequest = { viewingPhotoUri = null },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .clickable { viewingPhotoUri = null }
                ) {
                    var fullBitmap by remember(viewingPhotoUri) { mutableStateOf<ImageBitmap?>(null) }
                    LaunchedEffect(viewingPhotoUri) {
                        val b = withContext(Dispatchers.IO) {
                            loadHdBitmap(context, viewingPhotoUri!!, 2400)
                        }
                        fullBitmap = b?.asImageBitmap()
                    }
                    if (fullBitmap != null) {
                        Image(
                            bitmap = fullBitmap!!,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    IconButton(
                        onClick = { viewingPhotoUri = null },
                        modifier = Modifier
                            .safeStatusBarPadding()
                            .padding(8.dp)
                            .align(Alignment.TopStart)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Разбивка альбома из N элементов на строки (соответствует Telegram-коллажу):
 * Для 10 элементов -> [3, 4, 3]
 * Для 9 элементов -> [3, 3, 3]
 * Для 6 элементов -> [3, 3]
 */
fun calculateAlbumRows(count: Int): List<Int> {
    return when (count) {
        1 -> listOf(1)
        2 -> listOf(2)
        3 -> listOf(3)
        4 -> listOf(2, 2)
        5 -> listOf(3, 2)
        6 -> listOf(3, 3)
        7 -> listOf(3, 4)
        8 -> listOf(4, 4)
        9 -> listOf(3, 3, 3)
        10 -> listOf(3, 4, 3)
        else -> {
            val rows = mutableListOf<Int>()
            var rem = count
            while (rem > 0) {
                val take = if (rem >= 4) 4 else rem
                rows.add(take)
                rem -= take
            }
            rows
        }
    }
}

fun getRowHeightDp(itemCountInRow: Int): Dp {
    return when (itemCountInRow) {
        1 -> 375.dp
        2 -> 240.dp
        3 -> 205.dp
        4 -> 130.dp
        else -> 110.dp
    }
}

/** Смещение индекса при перетаскивании: по горизонтали на 1 ячейку, по вертикали на ряд(ы), в т.ч. в соседний альбом. */
fun computeSwapDelta(offset: Offset, itemsInRow: Int): Int {
    val deltaX = if (offset.x > 35f) 1 else if (offset.x < -35f) -1 else 0
    val rows = when {
        offset.y > 45f -> kotlin.math.max(1, Math.round(offset.y / 380f))
        offset.y < -45f -> -kotlin.math.max(1, Math.round(-offset.y / 380f))
        else -> 0
    }
    return if (rows != 0) rows * itemsInRow + deltaX else deltaX
}

/**
 * Единый баббл альбома сообщений (до 10 фото в чанке), с фиолетовой рамкой и разделителями.
 * Для 2, 3 и 4 фото используются фирменные вертикальные раскладки Telegram.
 */
@Composable
private fun AlbumBubble(
    mediaItems: List<GalleryMedia>,
    startOrderNumber: Int,
    startGlobalIndex: Int,
    onPhotoClick: (Uri) -> Unit,
    onSwap: (Int, Offset, Int) -> Unit,
    isAnyDragging: Boolean = false,
    onDragStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(16.dp)
    val count = mediaItems.size

    // Для 2, 3, 4 фото уменьшаем ширину баббла по центру (~3 см на колонку, т.е. ~88% ширины), чтобы фото были вертикальными 3×6 см
    val bubbleWidthFraction = if (count in 2..4) 0.90f else 1f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(bubbleWidthFraction)
                .then(if (isAnyDragging) Modifier else Modifier.clip(bubbleShape))
                .border(1.5.dp, Color(0xFFC084FC), bubbleShape)
                .background(Color(0xFF131317))
        ) {
            when (count) {
                2 -> {
                    // 2 фото: рядом друг с другом с разделительной полосой, каждое ~3 см по горизонтали и ~6 см по вертикали (соотношение 1:2)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(370.dp)
                    ) {
                        PreviewCollageItem(
                            media = mediaItems[0],
                            orderNumber = startOrderNumber,
                            globalIndex = startGlobalIndex,
                            onPhotoClick = onPhotoClick,
                            onDragStateChange = onDragStateChange,
                            onDragSwap = { offset -> onSwap(startGlobalIndex, offset, 2) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFC084FC))
                        )
                        PreviewCollageItem(
                            media = mediaItems[1],
                            orderNumber = startOrderNumber + 1,
                            globalIndex = startGlobalIndex + 1,
                            onPhotoClick = onPhotoClick,
                            onDragStateChange = onDragStateChange,
                            onDragSwap = { offset -> onSwap(startGlobalIndex + 1, offset, 2) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
                3 -> {
                    // 3 фото: первое фото слева большое (3 см × 6 см), а 2-е и 3-е справа друг над другом (вместе 6 см в высоту)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(370.dp)
                    ) {
                        PreviewCollageItem(
                            media = mediaItems[0],
                            orderNumber = startOrderNumber,
                            globalIndex = startGlobalIndex,
                            onPhotoClick = onPhotoClick,
                            onDragStateChange = onDragStateChange,
                            onDragSwap = { offset -> onSwap(startGlobalIndex, offset, 2) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFC084FC))
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            PreviewCollageItem(
                                media = mediaItems[1],
                                orderNumber = startOrderNumber + 1,
                                globalIndex = startGlobalIndex + 1,
                                onPhotoClick = onPhotoClick,
                                onDragStateChange = onDragStateChange,
                                onDragSwap = { offset -> onSwap(startGlobalIndex + 1, offset, 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(1.5.dp)
                                    .background(Color(0xFFC084FC))
                            )
                            PreviewCollageItem(
                                media = mediaItems[2],
                                orderNumber = startOrderNumber + 2,
                                globalIndex = startGlobalIndex + 2,
                                onPhotoClick = onPhotoClick,
                                onDragStateChange = onDragStateChange,
                                onDragSwap = { offset -> onSwap(startGlobalIndex + 2, offset, 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
                4 -> {
                    // 4 фото (группировка Telegram): 1 большое вертикальное фото слева + 3 фото столбиком справа
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {
                        PreviewCollageItem(
                            media = mediaItems[0],
                            orderNumber = startOrderNumber,
                            globalIndex = startGlobalIndex,
                            onPhotoClick = onPhotoClick,
                            onDragStateChange = onDragStateChange,
                            onDragSwap = { offset -> onSwap(startGlobalIndex, offset, 2) },
                            modifier = Modifier
                                .weight(1.1f)
                                .fillMaxHeight()
                        )
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .fillMaxHeight()
                                .background(Color(0xFFC084FC))
                        )
                        Column(
                            modifier = Modifier
                                .weight(0.9f)
                                .fillMaxHeight()
                        ) {
                            for (idx in 1..3) {
                                if (idx > 1) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.5.dp)
                                            .background(Color(0xFFC084FC))
                                    )
                                }
                                PreviewCollageItem(
                                    media = mediaItems[idx],
                                    orderNumber = startOrderNumber + idx,
                                    globalIndex = startGlobalIndex + idx,
                                    onPhotoClick = onPhotoClick,
                                    onDragStateChange = onDragStateChange,
                                    onDragSwap = { offset -> onSwap(startGlobalIndex + idx, offset, 1) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                )
                            }
                        }
                    }
                }
                else -> {
                    val rowCounts = remember(count) { calculateAlbumRows(count) }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        var itemPointer = 0
                        rowCounts.forEachIndexed { rowIndex, countInRow ->
                            if (rowIndex > 0) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(Color(0xFFC084FC))
                                )
                            }

                            val rowHeight = getRowHeightDp(countInRow)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(rowHeight)
                            ) {
                                for (c in 0 until countInRow) {
                                    val currentItemIdx = itemPointer + c
                                    if (currentItemIdx < mediaItems.size) {
                                        if (c > 0) {
                                            Box(
                                                modifier = Modifier
                                                    .width(1.5.dp)
                                                    .fillMaxHeight()
                                                    .background(Color(0xFFC084FC))
                                            )
                                        }
                                        val media = mediaItems[currentItemIdx]
                                        val globalIdx = startGlobalIndex + currentItemIdx
                                        val orderNum = startOrderNumber + currentItemIdx

                                        PreviewCollageItem(
                                            media = media,
                                            orderNumber = orderNum,
                                            globalIndex = globalIdx,
                                            onPhotoClick = onPhotoClick,
                                            onDragStateChange = onDragStateChange,
                                            onDragSwap = { offset ->
                                                onSwap(globalIdx, offset, countInRow)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                        )
                                    }
                                }
                            }
                            itemPointer += countInRow
                        }
                    }
                }
            }
        }
    }
}

/**
 * Одиночный баббл фото (1 фото выбрано, либо одиночный остаток после разбивки по 10):
 * Стоит ровно по центру по горизонтали ниже шапки «Предпросмотр»,
 * вертикальный размер ~4.5 см по горизонтали и ~10 см по вертикали (соотношение ~9:20).
 */
@Composable
private fun SinglePhotoBubble(
    media: GalleryMedia,
    orderNumber: Int,
    globalIndex: Int,
    onPhotoClick: (Uri) -> Unit,
    onSwap: (Int, Offset, Int) -> Unit,
    isAnyDragging: Boolean = false,
    onDragStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .height(540.dp)
                .then(if (isAnyDragging) Modifier else Modifier.clip(bubbleShape))
                .border(1.5.dp, Color(0xFFC084FC), bubbleShape)
                .background(Color(0xFF131317))
        ) {
            PreviewCollageItem(
                media = media,
                orderNumber = orderNumber,
                globalIndex = globalIndex,
                onPhotoClick = onPhotoClick,
                onDragStateChange = onDragStateChange,
                onDragSwap = { offset -> onSwap(globalIndex, offset, 1) },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Элемент коллажа в экране предпросмотра:
 * - Одиночный тап: полноэкранный просмотр
 * - Долгое нажатие (зажатие): карточка уменьшается (scale 0.78), elevation 24dp и плавно следует за пальцем
 * - Обмен местами (swap) строго по onDragEnd при отпускании пальца (без рывков во время движения)
 * - Спойлерный блюр и мерцающие частицы, если спойлер включён
 * - Фиолетовый круглый бейдж порядка (1), (2)...
 */
@Composable
private fun PreviewCollageItem(
    media: GalleryMedia,
    orderNumber: Int,
    globalIndex: Int,
    onPhotoClick: (Uri) -> Unit,
    onDragSwap: (Offset) -> Unit,
    onDragStateChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(media.uri) { mutableStateOf<ImageBitmap?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragAccumulated by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(targetValue = if (isDragging) 0.78f else 1.0f)
    val zIndexVal = if (isDragging) 40f else 1f

    LaunchedEffect(media.uri) {
        // Сначала мгновенно быстрое превью, затем HD-версия без «мыла»
        val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, media.uri) }
        if (bitmap == null) bitmap = quick?.asImageBitmap()
        val hd = withContext(Dispatchers.IO) { loadHdBitmap(context, media.uri) }
        if (hd != null) bitmap = hd.asImageBitmap()
    }

    Box(
        modifier = modifier
            .zIndex(zIndexVal)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = if (isDragging) dragAccumulated.x else 0f
                translationY = if (isDragging) dragAccumulated.y else 0f
                shadowElevation = if (isDragging) 24f else 0f
            }
            .background(Color(0xFF1E1E1E))
            .pointerInput(media.uri) {
                detectTapGestures(
                    onTap = { onPhotoClick(media.uri) }
                )
            }
            .pointerInput(globalIndex) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        onDragStateChange(true)
                        dragAccumulated = Offset.Zero
                    },
                    onDragEnd = {
                        isDragging = false
                        onDragStateChange(false)
                        if (dragAccumulated.getDistance() > 36f) {
                            onDragSwap(dragAccumulated)
                        }
                        dragAccumulated = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        onDragStateChange(false)
                        dragAccumulated = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulated += dragAmount
                    }
                )
            }
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (media.hasSpoiler) Modifier.blur(22.dp) else Modifier)
            )
        }

        // Анимированный спойлер с мерцающими частицами, если включён
        if (media.hasSpoiler) {
            SpoilerOverlay(modifier = Modifier.fillMaxSize())
        }

        // Бейдж порядка в верхнем правом углу: круглая фиолетовая плашка с цифрой
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFFC084FC))
                .border(1.dp, Color(0xFF1E1E1E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = orderNumber.toString(),
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
