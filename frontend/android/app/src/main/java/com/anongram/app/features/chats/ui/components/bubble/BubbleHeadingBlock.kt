package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

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
    val scheme = LocalAnonGramScheme.current
    val contentTint = LocalContentColor.current
    val (fontSizeSp, fontWeight) = when (level.coerceIn(1, 6)) {
        1 -> 24.sp to FontWeight.Bold
        2 -> 21.sp to FontWeight.Bold
        3 -> 18.sp to FontWeight.Bold
        4 -> 16.sp to FontWeight.SemiBold
        5 -> 14.sp to FontWeight.SemiBold
        else -> 12.sp to FontWeight.SemiBold
    }

    if (text.contains("||")) {
        InlineSpoilerLine(
            line = text,
            textSizeSp = fontSizeSp.value.toInt(),
            accent = scheme.accent,
            modifier = modifier.padding(vertical = 2.dp)
        )
    } else {
        Text(
            text = buildRichAnnotatedText(text, scheme.accent),
            color = contentTint,
            fontSize = fontSizeSp,
            fontWeight = fontWeight,
            lineHeight = (fontSizeSp.value * 1.25f).sp,
            modifier = modifier.padding(vertical = 2.dp)
        )
    }
}
