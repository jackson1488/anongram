package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Аутентичный сворачиваемый блок по макету media_1794256738911.png (^ Заголовок / Тело / Автор):
 * - Акцентная вертикальная линия слева;
 * - Верхний заголовок со стрелкой-шевроном (^ / v);
 * - Скрываемое тело с поддержкой Markdown и спойлеров;
 * - Кнопка сворачивания/разворачивания в правом нижнем углу;
 * - Опциональная подпись автора («— Автор») в левом нижнем углу.
 */
@Composable
fun BubbleCollapsibleBlock(
    title: String,
    content: String,
    author: String? = null,
    initiallyExpanded: Boolean = false,
    textSizeSp: Int = 15,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.accent.copy(alpha = 0.12f))
            .animateContentSize()
    ) {
        // Вертикальная акцентная линия слева
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .matchParentSize()
                .align(Alignment.CenterStart)
                .background(scheme.accent, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 10.dp)
        ) {
            // Верхняя строка-заголовок с шевроном (^ / v) и названием
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                    tint = scheme.accent,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title.ifBlank { "Сворачиваемый блок" },
                    color = Color.White,
                    fontSize = (textSizeSp + 1).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }

            // Раскрывающееся содержимое блока
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    HorizontalDivider(
                        color = scheme.accent.copy(alpha = 0.25f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val bodyLines = content.lines()
                    bodyLines.forEach { bLine ->
                        if (bLine.contains("||")) {
                            InlineSpoilerLine(
                                line = bLine,
                                textSizeSp = textSizeSp,
                                accent = scheme.accent
                            )
                        } else if (bLine.isNotEmpty()) {
                            Text(
                                text = buildRichAnnotatedText(bLine, scheme.accent),
                                color = Color(0xFFE2E8F0),
                                fontSize = textSizeSp.sp,
                                lineHeight = (textSizeSp * 1.28f).sp
                            )
                        }
                    }
                }
            }

            // Нижняя строка: Автор слева и кнопка Развернуть/Свернуть в правом углу
            Spacer(modifier = Modifier.height(6.dp))
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

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = scheme.accent.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, scheme.accent.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { isExpanded = !isExpanded }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isExpanded) "Свернуть" else "Развернуть",
                            color = scheme.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                            tint = scheme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
