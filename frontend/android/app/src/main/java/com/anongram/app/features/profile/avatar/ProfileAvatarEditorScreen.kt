package com.anongram.app.features.profile.avatar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.view.TextureView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Rotate90DegreesCcw
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.mediaeditor.api.EditableMediaItem
import com.anongram.app.features.chats.mediaeditor.engine.BitmapExportRenderer
import com.anongram.app.features.chats.mediaeditor.engine.PhotoFilterEngine
import com.anongram.app.features.chats.mediaeditor.model.CropTransform
import com.anongram.app.features.chats.mediaeditor.model.PhotoEditState
import com.anongram.app.features.chats.mediaeditor.ui.adjust.AdjustSubTab
import com.anongram.app.features.chats.mediaeditor.ui.adjust.AdjustmentsPanel
import com.anongram.app.features.chats.mediaeditor.ui.paint.PaintModeTab
import com.anongram.app.features.chats.mediaeditor.ui.paint.PaintOverlayPanel
import com.anongram.app.features.chats.mediaeditor.ui.paint.StrokesPreviewCanvas
import com.anongram.app.features.chats.mediaeditor.ui.video.VideoTimelineBar
import com.anongram.app.features.profile.i18n.ProfileI18n
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class AvatarEditorSubMode {
    CROP,
    PAINT,
    ADJUST
}

/**
 * Редактор аватара профиля с круглым кропом и наследованием инструментов [MediaEditorContent]:
 * - Поворот, зеркалирование, рисование, HD (для фото) и улучшение цвета;
 * - Жесты pinch-to-zoom и перемещение внутри круга;
 * - Для видео: дорожка обрезки [VideoTimelineBar] с авто-ограничением до 20 секунд
 *   и выбором кадра миниатюры.
 */
