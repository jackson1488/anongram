package com.anongram.app.features.chats.ui.components.bubble

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
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Блок выноски (Callout box) в стиле Word / Notion с поддержкой цитирования и автора.
 */
@Composable
fun BubbleCalloutBlock(
    text: String,
    author: String? = null,
    isQuote: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    val containerBg = scheme.accent.copy(alpha = 0.12f)
    val accentColor = scheme.accent
    val contentTint = LocalContentColor.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(containerBg)
    ) {
        // Вертикальная акцентная линия цитаты / выноски
        Box(
            modifier = Modifier
                .width(3.5.dp)
                .matchParentSize()
                .align(Alignment.CenterStart)
                .background(accentColor, RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 6.dp, end = 10.dp, top = 8.dp, bottom = 8.dp)
        ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isQuote) Icons.Outlined.FormatQuote else Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = text,
                            color = contentTint,
                            fontSize = 14.5.sp,
                            fontStyle = if (isQuote) FontStyle.Italic else FontStyle.Normal,
                            lineHeight = 21.sp
                        )

                        if (!author.isNullOrBlank() && author.trim() != "—" && author.trim() != "-") {
                            Spacer(modifier = Modifier.height(4.dp))
                            val cleanAuthor = author.trim().removePrefix("—").removePrefix("-").trim()
                            Text(
                                text = "— $cleanAuthor",
                                color = contentTint.copy(alpha = 0.65f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                fontStyle = FontStyle.Italic,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }
    }
