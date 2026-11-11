package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Модульный компонент отображения абзаца текста с поддержкой:
 * - Инлайн-спойлеров ||скрытый текст||;
 * - **Жирного**, *курсива*, ~~зачёркнутого~~, <u>подчёркнутого</u>;
 * - Моноширинного кода `code` и кастомных шрифтов.
 */
@Composable
fun BubbleParagraphBlock(
    text: String,
    textSizeSp: Int = 15,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val contentTint = LocalContentColor.current
    val lines = text.split("\n")

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEachIndexed { lineIdx, line ->
            if (line.contains("||") && line.indexOf("||") != line.lastIndexOf("||")) {
                InlineSpoilerLine(
                    line = line,
                    textSizeSp = textSizeSp,
                    accent = scheme.accent
                )
            } else if (line.isNotEmpty()) {
                Text(
                    text = buildRichAnnotatedText(line, scheme.accent),
                    color = contentTint,
                    fontSize = textSizeSp.sp,
                    lineHeight = (textSizeSp * 1.28f).sp
                )
            }
            if (lineIdx < lines.size - 1 && line.isEmpty()) {
                Spacer(modifier = Modifier.height((textSizeSp * 0.45f).dp))
            }
        }
    }
}
