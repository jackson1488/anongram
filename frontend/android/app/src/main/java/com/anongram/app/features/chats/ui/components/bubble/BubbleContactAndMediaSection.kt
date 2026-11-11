package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.onSizeChanged
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import android.util.LruCache
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.alpha
import com.anongram.app.features.chats.media.player.ChatPlayerState
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.attachmentColor
import com.anongram.app.features.chats.ui.components.LocalIsListScrollingState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

private fun normalizeVoiceWaveformBars(samples: List<Int>, targetBars: Int = 34): List<Int> {
    if (targetBars <= 0) return emptyList()
    if (samples.isEmpty()) {
        return List(targetBars) { idx -> 3 + ((idx * 5 + 2) % 9) }
    }
    if (samples.size == targetBars) {
        return samples.map { it.coerceIn(2, 16) }
    }
    return List(targetBars) { barIdx ->
        val start = (barIdx.toFloat() / targetBars * samples.size).toInt().coerceIn(0, samples.lastIndex)
        val end = (((barIdx + 1).toFloat() / targetBars) * samples.size).toInt().coerceIn(start + 1, samples.size)
        var peak = 2
        for (i in start until end) {
            if (samples[i] > peak) peak = samples[i]
        }
        peak.coerceIn(2, 16)
    }
}

private fun parseVoiceDurationMs(label: String): Long {
    val parts = label.trim().split(":").mapNotNull { it.toLongOrNull() }
    val seconds = when (parts.size) {
        3 -> parts[0] * 3600L + parts[1] * 60L + parts[2]
        2 -> parts[0] * 60L + parts[1]
        1 -> parts[0]
        else -> 15L
    }
    return (seconds.coerceAtLeast(1L)) * 1000L
}

private fun formatVoiceTimeMs(ms: Long): String {
    val totalSec = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSec / 3600
    val minutes = (totalSec % 3600) / 60
    val seconds = totalSec % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%d:%02d", minutes, seconds)
    }
}

