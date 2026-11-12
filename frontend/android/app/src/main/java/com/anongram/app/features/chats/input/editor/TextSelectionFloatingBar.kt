package com.anongram.app.features.chats.input.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatStrikethrough
import androidx.compose.material.icons.outlined.FormatUnderlined
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.input.i18n.LocalInputStrings

/**
 * Плавающая панель форматирования выделенного текста по макету media_1794257026295.png.
 * Появляется над клавиатурой, когда пользователь выделил диапазон текста.
 */
@Composable
fun TextSelectionFloatingBar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrikethrough: () -> Unit,
    onSpoiler: () -> Unit,
    onFontSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    var isFontMenuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.islandBg)
            .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // B — Полужирный
            SelectionActionItem(
                onClick = onBold,
                content = {
                    Text(
                        text = "B",
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )

            // I — Курсив
            SelectionActionItem(
                onClick = onItalic,
                content = {
                    Text(
                        text = "I",
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold
                    )
                }
            )

            // U — Подчёркнутый
            SelectionActionItem(
                onClick = onUnderline,
                content = {
                    Text(
                        text = "U",
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        textDecoration = TextDecoration.Underline,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )

            // S̶ — Зачёркнутый
            SelectionActionItem(
                onClick = onStrikethrough,
                content = {
                    Text(
                        text = "S̶",
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            )

            // ✦ — Спойлер (сверкающие искры)
            SelectionActionItem(
                onClick = onSpoiler,
                content = {
                    Text(
                        text = "✦",
                        color = scheme.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            )

            // Aa — Выбор шрифта фрагмента
            Box {
                SelectionActionItem(
                    onClick = { isFontMenuOpen = !isFontMenuOpen },
                    content = {
                        Text(
                            text = "Aa",
                            color = scheme.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                DropdownMenu(
                    expanded = isFontMenuOpen,
                    onDismissRequest = { isFontMenuOpen = false },
                    containerColor = scheme.card
                ) {
                    val fonts = listOf(
                        "Sans-Serif" to "sans",
                        "Serif" to "serif",
                        strings.fontMonoShort to "mono",
                        strings.fontCursiveShort to "cursive"
                    )
                    fonts.forEach { (label, key) ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = label,
                                    fontFamily = when (key) {
                                        "serif" -> FontFamily.Serif
                                        "mono" -> FontFamily.Monospace
                                        "cursive" -> FontFamily.Cursive
                                        else -> FontFamily.SansSerif
                                    },
                                    color = scheme.textPrimary
                                )
                            },
                            onClick = {
                                onFontSelected(key)
                                isFontMenuOpen = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectionActionItem(
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
