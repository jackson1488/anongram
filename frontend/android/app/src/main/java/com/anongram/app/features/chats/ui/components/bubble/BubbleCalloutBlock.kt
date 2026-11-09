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

    val containerBg = if (isQuote) Color(0xFF1E1F29) else Color(0xFF261D35)
    val borderColor = if (isQuote) scheme.accent.copy(alpha = 0.5f) else Color(0xFFC084FC).copy(alpha = 0.6f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerBg)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (isQuote) Icons.Outlined.FormatQuote else Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = if (isQuote) scheme.accent else Color(0xFFC084FC),
                    modifier = Modifier
                        .size(20.dp)
                        .padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = text,
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontStyle = if (isQuote) FontStyle.Italic else FontStyle.Normal,
                        lineHeight = 21.sp
                    )

                    if (!author.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (author.startsWith("—")) author else "— $author",
                            color = Color(0xFFA1A1AA),
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
