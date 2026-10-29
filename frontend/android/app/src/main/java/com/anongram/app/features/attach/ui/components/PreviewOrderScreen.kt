package com.anongram.app.features.attach.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.LayersClear
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.attach.ui.tabs.GalleryMedia
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.attach.ui.theme.AttachBorder
import com.anongram.app.features.attach.ui.theme.AttachCardDark
import com.anongram.app.features.attach.ui.theme.AttachPurple
import com.anongram.app.features.wallpapers.ui.WallpaperBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Экран предпросмотра и изменения порядка медиа (ТЗ: Экран предпросмотра выбора сообщений).
 * - Слева вверху: "< Предпросмотр"
 * - Справа вверху: "⋮" с выпадающим меню (Назад, Без сжатия, Без группировки, Спойлер, HD)
 * - Обои чата на фоне (WallpaperBackground)
 * - Мини-превью "Как будет выглядеть в чате" (бабл сообщения с коллажем)
 * - Карточки выбранных медиа с бейджами порядка (1), (2)...
 * - Переключение спойлера для каждого фото или для всех
 * - Перемещение порядка карточек (drag and drop / стрелки)
 * - Плашка внизу: "Перетащите для изменения порядка"
 * - Свободное перемещение и фиксация в любом положении от 50% до 100%
 * - Кнопка отправки с подписью
 */
