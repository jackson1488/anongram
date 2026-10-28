package com.anongram.app.features.mediaeditor.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropRotate
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.features.attach.api.MediaTtl
import com.anongram.app.features.mediaeditor.api.EditableMediaItem
import com.anongram.app.features.mediaeditor.api.EditedMedia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private val PurpleAccent = Color(0xFF8B5CF6)
private val DarkBg = Color(0xFF0A0F1D)

@Composable
fun MediaEditorContent(
    items: List<EditableMediaItem>,
    recipientName: String,
    onSend: (List<EditedMedia>) -> Unit,
    onAddMore: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val currentItems = remember(items) {
        mutableStateListOf<EditableMediaItem>().apply { addAll(items) }
    }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { currentItems.size })
    val currentIndex = pagerState.currentPage.coerceIn(0, currentItems.lastIndex)

    var showThumbnailsStrip by remember { mutableStateOf(false) }
    var showTtlMenu by remember { mutableStateOf(false) }
    var showTuneDialog by remember { mutableStateOf(false) }
    var isBrushMode by remember { mutableStateOf(false) }

    // Brush strokes: list of paths (points)
    val brushPaths = remember { mutableStateListOf<List<Offset>>() }
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }

    val activeItem = currentItems[currentIndex]

    fun updateCurrentItem(transform: (EditableMediaItem) -> EditableMediaItem) {
        val updated = transform(currentItems[currentIndex])
        currentItems[currentIndex] = updated
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // Main Horizontal Pager for Media
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = currentItems[page]
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                EditorMediaPreview(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Brush Canvas Overlay
        if (isBrushMode) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = listOf(offset)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                currentPath = currentPath + change.position
                            },
                            onDragEnd = {
                                if (currentPath.isNotEmpty()) {
                                    brushPaths.add(currentPath)
                                    currentPath = emptyList()
                                }
                            }
                        )
                    }
            ) {
                for (path in brushPaths) {
                    for (i in 0 until path.size - 1) {
                        drawLine(
                            color = PurpleAccent,
                            start = path[i],
                            end = path[i + 1],
                            strokeWidth = 6.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                for (i in 0 until currentPath.size - 1) {
                    drawLine(
                        color = PurpleAccent,
                        start = currentPath[i],
                        end = currentPath[i + 1],
                        strokeWidth = 6.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x66000000))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }

            // Recipient title and count pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x66000000))
                    .clickable { showThumbnailsStrip = !showThumbnailsStrip }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (recipientName.isNotBlank()) recipientName else "Предпросмотр",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PurpleAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${currentIndex + 1}/${currentItems.size}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Purple checkmark button
            IconButton(
                onClick = {
                    scope.launch {
                        exportAndSend(context, currentItems, onSend)
                    }
                },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(PurpleAccent)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirm",
                    tint = Color.White
                )
            }
        }

        // Thumbnails Strip (collapsible)
        AnimatedVisibility(
            visible = showThumbnailsStrip,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(top = 76.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xDD0F1626))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currentItems.forEachIndexed { idx, item ->
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(
                                width = if (idx == currentIndex) 2.dp else 0.dp,
                                color = if (idx == currentIndex) PurpleAccent else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                scope.launch { pagerState.scrollToPage(idx) }
                            }
                    ) {
                        EditorThumbnail(item = item)
                    }
                }
            }
        }

        // Bottom Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp)
        ) {
            // Caption Input Row with AddMore and TTL buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = activeItem.caption,
                    onValueChange = { newCap ->
                        updateCurrentItem { it.copy(caption = newCap) }
                    },
                    placeholder = {
                        Text("Добавить подпись...", color = Color(0xFF94A3B8), fontSize = 14.sp)
                    },
                    leadingIcon = {
                        IconButton(onClick = onAddMore) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add more",
                                tint = Color.White
                            )
                        }
                    },
                    trailingIcon = {
                        Box {
                            IconButton(onClick = { showTtlMenu = true }) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Timer",
                                    tint = if (activeItem.ttl != MediaTtl.Never) PurpleAccent else Color(0xFF94A3B8)
                                )
                            }

                            // TTL Pop-up Menu
                            DropdownMenu(
                                expanded = showTtlMenu,
                                onDismissRequest = { showTtlMenu = false },
                                modifier = Modifier
                                    .background(Color(0xFF182238))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Выберите, через какое время после открытия медиафайл исчезнет:",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(8.dp)
                                )

                                val ttlOptions = listOf(
                                    "Один просмотр" to MediaTtl.OneView,
                                    "3 сек." to MediaTtl.Sec3,
                                    "10 сек." to MediaTtl.Sec10,
                                    "30 сек." to MediaTtl.Sec30,
                                    "Никогда" to MediaTtl.Never
                                )

                                ttlOptions.forEach { (title, ttlVal) ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                RadioButton(
                                                    selected = (activeItem.ttl == ttlVal),
                                                    onClick = null,
                                                    colors = RadioButtonDefaults.colors(selectedColor = PurpleAccent)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = title, color = Color.White, fontSize = 14.sp)
                                            }
                                        },
                                        onClick = {
                                            updateCurrentItem { it.copy(ttl = ttlVal) }
                                            showTtlMenu = false
                                        }
                                    )
                                }

                                Text(
                                    text = "Получатель всё равно может сфотографировать экран",
                                    color = Color(0xFF64748B),
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xCC131D31),
                        unfocusedContainerColor = Color(0xCC131D31),
                        focusedBorderColor = PurpleAccent,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Purple Send Button
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(PurpleAccent)
                        .clickable {
                            scope.launch {
                                exportAndSend(context, currentItems, onSend)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tools Pill Bar (Rotate, Brush, SD/HD, Tune Settings)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xCC131D31))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rotate / Crop
                IconButton(
                    onClick = {
                        val nextRotation = (activeItem.rotationDegrees + 90) % 360
                        updateCurrentItem { it.copy(rotationDegrees = nextRotation) }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.CropRotate,
                        contentDescription = "Rotate",
                        tint = if (activeItem.rotationDegrees != 0) PurpleAccent else Color.White
                    )
                }

                // Brush
                IconButton(
                    onClick = { isBrushMode = !isBrushMode }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Brush,
                        contentDescription = "Brush",
                        tint = if (isBrushMode) PurpleAccent else Color.White
                    )
                }

                // SD / HD toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            updateCurrentItem { it.copy(hd = !it.hd) }
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.HighQuality,
                        contentDescription = "Quality",
                        tint = if (activeItem.hd) PurpleAccent else Color(0xFF94A3B8),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (activeItem.hd) "HD" else "SD",
                        color = if (activeItem.hd) PurpleAccent else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Tune Dialog (Brightness, Contrast, Saturation)
                IconButton(onClick = { showTuneDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Tune",
                        tint = if (activeItem.brightness != 0f || activeItem.contrast != 1f || activeItem.saturation != 1f) {
                            PurpleAccent
                        } else Color.White
                    )
                }
            }
        }
    }

    // Tune Settings Dialog
    if (showTuneDialog) {
        AlertDialog(
            onDismissRequest = { showTuneDialog = false },
            title = {
                Text("Настройки изображения", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("Яркость: ${String.format("%.1f", activeItem.brightness)}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Slider(
                        value = activeItem.brightness,
                        onValueChange = { b -> updateCurrentItem { it.copy(brightness = b) } },
                        valueRange = -50f..50f,
                        colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Контраст: ${String.format("%.1f", activeItem.contrast)}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Slider(
                        value = activeItem.contrast,
                        onValueChange = { c -> updateCurrentItem { it.copy(contrast = c) } },
                        valueRange = 0.5f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Насыщенность: ${String.format("%.1f", activeItem.saturation)}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                    Slider(
                        value = activeItem.saturation,
                        onValueChange = { s -> updateCurrentItem { it.copy(saturation = s) } },
                        valueRange = 0f..2.0f,
                        colors = SliderDefaults.colors(thumbColor = PurpleAccent, activeTrackColor = PurpleAccent)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTuneDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleAccent)
                ) {
                    Text("Готово", color = Color.White)
                }
            },
            containerColor = Color(0xFF182238),
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun EditorMediaPreview(
    item: EditableMediaItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var imageBitmap by remember(item.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.uri) {
        withContext(Dispatchers.IO) {
            try {
                val input = context.contentResolver.openInputStream(item.uri)
                val bmp = BitmapFactory.decodeStream(input)
                imageBitmap = bmp?.asImageBitmap()
            } catch (_: Exception) {}
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (imageBitmap != null) {
            val colorMatrix = remember(item.brightness, item.contrast, item.saturation) {
                ColorMatrix().apply {
                    setToSaturation(item.saturation)
                    // Apply brightness & contrast
                    val scale = item.contrast
                    val translate = item.brightness
                    val array = floatArrayOf(
                        scale, 0f, 0f, 0f, translate,
                        0f, scale, 0f, 0f, translate,
                        0f, 0f, scale, 0f, translate,
                        0f, 0f, 0f, 1f, 0f
                    )
                    timesAssign(ColorMatrix(array))
                }
            }

            Image(
                bitmap = imageBitmap!!,
                contentDescription = null,
                colorFilter = ColorFilter.colorMatrix(colorMatrix),
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(item.rotationDegrees.toFloat())
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = if (item.isVideo) Icons.Rounded.Videocam else Icons.Default.HighQuality,
                    contentDescription = null,
                    tint = PurpleAccent,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (item.isVideo) "Видеозапись" else "Загрузка изображения...",
                    color = Color(0xFF94A3B8),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun EditorThumbnail(item: EditableMediaItem) {
    val context = LocalContext.current
    var thumbBitmap by remember(item.uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(item.uri) {
        withContext(Dispatchers.IO) {
            try {
                val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                val input = context.contentResolver.openInputStream(item.uri)
                val bmp = BitmapFactory.decodeStream(input, null, opts)
                thumbBitmap = bmp?.asImageBitmap()
            } catch (_: Exception) {}
        }
    }

    if (thumbBitmap != null) {
        Image(
            bitmap = thumbBitmap!!,
            contentDescription = null,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF1E293B)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (item.isVideo) Icons.Rounded.Videocam else Icons.Default.HighQuality,
                contentDescription = null,
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

private suspend fun exportAndSend(
    context: Context,
    items: List<EditableMediaItem>,
    onSend: (List<EditedMedia>) -> Unit
) {
    val results = withContext(Dispatchers.IO) {
        items.map { item ->
            // Copy to private cache and clean EXIF
            val extension = if (item.isVideo) "mp4" else "jpg"
            val targetFile = File(context.cacheDir, "edited_media_${System.currentTimeMillis()}_${item.id.take(4)}.$extension")

            try {
                if (!item.isVideo) {
                    val input = context.contentResolver.openInputStream(item.uri)
                    val originalBitmap = BitmapFactory.decodeStream(input)
                    if (originalBitmap != null) {
                        val maxDim = if (item.hd) 2048 else 1280
                        val scale = (maxDim.toFloat() / maxOf(originalBitmap.width, originalBitmap.height)).coerceAtMost(1f)
                        val scaledWidth = (originalBitmap.width * scale).toInt().coerceAtLeast(1)
                        val scaledHeight = (originalBitmap.height * scale).toInt().coerceAtLeast(1)

                        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

                        val matrix = Matrix()
                        if (item.rotationDegrees != 0) {
                            matrix.postRotate(item.rotationDegrees.toFloat())
                        }
                        val finalBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0, scaledWidth, scaledHeight, matrix, true)

                        FileOutputStream(targetFile).use { fos ->
                            finalBitmap.compress(Bitmap.CompressFormat.JPEG, if (item.hd) 92 else 82, fos)
                        }
                    }
                } else {
                    // Video copy to cache
                    context.contentResolver.openInputStream(item.uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            } catch (_: Exception) {}

            EditedMedia(
                uri = if (targetFile.exists() && targetFile.length() > 0) Uri.fromFile(targetFile) else item.uri,
                isVideo = item.isVideo,
                caption = item.caption,
                hd = item.hd,
                ttl = item.ttl
            )
        }
    }
    onSend(results)
}
