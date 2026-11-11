package com.anongram.app.features.wallpapers.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Превью пузырей чата для экрана настройки и предпросмотра обоев:
 * - Системная плашка с датой
 * - Один входящий пузырь сообщения
 * - Один исходящий пузырь сообщения в палитре активной темы
 */
@Composable
fun WallpaperMockChat(
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val incomingShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp)
    val outgoingShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Системная плашка с датой
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.bannerBg)
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Text(
                text = "Сегодня",
                color = scheme.textSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Входящее сообщение
        Box(
            modifier = Modifier
                .align(Alignment.Start)
                .fillMaxWidth(0.78f)
                .clip(incomingShape)
                .background(scheme.incomingBubbleBg.copy(alpha = 0.94f))
                .border(1.dp, scheme.incomingBubbleBorder, incomingShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = "Привет! Как тебе новый фон чата? 🔥",
                    color = scheme.incomingText,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "19:42",
                    color = scheme.textSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Исходящее сообщение
        Box(
            modifier = Modifier
                .align(Alignment.End)
                .fillMaxWidth(0.78f)
                .clip(outgoingShape)
                .background(
                    Brush.horizontalGradient(
                        listOf(scheme.outgoingBubbleStart, scheme.outgoingBubbleEnd)
                    )
                )
                .border(1.dp, scheme.outgoingBubbleBorder, outgoingShape)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = "Выглядит шикарно, очень стильно и анонимно! 🚀",
                    color = scheme.outgoingText,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(
                        text = "19:43",
                        color = scheme.outgoingText.copy(alpha = 0.78f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = null,
                        tint = scheme.outgoingText.copy(alpha = 0.85f),
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