@Composable
fun PreviewOrderScreen(
    initialMediaList: List<GalleryMedia>,
    caption: String,
    onCaptionChange: (String) -> Unit,
    onClose: () -> Unit,
    onSend: (List<GalleryMedia>, Boolean, Boolean, Boolean) -> Unit,
    onUpdateOrderAndSpoilers: (List<GalleryMedia>) -> Unit = {},
    chatId: String = "",
    modifier: Modifier = Modifier
) {
    val orderedList = remember { mutableStateListOf<GalleryMedia>().apply { addAll(initialMediaList) } }
    var isMenuOpen by remember { mutableStateOf(false) }
    var isUncompressed by remember { mutableStateOf(false) }
    var isUngrouped by remember { mutableStateOf(false) }
    var isHd by remember { mutableStateOf(false) }

    fun handleClose() {
        onUpdateOrderAndSpoilers(orderedList.toList())
        onClose()
    }

    BackHandler { handleClose() }

    val allHaveSpoiler = orderedList.isNotEmpty() && orderedList.all { it.hasSpoiler }

    fun toggleSpoilerForAll() {
        val target = !allHaveSpoiler
        val updated = orderedList.map { it.copy(hasSpoiler = target) }
        orderedList.clear()
        orderedList.addAll(updated)
        onUpdateOrderAndSpoilers(orderedList.toList())
    }

    fun toggleSpoilerForIndex(index: Int) {
        if (index in orderedList.indices) {
            val item = orderedList[index]
            orderedList[index] = item.copy(hasSpoiler = !item.hasSpoiler)
            onUpdateOrderAndSpoilers(orderedList.toList())
        }
    }

    fun swapItems(fromIdx: Int, toIdx: Int) {
        if (fromIdx in orderedList.indices && toIdx in orderedList.indices) {
            val item = orderedList.removeAt(fromIdx)
            orderedList.add(toIdx, item)
            onUpdateOrderAndSpoilers(orderedList.toList())
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
    ) {
        val H = constraints.maxHeight.toFloat()
        val halfOffset = H * 0.5f
        var previewOffsetPx by remember(H) { mutableFloatStateOf(0f) }

        // 1. Обои текущего чата на фоне
        if (chatId.isNotBlank()) {
            WallpaperBackground(
                chatId = chatId,
                modifier = Modifier.fillMaxSize()
            )
            // Затемнение для читаемости текста и элементов
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
            )
        }

        // 2. Контейнер экрана предпросмотра: поддерживает свайп и фиксацию от 50% до 100%
        Column(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationY = previewOffsetPx }
        ) {
            // Тонкая ручка для перемещения и фиксации от 50% до 100%
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { d ->
                            previewOffsetPx = (previewOffsetPx + d).coerceAtLeast(0f)
                        },
                        onDragStopped = { v ->
                            if (v > 850f || previewOffsetPx > halfOffset + 80f) {
                                handleClose()
                            } else {
                                // Свободная фиксация в любом положении от 50 до 100
                                previewOffsetPx = previewOffsetPx.coerceIn(0f, halfOffset)
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

            // Верхняя шапка: "< Предпросмотр" и "⋮"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeStatusBarPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
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
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Предпросмотр",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Кнопка "⋮" с выпадающим меню прямо у кнопки
                Box {
                    IconButton(onClick = { isMenuOpen = true }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "Опции",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = isMenuOpen,
                        onDismissRequest = { isMenuOpen = false },
                        modifier = Modifier
                            .background(AttachCardDark)
                            .border(1.dp, AttachBorder, RoundedCornerShape(14.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Назад", color = Color.White, fontSize = 14.sp) },
                            onClick = { isMenuOpen = false },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isUncompressed) "Отправить со сжатием" else "Отправить без сжатия",
                                    color = Color.White,
                                    fontSize = 14.sp
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
                        if (orderedList.size >= 2) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isUngrouped) "Сгруппировать" else "Отправить без группировки",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                },
                                onClick = {
                                    isUngrouped = !isUngrouped
                                    isMenuOpen = false
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.LayersClear,
                                        contentDescription = null,
                                        tint = if (isUngrouped) AttachPurple else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (allHaveSpoiler) "Отменить скрытие" else "Скрыть под спойлер",
                                    color = Color.White,
                                    fontSize = 14.sp
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
                                    fontSize = 14.sp
                                )
                            },
                            onClick = {
                                isHd = !isHd
                                isMenuOpen = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.HighQuality,
                                    contentDescription = null,
                                    tint = if (isHd) AttachPurple else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }

            // 3. Блок мини-превью как в чате (пузырь сообщения с коллажем фото)
            MiniChatBubblePreview(
                mediaList = orderedList,
                caption = caption,
                modifier = Modifier.fillMaxWidth()
            )

            // 4. Отображение карточек предпросмотра для изменения порядка
            if (orderedList.size <= 2) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    orderedList.forEachIndexed { index, item ->
                        PreviewMediaCard(
                            media = item,
                            orderNumber = index + 1,
                            canMoveLeft = index > 0,
                            canMoveRight = index < orderedList.size - 1,
                            onMoveLeft = { swapItems(index, index - 1) },
                            onMoveRight = { swapItems(index, index + 1) },
                            onToggleSpoiler = { toggleSpoilerForIndex(index) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    itemsIndexed(orderedList, key = { _, item -> item.uri.toString() }) { index, item ->
                        PreviewMediaCard(
                            media = item,
                            orderNumber = index + 1,
                            canMoveLeft = index > 0,
                            canMoveRight = index < orderedList.size - 1,
                            onMoveLeft = { swapItems(index, index - 1) },
                            onMoveRight = { swapItems(index, index + 1) },
                            onToggleSpoiler = { toggleSpoilerForIndex(index) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.72f)
                        )
                    }
                }
            }
        }

        // 5. Нижняя часть: плашка "Перетащите для изменения порядка" и панель отправки
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Плашка "Перетащите для изменения порядка"
            Box(
                modifier = Modifier
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF26262E))
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

            // Нижняя панель ввода и круглая кнопка отправки
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
    }
}

/**
 * Мини-превью "Как будет выглядеть в чате": пузырь сообщения Telegram-style
 */
@Composable
private fun MiniChatBubblePreview(
    mediaList: List<GalleryMedia>,
    caption: String,
    modifier: Modifier = Modifier
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val currentTime = remember { timeFormat.format(Date()) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.End
    ) {
        // Подпись над превью
        Text(
            text = "ПРЕДПРОСМОТР В ЧАТЕ",
            color = Color(0xFFC0B8DC),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            modifier = Modifier.padding(bottom = 3.dp, end = 4.dp)
        )

        // Пузырь сообщения
        Box(
            modifier = Modifier
                .widthIn(max = 260.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                .background(Color(0xFF261D3B))
                .border(1.dp, Color(0x338B5CF6), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp))
                .padding(3.dp)
        ) {
            Column {
                if (mediaList.size == 1) {
                    MiniPhotoTile(
                        media = mediaList[0],
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else if (mediaList.size == 2) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(105.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        MiniPhotoTile(
                            media = mediaList[0],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp))
                        )
                        MiniPhotoTile(
                            media = mediaList[1],
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp))
                        )
                    }
                } else {
                    // 3+ фото: сетка в 2 ряда
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(75.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            MiniPhotoTile(
                                media = mediaList[0],
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topStart = 12.dp))
                            )
                            MiniPhotoTile(
                                media = mediaList[1],
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(topEnd = 12.dp))
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(75.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            for (i in 2 until minOf(4, mediaList.size)) {
                                MiniPhotoTile(
                                    media = mediaList[i],
                                    extraCount = if (i == 3 && mediaList.size > 4) mediaList.size - 4 else 0,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(
                                            if (i == 2) RoundedCornerShape(bottomStart = 12.dp)
                                            else RoundedCornerShape(bottomEnd = 12.dp)
                                        )
                                )
                            }
                        }
                    }
                }

                // Строка подписи и времени отправки
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (caption.isNotBlank()) {
                        Text(
                            text = caption,
                            color = Color.White,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 4.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = currentTime, color = Color(0xFFC0B0E0), fontSize = 10.sp)
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF8B5CF6),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Миниатюра фото для пузыря сообщения предпросмотра
 */
