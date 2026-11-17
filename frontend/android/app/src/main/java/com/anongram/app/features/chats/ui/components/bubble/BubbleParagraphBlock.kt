package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.links.browser.InAppBrowserStore

/**
 * Модульный компонент отображения абзаца текста с поддержкой:
 * - Инлайн-спойлеров ||скрытый текст|| с частицами и радиальным раскрытием;
 * - **Жирного**, *курсива*, ~~зачёркнутого~~, <u>подчёркнутого</u>, [текст](url);
 * - Моноширинного кода `code` и кастомных шрифтов.
 */
@Composable
fun BubbleParagraphBlock(
    text: String,
    textSizeSp: Int = 15,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    val contentTint = LocalContentColor.current
    val motionEnabled by ChatTextSettingsStore.spoilerParticleMotionEnabled.collectAsState()
    val lines = text.split("\n")

    Column(modifier = modifier.fillMaxWidth()) {
        lines.forEachIndexed { lineIdx, line ->
            if (line.isNotEmpty()) {
                val annotated = remember(line, scheme.accent) {
                    buildRichAnnotatedText(line, scheme.accent) { url ->
                        InAppBrowserStore.openUrl(context, url)
                    }
                }
                val spoilerRanges = remember(annotated) {
                    annotated.getStringAnnotations(tag = "SPOILER", start = 0, end = annotated.length)
                        .map { it.start until it.end }
                }
                var revealedIndices by remember(line) { mutableStateOf(emptySet<Int>()) }
                var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

                val renderedText = remember(annotated, spoilerRanges, revealedIndices) {
                    if (spoilerRanges.isEmpty()) {
                        annotated
                    } else {
                        buildAnnotatedString {
                            append(annotated)
                            spoilerRanges.forEachIndexed { idx, range ->
                                if (idx !in revealedIndices && range.first <= range.last) {
                                    addStyle(SpanStyle(color = Color.Transparent), range.first, range.last + 1)
                                }
                            }
                        }
                    }
                }

                Box {
                    Text(
                        text = renderedText,
                        color = contentTint,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.28f).sp,
                        onTextLayout = { layoutResult = it }
                    )
                    if (spoilerRanges.isNotEmpty()) {
                        TelegramTextSpoilerCanvas(
                            textLayoutResult = layoutResult,
                            spoilerRanges = spoilerRanges,
                            revealedIndices = revealedIndices,
                            motionEnabled = motionEnabled,
                            onRevealIndex = { idx -> revealedIndices = revealedIndices + idx },
                            modifier = Modifier.matchParentSize()
                        )
                    }
                }
            }
            if (lineIdx < lines.size - 1 && line.isEmpty()) {
                Spacer(modifier = Modifier.height((textSizeSp * 0.45f).dp))
            }
        }
    }
}
