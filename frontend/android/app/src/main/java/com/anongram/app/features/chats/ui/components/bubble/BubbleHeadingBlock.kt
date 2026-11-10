package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Модульный компонент отображения чистого заголовка H1..H6 без символов решетки (#).
 * Обеспечивает четкую визуальную иерархию Word / Telegram с высокой контрастностью.
 */
@Composable
fun BubbleHeadingBlock(
    level: Int,
    text: String,
    modifier: Modifier = Modifier
) {
    val (fontSizeSp, fontWeight) = when (level.coerceIn(1, 6)) {
        1 -> 24.sp to FontWeight.Bold
        2 -> 21.sp to FontWeight.Bold
        3 -> 18.sp to FontWeight.Bold
        4 -> 16.sp to FontWeight.SemiBold
        5 -> 14.sp to FontWeight.SemiBold
        else -> 12.sp to FontWeight.SemiBold
    }

    Text(
        text = text,
        color = Color.White,
        fontSize = fontSizeSp,
        fontWeight = fontWeight,
        lineHeight = (fontSizeSp.value * 1.25f).sp,
        modifier = modifier.padding(vertical = 2.dp)
    )
}
