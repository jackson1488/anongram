package com.anongram.app.features.chats.media.circle

import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.util.LruCache
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewOutlineProvider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.anongram.app.core.theme.FlameOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val videoThumbnailCache = LruCache<String, Bitmap>(32)

/**
 * Развёрнутый плеер видео-кружочка с круговым SeekBar-ом:
 * - Крупный круг по центру экрана (диаметр ~300.dp)
 * - Воспроизведение реального видео через MediaPlayer в TextureView
 * - По внешнему ободу белая дуга прогресса воспроизведения
 * - По тапу во время воспроизведения: ставится на паузу и появляется белая точка-ручка (●)
 * - Белую точку можно пальцем водить по кругу для перемотки видеосообщения в любое место
 */
@Composable
fun VideoCircleExpandedPlayer(
    attachment: com.anongram.app.features.chats.messages.domain.MessageAttachment.VideoCircle? = null,
    durationSeconds: Int = 14,
    timeLabel: String = "19:07",
    onDismiss: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val closeHandler = onClose ?: onDismiss
    val durSec = remember(attachment, durationSeconds) {
        if (attachment != null) {
            val parts = attachment.durationLabel.split(":")
            if (parts.size == 2) {
                (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 14)
            } else 14
        } else {
            durationSeconds
        }
    }
    val totalMs = (durSec * 1000L).coerceAtLeast(1000L)
    var currentPosMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var isDragging by remember { mutableStateOf(false) }

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val videoFile = remember(attachment?.uri) {
        attachment?.uri?.let { File(it) }?.takeIf { it.exists() }
    }

    val cachedThumb = remember(videoFile?.absolutePath) {
        videoFile?.absolutePath?.let { videoThumbnailCache.get(it) }
    }
    val thumbnailBitmap by produceState(initialValue = cachedThumb, key1 = videoFile?.absolutePath) {
        if (cachedThumb != null) {
            value = cachedThumb
            return@produceState
        }
        val file = videoFile ?: return@produceState
        withContext(Dispatchers.IO) {
            val mmr = MediaMetadataRetriever()
            try {
                mmr.setDataSource(file.absolutePath)
                val bmp = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                if (bmp != null) {
                    videoThumbnailCache.put(file.absolutePath, bmp)
                    value = bmp
                }
            } catch (_: Exception) {
            } finally {
                runCatching { mmr.release() }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.run {
                runCatching { stop() }
                release()
            }
            mediaPlayer = null
        }
    }

    // Синхронизация прогресса с реальным MediaPlayer или таймером
    LaunchedEffect(isPlaying, isDragging) {
        while (isActive && isPlaying && !isDragging) {
            delay(50)
            val mp = mediaPlayer
            if (mp != null && mp.isPlaying) {
                currentPosMs = mp.currentPosition.toLong()
            } else if (mp == null) {
                currentPosMs += 50
                if (currentPosMs >= totalMs) {
                    currentPosMs = totalMs
                    isPlaying = false
                }
            }
        }
    }

    val progress = (currentPosMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "circleProgress")

    val elapsedSec = (currentPosMs / 1000).toInt()
    val elapsedLabel = "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp
    val circleSize = (screenWidth * 0.94f).coerceAtMost(screenHeight * 0.72f)
    val innerVideoSize = circleSize - 12.dp

    val density = LocalDensity.current
    val circleSizePx = with(density) { circleSize.toPx() }
    val radiusPx = (circleSizePx / 2f) - with(density) { 6.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = closeHandler
            ),
        contentAlignment = Alignment.Center
    ) {
        // Контейнер кругового видео и кольца SeekBar
        Box(
            modifier = Modifier
                .size(circleSize)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        if (!isDragging) {
                            isPlaying = !isPlaying
                            val mp = mediaPlayer
                            if (mp != null) {
                                if (isPlaying) mp.start() else mp.pause()
                            }
                        }
                    }
                )
                .pointerInput(totalMs, circleSizePx) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val dx = offset.x - (circleSizePx / 2f)
                            val dy = offset.y - (circleSizePx / 2f)
                            val angle = atan2(dy, dx)
                            var deg = (angle * 180f / PI.toFloat()) + 90f
                            if (deg < 0f) deg += 360f
                            val fraction = (deg / 360f).coerceIn(0f, 1f)
                            currentPosMs = (fraction * totalMs).toLong()
                            mediaPlayer?.seekTo(currentPosMs.toInt())
                        },
                        onDragEnd = {
                            isDragging = false
                            mediaPlayer?.seekTo(currentPosMs.toInt())
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val touchPos = change.position
                            val dx = touchPos.x - (circleSizePx / 2f)
                            val dy = touchPos.y - (circleSizePx / 2f)
                            val angle = atan2(dy, dx)
                            var deg = (angle * 180f / PI.toFloat()) + 90f
                            if (deg < 0f) deg += 360f
                            val fraction = (deg / 360f).coerceIn(0f, 1f)
                            currentPosMs = (fraction * totalMs).toLong()
                            mediaPlayer?.seekTo(currentPosMs.toInt())
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Тело кругового видео
            Box(
                modifier = Modifier
                    .size(innerVideoSize)
                    .clip(CircleShape)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (videoFile != null) {
                    // Реальное воспроизведение видеофайла через TextureView
                    AndroidView(
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                isOpaque = false
                                clipToOutline = true
                                outlineProvider = object : ViewOutlineProvider() {
                                    override fun getOutline(view: View, outline: Outline) {
                                        outline.setOval(0, 0, view.width, view.height)
                                    }
                                }
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                        invalidateOutline()
                                        try {
                                            val mp = MediaPlayer().apply {
                                                setSurface(Surface(st))
                                                setDataSource(videoFile.absolutePath)
                                                isLooping = true
                                                prepare()
                                                if (isPlaying) start()
                                            }
                                            mediaPlayer = mp
                                        } catch (_: Exception) {}
                                    }

                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                                        invalidateOutline()
                                    }
                                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                        mediaPlayer?.run {
                                            runCatching { stop() }
                                            release()
                                        }
                                        mediaPlayer = null
                                        return true
                                    }
                                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Fallback иконка если файл не задан
                    Icon(
                        imageVector = Icons.Outlined.Videocam,
                        contentDescription = null,
                        tint = FlameOrange.copy(alpha = 0.6f),
                        modifier = Modifier.size(64.dp)
                    )
                }

                // Иконка паузы / воспроизведения по центру при паузе
                if (!isPlaying) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Воспроизвести",
                            tint = Color.White,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }

                // Внизу слева счётчик времени воспроизведения
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 28.dp, bottom = 20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$elapsedLabel ${if (isPlaying) "🔊" else "⏸"}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Внизу справа время сообщения и галочки
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 28.dp, bottom = 20.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "$timeLabel ✓✓",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Круговой SeekBar (дуга прогресса + точка ручки)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = radiusPx
                val strokeWidth = 4.dp.toPx()
                val centerOffset = Offset(size.width / 2f, size.height / 2f)

                // Фоновое кольцо (полупрозрачное)
                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Белая дуга прогресса
                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = animatedProgress * 360f,
                    useCenter = false,
                    topLeft = Offset(centerOffset.x - radius, centerOffset.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }

            // Белая точка-ручка (●) на ободе при паузе или перемотке (как на фото пользователя!)
            if (!isPlaying || isDragging) {
                val angleDeg = -90f + animatedProgress * 360f
                val angleRad = angleDeg * (PI.toFloat() / 180f)
                val handleX = (radiusPx * cos(angleRad)).roundToInt()
                val handleY = (radiusPx * sin(angleRad)).roundToInt()
                val scheme = com.anongram.app.core.theme.LocalAnonGramScheme.current

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset { IntOffset(handleX, handleY) }
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, scheme.accent, CircleShape)
                )
            }
        }
    }
}
