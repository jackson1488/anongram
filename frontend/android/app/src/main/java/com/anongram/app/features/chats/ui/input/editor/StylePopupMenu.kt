package com.anongram.app.features.chats.ui.input.editor

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

enum class TextStyleFormat(val label: String, val glyph: String) {
    BOLD("Жирный", "B"),
    ITALIC("Курсив", "I"),
    UNDERLINE("Подчёркнутый", "U"),
    STRIKE("Зачёркнутый", "S̶"),
    HEADING("Заголовок", "H"),
    QUOTE("Цитата", "“"),
    CALLOUT("Выноска", "≡"),
    FONT("Шрифт", "Aa"),
    SPOILER("Спойлер", "✦"),
    CODE("Код", "</>"),
    NORMAL("Обычный текст", "T")
}

val CodeLanguages = listOf(
    "ABNF", "Kotlin", "Java", "Python", "JavaScript", "TypeScript",
    "Rust", "C++", "Go", "JSON", "SQL", "Markdown", "Shell"
)

private enum class ActiveSubmenu {
    NONE, HEADING, QUOTE, CALLOUT, FONT
}

/**
 * Расширенное меню стилей форматирования Aa:
 * - Прямой выбор стилей: Жирный, Курсив, Подчёркнутый, Зачёркнутый, Спойлер, Код;
 * - Подменю уровней заголовков H1..H6;
 * - Подменю выбора цитат и выносок (Обычная / Сворачиваемая);
 * - Подменю выбора шрифтов (Sans-Serif, Serif, Mono, Cursive).
 */
@Composable
fun StylePopupMenu(
    selectedFormat: TextStyleFormat,
    onSelectFormat: (TextStyleFormat) -> Unit,
    onSelectHeading: (Int) -> Unit = {},
    selectedHeadingLevel: Int? = 1,
    onSelectFont: (String) -> Unit = {},
    onSelectQuoteVariant: (isCollapsible: Boolean) -> Unit = {},
    onSelectCalloutVariant: (isCollapsible: Boolean) -> Unit = {},
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    var activeSubmenu by remember { mutableStateOf(ActiveSubmenu.NONE) }

    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF232328))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .padding(vertical = 6.dp)
            .width(230.dp)
    ) {
        AnimatedContent(
            targetState = activeSubmenu,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "styleMenuTransition"
        ) { submenu ->
            when (submenu) {
                ActiveSubmenu.HEADING -> {
                    HeadingPickerSubmenu(
                        currentLevel = selectedHeadingLevel,
                        onSelectHeading = { level ->
                            onSelectHeading(level)
                            onSelectFormat(TextStyleFormat.HEADING)
                            onDismiss()
                        },
                        onBack = { activeSubmenu = ActiveSubmenu.NONE }
                    )
                }
                ActiveSubmenu.FONT -> {
                    FontPickerSubmenu(
                        onSelectFont = { fontKey ->
                            onSelectFont(fontKey)
                            onDismiss()
                        },
                        onBack = { activeSubmenu = ActiveSubmenu.NONE }
                    )
                }
                ActiveSubmenu.QUOTE -> {
                    QuoteVariantSubmenu(
                        title = "Цитата",
                        onSelectVariant = { isCollapsible ->
                            onSelectQuoteVariant(isCollapsible)
                            onSelectFormat(TextStyleFormat.QUOTE)
                            onDismiss()
                        },
                        onBack = { activeSubmenu = ActiveSubmenu.NONE }
                    )
                }
                ActiveSubmenu.CALLOUT -> {
                    QuoteVariantSubmenu(
                        title = "Выноска",
                        onSelectVariant = { isCollapsible ->
                            onSelectCalloutVariant(isCollapsible)
                            onSelectFormat(TextStyleFormat.CALLOUT)
                            onDismiss()
                        },
                        onBack = { activeSubmenu = ActiveSubmenu.NONE }
                    )
                }
                ActiveSubmenu.NONE -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        TextStyleFormat.values().forEach { format ->
                            val isSelected = format == selectedFormat
                            val hasSubmenu = format == TextStyleFormat.HEADING ||
                                format == TextStyleFormat.FONT ||
                                format == TextStyleFormat.QUOTE ||
                                format == TextStyleFormat.CALLOUT

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        when (format) {
                                            TextStyleFormat.HEADING -> activeSubmenu = ActiveSubmenu.HEADING
                                            TextStyleFormat.FONT -> activeSubmenu = ActiveSubmenu.FONT
                                            TextStyleFormat.QUOTE -> activeSubmenu = ActiveSubmenu.QUOTE
                                            TextStyleFormat.CALLOUT -> activeSubmenu = ActiveSubmenu.CALLOUT
                                            else -> {
                                                val newFormat = if (isSelected) TextStyleFormat.NORMAL else format
                                                onSelectFormat(newFormat)
                                                onDismiss()
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier.size(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = format.glyph,
                                            color = if (isSelected) scheme.accent else Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = when (format) {
                                                TextStyleFormat.BOLD -> FontWeight.Bold
                                                else -> FontWeight.SemiBold
                                            },
                                            fontStyle = if (format == TextStyleFormat.ITALIC) FontStyle.Italic else FontStyle.Normal,
                                            textDecoration = when (format) {
                                                TextStyleFormat.UNDERLINE -> TextDecoration.Underline
                                                TextStyleFormat.STRIKE -> TextDecoration.LineThrough
                                                else -> TextDecoration.None
                                            },
                                            fontFamily = if (format == TextStyleFormat.CODE) FontFamily.Monospace else FontFamily.Default
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Text(
                                        text = format.label,
                                        color = if (isSelected) scheme.accent else Color.White,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }

                                if (hasSubmenu) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = "Подменю",
                                        tint = Color(0xFFA1A1AA),
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = "Выбрано",
                                        tint = scheme.accent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FontPickerSubmenu(
    onSelectFont: (String) -> Unit,
    onBack: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val fonts = listOf(
        "Стандартный (Sans)" to "sans",
        "С засечками (Serif)" to "serif",
        "Моноширинный (Mono)" to "mono",
        "Рукописный (Cursive)" to "cursive"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        SubmenuHeader(title = "Назад к стилям", onBack = onBack)

        fonts.forEach { (label, key) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectFont(key) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    color = Color.White,
                    fontSize = 13.5.sp,
                    fontFamily = when (key) {
                        "serif" -> FontFamily.Serif
                        "mono" -> FontFamily.Monospace
                        "cursive" -> FontFamily.Cursive
                        else -> FontFamily.SansSerif
                    }
                )
            }
        }
    }
}

@Composable
private fun QuoteVariantSubmenu(
    title: String,
    onSelectVariant: (isCollapsible: Boolean) -> Unit,
    onBack: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current

    Column(modifier = Modifier.fillMaxWidth()) {
        SubmenuHeader(title = "Назад к стилям", onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectVariant(false) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Обычная $title",
                color = Color.White,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectVariant(true) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Сворачиваемая $title (^)",
                color = scheme.accent,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SubmenuHeader(title: String, onBack: () -> Unit) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = "Назад",
            tint = scheme.accent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            color = scheme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    HorizontalDivider(
        color = Color(0x33FFFFFF),
        thickness = 0.5.dp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
