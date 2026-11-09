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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

enum class TextStyleFormat(val label: String, val glyph: String, val hasStar: Boolean = false) {
    HEADING("Заголовок", "H", hasStar = true),
    NORMAL("Текст", "T"),
    QUOTE("Цитата", "“"),
    CALLOUT("Выноска", "≡", hasStar = true),
    SPOILER("Спойлер", "✦", hasStar = true),
    CODE("Код", "</>"),
    SMALL("Мелкий шрифт", "≡", hasStar = true)
}

val CodeLanguages = listOf(
    "ABNF", "Kotlin", "Java", "Python", "JavaScript", "TypeScript",
    "Rust", "C++", "Go", "JSON", "SQL", "Markdown", "Shell"
)

/**
 * Выпадающее меню стилей форматирования Aa (макет media_1794245806073.png)
 * с поддержкой подменю уровней заголовков H1..H6.
 */
@Composable
fun StylePopupMenu(
    selectedFormat: TextStyleFormat,
    onSelectFormat: (TextStyleFormat) -> Unit,
    onSelectHeading: (Int) -> Unit = {},
    selectedHeadingLevel: Int? = 1,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    var isHeadingSubmenuOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF232328))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .padding(vertical = 6.dp)
            .width(210.dp)
    ) {
        AnimatedContent(
            targetState = isHeadingSubmenuOpen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "styleMenuTransition"
        ) { inHeadingSubmenu ->
            if (inHeadingSubmenu) {
                HeadingPickerSubmenu(
                    currentLevel = selectedHeadingLevel,
                    onSelectHeading = { level ->
                        onSelectHeading(level)
                        onSelectFormat(TextStyleFormat.HEADING)
                        onDismiss()
                    },
                    onBack = { isHeadingSubmenuOpen = false }
                )
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TextStyleFormat.values().forEach { format ->
                        val isSelected = format == selectedFormat
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (format == TextStyleFormat.HEADING) {
                                        isHeadingSubmenuOpen = true
                                    } else {
                                        onSelectFormat(format)
                                        onDismiss()
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
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
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = if (format == TextStyleFormat.CODE) FontFamily.Monospace else FontFamily.Default
                                    )
                                    if (format.hasStar) {
                                        Text(
                                            text = "★",
                                            color = Color(0xFFC084FC),
                                            fontSize = 7.sp,
                                            modifier = Modifier.align(Alignment.BottomEnd)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Text(
                                    text = format.label,
                                    color = if (isSelected) scheme.accent else Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            if (format == TextStyleFormat.HEADING) {
                                Icon(
                                    imageVector = Icons.Outlined.ChevronRight,
                                    contentDescription = "Уровни заголовков",
                                    tint = Color(0xFFA1A1AA),
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (isSelected) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Выбрано",
                                    tint = Color.White,
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
