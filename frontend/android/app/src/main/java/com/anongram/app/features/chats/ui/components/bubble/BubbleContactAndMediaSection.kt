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
 * Видео-кружок в пузыре (маленький круг в чате).
 */
@Composable
internal fun ChatVideoCircleSection(
    attachment: MessageAttachment.VideoCircle,
    onClick: () -> Unit = {}
) {
    val thumbnailBitmap by produceState<Bitmap?>(initialValue = null, key1 = attachment.uri) {
        val uriStr = attachment.uri
        if (!uriStr.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                val file = File(uriStr)
                if (file.exists()) {
                    val mmr = MediaMetadataRetriever()
                    try {
                        mmr.setDataSource(file.absolutePath)
                        value = mmr.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                    } catch (_: Exception) {
                    } finally {
                        mmr.release()
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(attachmentColor(attachment.colorSeed))
            .border(1.5.dp, Color.White.copy(alpha = 0.25f), CircleShape)
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

        // Кнопка воспроизведения по центру
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Смотреть кружочек",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }

        // Внизу длительность со значком звука
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "${attachment.durationLabel} 🔊",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
