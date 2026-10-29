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
                    if (previewOffsetPx > halfOffset + 60f || available.y > 600f) {
                        showDiscardConfirmDialog = true
                    } else if (previewOffsetPx < halfOffset * 0.45f || available.y < -400f) {
                        previewOffsetPx = 0f
                        onOffsetChange(0f)
                    } else {
                        previewOffsetPx = halfOffset
                        onOffsetChange(halfOffset)
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
                                    if (velocity > 600f || previewOffsetPx > halfOffset + 60f) {
                                        showDiscardConfirmDialog = true
                                    } else if (velocity < -400f || previewOffsetPx < halfOffset * 0.45f) {
                                        previewOffsetPx = 0f
                                        onOffsetChange(0f)
                                    } else {
                                        previewOffsetPx = halfOffset
                                        onOffsetChange(halfOffset)
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
                                    if (velocity > 600f || previewOffsetPx > halfOffset + 60f) {
                                        showDiscardConfirmDialog = true
                                    } else if (velocity < -400f || previewOffsetPx < halfOffset * 0.45f) {
                                        previewOffsetPx = 0f
                                        onOffsetChange(0f)
                                    } else {
                                        previewOffsetPx = halfOffset
                                        onOffsetChange(halfOffset)
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

                    // Скроллируемая область с альбомами сообщений
                    val listState = rememberLazyListState()
                    val albumChunks = remember(orderedList.toList()) { orderedList.chunked(20) }

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
                            val startOrderNumber = chunkIdx * 20 + 1
                            val startGlobalIndex = chunkIdx * 20

                            if (chunk.size == 1) {
                                SinglePhotoBubble(
                                    media = chunk[0],
                                    orderNumber = startOrderNumber,
                                    globalIndex = startGlobalIndex,
                                    onPhotoClick = { viewingPhotoUri = it },
                                    onSwap = { fromIdx, offset, itemsInRow ->
                                        val deltaX = if (offset.x > 35f) 1 else if (offset.x < -35f) -1 else 0
                                        val deltaY = if (offset.y > 45f) itemsInRow else if (offset.y < -45f) -itemsInRow else 0
                                        val delta = if (deltaY != 0) deltaY + deltaX else deltaX
                                        if (delta != 0) {
                                            val target = (fromIdx + delta).coerceIn(0, orderedList.size - 1)
                                            if (target != fromIdx) swapItems(fromIdx, target)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                AlbumBubble(
                                    mediaItems = chunk,
                                    startOrderNumber = startOrderNumber,
                                    startGlobalIndex = startGlobalIndex,
                                    onPhotoClick = { viewingPhotoUri = it },
                                    onSwap = { fromIdx, offset, itemsInRow ->
                                        val deltaX = if (offset.x > 35f) 1 else if (offset.x < -35f) -1 else 0
                                        val deltaY = if (offset.y > 45f) itemsInRow else if (offset.y < -45f) -itemsInRow else 0
                                        val delta = if (deltaY != 0) deltaY + deltaX else deltaX
                                        if (delta != 0) {
                                            val target = (fromIdx + delta).coerceIn(0, orderedList.size - 1)
                                            if (target != fromIdx) swapItems(fromIdx, target)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
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
                            loadMediaThumbnail(context, viewingPhotoUri!!)
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
 * Для 13 элементов -> [3, 4, 3, 3] (скриншот 1)
 * Для 10 элементов -> [3, 4, 3] (скриншот 2)
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
        11 -> listOf(4, 3, 4)
        12 -> listOf(3, 3, 3, 3)
        13 -> listOf(3, 4, 3, 3)
        14 -> listOf(4, 3, 3, 4)
        15 -> listOf(4, 4, 4, 3)
        16 -> listOf(4, 4, 4, 4)
        17 -> listOf(4, 4, 3, 3, 3)
        18 -> listOf(4, 4, 4, 3, 3)
        19 -> listOf(4, 4, 4, 4, 3)
        20 -> listOf(4, 4, 4, 4, 4)
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
        1 -> 200.dp
        2 -> 155.dp
        3 -> 130.dp
        4 -> 105.dp
        else -> 95.dp
    }
}

/**
 * Единый баббл альбома сообщений (до 20 фото), с фиолетовой рамкой и разделителями
 */
@Composable
private fun AlbumBubble(
    mediaItems: List<GalleryMedia>,
    startOrderNumber: Int,
    startGlobalIndex: Int,
    onPhotoClick: (Uri) -> Unit,
    onSwap: (Int, Offset, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowCounts = remember(mediaItems.size) { calculateAlbumRows(mediaItems.size) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFC084FC), RoundedCornerShape(16.dp))
            .background(Color(0xFF131317))
    ) {
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

/**
 * Одиночный баббл фото (если осталось 1 фото после разбивки по 20)
 */
@Composable
private fun SinglePhotoBubble(
    media: GalleryMedia,
    orderNumber: Int,
    globalIndex: Int,
    onPhotoClick: (Uri) -> Unit,
    onSwap: (Int, Offset, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, Color(0xFFC084FC), RoundedCornerShape(16.dp))
            .background(Color(0xFF131317))
            .height(200.dp)
    ) {
        PreviewCollageItem(
            media = media,
            orderNumber = orderNumber,
            globalIndex = globalIndex,
            onPhotoClick = onPhotoClick,
            onDragSwap = { offset -> onSwap(globalIndex, offset, 1) },
            modifier = Modifier.fillMaxSize()
        )
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
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(media.uri) { mutableStateOf<ImageBitmap?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragAccumulated by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(targetValue = if (isDragging) 0.78f else 1.0f)
    val zIndexVal = if (isDragging) 40f else 1f

    LaunchedEffect(media.uri) {
        val bmp = withContext(Dispatchers.IO) {
            loadMediaThumbnail(context, media.uri)
        }
        bitmap = bmp?.asImageBitmap()
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
                        dragAccumulated = Offset.Zero
                    },
                    onDragEnd = {
                        isDragging = false
                        if (dragAccumulated.getDistance() > 36f) {
                            onDragSwap(dragAccumulated)
                        }
                        dragAccumulated = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
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
