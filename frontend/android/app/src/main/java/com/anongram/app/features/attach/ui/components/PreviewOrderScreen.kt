package com.anongram.app.features.attach.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
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

    fun removeItemAt(globalIdx: Int) {
        if (globalIdx in orderedList.indices) {
            orderedList.removeAt(globalIdx)
            onUpdateOrderAndSpoilers(orderedList.toList())
            if (orderedList.isEmpty()) {
                onClose()
            }
        }
    }

    val itemBoundsMap = remember { mutableStateMapOf<Int, Rect>() }
    var draggingGlobalIdx by remember { mutableIntStateOf(-1) }
    var dragFingerRootPos by remember { mutableStateOf(Offset.Zero) }
    var rootBoxOrigin by remember { mutableStateOf(Offset.Zero) }

    fun findDropTargetIndex(fingerRootPos: Offset, fromIdx: Int): Int {
        // Сначала ищем ячейку, чей Rect напрямую содержит палец
        for ((idx, rect) in itemBoundsMap) {
            if (idx in orderedList.indices && rect.contains(fingerRootPos)) {
                return idx
            }
        }
        // Если палец попал между ячейками, ищем ближайший центр в пределах 260px
        var bestIdx = fromIdx
        var bestDist = Float.MAX_VALUE
        for ((idx, rect) in itemBoundsMap) {
            if (idx in orderedList.indices) {
                val dist = (rect.center - fingerRootPos).getDistance()
                if (dist < bestDist && dist < 260f) {
                    bestDist = dist
                    bestIdx = idx
                }
            }
        }
        return bestIdx
    }

    var isEmojiPanelOpen by remember { mutableStateOf(false) }
    val density = LocalDensity.current

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                rootBoxOrigin = coords.positionInRoot()
            }
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
                    if (previewOffsetPx > halfOffset + 80f || (previewOffsetPx > halfOffset && available.y > 2000f)) {
                        showDiscardConfirmDialog = true
                    } else if (previewOffsetPx > halfOffset) {
                        previewOffsetPx = halfOffset
                        onOffsetChange(halfOffset)
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
                                    if (previewOffsetPx > halfOffset + 80f || (previewOffsetPx > halfOffset && velocity > 2000f)) {
                                        showDiscardConfirmDialog = true
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
                                    if (previewOffsetPx > halfOffset + 80f || (previewOffsetPx > halfOffset && velocity > 2000f)) {
                                        showDiscardConfirmDialog = true
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
                                    .background(Color(0xFF141418), RoundedCornerShape(18.dp))
                                    .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(18.dp))
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "Отправить без сжатия",
                                            color = Color.White,
                                            fontSize = 14.5.sp
                                        )
                                    },
                                    onClick = {
                                        isMenuOpen = false
                                        onSend(orderedList.toList(), true, isUngrouped, isHd)
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

                                if (orderedList.size >= 2) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Отправить без группировки",
                                                color = Color.White,
                                                fontSize = 14.5.sp
                                            )
                                        },
                                        onClick = {
                                            isMenuOpen = false
                                            // Массовая отправка каждого фото отдельным сообщением по одному
                                            onSend(orderedList.toList(), isUncompressed, true, isHd)
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
                                            text = "Отправить в HD-качестве",
                                            color = Color.White,
                                            fontSize = 14.5.sp
                                        )
                                    },
                                    onClick = {
                                        isMenuOpen = false
                                        onSend(orderedList.toList(), isUncompressed, isUngrouped, true)
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
                    }

                    // Скроллируемая область с альбомами сообщений (как в Telegram: до 10 медиа в одном альбоме)
                    val listState = rememberLazyListState()
                    val albumChunks = remember(orderedList.toList(), isUngrouped) {
                        if (isUngrouped) orderedList.chunked(1) else orderedList.chunked(10)
                    }

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
                            val chunkCaption = if (chunkIdx == albumChunks.lastIndex) caption else ""

                            if (chunk.size == 1) {
                                SinglePhotoBubble(
                                    media = chunk[0],
                                    orderNumber = startOrderNumber,
                                    globalIndex = startGlobalIndex,
                                    caption = chunkCaption,
                                    draggingGlobalIdx = draggingGlobalIdx,
                                    onPhotoClick = { viewingPhotoUri = it },
                                    onRemoveItem = { idx -> removeItemAt(idx) },
                                    onBoundsChanged = { idx, rect -> itemBoundsMap[idx] = rect },
                                    onDragStart = { idx, startRootPos ->
                                        draggingGlobalIdx = idx
                                        dragFingerRootPos = startRootPos
                                    },
                                    onDragMove = { delta ->
                                        dragFingerRootPos += delta
                                    },
                                    onDragEnd = { fromIdx ->
                                        val targetIdx = findDropTargetIndex(dragFingerRootPos, fromIdx)
                                        draggingGlobalIdx = -1
                                        if (targetIdx != fromIdx) {
                                            swapItems(fromIdx, targetIdx)
                                        }
                                    },
                                    onDragCancel = {
                                        draggingGlobalIdx = -1
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                AlbumBubble(
                                    mediaItems = chunk,
                                    startOrderNumber = startOrderNumber,
                                    startGlobalIndex = startGlobalIndex,
                                    caption = chunkCaption,
                                    draggingGlobalIdx = draggingGlobalIdx,
                                    onPhotoClick = { viewingPhotoUri = it },
                                    onRemoveItem = { idx -> removeItemAt(idx) },
                                    onBoundsChanged = { idx, rect -> itemBoundsMap[idx] = rect },
                                    onDragStart = { idx, startRootPos ->
                                        draggingGlobalIdx = idx
                                        dragFingerRootPos = startRootPos
                                    },
                                    onDragMove = { delta ->
                                        dragFingerRootPos += delta
                                    },
                                    onDragEnd = { fromIdx ->
                                        val targetIdx = findDropTargetIndex(dragFingerRootPos, fromIdx)
                                        draggingGlobalIdx = -1
                                        if (targetIdx != fromIdx) {
                                            swapItems(fromIdx, targetIdx)
                                        }
                                    },
                                    onDragCancel = {
                                        draggingGlobalIdx = -1
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // Плашка "Перетащите для изменения порядка" (строго в самом низу списка)
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

        // 2.5. Парящий поверх всех фото и альбомов оверлей перетаскиваемой миниатюры (никогда не обрезается и не уезжает)
        if (draggingGlobalIdx in orderedList.indices) {
            val draggedMedia = orderedList[draggingGlobalIdx]
            val draggedOrderNum = draggingGlobalIdx + 1
            val cardW = 110.dp
            val cardH = 148.dp
            val cardWPx = with(density) { cardW.toPx() }
            val cardHPx = with(density) { cardH.toPx() }
            val relX = (dragFingerRootPos.x - rootBoxOrigin.x - cardWPx / 2f).roundToInt()
            val relY = (dragFingerRootPos.y - rootBoxOrigin.y - cardHPx / 2f).roundToInt()

            var dragBitmap by remember(draggedMedia.uri) { mutableStateOf<ImageBitmap?>(null) }
            LaunchedEffect(draggedMedia.uri) {
                val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, draggedMedia.uri) }
                if (dragBitmap == null) dragBitmap = quick?.asImageBitmap()
            }

            Box(
                modifier = Modifier
                    .offset { IntOffset(relX, relY) }
                    .size(width = cardW, height = cardH)
                    .zIndex(250f)
                    .shadow(24.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .border(2.dp, Color(0xFFC084FC), RoundedCornerShape(14.dp))
                    .background(Color(0xFF1E1E1E))
            ) {
                if (dragBitmap != null) {
                    Image(
                        bitmap = dragBitmap!!,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (draggedMedia.hasSpoiler) Modifier.blur(18.dp) else Modifier)
                    )
                }
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
                        text = draggedOrderNum.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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
 * - 7 элементов -> [3, 4] (сверху 3 больших, снизу 4 увеличенных вертикальных)
 * - 8 элементов -> [3, 2, 3] (сверху 3, посередине 2, снизу 3)
 * - 9 элементов -> [3, 3, 3]
 * - 10 элементов -> [3, 4, 3]
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
        8 -> listOf(3, 2, 3)
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
        2 -> 225.dp
        3 -> 210.dp
        4 -> 170.dp // Увеличенная вертикальная высота для ряда из 4 фото (в альбомах 7 и 10 фото)
        else -> 130.dp
    }
}

/**
 * Единый баббл альбома сообщений (до 10 фото в чанке), с фиолетовой рамкой и разделителями.
 * Для 2, 3 и 4 фото используются фирменные вертикальные раскладки Telegram (правые миниатюры тоже вертикальные, не квадратные).
 * Если передан непустой caption (для самого последнего альбома), он крепится внизу внутри этого же баббла.
 */
@Composable
private fun AlbumBubble(
    mediaItems: List<GalleryMedia>,
    startOrderNumber: Int,
    startGlobalIndex: Int,
    caption: String = "",
    draggingGlobalIdx: Int = -1,
    onPhotoClick: (Uri) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onBoundsChanged: (Int, Rect) -> Unit,
    onDragStart: (Int, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Int) -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(16.dp)
    val count = mediaItems.size

    // Для 2, 3, 4 фото сужаем баббл (~86%), чтобы и левое большое, и правые малые фото были выраженно вертикальными
    val bubbleWidthFraction = when (count) {
        2 -> 0.88f
        3, 4 -> 0.86f
        else -> 1f
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(bubbleWidthFraction)
                .animateContentSize()
                .clip(bubbleShape)
                .border(1.5.dp, Color(0xFFC084FC), bubbleShape)
                .background(Color(0xFF131317))
        ) {
            when (count) {
                2 -> {
                    // 2 фото: рядом друг с другом с разделительной полосой, каждое вертикальное (~3×6 см)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {
                        PreviewCollageItem(
                            media = mediaItems[0],
                            orderNumber = startOrderNumber,
                            globalIndex = startGlobalIndex,
                            isBeingDragged = draggingGlobalIdx == startGlobalIndex,
                            onPhotoClick = onPhotoClick,
                            onRemoveItem = onRemoveItem,
                            onBoundsChanged = onBoundsChanged,
                            onDragStart = onDragStart,
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
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
                            isBeingDragged = draggingGlobalIdx == startGlobalIndex + 1,
                            onPhotoClick = onPhotoClick,
                            onRemoveItem = onRemoveItem,
                            onBoundsChanged = onBoundsChanged,
                            onDragStart = onDragStart,
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
                3 -> {
                    // 3 фото: 1 большое вертикальное слева (weight 1.12f) + 2 меньших справа (weight 0.85f),
                    // при общей высоте 430.dp правые фото имеют пропорцию ~130×214dp (выраженно вертикальные, не квадрат!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(430.dp)
                    ) {
                        PreviewCollageItem(
                            media = mediaItems[0],
                            orderNumber = startOrderNumber,
                            globalIndex = startGlobalIndex,
                            isBeingDragged = draggingGlobalIdx == startGlobalIndex,
                            onPhotoClick = onPhotoClick,
                            onRemoveItem = onRemoveItem,
                            onBoundsChanged = onBoundsChanged,
                            onDragStart = onDragStart,
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                            modifier = Modifier
                                .weight(1.12f)
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
                                .weight(0.85f)
                                .fillMaxHeight()
                        ) {
                            PreviewCollageItem(
                                media = mediaItems[1],
                                orderNumber = startOrderNumber + 1,
                                globalIndex = startGlobalIndex + 1,
                                isBeingDragged = draggingGlobalIdx == startGlobalIndex + 1,
                                onPhotoClick = onPhotoClick,
                                onRemoveItem = onRemoveItem,
                                onBoundsChanged = onBoundsChanged,
                                onDragStart = onDragStart,
                                onDragMove = onDragMove,
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragCancel,
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
                                isBeingDragged = draggingGlobalIdx == startGlobalIndex + 2,
                                onPhotoClick = onPhotoClick,
                                onRemoveItem = onRemoveItem,
                                onBoundsChanged = onBoundsChanged,
                                onDragStart = onDragStart,
                                onDragMove = onDragMove,
                                onDragEnd = onDragEnd,
                                onDragCancel = onDragCancel,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                            )
                        }
                    }
                }
                4 -> {
                    // 4 фото: 1 большое вертикальное слева (weight 1.15f) + 3 вертикальных столбиком справа (weight 0.82f),
                    // при высоте 470.dp правые фото имеют размер ~125×155dp (вертикальные портретные!)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(470.dp)
                    ) {
                        PreviewCollageItem(
                            media = mediaItems[0],
                            orderNumber = startOrderNumber,
                            globalIndex = startGlobalIndex,
                            isBeingDragged = draggingGlobalIdx == startGlobalIndex,
                            onPhotoClick = onPhotoClick,
                            onRemoveItem = onRemoveItem,
                            onBoundsChanged = onBoundsChanged,
                            onDragStart = onDragStart,
                            onDragMove = onDragMove,
                            onDragEnd = onDragEnd,
                            onDragCancel = onDragCancel,
                            modifier = Modifier
                                .weight(1.15f)
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
                                .weight(0.82f)
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
                                    isBeingDragged = draggingGlobalIdx == startGlobalIndex + idx,
                                    onPhotoClick = onPhotoClick,
                                    onRemoveItem = onRemoveItem,
                                    onBoundsChanged = onBoundsChanged,
                                    onDragStart = onDragStart,
                                    onDragMove = onDragMove,
                                    onDragEnd = onDragEnd,
                                    onDragCancel = onDragCancel,
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
                                            isBeingDragged = draggingGlobalIdx == globalIdx,
                                            onPhotoClick = onPhotoClick,
                                            onRemoveItem = onRemoveItem,
                                            onBoundsChanged = onBoundsChanged,
                                            onDragStart = onDragStart,
                                            onDragMove = onDragMove,
                                            onDragEnd = onDragEnd,
                                            onDragCancel = onDragCancel,
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

            // Подпись крепится в конце самого последнего альбома
            if (caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color(0xFFC084FC))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF191922))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = caption,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Одиночный баббл фото (1 фото выбрано, либо одиночный остаток после разбивки по 10):
 * Стоит ровно по центру по горизонтали ниже шапки «Предпросмотр»,
 * вертикальный размер ~4.5 см по горизонтали и ~10 см по вертикали.
 */
@Composable
private fun SinglePhotoBubble(
    media: GalleryMedia,
    orderNumber: Int,
    globalIndex: Int,
    caption: String = "",
    draggingGlobalIdx: Int = -1,
    onPhotoClick: (Uri) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onBoundsChanged: (Int, Rect) -> Unit,
    onDragStart: (Int, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Int) -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bubbleShape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.68f)
                .animateContentSize()
                .clip(bubbleShape)
                .border(1.5.dp, Color(0xFFC084FC), bubbleShape)
                .background(Color(0xFF131317))
        ) {
            PreviewCollageItem(
                media = media,
                orderNumber = orderNumber,
                globalIndex = globalIndex,
                isBeingDragged = draggingGlobalIdx == globalIndex,
                onPhotoClick = onPhotoClick,
                onRemoveItem = onRemoveItem,
                onBoundsChanged = onBoundsChanged,
                onDragStart = onDragStart,
                onDragMove = onDragMove,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(520.dp)
            )
            if (caption.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .background(Color(0xFFC084FC))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF191922))
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = caption,
                        color = Color.White,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

/**
 * Элемент коллажа в экране предпросмотра:
 * - Одиночный тап по фото: полноэкранный просмотр
 * - Тап по круглому фиолетовому бейджу с номером: снятие выбора (фото исчезает, альбом плавно перестраивается)
 * - Долгое нажатие (зажатие): плавное перетаскивание поверх всех фото и альбомов через корневой оверлей
 */
@Composable
private fun PreviewCollageItem(
    media: GalleryMedia,
    orderNumber: Int,
    globalIndex: Int,
    isBeingDragged: Boolean,
    onPhotoClick: (Uri) -> Unit,
    onRemoveItem: (Int) -> Unit,
    onBoundsChanged: (Int, Rect) -> Unit,
    onDragStart: (Int, Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: (Int) -> Unit,
    onDragCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(media.uri) { mutableStateOf<ImageBitmap?>(null) }
    var itemRectInRoot by remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(media.uri) {
        val quick = withContext(Dispatchers.IO) { loadMediaThumbnail(context, media.uri) }
        if (bitmap == null) bitmap = quick?.asImageBitmap()
        val hd = withContext(Dispatchers.IO) { loadHdBitmap(context, media.uri) }
        if (hd != null) bitmap = hd.asImageBitmap()
    }

    Box(
        modifier = modifier
            .onGloballyPositioned { coords ->
                val rect = coords.boundsInRoot()
                itemRectInRoot = rect
                onBoundsChanged(globalIndex, rect)
            }
            .graphicsLayer {
                alpha = if (isBeingDragged) 0.25f else 1f
            }
            .background(Color(0xFF1E1E1E))
            .pointerInput(media.uri) {
                detectTapGestures(
                    onTap = { onPhotoClick(media.uri) }
                )
            }
            .pointerInput(globalIndex) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        val startRoot = Offset(
                            itemRectInRoot.left + startOffset.x,
                            itemRectInRoot.top + startOffset.y
                        )
                        onDragStart(globalIndex, startRoot)
                    },
                    onDragEnd = {
                        onDragEnd(globalIndex)
                    },
                    onDragCancel = {
                        onDragCancel()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        onDragMove(dragAmount)
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

        if (media.hasSpoiler) {
            SpoilerOverlay(modifier = Modifier.fillMaxSize())
        }

        // Фиолетовый бейдж с номером в правом верхнем углу: тап по бейджу отменяет выбор этого фото, и альбом перестраивается
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFC084FC))
                .border(1.5.dp, Color(0xFF1E1E1E), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    onRemoveItem(globalIndex)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = orderNumber.toString(),
                color = Color.White,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

