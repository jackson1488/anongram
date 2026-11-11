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
import com.anongram.app.features.chats.ui.input.i18n.InputI18nStrings
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings

enum class TextStyleFormat(val glyph: String) {
    BOLD("B"),
    ITALIC("I"),
    UNDERLINE("U"),
    STRIKE("S̶"),
    HEADING("H"),
    QUOTE("“"),
    CALLOUT("≡"),
    FONT("Aa"),
    SPOILER("✦"),
    CODE("</>"),
    NORMAL("T")
}

fun TextStyleFormat.getLabel(strings: InputI18nStrings): String = when (this) {
    TextStyleFormat.BOLD -> strings.styleBold
    TextStyleFormat.ITALIC -> strings.styleItalic
    TextStyleFormat.UNDERLINE -> strings.styleUnderline
    TextStyleFormat.STRIKE -> strings.styleStrikethrough
    TextStyleFormat.HEADING -> strings.styleHeading
    TextStyleFormat.QUOTE -> strings.styleQuote
    TextStyleFormat.CALLOUT -> strings.styleCallout
    TextStyleFormat.FONT -> strings.styleFont
    TextStyleFormat.SPOILER -> strings.styleSpoiler
    TextStyleFormat.CODE -> strings.styleCode
    TextStyleFormat.NORMAL -> strings.styleRegular
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
    val strings = LocalInputStrings.current
    var activeSubmenu by remember { mutableStateOf(ActiveSubmenu.NONE) }

    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.islandBg)
            .border(1.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
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
                        title = strings.styleQuote,
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
                        title = strings.styleCallout,
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
                                            color = if (isSelected) scheme.accent else scheme.textPrimary,
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
                                        text = format.getLabel(strings),
                                        color = if (isSelected) scheme.accent else scheme.textPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }

                                if (hasSubmenu) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChevronRight,
                                        contentDescription = strings.submenu,
                                        tint = scheme.textMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = strings.selected,
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
    val strings = LocalInputStrings.current
    val fonts = listOf(
        strings.fontSans to "sans",
        strings.fontSerif to "serif",
        strings.fontMono to "mono",
        strings.fontCursive to "cursive"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        SubmenuHeader(title = strings.backToStyles, onBack = onBack)

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
                    color = scheme.textPrimary,
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
    val strings = LocalInputStrings.current

    Column(modifier = Modifier.fillMaxWidth()) {
        SubmenuHeader(title = strings.backToStyles, onBack = onBack)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectVariant(false) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.standardQuote(title),
                color = scheme.textPrimary,
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
                text = strings.collapsibleQuote(title),
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
    val strings = LocalInputStrings.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onBack)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
            contentDescription = strings.back,
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
        color = scheme.divider,
        thickness = 0.5.dp,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}