@Composable
private fun MiniPhotoTile(
    media: GalleryMedia,
    extraCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(media.uri) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(media.uri) {
        val bmp = withContext(Dispatchers.IO) { loadMediaThumbnail(context, media.uri) }
        bitmap = bmp?.asImageBitmap()
    }

    Box(modifier = modifier.background(Color(0xFF1F1F24))) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        if (media.hasSpoiler) {
            SpoilerOverlay(modifier = Modifier.fillMaxSize())
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$extraCount",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Карточка медиа на экране предпросмотра с бейджем порядка (1), (2)...,
 * эффектом спойлера, перетаскиванием и кнопками перемещения.
 */
@Composable
private fun PreviewMediaCard(
    media: GalleryMedia,
    orderNumber: Int,
    canMoveLeft: Boolean,
    canMoveRight: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onToggleSpoiler: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bitmap by remember(media.uri) { mutableStateOf<ImageBitmap?>(null) }
    var dragAccumulated by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(media.uri) {
        val bmp = withContext(Dispatchers.IO) {
            loadMediaThumbnail(context, media.uri)
        }
        bitmap = bmp?.asImageBitmap()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = 1.dp,
                color = if (media.hasSpoiler) Color(0xFF8B5CF6).copy(alpha = 0.8f) else Color(0x33FFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .background(Color(0xFF1E1E1E))
            .pointerInput(media.uri) {
                detectHorizontalDragGestures(
                    onDragEnd = { dragAccumulated = 0f },
                    onDragCancel = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulated += dragAmount
                        if (dragAccumulated > 60f && canMoveRight) {
                            onMoveRight()
                            dragAccumulated = 0f
                        } else if (dragAccumulated < -60f && canMoveLeft) {
                            onMoveLeft()
                            dragAccumulated = 0f
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
                modifier = Modifier.fillMaxSize()
            )
        }

        // Анимированный спойлер, если включён
        if (media.hasSpoiler) {
            SpoilerOverlay(modifier = Modifier.fillMaxSize())
        }

        // Бейдж порядка в верхнем правом углу: круглая фиолетовая плашка с цифрой
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(AttachPurple),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = orderNumber.toString(),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Нижняя панель действий на карточке: кнопка Спойлера слева и стрелки перестановки
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Кнопка спойлера для этой карточки
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (media.hasSpoiler) AttachPurple else Color(0x33FFFFFF))
                    .clickable { onToggleSpoiler() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (media.hasSpoiler) Icons.Outlined.VisibilityOff else Icons.Outlined.AutoAwesome,
                        contentDescription = "Спойлер",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Спойлер",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Стрелки перемещения порядка
            Row {
                if (canMoveLeft) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable { onMoveLeft() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("←", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (canMoveRight) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable { onMoveRight() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("→", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