/**
 * Голосовое сообщение в пузыре в стиле Telegram:
 * круглая кнопка Play/Pause, скраббинг по дорожке (тап/перетаскивание),
 * длительность без ограничений (M:SS / H:MM:SS) и кнопка →A для расшифровки.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatVoiceSection(
    message: ChatMessage,
    attachment: MessageAttachment.Voice,
    accent: Color,
    isIncoming: Boolean = false,
    onLongClick: () -> Unit = {},
    onPlay: () -> Unit = {}
) {
    val context = LocalContext.current
    val audioController = remember { ChatAudioController.getInstance(context) }
    val voiceTrackId = "voice_${message.id}"
    val isCurrentVoice by remember(voiceTrackId) {
        audioController.state
            .map { it.currentTrack?.id == voiceTrackId }
            .distinctUntilChanged()
    }.collectAsState(initial = false)
    val playerState: ChatPlayerState? = if (isCurrentVoice) {
        audioController.state.collectAsState().value
    } else null
    val isPlaying = isCurrentVoice && (playerState?.isPlaying == true)

    val bars = remember(attachment.waveform) {
        normalizeVoiceWaveformBars(attachment.waveform, targetBars = 34)
    }
    val fallbackDurationMs = remember(attachment.durationLabel) {
        parseVoiceDurationMs(attachment.durationLabel)
    }
    val durationMs = (playerState?.durationMs ?: 0L).takeIf { it > 0L } ?: fallbackDurationMs
    val activePosMs = playerState?.progressMs ?: 0L

    var waveformWidthPx by remember { mutableIntStateOf(0) }
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    var isTranscriptExpanded by remember(message.id) { mutableStateOf(false) }

    val currentIsCurrentVoice by rememberUpdatedState(isCurrentVoice)
    val currentDurationMs by rememberUpdatedState(durationMs)
    val currentOnPlay by rememberUpdatedState(onPlay)

    val seekToFraction: (Float) -> Unit = { fraction ->
        val clamped = fraction.coerceIn(0f, 1f)
        val targetMs = (clamped * currentDurationMs).toLong()
        if (currentIsCurrentVoice) {
            audioController.seek(targetMs)
        } else {
            currentOnPlay()
            audioController.seek(targetMs)
        }
    }

    val effectiveProgress = dragFraction
        ?: if (isCurrentVoice) (playerState?.progress ?: 0f).coerceIn(0f, 1f) else 0f

    val playButtonBg = if (!isIncoming) Color.White else accent
    val playIconTint = if (!isIncoming) accent else Color.White
    val activeBarColor = if (!isIncoming) Color.White else accent
    val inactiveBarColor = if (!isIncoming) Color.White.copy(alpha = 0.42f) else accent.copy(alpha = 0.34f)
    val timeColor = if (!isIncoming) Color.White.copy(alpha = 0.85f) else TextMuted
    val pillBg = if (!isIncoming) Color.White.copy(alpha = 0.20f) else accent.copy(alpha = 0.16f)
    val pillActiveBg = if (!isIncoming) Color.White.copy(alpha = 0.34f) else accent.copy(alpha = 0.28f)
    val pillText = if (!isIncoming) Color.White else accent

    Column(
        modifier = Modifier
            .widthIn(min = 232.dp, max = 278.dp)
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(playButtonBg)
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
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Пауза" else "Слушать",
                    tint = playIconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                        .onSizeChanged { waveformWidthPx = it.width }
                        .pointerInput(voiceTrackId) {
                            detectTapGestures(
                                onTap = { offset ->
                                    if (waveformWidthPx > 0) {
                                        seekToFraction(offset.x / waveformWidthPx.toFloat())
                                    }
                                },
                                onLongPress = { onLongClick() }
                            )
                        }
                        .pointerInput(voiceTrackId) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    if (waveformWidthPx > 0) {
                                        dragFraction = (offset.x / waveformWidthPx.toFloat()).coerceIn(0f, 1f)
                                    }
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    if (waveformWidthPx > 0) {
                                        dragFraction = (change.position.x / waveformWidthPx.toFloat()).coerceIn(0f, 1f)
                                    }
                                },
                                onDragEnd = {
                                    dragFraction?.let { seekToFraction(it) }
                                    dragFraction = null
                                },
                                onDragCancel = {
                                    dragFraction = null
                                }
                            )
                        },
                    verticalAlignment = Alignment.Bottom
                ) {
                    val totalBars = bars.size.coerceAtLeast(1)
                    bars.forEachIndexed { index, amp ->
                        val barFraction = (index + 1).toFloat() / totalBars.toFloat()
                        val isActive = (isCurrentVoice || dragFraction != null) && barFraction <= effectiveProgress + 0.015f
                        val barHeightDp = (amp.coerceIn(2, 16) * 1.35f).dp.coerceIn(3.dp, 22.dp)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 0.75.dp)
                                .height(barHeightDp)
                                .clip(RoundedCornerShape(1.5.dp))
                                .background(if (isActive) activeBarColor else inactiveBarColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val displayedTime = when {
                    dragFraction != null -> formatVoiceTimeMs((dragFraction!! * durationMs).toLong())
                    isCurrentVoice && (isPlaying || activePosMs > 0L) ->
                        playerState?.positionLabel ?: attachment.durationLabel
                    else -> attachment.durationLabel
                }
                Text(
                    text = "$displayedTime •",
                    color = timeColor,
                    fontSize = 11.5.sp,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isTranscriptExpanded) pillActiveBg else pillBg)
                    .clickable { isTranscriptExpanded = !isTranscriptExpanded }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "→A",
                    color = pillText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (isTranscriptExpanded) {
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (!isIncoming) Color.White.copy(alpha = 0.14f)
                        else SurfaceDark.copy(alpha = 0.55f)
                    )
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Расшифровка речи скоро будет доступна",
                    color = if (!isIncoming) Color.White.copy(alpha = 0.9f) else TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private data class VideoCircleMeta(
    val bitmap: Bitmap? = null,
    val width: Int = 0,
    val height: Int = 0
)

private object VideoCircleMetaCache {
    private val cache = LruCache<String, VideoCircleMeta>(48)

    fun get(uri: String?): VideoCircleMeta? = uri?.takeIf { it.isNotBlank() }?.let { cache.get(it) }

    fun put(uri: String, meta: VideoCircleMeta) {
        if (uri.isNotBlank()) {
            cache.put(uri, meta)
        }
    }
}

private object VideoCirclePreviewPool {
    private const val MAX_CONCURRENT_PREVIEWS = 2
    private val activeSlots = ArrayDeque<Pair<String, () -> Unit>>()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun releasePlayerAsync(mp: android.media.MediaPlayer?, surfaceToRelease: android.view.Surface? = null) {
        if (mp == null && surfaceToRelease == null) return
        ioScope.launch {
            if (mp != null) {
                runCatching { if (mp.isPlaying) mp.stop() }
                runCatching { mp.release() }
            }
            if (surfaceToRelease != null) {
                runCatching { surfaceToRelease.release() }
            }
        }
    }

    @Synchronized
    fun acquire(key: String, releaseSelf: () -> Unit) {
        activeSlots.removeAll { it.first == key }
        while (activeSlots.size >= MAX_CONCURRENT_PREVIEWS) {
            val oldest = activeSlots.removeFirst()
            runCatching { oldest.second() }
        }
        activeSlots.addLast(key to releaseSelf)
    }

    @Synchronized
    fun release(key: String) {
        activeSlots.removeAll { it.first == key }
    }
}

private fun applyVideoCircleCenterCrop(
    textureView: android.view.TextureView,
    videoWidth: Int,
    videoHeight: Int
) {
    val viewW = textureView.width.toFloat()
    val viewH = textureView.height.toFloat()
    if (videoWidth <= 0 || videoHeight <= 0 || viewW <= 0f || viewH <= 0f) return
    val videoAspect = videoWidth.toFloat() / videoHeight.toFloat()
    val viewAspect = viewW / viewH
    val scaleX: Float
    val scaleY: Float
    if (videoAspect > viewAspect) {
        scaleX = videoAspect / viewAspect
        scaleY = 1f
    } else {
        scaleX = 1f
        scaleY = viewAspect / videoAspect
    }
    val matrix = android.graphics.Matrix()
    matrix.setScale(scaleX, scaleY, viewW / 2f, viewH / 2f)
    textureView.setTransform(matrix)
}

/**
 * Видео-кружок в стиле Telegram:
 * - Свёрнутый вид (220.dp): зацикленное беззвучное авто-превью, без дуги/ползунка и без кнопок
 * - 1-й тап: плавное увеличение прямо в чате почти на всю ширину экрана, воспроизведение со звуком 1 раз и уведомление в шторке
 * - 2-й тап (пауза): появляется только перетаскиваемая точка-ползунок ● на ободе (без кнопок паузы/перемотки)
 * - После окончания или при тапе по пустой области чата кружок сворачивается обратно
 * - Длительность и время отправки с галочками вынесены под круг (снаружи)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatVideoCircleSection(
    message: ChatMessage,
    attachment: MessageAttachment.VideoCircle,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
    onLongClick: () -> Unit = {},
    onOpenContextMenu: () -> Unit = {},
    onPlay: () -> Unit = {}
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scheme = LocalAnonGramScheme.current
    val screenWidth = configuration.screenWidthDp.dp
    val scrollingState = LocalIsListScrollingState.current

    val audioController = remember { ChatAudioController.getInstance(context) }
    val circleTrackId = "circle_${message.id}"
    val isExpanded by remember(circleTrackId) {
        audioController.state
            .map { it.currentTrack?.id == circleTrackId }
            .distinctUntilChanged()
    }.collectAsState(initial = false)
    val playerState: ChatPlayerState? = if (isExpanded) {
        audioController.state.collectAsState().value
    } else null
    val isPlaying = if (isExpanded) (playerState?.isPlaying == true) else true

    val normalSize = 220.dp
    val expandedSize = (screenWidth - 16.dp).coerceIn(300.dp, 410.dp)
    val currentSize by animateDpAsState(
        targetValue = if (isExpanded) expandedSize else normalSize,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "circleSize"
    )

    val currentSizePx = with(density) { currentSize.toPx() }
    val radiusPx = (currentSizePx / 2f) - with(density) { 4.dp.toPx() }

    val videoFile = remember(attachment.uri) {
        attachment.uri?.takeIf { it.isNotBlank() }?.let { File(it) }?.takeIf { it.exists() && it.length() > 0L }
    }

    val fallbackTotalMs = remember(attachment.durationLabel) {
        val parts = attachment.durationLabel.split(":")
        val sec = if (parts.size == 2) {
            (parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 14)
        } else 14
        (sec * 1000L).coerceAtLeast(1000L)
    }
    val totalMs = if (isExpanded && (playerState?.durationMs ?: 0L) > 0L) playerState!!.durationMs else fallbackTotalMs

    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    var surfaceRef by remember { mutableStateOf<android.view.Surface?>(null) }
    var textureViewRef by remember { mutableStateOf<android.view.TextureView?>(null) }
    var mutedPreviewPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
    var hasRenderedVideoFrame by remember(videoFile?.absolutePath) { mutableStateOf(false) }

    val cachedMeta = remember(attachment.uri) { VideoCircleMetaCache.get(attachment.uri) }
    val videoMeta by produceState(initialValue = cachedMeta ?: VideoCircleMeta(), key1 = attachment.uri) {
        if (cachedMeta != null) {
            value = cachedMeta
            return@produceState
        }
        val file = videoFile
        if (file != null) {
            withContext(Dispatchers.IO) {
                val mmr = MediaMetadataRetriever()
                try {
                    mmr.setDataSource(file.absolutePath)
                    val bmp = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    val rawW = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                    val rawH = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                    val rot = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
                    val (w, h) = if (rot == 90 || rot == 270) rawH to rawW else rawW to rawH
                    val meta = VideoCircleMeta(bmp, w, h)
                    VideoCircleMetaCache.put(file.absolutePath, meta)
                    value = meta
                } catch (_: Exception) {
                } finally {
                    runCatching { mmr.release() }
                }
            }
        }
    }

    // При обновлении размеров кружка или видео обновляем CenterCrop-матрицу
    LaunchedEffect(videoMeta.width, videoMeta.height, currentSizePx) {
        textureViewRef?.let { tv ->
            applyVideoCircleCenterCrop(tv, videoMeta.width, videoMeta.height)
        }
    }

    // Переключаем Surface между локальным muted-превью (IO-поток + пул max 2) и общим ChatAudioController
    LaunchedEffect(isExpanded, surfaceRef, videoFile?.absolutePath) {
        val surface = surfaceRef ?: return@LaunchedEffect
        if (isExpanded) {
            VideoCirclePreviewPool.release(circleTrackId)
            val prev = mutedPreviewPlayer
            mutedPreviewPlayer = null
            VideoCirclePreviewPool.releasePlayerAsync(prev)
            audioController.attachVideoSurface(circleTrackId, surface)
        } else {
            audioController.detachVideoSurface(surface)
            val file = videoFile ?: return@LaunchedEffect
            snapshotFlow { scrollingState.value }
                .distinctUntilChanged()
                .collectLatest { isListScrolling ->
                    if (isListScrolling) {
                        mutedPreviewPlayer?.let { mp ->
                            withContext(Dispatchers.IO) {
                                runCatching { if (mp.isPlaying) mp.pause() }
                            }
                        }
                    } else {
                        delay(160)
                        val existing = mutedPreviewPlayer
                        if (existing != null) {
                            withContext(Dispatchers.IO) {
                                runCatching { if (!existing.isPlaying) existing.start() }
                            }
                        } else {
                            hasRenderedVideoFrame = false
                            val preparedMp = withContext(Dispatchers.IO) {
                                try {
                                    android.media.MediaPlayer().apply {
                                        setSurface(surface)
                                        setDataSource(file.absolutePath)
                                        setVolume(0f, 0f)
                                        isLooping = true
                                    }
                                } catch (_: Exception) {
                                    null
                                }
                            }
                            if (preparedMp != null) {
                                VideoCirclePreviewPool.acquire(circleTrackId) {
                                    if (mutedPreviewPlayer === preparedMp) {
                                        mutedPreviewPlayer = null
                                        hasRenderedVideoFrame = false
                                    }
                                    VideoCirclePreviewPool.releasePlayerAsync(preparedMp)
                                }
                                preparedMp.setOnVideoSizeChangedListener { _, vw, vh ->
                                    textureViewRef?.let { applyVideoCircleCenterCrop(it, vw, vh) }
                                }
                                preparedMp.setOnInfoListener { _, what, _ ->
                                    if (what == android.media.MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                                        hasRenderedVideoFrame = true
                                    }
                                    false
                                }
                                preparedMp.setOnPreparedListener { player ->
                                    if (mutedPreviewPlayer === player && !scrollingState.value) {
                                        runCatching { player.start() }
                                    }
                                }
                                mutedPreviewPlayer = preparedMp
                                withContext(Dispatchers.IO) {
                                    runCatching { preparedMp.prepareAsync() }
                                }
                            }
                        }
                    }
                }
        }
    }

    DisposableEffect(videoFile?.absolutePath) {
        onDispose {
            VideoCirclePreviewPool.release(circleTrackId)
            val surf = surfaceRef
            surfaceRef = null
            if (surf != null) {
                audioController.detachVideoSurface(surf)
            }
            val mp = mutedPreviewPlayer
            mutedPreviewPlayer = null
            hasRenderedVideoFrame = false
            VideoCirclePreviewPool.releasePlayerAsync(mp, surf)
        }
    }

    val effectivePosMs = if (isDragging) {
        (dragFraction * totalMs).toLong()
    } else if (isExpanded) {
        playerState?.positionMs ?: 0L
    } else {
        0L
    }
    val rawProgress = if (isDragging) {
        dragFraction
    } else if (isExpanded && totalMs > 0L) {
        (effectivePosMs.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val animatedProgress by animateFloatAsState(targetValue = rawProgress, label = "circleProgress")

    val elapsedSec = (effectivePosMs / 1000L).toInt()
    val elapsedLabel = "%02d:%02d".format(elapsedSec / 60, elapsedSec % 60)
    val isIncoming = message.direction == MessageDirection.INCOMING

    val currentSelectionMode by rememberUpdatedState(isSelectionMode)
    val currentExpanded by rememberUpdatedState(isExpanded)
    val currentDragging by rememberUpdatedState(isDragging)
    val currentSizePxState by rememberUpdatedState(currentSizePx)
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnLongClick by rememberUpdatedState(onLongClick)
    val currentOnOpenContextMenu by rememberUpdatedState(onOpenContextMenu)
    val currentOnPlay by rememberUpdatedState(onPlay)

    Column(
        horizontalAlignment = if (isIncoming) Alignment.Start else Alignment.End
    ) {
        Box(
            modifier = Modifier
                .size(currentSize)
                .clip(CircleShape)
                .background(Color(0xFF1B1B22))
                .border(
                    width = if (isSelected) 3.dp else 1.5.dp,
                    color = if (isSelected) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.22f),
                    shape = CircleShape
                )
                .pointerInput(message.id) {
                    detectTapGestures(
                        onTap = { offset ->
                            val r = currentSizePxState / 2f
                            val dx = offset.x - r
                            val dy = offset.y - r
                            val insideCircle = (dx * dx + dy * dy) <= (r * r)
                            if (currentSelectionMode) {
                                currentOnSelect()
                            } else if (insideCircle) {
                                if (!currentExpanded) {
                                    currentOnPlay()
                                } else if (!currentDragging) {
                                    audioController.togglePlayPause()
                                }
                            } else {
                                currentOnOpenContextMenu()
                            }
                        },
                        onLongPress = {
                            if (currentSelectionMode) currentOnSelect() else currentOnLongClick()
                        }
                    )
                }
                .then(
                    // Перемотка точкой-ползунком доступна в раскрытом состоянии на паузе (после тапа на кружок)
                    if (isExpanded && (!isPlaying || isDragging)) {
                        Modifier.pointerInput(totalMs, currentSizePx) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDragging = true
                                    val dx = offset.x - (currentSizePx / 2f)
                                    val dy = offset.y - (currentSizePx / 2f)
                                    var deg = (atan2(dy, dx) * 180f / PI.toFloat()) + 90f
                                    if (deg < 0f) deg += 360f
                                    val fraction = (deg / 360f).coerceIn(0f, 1f)
                                    dragFraction = fraction
                                    audioController.seek((fraction * totalMs).toLong())
                                },
                                onDragEnd = {
                                    audioController.seek((dragFraction * totalMs).toLong())
                                    isDragging = false
                                },
                                onDragCancel = {
                                    isDragging = false
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val touchPos = change.position
                                    val dx = touchPos.x - (currentSizePx / 2f)
                                    val dy = touchPos.y - (currentSizePx / 2f)
                                    var deg = (atan2(dy, dx) * 180f / PI.toFloat()) + 90f
                                    if (deg < 0f) deg += 360f
                                    val fraction = (deg / 360f).coerceIn(0f, 1f)
                                    dragFraction = fraction
                                    audioController.seek((fraction * totalMs).toLong())
                                }
                            )
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            val bmp = videoMeta.bitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Видео-сообщение",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            if (videoFile != null) {
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { ctx ->
                        android.view.TextureView(ctx).apply {
                            isOpaque = false
                            textureViewRef = this
                            surfaceTextureListener = object : android.view.TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(
                                    st: android.graphics.SurfaceTexture,
                                    w: Int,
                                    h: Int
                                ) {
                                    val oldSurface = surfaceRef
                                    if (oldSurface != null) {
                                        audioController.detachVideoSurface(oldSurface)
                                    }
                                    surfaceRef = android.view.Surface(st)
                                    if (oldSurface != null) {
                                        VideoCirclePreviewPool.releasePlayerAsync(null, oldSurface)
                                    }
                                    applyVideoCircleCenterCrop(this@apply, videoMeta.width, videoMeta.height)
                                }

                                override fun onSurfaceTextureSizeChanged(
                                    st: android.graphics.SurfaceTexture,
                                    w: Int,
                                    h: Int
                                ) {
                                    applyVideoCircleCenterCrop(this@apply, videoMeta.width, videoMeta.height)
                                }

                                override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
                                    val surf = surfaceRef
                                    surfaceRef = null
                                    if (surf != null) {
                                        audioController.detachVideoSurface(surf)
                                    }
                                    val mp = mutedPreviewPlayer
                                    mutedPreviewPlayer = null
                                    hasRenderedVideoFrame = false
                                    VideoCirclePreviewPool.releasePlayerAsync(mp, surf)
                                    return true
                                }

                                override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {
                                    if (!hasRenderedVideoFrame) hasRenderedVideoFrame = true
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(if (hasRenderedVideoFrame || isExpanded) 1f else 0f)
                )
            }

            // Тонкая дуга прогресса показывается только при раскрытом воспроизведении (без ползунка в превью)
            if (isExpanded) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = radiusPx
                    val strokeWidth = 3.5.dp.toPx()
                    val centerOffset = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = radius,
                        center = centerOffset,
                        style = Stroke(width = strokeWidth)
                    )

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
            }

            // Перетаскиваемая точка-ползунок ● на ободе появляется ТОЛЬКО при тапе (на паузе) в полном просмотре
            if (isExpanded && (!isPlaying || isDragging)) {
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
                        .border(2.dp, scheme.accent, CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Вне кружочка снизу: слева длительность/время проигрывания, справа время отправки и статус
        Row(
            modifier = Modifier
                .width(currentSize)
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isExpanded) elapsedLabel else attachment.durationLabel,
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
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
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
}

