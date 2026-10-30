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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Forward
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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

/**
 * Строка музыкального трека (одиночного или в слитной группе до 10 треков):
 * - Слева круглая кнопка Play/Pause 44.dp
 * - Справа название трека и снизу прогресс «0:00 / X:XX»
 * - В правом верхнем углу (только у первого трека в группе) — кнопка «⋮» со строгим пунктом «Сохранить в музыку»
 * - В режиме выбора при выборе одного трека горит только галочка (без подсветки строки), а при выборе всей группы светится вся группа
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
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onSwipeReply: () -> Unit,
    onForwardItem: () -> Unit,
    onDeleteItem: () -> Unit,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isPlaying by remember { mutableStateOf(false) }
    var isMenuOpen by remember { mutableStateOf(false) }

    val displayTitle = remember(music.artist, music.title) {
        if (music.artist.isNotBlank() && music.artist != "Неизвестен" && !music.title.contains(music.artist)) {
            "${music.artist} - ${music.title}"
        } else {
            music.title
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
                        isPlaying = !isPlaying
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
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isIncoming) accent else Color.White.copy(alpha = 0.24f))
                        .clickable {
                            if (isSelectionMode) onItemClick() else isPlaying = !isPlaying
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                        contentDescription = if (isPlaying) "Пауза" else "Играть",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // В режиме выбора на отдельном треке загорается только круглая галочка
                if (isSelectionMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(if (isItemSelected) accent else Color.Black.copy(alpha = 0.55f))
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isPlaying) "0:14 / ${music.durationLabel}" else "0:00 / ${music.durationLabel}",
                    color = TextSecondary,
                    fontSize = 12.5.sp,
                    maxLines = 1
                )
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

                    DropdownMenuItem(
                        text = { Text("Ответить", color = Color.White, fontSize = 14.5.sp) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Reply, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        },
                        onClick = {
                            isMenuOpen = false
                            onSwipeReply()
                        }
                    )

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
