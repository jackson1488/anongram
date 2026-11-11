package com.anongram.app.features.settings.chat_settings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlin.math.roundToInt

/**
 * Карточка регулировки размера текста сообщений (1..30 sp) с живым превью пузырей чата.
 * Адаптируется к активной теме (`LocalAnonGramScheme.current`).
 */
@Composable
fun ChatTextSizeCard(
    textSizeSp: Int,
    userName: String,
    onTextSizeChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.card)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Размер текста сообщений",
                color = scheme.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = textSizeSp.toFloat(),
                    onValueChange = { raw ->
                        onTextSizeChange(raw.roundToInt().coerceIn(1, 30))
                    },
                    valueRange = 1f..30f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = scheme.accent,
                        activeTrackColor = scheme.accent,
                        inactiveTrackColor = scheme.divider,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = textSizeSp.toString(),
                    color = scheme.accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        val textScale = (textSizeSp / 16f).coerceIn(0.35f, 1.5f)
        val bubbleRadius = (16f * textScale).coerceIn(7f, 20f).dp
        val bubbleTail = (5f * textScale).coerceIn(2.5f, 6.5f).dp
        val padH = (11f * textScale).coerceIn(5f, 15f).dp
        val padV = (7f * textScale).coerceIn(2.5f, 10f).dp
        val timeFontSize = (10.5f * textScale).coerceIn(5.5f, 14.5f).sp
        val tickSize = (13f * textScale).coerceIn(7f, 17.5f).dp
        val gapBetweenTextAndTime = (8f * textScale).coerceIn(4f, 11f).dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.surface)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 285.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = bubbleRadius,
                                topEnd = bubbleRadius,
                                bottomStart = bubbleTail,
                                bottomEnd = bubbleRadius
                            )
                        )
                        .background(scheme.incomingBubbleBg)
                        .border(
                            1.dp,
                            scheme.incomingBubbleBorder,
                            RoundedCornerShape(
                                topStart = bubbleRadius,
                                topEnd = bubbleRadius,
                                bottomStart = bubbleTail,
                                bottomEnd = bubbleRadius
                            )
                        )
                        .padding(horizontal = padH, vertical = padV)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape((8f * textScale).coerceIn(4f, 10f).dp))
                            .background(scheme.replyStripIncoming)
                            .padding(
                                horizontal = (8f * textScale).coerceIn(4f, 10f).dp,
                                vertical = (5f * textScale).coerceIn(2.5f, 7f).dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width((3f * textScale).coerceIn(2f, 4f).dp)
                                .height((30f * textScale).coerceIn(14f, 38f).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(scheme.accent)
                        )
                        Spacer(modifier = Modifier.width((8f * textScale).coerceIn(4f, 10f).dp))
                        Column {
                            Text(
                                text = userName,
                                color = scheme.accent,
                                fontSize = (11.5f * textScale).coerceIn(6f, 15.5f).sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Доброе утро! 👋",
                                color = scheme.incomingText.copy(alpha = 0.85f),
                                fontSize = (12.5f * textScale).coerceIn(6.5f, 16.5f).sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height((5f * textScale).coerceIn(2f, 7f).dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(gapBetweenTextAndTime)
                    ) {
                        Text(
                            text = "Знаешь, который час?",
                            color = scheme.incomingText,
                            fontSize = textSizeSp.sp,
                            lineHeight = (textSizeSp * 1.26f).coerceAtLeast(textSizeSp + 2f).sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "20:07",
                            color = scheme.textMuted,
                            fontSize = timeFontSize,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                val outShape = RoundedCornerShape(
                    topStart = bubbleRadius,
                    topEnd = bubbleRadius,
                    bottomStart = bubbleRadius,
                    bottomEnd = bubbleTail
                )
                Row(
                    modifier = Modifier
                        .widthIn(max = 285.dp)
                        .clip(outShape)
                        .background(
                            Brush.linearGradient(
                                listOf(scheme.outgoingBubbleStart, scheme.outgoingBubbleEnd)
                            )
                        )
                        .border(1.dp, scheme.outgoingBubbleBorder, outShape)
                        .padding(horizontal = padH, vertical = padV),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(gapBetweenTextAndTime)
                ) {
                    Text(
                        text = "В Токио утро 😎",
                        color = scheme.outgoingText,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.26f).coerceAtLeast(textSizeSp + 2f).sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "20:22",
                            color = scheme.outgoingText.copy(alpha = 0.78f),
                            fontSize = timeFontSize,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width((3f * textScale).coerceIn(1.5f, 4.5f).dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tick_double),
                            contentDescription = null,
                            tint = scheme.outgoingText,
                            modifier = Modifier.size(tickSize)
                        )
                    }
                }
            }
        }
    }
}
