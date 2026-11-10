package com.anongram.app.features.chats.ui.components.bubble

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Сворачиваемый блок по макету media_1794256738911.png (^ Заголовок / Тело).
 * По клику плавно сворачивает / разворачивает содержимое.
 */
@Composable
fun BubbleCollapsibleBlock(
    title: String,
    content: String,
    initiallyExpanded: Boolean = true,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(scheme.accent.copy(alpha = 0.12f))
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
                .padding(start = 6.dp, top = 4.dp, bottom = 4.dp, end = 4.dp)
        ) {
                // Заголовок со стрелкой-шевроном (^ / v)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                        tint = scheme.accent,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = title.ifBlank { "Сворачиваемый блок" },
                        color = Color.White,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Скрываемое тело блока
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        HorizontalDivider(
                            color = scheme.accent.copy(alpha = 0.2f),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        Text(
                            text = content,
                            color = Color(0xFFE2E8F0),
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
