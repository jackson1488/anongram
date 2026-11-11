package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.anongram.app.features.chats.media.player.ChatAudioController
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.timeLabel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor

/**
 * Карточка контакта внутри пузыря сообщения (каждый контакт отдельно, внутри вертикальная полоска слева,
 * аватар, имя, номер и кнопка «НАПИСАТЬ» с мгновенным переходом в чат).
 */
@Composable
internal fun ChatContactCardSection(
    attachment: MessageAttachment.Contact,
    isIncoming: Boolean,
    accent: Color,
    searchQuery: String,
    isSelectionMode: Boolean,
    onSelect: () -> Unit,
    onOpenContactChat: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable {
                if (isSelectionMode) {
                    onSelect()
                } else {
                    onOpenContactChat(attachment.name, attachment.phone)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.5.dp)
                    .fillMaxHeight()
                    .background(if (isIncoming) accent else Color.White)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp, end = 10.dp, top = 8.dp, bottom = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.30f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = attachment.name.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                buildHighlightedText(attachment.name, searchQuery, accent)
                            } else {
                                AnnotatedString(attachment.name)
                            },
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) {
                                buildHighlightedText(attachment.phone, searchQuery, accent)
                            } else {
                                AnnotatedString(attachment.phone)
                            },
                            color = TextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.14f), thickness = 0.7.dp)
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "НАПИСАТЬ",
                    color = if (isIncoming) accent else Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelectionMode) {
                                onSelect()
                            } else {
                                onOpenContactChat(attachment.name, attachment.phone)
                            }
                        }
                        .padding(vertical = 2.dp)
                )
            }
        }
    }
}

/**
 * Карточка геопозиции в пузыре сообщения.
 */
@Composable
internal fun ChatGeoSection(
    attachment: MessageAttachment.Geo,
    accent: Color,
    searchQuery: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceDark)
            .padding(8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null,
                tint = FlameOrange,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Геопозиция",
                color = FlameOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (searchQuery.isNotBlank()) {
                buildHighlightedText(attachment.address, searchQuery, accent)
            } else {
                AnnotatedString(attachment.address)
            },
            color = TextPrimary,
            fontSize = 13.sp
        )
    }
}

/**
 * Голосовое сообщение в пузыре (поддержка реальной подсветки волны и цепочки воспроизведения).
 */
@Composable
internal fun ChatVoiceSection(
    message: ChatMessage,
    attachment: MessageAttachment.Voice,
    accent: Color,
    onPlay: () -> Unit = {}
) {
    val context = LocalContext.current
    val audioController = remember { ChatAudioController.getInstance(context) }
    val playerState = audioController.state.collectAsState().value
    val isCurrentVoice = playerState.currentTrack?.id == "voice_${message.id}"
    val isPlaying = isCurrentVoice && playerState.isPlaying

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accent)
                .clickable {
                    if (isCurrentVoice) {
                        audioController.togglePlayPause()
                    } else {
                        onPlay()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) "Пауза" else "Слушать",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val totalBars = attachment.waveform.size.coerceAtLeast(1)
            val currentProgress = if (isCurrentVoice) playerState.progress else 0f
            val activeBars = (currentProgress * totalBars).toInt()

            attachment.waveform.forEachIndexed { index, height ->
                val isActive = isCurrentVoice && index <= activeBars
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height((height * 1.5f).dp.coerceIn(4.dp, 24.dp))
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isActive) Color.White else accent.copy(alpha = 0.55f))
                )
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = if (isCurrentVoice && (isPlaying || playerState.progressMs > 0)) playerState.positionLabel else attachment.durationLabel,
            color = TextMuted,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}

/**
 * Видео-кружок в пузыре чата (Telegram-style, Фото 2 и 4):
 * - По умолчанию 256dp с беззвучным автовоспроизведением
 * - При тапе увеличивается ПРЯМО В ЧАТЕ почти на всю ширину экрана, включает звук и круговой ползунок навигации (SeekBar)
 * - Слева внизу (снаружи дуги круга): время записи / воспроизведения со значком кружочка [ 00:16 • ]
 * - Справа внизу (снаружи дуги круга): время отправки и статус доставки [ 19:29 ✓✓ ]
 */
