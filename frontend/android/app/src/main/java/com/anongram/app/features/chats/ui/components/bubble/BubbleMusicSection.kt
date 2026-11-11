package com.anongram.app.features.chats.ui.components.bubble

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import com.anongram.app.features.chats.media.player.ChatAudioController
import com.anongram.app.features.chats.media.player.ChatTrack
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageAttachment
import com.anongram.app.features.chats.messages.domain.timeLabel
import kotlinx.coroutines.launch

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.anongram.app.features.chats.media.player.AudioMetadataHelper
import kotlin.math.roundToInt

/**
 * Строка музыкального трека (одиночного или в слитной группе до 10 треков):
 * - Слева круглая кнопка Play/Pause 44.dp с реальной обложкой альбома
 * - Справа название трека и снизу интерактивный ползунок или «X:XX • X.X MB»
 * - В правом верхнем углу (только у первого трека в группе) — кнопка «⋮»
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun ChatMusicItemRow(
    message: ChatMessage,
    music: MessageAttachment.Music,
    isIncoming: Boolean,
    accent: Color,
    searchQuery: String,
    showThreeDots: Boolean,
    showBottomTimestamp: Boolean,
    isSelectionMode: Boolean,
    isItemSelected: Boolean,
    isPinned: Boolean = false,
    onPlayTrack: ((ChatTrack) -> Unit)? = null,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onSwipeReply: () -> Unit,
    onForwardItem: () -> Unit,
    onPinItem: (() -> Unit)? = null,
    onEditItem: (() -> Unit)? = null,
    onDeleteItem: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioController = remember { ChatAudioController.getInstance(context) }
    val playerState by audioController.state.collectAsState()
    val isCurrentlyActive = playerState.currentTrack?.title == music.title
    val isPlaying = isCurrentlyActive && playerState.isPlaying
    var isMenuOpen by remember { mutableStateOf(false) }

    var albumArt by remember(music.uri, music.title, music.artist) {
        mutableStateOf(AudioMetadataHelper.getCachedArt(music.uri ?: "${music.artist}_${music.title}"))
    }
    LaunchedEffect(music.uri, music.title, music.artist) {
        if (albumArt == null) {
            val parsedUri = music.uri?.let { android.net.Uri.parse(it) }
            val meta = AudioMetadataHelper.extractMetadata(context, parsedUri, music.title, music.artist)
            albumArt = meta.albumArt
        }
    }

    val displayTitle = remember(music.artist, music.title) {
        if (music.artist.isNotBlank() && music.artist != "Неизвестен" && !music.title.contains(music.artist)) {
            "${music.artist} — ${music.title}"
        } else {
            music.title
        }
    }

    val togglePlay = {
        if (isCurrentlyActive) {
            audioController.togglePlayPause()
        } else {
            val durMs = AudioMetadataHelper.parseDurationLabelToMs(music.durationLabel)
            val track = ChatTrack(
                id = "${message.id}_${music.title}",
                messageId = message.id,
                title = music.title,
                artist = music.artist,
                performer = if (music.artist.isNotBlank() && music.artist != "Неизвестен") music.artist else "AnonGram",
                durationLabel = music.durationLabel,
                durationMs = durMs,
                sizeLabel = music.sizeLabel,
                albumArtBitmap = albumArt,
                uri = music.uri?.let { android.net.Uri.parse(it) } ?: android.net.Uri.EMPTY
            )
            onPlayTrack?.invoke(track) ?: audioController.play(listOf(track), track)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onItemClick()
                    } else {
                        togglePlay()
                    }
                },
                onLongClick = onItemLongClick
            )
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Круглая обложка 44.dp с иконкой Play/Pause поверх
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .clickable {
                            if (isSelectionMode) onItemClick() else togglePlay()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (albumArt != null) {
                        Image(
                            bitmap = albumArt!!.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.35f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (isPlaying) "Пауза" else "Играть",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isIncoming) accent else Color.White.copy(alpha = 0.24f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = if (isPlaying) "Пауза" else "Играть",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // В режиме выбора на отдельном треке загорается круглая галочка
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isItemSelected) Color(0xFF8B5CF6) else Color.Black.copy(alpha = 0.55f))
                            .border(1.5.dp, Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isItemSelected) {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = "Выбрано",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (showThreeDots) 24.dp else 4.dp)
            ) {
                Text(
                    text = if (searchQuery.isNotBlank()) {
                        buildHighlightedText(displayTitle, searchQuery, accent)
                    } else {
                        AnnotatedString(displayTitle)
                    },
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isCurrentlyActive && (isPlaying || playerState.progressMs > 0)) {
                    // Активное воспроизведение: тайминг + интерактивный слайдер с круглым манипулятором
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = playerState.positionLabel,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        var isDragging by remember { mutableStateOf(false) }
                        var dragProgress by remember { mutableFloatStateOf(0f) }
                        val currentProgress = if (isDragging) dragProgress else playerState.progress

                        BoxWithConstraints(
                            modifier = Modifier
                                .weight(1f)
                                .height(20.dp)
                                .pointerInput(playerState.durationMs) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { offset ->
                                            isDragging = true
                                            dragProgress = (offset.x / size.width).coerceIn(0f, 1f)
                                        },
                                        onDragEnd = {
                                            isDragging = false
                                            val targetMs = (dragProgress * playerState.durationMs.coerceAtLeast(1L)).toLong()
                                            audioController.seek(targetMs)
                                        },
                                        onDragCancel = {
                                            isDragging = false
                                        },
                                        onHorizontalDrag = { change, _ ->
                                            change.consume()
                                            dragProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                                        }
                                    )
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            val widthPx = constraints.maxWidth.toFloat()
                            val thumbXPx = (currentProgress * widthPx).coerceIn(0f, widthPx)

                            // Неактивная полоса
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.28f))
                            )
                            // Активная полоса
                            Box(
                                modifier = Modifier
                                    .width(with(LocalDensity.current) { thumbXPx.toDp() })
                                    .height(2.5.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White)
                            )
                            // Белый круглый бегунок
                            Box(
                                modifier = Modifier
                                    .offset { IntOffset((thumbXPx - 5.dp.toPx()).roundToInt(), 0) }
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                } else {
                    // В неактивном состоянии: "3:56 • 3.7 MB"
                    Text(
                        text = "${music.durationLabel} • ${music.sizeLabel}",
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.5.sp,
                        maxLines = 1
                    )
                }

                if (showBottomTimestamp) {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }

        if (showThreeDots) {
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                IconButton(
                    onClick = { isMenuOpen = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "Меню музыки",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                DropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    modifier = Modifier
                        .width(235.dp)
                        .background(Color(0xFF14151B), RoundedCornerShape(16.dp))
                        .border(0.5.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                ) {
                    // 1. Ответить (первый пункт со стрелкой влево)
                    DropdownMenuItem(
                        text = { Text("Ответить", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.AutoMirrored.Outlined.Reply, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onSwipeReply()
                        }
                    )

                    // 2. Сохранить в музыку
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Сохранить в музыку",
                                color = Color.White,
                                fontSize = 14.5.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Download,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            isMenuOpen = false
                            scope.launch {
                                val ok = saveMusicToDeviceMusic(context, music)
                                Toast.makeText(
                                    context,
                                    if (ok) "Сохранено в музыку: $displayTitle" else "Не удалось сохранить трек",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )

                    // 3. Поделиться
                    DropdownMenuItem(
                        text = { Text("Поделиться", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Share, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            shareTextOrFileFallback(context, displayTitle)
                        }
                    )

                    // 4. Переслать
                    DropdownMenuItem(
                        text = { Text("Переслать", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Forward, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onForwardItem()
                        }
                    )

                    // 5. Закрепить / Открепить
                    DropdownMenuItem(
                        text = { Text(if (isPinned) "Открепить" else "Закрепить", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.PushPin, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onPinItem?.invoke()
                        }
                    )

                    // 6. Изменить (замена трека)
                    DropdownMenuItem(
                        text = { Text("Изменить", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onEditItem?.invoke()
                        }
                    )

                    // 7. Удалить
                    DropdownMenuItem(
                        text = { Text("Удалить", color = Color(0xFFFF5252), fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Delete, null, tint = Color(0xFFFF5252), modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onDeleteItem()
                        }
                    )
                }
            }
        }

        if (showBottomTimestamp) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 2.dp, bottom = 1.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = message.timeLabel(),
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!isIncoming) {
                    Spacer(modifier = Modifier.width(3.dp))
                    DeliveryStatus(status = message.status, onRetry = onRetry)
                }
            }
        }
    }
}
