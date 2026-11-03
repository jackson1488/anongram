package com.anongram.app.features.chats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.TextPrimary

/**
 * Панель режима выделения сообщений в стиле плавающих островков:
 * 1. Левый островок: кнопка-крестик (отмена выделения).
 * 2. Центральный островок: «Выбрано N».
 * 3. Правый островок:
 *    - Если выбрано 1 фото: Кисточка, Переслать (стрелка вправо ТГ), Удалить.
 *    - Если выбран 1 файл / трек: Скачать, Переслать (стрелка вправо ТГ), Удалить.
 *    - Иначе (текст / мультивыбор): Копировать, Переслать (стрелка вправо ТГ), Удалить.
 */
@Composable
internal fun SelectionPanel(
    selectedCount: Int,
    isPhoto: Boolean = false,
    isFileOrMusic: Boolean = false,
    isVideoCircle: Boolean = false,
    isVoice: Boolean = false,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onBrushEdit: () -> Unit = {},
    onDownload: () -> Unit = {},
    onShare: () -> Unit = {},
    onForward: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Островок 1: Кнопка закрытия выбора (крестик, 50dp)
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(IslandBackground)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Отмена выбора",
                    tint = TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Островок 2: Счётчик «Выбрано N» (50dp, одинаковая толщина)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(IslandBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(25.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = "Выбрано $selectedCount",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Островок 3: Действия
        Row(
            modifier = Modifier
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(IslandBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(25.dp))
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            when {
                selectedCount == 1 && isVideoCircle -> {
                    // Видео-кружочек: Скачать, Переслать, Поделиться, Удалить
                    IconButton(
                        onClick = onDownload,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FileDownload,
                            contentDescription = "Скачать",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onForward,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Reply,
                            contentDescription = "Переслать",
                            tint = TextPrimary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { scaleX = -1f }
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Поделиться",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Удалить",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                selectedCount == 1 && isVoice -> {
                    // Голосовое: Редактировать / обрезать, Переслать, Поделиться, Удалить
                    IconButton(
                        onClick = onBrushEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Brush,
                            contentDescription = "Редактировать",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onForward,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Reply,
                            contentDescription = "Переслать",
                            tint = TextPrimary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { scaleX = -1f }
                        )
                    }
                    IconButton(
                        onClick = onShare,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Поделиться",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Удалить",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                else -> {
                    when {
                        selectedCount == 1 && isPhoto -> {
                            // Фото: Кисточка
                            IconButton(
                                onClick = onBrushEdit,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Brush,
                                    contentDescription = "Редактировать / Заменить",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        selectedCount == 1 && isFileOrMusic -> {
                            // Файл или музыка: Скачать
                            IconButton(
                                onClick = onDownload,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.FileDownload,
                                    contentDescription = "Сохранить",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        else -> {
                            // Текст или мультивыбор: Копировать
                            IconButton(
                                onClick = onCopy,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Копировать",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Переслать — чистая стрелка вправо как в Telegram
                    IconButton(
                        onClick = onForward,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Reply,
                            contentDescription = "Переслать",
                            tint = TextPrimary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { scaleX = -1f }
                        )
                    }

                    // Удалить
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Удалить",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
