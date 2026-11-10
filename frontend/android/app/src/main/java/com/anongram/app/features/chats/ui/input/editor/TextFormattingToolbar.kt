package com.anongram.app.features.chats.ui.input.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Functions
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Центрированная парящая панель инструментов форматирования над клавиатурой
 * по макетам media_1794253331200.png и media_1794253317328.png.
 */
@Composable
fun TextFormattingToolbar(
    isAaActive: Boolean,
    onToggleAa: () -> Unit,
    isEmojiActive: Boolean = false,
    onToggleEmoji: () -> Unit,
    isListActive: Boolean = false,
    onToggleList: () -> Unit,
    isFormulaActive: Boolean = false,
    onToggleFormula: () -> Unit,
    onOpenAttach: () -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val itemColor = Color(0xFFB0B0B5)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Центрированный парящий островок инструментов форматирования
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF1E1E24))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(22.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Кнопка Эмодзи / Клавиатура
                IconButton(onClick = onToggleEmoji, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = if (isEmojiActive) Icons.Outlined.Keyboard else Icons.Outlined.SentimentSatisfiedAlt,
                        contentDescription = if (isEmojiActive) "Клавиатура" else "Эмодзи",
                        tint = if (isEmojiActive) scheme.accent else itemColor,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Кнопка Aa (Стили текста)
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

                // Кнопка списков ≡
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isListActive) scheme.accent.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable(onClick = onToggleList),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.FormatListBulleted,
                        contentDescription = "Списки",
                        tint = if (isListActive) scheme.accent else itemColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Кнопка формулы ∑
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isFormulaActive) scheme.accent.copy(alpha = 0.25f) else Color.Transparent)
                        .clickable(onClick = onToggleFormula),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Functions,
                        contentDescription = "Формула LaTeX",
                        tint = if (isFormulaActive) scheme.accent else itemColor,
                        modifier = Modifier.size(19.dp)
                    )
                }

                // Кнопка скрепки 📎
                IconButton(onClick = onOpenAttach, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = "Прикрепить файл/фото",
                        tint = itemColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Фиолетовая кнопка отправки справа
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(42.dp)
                .clip(CircleShape)
                .background(scheme.accent)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center
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
