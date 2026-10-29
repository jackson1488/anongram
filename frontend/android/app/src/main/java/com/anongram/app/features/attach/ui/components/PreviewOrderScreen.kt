package com.anongram.app.features.attach.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.attach.ui.tabs.GalleryMedia
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.attach.ui.theme.AttachBorder
import com.anongram.app.features.attach.ui.theme.AttachPurple
import com.anongram.app.features.wallpapers.ui.WallpaperBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Экран предпросмотра и изменения порядка медиа:
 * - Открывается на том же уровне высоты, где находилась шторка (initialOffsetPx)
 * - Сверху тонкая ручка; при свайпе вниз появляется диалог "Сбросить выбор?"
 * - Кнопка "< Предпросмотр" и системный жест "Назад" возвращают в галерею
 * - Одиночный тап по фото: открывает полноэкранный просмотр фото
 * - Зажатие (long-press): карточка уменьшается (scale 0.92) и перетаскивается с обменом местами (swap)
 * - Сетка коллажа адаптируется до 20 элементов в альбоме
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
    var showDiscardConfirmDialog by remember { mutableStateOf(false) }
    var viewingPhotoUri by remember { mutableStateOf<Uri?>(null) }

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

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val H = constraints.maxHeight.toFloat()
        val halfOffset = H * 0.5f
        var previewOffsetPx by remember(H) { mutableFloatStateOf(initialOffsetPx.coerceIn(0f, halfOffset)) }

        // 1. Обои текущего чата на фоне с затемнением
        if (chatId.isNotBlank()) {
            WallpaperBackground(
                chatId = chatId,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F0F12))
            )
        }

        // 2. Контейнер экрана предпросмотра на текущей высоте шторки
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = previewOffsetPx }
                .background(
                    color = Color(0xFF131317),
                    shape = RoundedCornerShape(topStart = if (previewOffsetPx > 15f) 20.dp else 0.dp, topEnd = if (previewOffsetPx > 15f) 20.dp else 0.dp)
                )
                .safeStatusBarPadding()
        ) {
            // Тонкая ручка для перемещения шторки вверх/вниз
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { d ->
                            previewOffsetPx = (previewOffsetPx + d).coerceAtLeast(0f)
                            onOffsetChange(previewOffsetPx)
                        },
                        onDragStopped = { v ->
                            if (v > 850f || previewOffsetPx > halfOffset + 80f) {
                                // Потянули вниз: показываем диалог "Сбросить выбор?"
                                showDiscardConfirmDialog = true
                            } else if (previewOffsetPx < halfOffset * 0.5f) {
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
                        .size(width = 36.dp, height = 4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x66FFFFFF))
                )
            }

            // Верхняя шапка: "< Предпросмотр" и только кнопка "⋮" справа
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp),
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

            Spacer(modifier = Modifier.weight(0.04f))

            // 3. Основная область: мини-коллаж сообщения (ТЗ: точно по макету media_1793295326790.png)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
                    .padding(horizontal = 24.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, Color(0xFFC084FC), RoundedCornerShape(20.dp))
                    .background(Color(0xFF131317))
            ) {
                when {
                    orderedList.size == 1 -> {
                        PreviewCollageItem(
                            media = orderedList[0],
                            orderNumber = 1,
                            onPhotoClick = { viewingPhotoUri = it },
                            onDragSwap = {},
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    orderedList.size == 2 -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            PreviewCollageItem(
                                media = orderedList[0],
                                orderNumber = 1,
                                onPhotoClick = { viewingPhotoUri = it },
                                onDragSwap = { swapItems(0, 1) },
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
                                media = orderedList[1],
                                orderNumber = 2,
                                onPhotoClick = { viewingPhotoUri = it },
                                onDragSwap = { swapItems(1, 0) },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }
                    orderedList.size == 3 -> {
                        Row(modifier = Modifier.fillMaxSize()) {
                            PreviewCollageItem(
                                media = orderedList[0],
                                orderNumber = 1,
                                onPhotoClick = { viewingPhotoUri = it },
                                onDragSwap = { swapItems(0, 1) },
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
                            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                PreviewCollageItem(
                                    media = orderedList[1],
                                    orderNumber = 2,
                                    onPhotoClick = { viewingPhotoUri = it },
                                    onDragSwap = { swapItems(1, 2) },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.5.dp)
                                        .background(Color(0xFFC084FC))
                                )
                                PreviewCollageItem(
                                    media = orderedList[2],
                                    orderNumber = 3,
                                    onPhotoClick = { viewingPhotoUri = it },
                                    onDragSwap = { swapItems(2, 0) },
                                    modifier = Modifier.fillMaxWidth().weight(1f)
                                )
                            }
                        }
                    }
                    else -> {
                        // Для 4-20 фото: адаптивная сетка коллажа
                        val cols = if (orderedList.size <= 4) 2 else 3
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(cols),
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            itemsIndexed(orderedList, key = { _, item -> item.uri.toString() }) { index, item ->
                                PreviewCollageItem(
                                    media = item,
                                    orderNumber = index + 1,
                                    onPhotoClick = { viewingPhotoUri = it },
                                    onDragSwap = {
                                        val target = if (index < orderedList.size - 1) index + 1 else index - 1
                                        swapItems(index, target)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Плашка "Перетащите для изменения порядка" (макет media_1793295326790.png)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0x993E2E28))
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

            // Свободное пространство с обоями чата
            Spacer(modifier = Modifier.weight(1f))

            // 5. Нижняя панель ввода подписи и круглая кнопка отправки с бейджем количества
            AttachInputBar(
                caption = caption,
                onCaptionChange = onCaptionChange,
                selectedCount = orderedList.size,
                visible = true,
                onSend = {
                    onSend(orderedList.toList(), isUncompressed, isUngrouped, isHd)
                },
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
                            handleClose()
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
 * Элемент коллажа в экране предпросмотра:
 * - Одиночный тап: полноэкранный просмотр
 * - Долгое нажатие (зажатие): карточка уменьшается (scale 0.92) и перетаскивается пальцем (drag & drop)
 * - Спойлерный блюр и мерцающие частицы, если спойлер включён
 * - Фиолетовый круглый бейдж порядка (1), (2)...
 */
@Composable
private fun PreviewCollageItem(
    media: GalleryMedia,
    orderNumber: Int,
    onPhotoClick: (Uri) -> Unit,
    onDragSwap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(media.uri) { mutableStateOf<ImageBitmap?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var dragAccumulated by remember { mutableStateOf(Offset.Zero) }

    val scale by animateFloatAsState(targetValue = if (isDragging) 0.92f else 1.0f)
    val zIndexVal = if (isDragging) 10f else 1f

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
                translationX = if (isDragging) dragAccumulated.x * 0.35f else 0f
                translationY = if (isDragging) dragAccumulated.y * 0.35f else 0f
            }
            .background(Color(0xFF1E1E1E))
            .pointerInput(media.uri) {
                detectTapGestures(
                    onTap = { onPhotoClick(media.uri) }
                )
            }
            .pointerInput(orderNumber) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {
                        isDragging = true
                        dragAccumulated = Offset.Zero
                    },
                    onDragEnd = {
                        isDragging = false
                        dragAccumulated = Offset.Zero
                    },
                    onDragCancel = {
                        isDragging = false
                        dragAccumulated = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulated += dragAmount
                        if (dragAccumulated.getDistance() > 80f) {
                            onDragSwap()
                            dragAccumulated = Offset.Zero
                        }
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
                .padding(8.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFFC084FC))
                .border(1.5.dp, Color(0xFF1E1E1E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = orderNumber.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
