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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
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
 * Видео-кружок в пузыре с воспроизведением и увеличением прямо в чате (Фото 2 и Фото 4):
 * - Обычный размер 220.dp с беззвучным автовоспроизведением
 * - По тапу: плавное увеличение прямо в чате до ширины экрана (in-place) со звуком
 * - Круговой SeekBar вокруг круга с перетаскиваемой белой точкой-ручкой (●)
 * - Кнопки полной навигации: Play/Pause, быстрый скип 10 сек, кнопка сворачивания
 * - Внизу внутри кружочка: слева длительность/время записи [ 00:16 • ], справа время отправки и статус [ 19:29 ✓✓ ]
 */
@Composable
internal fun ChatVideoCircleSection(
    message: ChatMessage,
    attachment: MessageAttachment.VideoCircle,
    isSelectionMode: Boolean = false,
    onSelect: () -> Unit = {}
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidth = configuration.screenWidthDp.dp

    var isExpanded by remember { mutableStateOf(false) }

    val normalSize = 220.dp
    val expandedSize = (screenWidth - 20.dp).coerceIn(280.dp, 360.dp)
    val currentSize by animateDpAsState(
        targetValue = if (isExpanded) expandedSize else normalSize,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "circleSize"
    )

    val currentSizePx = with(density) { currentSize.toPx() }
    val radiusPx = (currentSizePx / 2f) - with(density) { 5.dp.toPx() }

    val videoFile = remember(attachment.uri) {
        attachment.uri?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
    }

    val durSec = remember(attachment.durationLabel) {
        val parts = attachment.durationLabel.split(":")
        if (parts.size == 2) {
            (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 14)
        } else 14
    }
    val totalMs = (durSec * 1000L).coerceAtLeast(1000L)

    var currentPosMs by remember { mutableLongStateOf(0L) }
    var isPlaying by remember { mutableStateOf(true) }
    var isDragging by remember { mutableStateOf(false) }
    var mediaPlayerRef by remember { mutableStateOf<android.media.MediaPlayer?>(null) }

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

    // Синхронизация прогресса воспроизведения
    LaunchedEffect(isExpanded, isPlaying, isDragging) {
        while (isActive && isPlaying && !isDragging) {
            delay(50)
            val mp = mediaPlayerRef
            if (mp != null && mp.isPlaying) {
                currentPosMs = mp.currentPosition.toLong()
            }
        }
    }

    val progress = (currentPosMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "circleProgress")

    val elapsedSec = (currentPosMs / 1000).toInt()
    val elapsedLabel = "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)

    val horizPadding = if (isExpanded) 22.dp else 16.dp
    val vertPadding = if (isExpanded) 16.dp else 12.dp

    Box(
        modifier = Modifier
            .size(currentSize)
            .clip(CircleShape)
            .background(Color(0xFF1B1B22))
            .border(1.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (isSelectionMode) {
                    onSelect()
                } else if (!isExpanded) {
                    // Раскрытие прямо в чате со звуком
                    isExpanded = true
                    mediaPlayerRef?.setVolume(1f, 1f)
                    if (isPlaying) {
                        mediaPlayerRef?.start()
                    }
                } else {
                    // В раскрытом состоянии тап по видео переключает паузу / воспроизведение
                    if (!isDragging) {
                        isPlaying = !isPlaying
                        val mp = mediaPlayerRef
                        if (mp != null) {
                            if (isPlaying) mp.start() else mp.pause()
                        }
                    }
                }
            }
            .then(
                if (isExpanded) {
                    Modifier.pointerInput(totalMs, currentSizePx) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isDragging = true
                                val dx = offset.x - (currentSizePx / 2f)
                                val dy = offset.y - (currentSizePx / 2f)
                                val angle = atan2(dy, dx)
                                var deg = (angle * 180f / PI.toFloat()) + 90f
                                if (deg < 0f) deg += 360f
                                val fraction = (deg / 360f).coerceIn(0f, 1f)
                                currentPosMs = (fraction * totalMs).toLong()
                                mediaPlayerRef?.seekTo(currentPosMs.toInt())
                            },
                            onDragEnd = {
                                isDragging = false
                                mediaPlayerRef?.seekTo(currentPosMs.toInt())
                            },
                            onDragCancel = {
                                isDragging = false
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                val touchPos = change.position
                                val dx = touchPos.x - (currentSizePx / 2f)
                                val dy = touchPos.y - (currentSizePx / 2f)
                                val angle = atan2(dy, dx)
                                var deg = (angle * 180f / PI.toFloat()) + 90f
                                if (deg < 0f) deg += 360f
                                val fraction = (deg / 360f).coerceIn(0f, 1f)
                                currentPosMs = (fraction * totalMs).toLong()
                                mediaPlayerRef?.seekTo(currentPosMs.toInt())
                            }
                        )
                    }
                } else Modifier
            ),
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
            DisposableEffect(videoFile.absolutePath) {
                onDispose {
                    mediaPlayerRef?.run {
                        runCatching { stop() }
                        runCatching { release() }
                    }
                    mediaPlayerRef = null
                }
            }
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.view.TextureView(ctx).apply {
                        isOpaque = false
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
                                        setVolume(if (isExpanded) 1f else 0f, if (isExpanded) 1f else 0f)
                                        isLooping = true
                                        setOnVideoSizeChangedListener { _, vw, vh ->
                                            if (vw > 0 && vh > 0 && width > 0 && height > 0) {
                                                val viewW = width.toFloat()
                                                val viewH = height.toFloat()
                                                val scaleX: Float
                                                val scaleY: Float
                                                if (vw.toFloat() / vh.toFloat() > viewW / viewH) {
                                                    scaleX = (vw.toFloat() / vh.toFloat()) / (viewW / viewH)
                                                    scaleY = 1f
                                                } else {
                                                    scaleX = 1f
                                                    scaleY = (viewW / viewH) / (vw.toFloat() / vh.toFloat())
                                                }
                                                val matrix = android.graphics.Matrix()
                                                matrix.setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
                                                setTransform(matrix)
                                            }
                                        }
                                        setOnPreparedListener { player ->
                                            runCatching {
                                                if (isPlaying) player.start()
                                            }
                                        }
                                        prepareAsync()
                                    }
                                    mediaPlayerRef = mp
                                } catch (_: Exception) {}
                            }

                            override fun onSurfaceTextureSizeChanged(
                                st: android.graphics.SurfaceTexture,
                                w: Int,
                                h: Int
                            ) {}

                            override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                                mediaPlayerRef?.run {
                                    runCatching { stop() }
                                    runCatching { release() }
                                }
                                mediaPlayerRef = null
                                return true
                            }

                            override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Круговой SeekBar вокруг круга (тонкое кольцо и белая дуга прогресса)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = radiusPx
            val strokeWidth = 3.5.dp.toPx()
            val centerOffset = Offset(size.width / 2f, size.height / 2f)

            // Фоновое кольцо
            drawCircle(
                color = Color.White.copy(alpha = if (isExpanded) 0.30f else 0.18f),
                radius = radius,
                center = centerOffset,
                style = Stroke(width = strokeWidth)
            )

            // Белая дуга прогресса воспроизведения
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

        // Перетаскиваемая белая точка-ручка (●) на ободе при увеличении (как на Фото пользователя)
        if (isExpanded) {
            val angleDeg = -90f + animatedProgress * 360f
            val angleRad = angleDeg * (PI.toFloat() / 180f)
            val handleX = (radiusPx * cos(angleRad)).roundToInt()
            val handleY = (radiusPx * sin(angleRad)).roundToInt()

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset(handleX, handleY) }
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, Color(0xFF6B4EEA), CircleShape)
            )
        }

        // Кнопка Play по центру при паузе
        if (isExpanded && !isPlaying) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable {
                        isPlaying = true
                        mediaPlayerRef?.start()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PlayArrow,
                    contentDescription = "Воспроизвести",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        // Кнопка сворачивания в правом верхнем углу при увеличении
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.58f))
                    .clickable {
                        isExpanded = false
                        mediaPlayerRef?.setVolume(0f, 0f)
                        if (!isPlaying) {
                            isPlaying = true
                            mediaPlayerRef?.start()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Свернуть",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Быстрые кнопки перемотки на 10 секунд назад и вперед
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // -10 сек
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable {
                            val newPos = (currentPosMs - 10000L).coerceAtLeast(0L)
                            currentPosMs = newPos
                            mediaPlayerRef?.seekTo(newPos.toInt())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastRewind,
                        contentDescription = "-10с",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // +10 сек
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable {
                            val newPos = (currentPosMs + 10000L).coerceAtMost(totalMs)
                            currentPosMs = newPos
                            mediaPlayerRef?.seekTo(newPos.toInt())
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FastForward,
                        contentDescription = "+10с",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Внизу внутри кружочка (Фото 2 и Фото 4 в стиле Telegram):
        // Слева: Длительность со значком кружочка [ 00:16 • ]
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = horizPadding, bottom = vertPadding)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isExpanded && (isPlaying || currentPosMs > 0)) elapsedLabel else attachment.durationLabel,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.9f))
                )
            }
        }

        // Справа: Время отправки и статус доставки/прочтения [ 19:29 ✓✓ ]
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = horizPadding, bottom = vertPadding)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
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

