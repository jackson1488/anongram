package com.anongram.app.features.chats.ui.input.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Горизонтальная панель инструментов форматирования над клавиатурой (макет media_1794245794747.png).
 */
@Composable
fun TextFormattingToolbar(
    isAaActive: Boolean,
    onToggleAa: () -> Unit,
    onToggleAi: () -> Unit,
    onToggleEmoji: () -> Unit,
    onInsertList: () -> Unit,
    onInsertTable: () -> Unit,
    onInsertFormula: () -> Unit,
    onOpenAttach: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val itemColor = Color(0xFFB0B0B5)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Кнопка Ai
            IconButton(onClick = onToggleAi, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "Ai Помощник",
                    tint = itemColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Кнопка Эмодзи
            IconButton(onClick = onToggleEmoji, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.SentimentSatisfiedAlt,
                    contentDescription = "Эмодзи",
                    tint = itemColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Кнопка Aa (Форматирование стиля)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isAaActive) scheme.accent.copy(alpha = 0.25f) else Color.Transparent)
                    .clickable(onClick = onToggleAa),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aa",
                    color = if (isAaActive) scheme.accent else itemColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Кнопка списков
            IconButton(onClick = onInsertList, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.FormatListBulleted,
                        contentDescription = "Список",
                        tint = itemColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "★",
                        color = Color(0xFFC084FC),
                        fontSize = 7.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            // Кнопка таблицы
            IconButton(onClick = onInsertTable, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.GridOn,
                        contentDescription = "Таблица",
                        tint = itemColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "★",
                        color = Color(0xFFC084FC),
                        fontSize = 7.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            // Кнопка формулы ∑
            IconButton(onClick = onInsertFormula, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Outlined.Functions,
                        contentDescription = "Формула",
                        tint = itemColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = "★",
                        color = Color(0xFFC084FC),
                        fontSize = 7.sp,
                        modifier = Modifier.align(Alignment.BottomEnd)
                    )
                }
            }

            // Кнопка скрепки
            IconButton(onClick = onOpenAttach, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = "Вложить",
                    tint = itemColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Фиолетовая кнопка отправки
        IconButton(
            onClick = onSend,
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(scheme.accent)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Отправить",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
