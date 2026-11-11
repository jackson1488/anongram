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
 * Видео-кружок в пузыре (круг в чате 250dp с живым беззвучным автовоспроизведением как в Telegram — Фото 2 и 4).
 */
@Composable
internal fun ChatVideoCircleSection(
    message: ChatMessage,
    attachment: MessageAttachment.VideoCircle,
    onClick: () -> Unit = {}
) {
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

    Box(
        modifier = Modifier
            .size(250.dp)
            .clip(CircleShape)
            .background(Color(0xFF1B1B22))
            .border(1.5.dp, Color.White.copy(alpha = 0.22f), CircleShape)
            .clickable(onClick = onClick),
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
            var loopPlayer by remember(videoFile.absolutePath) {
                androidx.compose.runtime.mutableStateOf<android.media.MediaPlayer?>(null)
            }
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
                                        setVolume(0f, 0f)
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
                            ) {}

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

        // Внизу внутри кружочка (Фото 2 и Фото 4 в стиле Telegram):
        // Слева: Длительность со значком кружочка [ 00:16 • ]
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 18.dp, bottom = 14.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.58f))
                .padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = attachment.durationLabel,
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
                .padding(end = 18.dp, bottom = 14.dp)
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

