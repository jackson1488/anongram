package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings

/**
 * Аутентичная цитата и выноска в стиле Telegram (макеты media_1794333692036.png и media_1794333701837.png):
 * - Вертикальная акцентная линия слева;
 * - Глиф кавычки ” в правом верхнем углу;
 * - Высококонтрастный текст с инлайн-стилями и спойлерами;
 * - Кнопка сворачивания/разворачивания в правом нижнем углу;
 * - Подпись автора («— Автор») в левом нижнем углу (как в цитате, так и в выноске).
 */
@Composable
fun BubbleQuoteBlock(
    text: String,
    title: String? = null,
    author: String? = null,
    isCollapsible: Boolean = false,
    isCallout: Boolean = false,
    initiallyExpanded: Boolean = false,
    textSizeSp: Int = 15,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    val blockAccent = if (isCallout) Color(0xFFC084FC) else scheme.accent
    val canCollapse = isCollapsible || isCallout || text.lines().size > 2 || text.length > 120
    var isExpanded by remember { mutableStateOf(if (canCollapse) initiallyExpanded else true) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(blockAccent.copy(alpha = if (isCallout) 0.16f else 0.12f))
            .animateContentSize()
    ) {
        // Вертикальная акцентная линия слева
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .matchParentSize()
                .align(Alignment.CenterStart)
                .background(blockAccent, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
        )

        // Верхний правый глиф кавычки ” по макету Telegram
        Icon(
            imageVector = Icons.Outlined.FormatQuote,
            contentDescription = null,
            tint = blockAccent.copy(alpha = 0.6f),
            modifier = Modifier
                .size(20.dp)
                .align(Alignment.TopEnd)
                .padding(top = 4.dp, end = 6.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
        ) {
            // Заголовок (если задан)
            if (!title.isNullOrBlank()) {
                Text(
                    text = title,
                    color = blockAccent,
                    fontSize = (textSizeSp + 1).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Текст цитаты / выноски
            if (canCollapse && !isExpanded) {
                if (text.contains("||")) {
                    Column {
                        text.lines().take(3).forEach { ql ->
                            if (ql.contains("||")) {
                                InlineSpoilerLine(ql, textSizeSp, blockAccent)
                            } else {
                                Text(
                                    text = buildRichAnnotatedText(ql, blockAccent),
                                    color = Color.White,
                                    fontSize = textSizeSp.sp,
                                    lineHeight = (textSizeSp * 1.28f).sp
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = buildRichAnnotatedText(text, blockAccent),
                        color = Color.White,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.28f).sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                if (text.contains("||")) {
                    Column {
                        text.lines().forEach { ql ->
                            if (ql.contains("||")) {
                                InlineSpoilerLine(ql, textSizeSp, blockAccent)
                            } else {
                                Text(
                                    text = buildRichAnnotatedText(ql, blockAccent),
                                    color = Color.White,
                                    fontSize = textSizeSp.sp,
                                    lineHeight = (textSizeSp * 1.28f).sp
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = buildRichAnnotatedText(text, blockAccent),
                        color = Color.White,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp * 1.28f).sp
                    )
                }
            }

            // Нижняя строка: Автор в левом углу и кнопка Развернуть/Свернуть в правом углу
            if (!author.isNullOrBlank() || canCollapse) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!author.isNullOrBlank()) {
                        Text(
                            text = "— $author",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            fontStyle = FontStyle.Italic,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    if (canCollapse) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = blockAccent.copy(alpha = 0.22f),
                            border = BorderStroke(1.dp, blockAccent.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { isExpanded = !isExpanded }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isExpanded) strings.collapse else strings.expand,
                                    color = blockAccent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) strings.collapse else strings.expand,
                                    tint = blockAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
