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
import androidx.compose.material.icons.outlined.InsertDriveFile
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
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
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
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.attach.ui.tabs.GalleryMedia
import com.anongram.app.features.attach.ui.tabs.loadMediaThumbnail
import com.anongram.app.features.attach.ui.theme.AttachBorder
import com.anongram.app.features.attach.ui.theme.AttachCardDark
import com.anongram.app.features.attach.ui.theme.AttachPurple
import com.anongram.app.features.wallpapers.ui.WallpaperBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Экран предпросмотра и изменения порядка медиа (ТЗ: Экран предпросмотра выбора сообщений).
 * - Открывается на том же уровне высоты, где находилась шторка (initialOffsetPx)
 * - Свободно тащится вверх (до 100%) и вниз (до закрытия)
 * - Слева вверху: "< Предпросмотр"
 * - Справа вверху: кнопка "⋮" с выпадающим меню (Без сжатия, HD, Спойлер)
 * - На фоне: обои текущего чата (WallpaperBackground) с мягким затемнением
 * - В центре: карточки в рамке с фиолетовой обводкой (border 1.5.dp AttachPurple), как на макете
 * - На карточках: бейджи порядка (1), (2)..., качественный блюр и мерцающие частицы спойлера
 * - Тап по карточке переключает спойлер; свайп меняет порядок фото
 * - Внизу: плашка "Перетащите для изменения порядка" и поле ввода с кнопкой отправки
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
    initialOffsetPx: Float = 0f,
    onOffsetChange: (Float) -> Unit = {},
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
        if (fromIdx in orderedList.indices && toIdx in orderedList.indices && fromIdx != toIdx) {
            val item = orderedList.removeAt(fromIdx)
            orderedList.add(toIdx, item)
            onUpdateOrderAndSpoilers(orderedList.toList())
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
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

        // 2. Контейнер экрана предпросмотра на фоне обоев чата (макет media_1793295326790.png)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeStatusBarPadding()
        ) {
            // Верхняя шапка: "< Предпросмотр" и только кнопка "⋮" справа
            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                            .background(AttachCardDark)
                            .border(1.dp, AttachBorder, RoundedCornerShape(14.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (isUncompressed) "Отменить сжатие" else "Отправить без сжатия",
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

                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = if (allHaveSpoiler) "Отменить спойлер" else "Скрыть под спойлер",
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

            Spacer(modifier = Modifier.weight(0.08f))

            // 3. Основная область: карточки медиа в рамке с фиолетовой обводкой (1 в 1 как на макете media_1793295326790.png)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, Color(0xFFC084FC), RoundedCornerShape(20.dp))
                    .background(Color(0xFF131317))
            ) {
                if (orderedList.size == 2) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        PreviewCollageItem(
                            media = orderedList[0],
                            orderNumber = 1,
                            onToggleSpoiler = { toggleSpoilerForIndex(0) },
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
                            onToggleSpoiler = { toggleSpoilerForIndex(1) },
                            onDragSwap = { swapItems(1, 0) },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                } else if (orderedList.size == 1) {
                    PreviewCollageItem(
                        media = orderedList[0],
                        orderNumber = 1,
                        onToggleSpoiler = { toggleSpoilerForIndex(0) },
                        onDragSwap = {},
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(orderedList, key = { _, item -> item.uri.toString() }) { index, item ->
                            PreviewCollageItem(
                                media = item,
                                orderNumber = index + 1,
                                onToggleSpoiler = { toggleSpoilerForIndex(index) },
                                onDragSwap = {
                                    val target = if (index < orderedList.size - 1) index + 1 else index - 1
                                    swapItems(index, target)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(0.72f)
                            )
                        }
                    }
                }
            }

            // 4. Плашка "Перетащите для изменения порядка" (макет media_1793295326790.png)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 10.dp)
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

            Spacer(modifier = Modifier.weight(0.08f))

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
    }
}

/**
 * Элемент коллажа в экране предпросмотра:
 * - Качественный Gaussian Blur при включении спойлера
 * - Эффект мерцающих частиц спойлера
 * - Фиолетовый кружок с номером порядка (1), (2)...
 * - Тап переключает спойлер
 * - Свайп влево/вправо меняет порядок
 */
@Composable
private fun PreviewCollageItem(
    media: GalleryMedia,
    orderNumber: Int,
    onToggleSpoiler: () -> Unit,
    onDragSwap: () -> Unit,
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
            .background(Color(0xFF1E1E1E))
            .clickable { onToggleSpoiler() }
            .pointerInput(media.uri) {
                detectHorizontalDragGestures(
                    onDragEnd = { dragAccumulated = 0f },
                    onDragCancel = { dragAccumulated = 0f },
                    onHorizontalDrag = { _, dragAmount ->
                        dragAccumulated += dragAmount
                        if (kotlin.math.abs(dragAccumulated) > 60f) {
                            onDragSwap()
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
    }
}