@Composable
internal fun ChatVideoCircleSection(
    message: ChatMessage,
    attachment: MessageAttachment.VideoCircle,
    isSelectionMode: Boolean = false,
    onSelect: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp

    var isExpanded by remember { androidx.compose.runtime.mutableStateOf(false) }
    var isPaused by remember { androidx.compose.runtime.mutableStateOf(false) }
    var isDragging by remember { androidx.compose.runtime.mutableStateOf(false) }

    val parsedSec = remember(attachment.durationLabel) {
        val parts = attachment.durationLabel.split(":")
        if (parts.size == 2) {
            ((parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 5)).coerceAtLeast(1)
        } else 5
    }
    var totalDurationMs by remember(parsedSec) { androidx.compose.runtime.mutableLongStateOf(parsedSec * 1000L) }
    var currentPosMs by remember { androidx.compose.runtime.mutableLongStateOf(0L) }

    val targetSize = if (isExpanded) {
        (screenWidth - 16.dp).coerceIn(310.dp, 390.dp)
    } else {
        256.dp
    }
    val circleBoxSize by androidx.compose.animation.core.animateDpAsState(
        targetValue = targetSize,
        animationSpec = androidx.compose.animation.core.tween(240),
        label = "inChatVideoCircleSize"
    )
    val innerVideoSize = if (isExpanded) circleBoxSize - 10.dp else circleBoxSize

    val videoFile = remember(attachment.uri) {
        attachment.uri?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
    }

    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = attachment.uri) {
        val file = videoFile
        if (file != null) {
            withContext(Dispatchers.IO) {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(file.absolutePath)
                    value = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                } catch (_: Exception) {
                } finally {
                    runCatching { mmr.release() }
                }
            }
        }
    }

    var loopPlayer by remember(videoFile?.absolutePath) {
        androidx.compose.runtime.mutableStateOf<android.media.MediaPlayer?>(null)
    }

    androidx.compose.runtime.LaunchedEffect(isExpanded, isPaused, isDragging, loopPlayer) {
        while (kotlinx.coroutines.isActive) {
            kotlinx.coroutines.delay(50)
            val mp = loopPlayer
            if (mp != null) {
                runCatching {
                    val dur = mp.duration.toLong()
                    if (dur > 0L) totalDurationMs = dur
                    if (mp.isPlaying && !isDragging) {
                        currentPosMs = mp.currentPosition.toLong()
                    }
                }
            } else if (isExpanded && !isPaused && !isDragging) {
                currentPosMs = (currentPosMs + 50L) % totalDurationMs.coerceAtLeast(1000L)
            }
        }
    }

    val progress = (currentPosMs.toFloat() / totalDurationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    val boxSizePx = with(density) { circleBoxSize.toPx() }
    val radiusPx = (boxSizePx / 2f) - with(density) { 4.dp.toPx() }

    // Внешний квадратный контейнер БЕЗ clip(CircleShape), чтобы плашки времени в углах (слева и справа внизу) никогда не обрезались!
    Box(
        modifier = Modifier
            .size(circleBoxSize)
            .pointerInput(isExpanded, isSelectionMode, boxSizePx, totalDurationMs) {
                if (isExpanded && !isSelectionMode) {
                    androidx.compose.foundation.gestures.detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val dx = offset.x - (boxSizePx / 2f)
                            val dy = offset.y - (boxSizePx / 2f)
                            val angle = kotlin.math.atan2(dy, dx)
                            var deg = (angle * 180f / kotlin.math.PI.toFloat()) + 90f
                            if (deg < 0f) deg += 360f
                            val fraction = (deg / 360f).coerceIn(0f, 1f)
                            currentPosMs = (fraction * totalDurationMs).toLong()
                            runCatching { loopPlayer?.seekTo(currentPosMs.toInt()) }
                        },
                        onDragEnd = {
                            isDragging = false
                            runCatching { loopPlayer?.seekTo(currentPosMs.toInt()) }
                        },
                        onDragCancel = {
                            isDragging = false
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val touchPos = change.position
                            val dx = touchPos.x - (boxSizePx / 2f)
                            val dy = touchPos.y - (boxSizePx / 2f)
                            val angle = kotlin.math.atan2(dy, dx)
                            var deg = (angle * 180f / kotlin.math.PI.toFloat()) + 90f
                            if (deg < 0f) deg += 360f
                            val fraction = (deg / 360f).coerceIn(0f, 1f)
                            currentPosMs = (fraction * totalDurationMs).toLong()
                            runCatching { loopPlayer?.seekTo(currentPosMs.toInt()) }
                        }
                    )
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Внутренний круг с видео
        Box(
            modifier = Modifier
                .size(innerVideoSize)
                .clip(CircleShape)
                .background(Color(0xFF1B1B22))
                .border(1.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
                .clickable {
                    if (isSelectionMode) {
                        onSelect()
                    } else if (!isExpanded) {
                        isExpanded = true
                        isPaused = false
                        runCatching {
                            loopPlayer?.setVolume(1f, 1f)
                            loopPlayer?.seekTo(0)
                            if (loopPlayer?.isPlaying == false) loopPlayer?.start()
                        }
                    } else {
                        isPaused = !isPaused
                        runCatching {
                            if (isPaused) loopPlayer?.pause() else loopPlayer?.start()
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val bmp = thumbnailBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Видео-сообщение",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (videoFile != null) {
                androidx.compose.runtime.DisposableEffect(videoFile.absolutePath) {
                    onDispose {
                        loopPlayer?.run {
                            runCatching { stop() }
                            runCatching { release() }
                        }
                        loopPlayer = null
                    }
                }
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.view.TextureView(ctx).apply {
                            isOpaque = false
                            fun applyCenterCrop(vw: Int, vh: Int, viewW: Int, viewH: Int) {
                                if (vw <= 0 || vh <= 0 || viewW <= 0 || viewH <= 0) return
                                val scaleX: Float
                                val scaleY: Float
                                if (vw.toFloat() / vh.toFloat() > viewW.toFloat() / viewH.toFloat()) {
                                    scaleX = (vw.toFloat() / vh.toFloat()) / (viewW.toFloat() / viewH.toFloat())
                                    scaleY = 1f
                                } else {
                                    scaleX = 1f
                                    scaleY = (viewW.toFloat() / viewH.toFloat()) / (vw.toFloat() / vh.toFloat())
                                }
                                val matrix = android.graphics.Matrix()
                                matrix.setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
                                setTransform(matrix)
                            }
                            surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    st: android.graphics.SurfaceTexture,
                                    w: Int,
                                    h: Int
                                ) {
                                    try {
                                        val mp = android.media.MediaPlayer().apply {
                                            setSurface(android.view.Surface(st))
                                            setDataSource(videoFile.absolutePath)
                                            val vol = if (isExpanded) 1f else 0f
                                            setVolume(vol, vol)
                                            isLooping = true
                                            setOnVideoSizeChangedListener { _, vw, vh ->
                                                applyCenterCrop(vw, vh, width, height)
                                            }
                                            setOnPreparedListener { player ->
                                                val dur = player.duration.toLong()
                                                if (dur > 0L) totalDurationMs = dur
                                                applyCenterCrop(player.videoWidth, player.videoHeight, width, height)
                                                runCatching { player.start() }
                                            }
                                            prepareAsync()
                                        }
                                        loopPlayer = mp
                                    } catch (_: Exception) {}
                                }

                                override fun onSurfaceTextureSizeChanged(
                                    st: android.graphics.SurfaceTexture,
                                    w: Int,
                                    h: Int
                                ) {
                                    val mp = loopPlayer
                                    if (mp != null) {
                                        runCatching { applyCenterCrop(mp.videoWidth, mp.videoHeight, w, h) }
                                    }
                                }

                                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                                    loopPlayer?.run {
                                        runCatching { stop() }
                                        runCatching { release() }
                                    }
                                    loopPlayer = null
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Иконка Play по центру при паузе в увеличенном режиме
            if (isExpanded && isPaused) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Воспроизвести",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
        }

        // Круговой ползунок навигации (SeekBar) прямо вокруг кружочка в чате при увеличении
        if (isExpanded) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 3.5.dp.toPx()
                val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)

                drawCircle(
                    color = Color.White.copy(alpha = 0.25f),
                    radius = radiusPx,
                    center = centerOffset,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )

                drawArc(
                    color = Color.White,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    topLeft = androidx.compose.ui.geometry.Offset(centerOffset.x - radiusPx, centerOffset.y - radiusPx),
                    size = androidx.compose.ui.geometry.Size(radiusPx * 2, radiusPx * 2),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = strokeWidth,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }

            // Белая точка-ручка ползунка навигации (●) на дуге круга
            val angleDeg = -90f + progress * 360f
            val angleRad = angleDeg * (kotlin.math.PI.toFloat() / 180f)
            val handleX = (radiusPx * kotlin.math.cos(angleRad)).roundToInt()
            val handleY = (radiusPx * kotlin.math.sin(angleRad)).roundToInt()

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { androidx.compose.ui.unit.IntOffset(handleX, handleY) }
                    .size(if (isPaused || isDragging) 18.dp else 12.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color.Black.copy(alpha = 0.35f), CircleShape)
            )

            // Кнопка сворачивания кружочка обратно в обычный размер чата (в правом верхнем углу)
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        isExpanded = false
                        isPaused = false
                        runCatching {
                            loopPlayer?.setVolume(0f, 0f)
                            if (loopPlayer?.isPlaying == false) loopPlayer?.start()
                        }
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "Свернуть",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Слева внизу (снаружи дуги круга, как на Фото 2 и 4): время записи / воспроизведения + белая точка [ 00:16 • ]
        val elapsedSec = (currentPosMs / 1000L).toInt()
        val leftDurationText = if (isExpanded) {
            "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)
        } else {
            attachment.durationLabel
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = leftDurationText,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.95f))
                )
            }
        }

        // Справа внизу (снаружи дуги круга, как на Фото 2 и 4): время сообщения и галочки [ 19:29 ✓✓ ]
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 4.dp, bottom = 4.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = message.timeLabel(),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium
                )
                if (message.direction == MessageDirection.OUTGOING) {
                    Spacer(modifier = Modifier.width(4.dp))
                    DeliveryStatus(
                        status = message.status,
                        onRetry = {},
                        iconSize = 12.dp
                    )
                }
            }
        }
    }
}