@Composable
fun ProfileAvatarEditorScreen(
    uri: Uri,
    isVideo: Boolean,
    initialDurationMs: Long,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var subMode by remember { mutableStateOf(AvatarEditorSubMode.CROP) }
    var isSaving by remember { mutableStateOf(false) }

    // Трансформация круглого кропа
    var zoom by remember { mutableFloatStateOf(1f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var rotationDeg by remember { mutableIntStateOf(0) }
    var mirrorH by remember { mutableStateOf(false) }
    var hdEnabled by remember { mutableStateOf(true) }

    // Наследуемое состояние MediaEditor (рисование + улучшение)
    var editState by remember { mutableStateOf(PhotoEditState()) }
    var adjustTab by remember { mutableStateOf(AdjustSubTab.SLIDERS) }

    // Видео-состояние (авто-ограничение 20с)
    var detectedDurationMs by remember(uri, initialDurationMs) {
        mutableLongStateOf(initialDurationMs.coerceAtLeast(1_000L))
    }
    var trimStartMs by remember { mutableLongStateOf(0L) }
    var trimEndMs by remember {
        mutableLongStateOf(
            if (initialDurationMs > 0L) initialDurationMs.coerceAtMost(MAX_PROFILE_VIDEO_MS)
            else MAX_PROFILE_VIDEO_MS
        )
    }
    var thumbnailFrameMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentPosMs by remember { mutableLongStateOf(0L) }

    // Декодирование фото-превью
    var sourceBitmap by remember(uri, isVideo) { mutableStateOf<Bitmap?>(null) }
    var filteredPreview by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri, isVideo) {
        if (!isVideo) {
            sourceBitmap = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                }.getOrNull()
            }
        } else {
            val dur = withContext(Dispatchers.IO) {
                val retriever = MediaMetadataRetriever()
                runCatching {
                    retriever.setDataSource(context, uri)
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull() ?: initialDurationMs
                }.getOrDefault(initialDurationMs).also {
                    runCatching { retriever.release() }
                }
            }
            if (dur > 0L) {
                detectedDurationMs = dur
                trimStartMs = 0L
                trimEndMs = dur.coerceAtMost(MAX_PROFILE_VIDEO_MS)
                thumbnailFrameMs = 0L
            }
        }
    }

    LaunchedEffect(sourceBitmap, editState.adjustments, editState.blur, editState.curves) {
        val src = sourceBitmap ?: return@LaunchedEffect
        filteredPreview = withContext(Dispatchers.Default) {
            runCatching {
                PhotoFilterEngine.applyPixelPipeline(
                    source = src,
                    state = editState
                ).asImageBitmap()
            }.getOrNull() ?: src.asImageBitmap()
        }
    }

    BackHandler {
        if (subMode != AvatarEditorSubMode.CROP) {
            subMode = AvatarEditorSubMode.CROP
        } else if (!isSaving) {
            onBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Полотно предпросмотра с жестами zoom/pan и круглым видоискателем
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clipToBounds()
                .pointerInput(subMode) {
                    if (subMode != AvatarEditorSubMode.CROP) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        do {
                            val event = awaitPointerEvent()
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (zoomChange != 1f || panChange != Offset.Zero) {
                                zoom = (zoom * zoomChange).coerceIn(1f, 4f)
                                val maxPan = 0.45f * zoom
                                panX = (panX + panChange.x / size.width.coerceAtLeast(1)).coerceIn(-maxPan, maxPan)
                                panY = (panY + panChange.y / size.height.coerceAtLeast(1)).coerceIn(-maxPan, maxPan)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (!isVideo) {
                val bmp = filteredPreview
                if (bmp != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = zoom * if (mirrorH) -1f else 1f
                                scaleY = zoom
                                translationX = panX * size.width
                                translationY = panY * size.height
                                rotationZ = rotationDeg.toFloat()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (editState.strokes.isNotEmpty()) {
                            StrokesPreviewCanvas(
                                strokes = editState.strokes,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            } else {
                var player by remember(uri) { mutableStateOf<MediaPlayer?>(null) }
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    st: android.graphics.SurfaceTexture,
                                    w: Int,
                                    h: Int
                                ) {
                                    val mp = MediaPlayer().apply {
                                        runCatching {
                                            setDataSource(ctx, uri)
                                            setSurface(android.view.Surface(st))
                                            setVolume(0f, 0f)
                                            isLooping = true
                                            setOnPreparedListener { p ->
                                                p.seekTo(trimStartMs.toInt())
                                                if (isPlaying) p.start()
                                            }
                                            prepareAsync()
                                        }
                                    }
                                    player = mp
                                }

                                override fun onSurfaceTextureSizeChanged(
                                    st: android.graphics.SurfaceTexture,
                                    w: Int,
                                    h: Int
                                ) = Unit

                                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                                    runCatching { player?.release() }
                                    player = null
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) = Unit
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = zoom * if (mirrorH) -1f else 1f
                            scaleY = zoom
                            translationX = panX * size.width
                            translationY = panY * size.height
                            rotationZ = rotationDeg.toFloat()
                        }
                )

                LaunchedEffect(player, trimStartMs, trimEndMs, isPlaying) {
                    val mp = player ?: return@LaunchedEffect
                    runCatching {
                        if (isPlaying && !mp.isPlaying) mp.start()
                        else if (!isPlaying && mp.isPlaying) mp.pause()
                    }
                    while (true) {
                        delay(120)
                        runCatching {
                            val pos = mp.currentPosition.toLong()
                            currentPosMs = pos
                            if (pos >= trimEndMs || pos < trimStartMs) {
                                mp.seekTo(trimStartMs.toInt())
                            }
                        }
                    }
                }

                DisposableEffect(uri) {
                    onDispose {
                        runCatching { player?.release() }
                        player = null
                    }
                }
            }

            // Круглый вырез-маска с затемнением вокруг (как на скриншотах Telegram)
            if (subMode == AvatarEditorSubMode.CROP) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                ) {
                    val radius = (minOf(size.width, size.height) * 0.44f)
                    drawRect(color = Color.Black.copy(alpha = 0.62f))
                    drawCircle(
                        color = Color.Transparent,
                        radius = radius,
                        center = center,
                        blendMode = BlendMode.Clear
                    )
                }
            }
        }

        // 2. Верхняя кнопка Назад
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .safeStatusBarPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.48f))
                    .clickable { if (!isSaving) onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = strings.back,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (isVideo && detectedDurationMs > MAX_PROFILE_VIDEO_MS) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black.copy(alpha = 0.56f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = strings.maxVideoLimitHint,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // 3. Нижняя панель (дорожка видео до 20с + панель инструментов + кнопка ✓)
        if (subMode == AvatarEditorSubMode.CROP) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .safeNavigationBarPadding()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isVideo) {
                    val clippedSec = ((trimEndMs - trimStartMs).coerceIn(1_000L, MAX_PROFILE_VIDEO_MS) / 1000L)
                    VideoTimelineBar(
                        videoUri = uri,
                        durationMs = detectedDurationMs,
                        trimStartMs = trimStartMs,
                        trimEndMs = trimEndMs,
                        currentPositionMs = currentPosMs,
                        isPlaying = isPlaying,
                        isMuted = true,
                        onTogglePlay = { isPlaying = !isPlaying },
                        onToggleMute = {},
                        onTrimChange = { newStart, newEnd ->
                            // Жёсткое авто-ограничение окна до 20 секунд
                            var s = newStart.coerceIn(0L, detectedDurationMs)
                            var e = newEnd.coerceIn(s + 1_000L, detectedDurationMs)
                            if (e - s > MAX_PROFILE_VIDEO_MS) {
                                if (s != trimStartMs) {
                                    e = (s + MAX_PROFILE_VIDEO_MS).coerceAtMost(detectedDurationMs)
                                } else {
                                    s = (e - MAX_PROFILE_VIDEO_MS).coerceAtLeast(0L)
                                }
                            }
                            trimStartMs = s
                            trimEndMs = e
                            thumbnailFrameMs = currentPosMs.coerceIn(s, e)
                        },
                        onSeekPosition = { pos ->
                            thumbnailFrameMs = pos.coerceIn(trimStartMs, trimEndMs)
                            currentPosMs = thumbnailFrameMs
                        },
                        onOpenCoverPicker = {}
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "${strings.selectThumbnailFrame} · 0:${clippedSec.toString().padStart(2, '0')}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Капсула инструментов: Поворот | Зеркалить | Рисование | (HD) | Улучшение
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(26.dp))
                            .background(Color(0xFF1C1E26).copy(alpha = 0.92f))
                            .border(0.8.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(26.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ToolbarIconBtn(
                            icon = Icons.Default.Rotate90DegreesCcw,
                            label = strings.rotate,
                            active = rotationDeg % 360 != 0,
                            accent = scheme.accent,
                            onClick = { rotationDeg = (rotationDeg + 90) % 360 }
                        )
                        ToolbarIconBtn(
                            icon = Icons.Default.Flip,
                            label = strings.mirror,
                            active = mirrorH,
                            accent = scheme.accent,
                            onClick = { mirrorH = !mirrorH }
                        )
                        if (!isVideo) {
                            ToolbarIconBtn(
                                icon = Icons.Rounded.Brush,
                                label = strings.draw,
                                active = editState.strokes.isNotEmpty(),
                                accent = scheme.accent,
                                onClick = { subMode = AvatarEditorSubMode.PAINT }
                            )
                            ToolbarIconBtn(
                                icon = Icons.Default.HighQuality,
                                label = "HD",
                                active = hdEnabled,
                                accent = scheme.accent,
                                onClick = { hdEnabled = !hdEnabled }
                            )
                            ToolbarIconBtn(
                                icon = Icons.Default.Tune,
                                label = strings.adjust,
                                active = false,
                                accent = scheme.accent,
                                onClick = { subMode = AvatarEditorSubMode.ADJUST }
                            )
                        }
                    }

                    // Круглая акцентная кнопка «Готово» (✓)
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(scheme.accent)
                            .clickable(enabled = !isSaving) {
                                isSaving = true
                                scope.launch {
                                    if (!isVideo) {
                                        val finalEditState = editState.copy(
                                            crop = CropTransform(
                                                baseRotation90 = rotationDeg,
                                                mirrorHorizontal = mirrorH
                                            )
                                        )
                                        val editable = EditableMediaItem(
                                            uri = uri,
                                            isVideo = false,
                                            hd = hdEnabled,
                                            rotationDegrees = rotationDeg,
                                            editState = finalEditState
                                        )
                                        val exported = withContext(Dispatchers.IO) {
                                            runCatching {
                                                val out = BitmapExportRenderer.exportAll(context, listOf(editable)).firstOrNull()
                                                val target = File(context.filesDir, "profile_avatar_${System.currentTimeMillis()}.jpg")
                                                if (out != null) {
                                                    context.contentResolver.openInputStream(out.uri)?.use { inp ->
                                                        target.outputStream().use { outStr -> inp.copyTo(outStr) }
                                                    }
                                                    Uri.fromFile(target)
                                                } else uri
                                            }.getOrDefault(uri)
                                        }
                                        ProfileAvatarStore.addAvatar(
                                            context,
                                            ProfileAvatar.Photo(
                                                uriString = exported.toString(),
                                                zoom = zoom,
                                                panX = panX,
                                                panY = panY,
                                                rotationDegrees = 0,
                                                mirrorHorizontal = false
                                            )
                                        )
                                    } else {
                                        ProfileAvatarStore.addAvatar(
                                            context,
                                            ProfileAvatar.Video(
                                                uriString = uri.toString(),
                                                trimStartMs = trimStartMs,
                                                trimEndMs = trimEndMs.coerceAtMost(trimStartMs + MAX_PROFILE_VIDEO_MS),
                                                thumbnailFrameMs = thumbnailFrameMs,
                                                zoom = zoom,
                                                panX = panX,
                                                panY = panY,
                                                rotationDegrees = rotationDeg,
                                                mirrorHorizontal = mirrorH
                                            )
                                        )
                                    }
                                    isSaving = false
                                    onSaved()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                color = scheme.onAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = strings.done,
                                tint = scheme.onAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }

        // 4. Вложенный режим рисования из MediaEditor
        if (subMode == AvatarEditorSubMode.PAINT) {
            PaintOverlayPanel(
                previewBitmap = filteredPreview,
                initialStrokes = editState.strokes,
                initialTextLayers = editState.textLayers,
                initialTab = PaintModeTab.DRAWING,
                isVideo = isVideo,
                onApply = { strokes, textLayers ->
                    editState = editState.copy(strokes = strokes, textLayers = textLayers)
                    subMode = AvatarEditorSubMode.CROP
                },
                onCancel = { subMode = AvatarEditorSubMode.CROP },
                onOpenDownloadFonts = {}
            )
        }

        // 5. Вложенный режим улучшения фото из MediaEditor
        if (subMode == AvatarEditorSubMode.ADJUST) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                AdjustmentsPanel(
                    activeTab = adjustTab,
                    onTabChange = { adjustTab = it },
                    adjustments = editState.adjustments,
                    onAdjustmentsChange = { editState = editState.copy(adjustments = it) },
                    blur = editState.blur,
                    onBlurChange = { editState = editState.copy(blur = it) },
                    curves = editState.curves,
                    onCurvesChange = { editState = editState.copy(curves = it) },
                    onCancel = { subMode = AvatarEditorSubMode.CROP },
                    onDone = { subMode = AvatarEditorSubMode.CROP }
                )
            }
        }
    }
}

@Composable
private fun ToolbarIconBtn(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (active) accent else Color.White,
            modifier = Modifier.size(21.dp)
        )
    }
}
