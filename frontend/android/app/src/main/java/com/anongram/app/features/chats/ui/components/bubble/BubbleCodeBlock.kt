package com.anongram.app.features.chats.ui.components.bubble

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.anongram.app.features.chats.ui.input.banner.ChatActionBannerBus
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.messages.domain.ChatMessage
import com.anongram.app.features.chats.messages.domain.MessageDirection
import com.anongram.app.features.chats.messages.domain.MessageStatus
import com.anongram.app.features.chats.messages.domain.timeLabel

/**
 * Стилизованный пузырь блока кода:
 * - Акцентная обводка из текущей темы;
 * - Верхняя плашка с языком (ABNF, Kotlin и др.), кнопкой копирования и меню;
 * - Моноширинный шрифт;
 * - Сворачивание при длине свыше 4000 символов с кнопкой «Читать дальше».
 */
@Composable
fun BubbleCodeBlock(
    message: ChatMessage,
    language: String,
    codeText: String,
    textScale: Float,
    onOpenFullText: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val isIncoming = message.direction == MessageDirection.INCOMING
    val isLong = codeText.length > 4000
    val displayText = if (isLong) codeText.take(4000) + "…" else codeText

    CompositionLocalProvider(LocalContentColor provides scheme.textPrimary) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.surface)
                .border(1.5.dp, scheme.accent, RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Верхняя шапка: Название языка, меню и кнопка копирования
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = language.uppercase(),
                        color = scheme.textPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Menu,
                            contentDescription = "Меню кода",
                            tint = scheme.textMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("code", codeText))
                                ChatActionBannerBus.show(Icons.Outlined.ContentCopy) { it.bannerCodeCopied }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "Копировать код",
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Текст кода
                Text(
                    text = displayText,
                    color = scheme.textPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (13f * textScale).coerceIn(9f, 18f).sp,
                    lineHeight = (18f * textScale).coerceIn(13f, 24f).sp
                )

                // Кнопка «Читать дальше» для длинных листингов
                if (isLong) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Читать дальше (${codeText.length} символов) →",
                        color = scheme.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable(onClick = onOpenFullText)
                            .padding(vertical = 4.dp)
                    )
                }

            Spacer(modifier = Modifier.height(4.dp))

                // Время и статус доставки
                BubbleTimestampRow(
                    timeLabel = message.timeLabel(),
                    isEdited = message.isEdited,
                    isIncoming = isIncoming,
                    status = message.status,
                    textScale = textScale,
                    onRetry = onRetry,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
