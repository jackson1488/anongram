package com.anongram.app.features.chats.ui.input.editor

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
import androidx.compose.material.icons.automirrored.outlined.FormatIndentDecrease
import androidx.compose.material.icons.automirrored.outlined.FormatIndentIncrease
import androidx.compose.material.icons.automirrored.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.FormatClear
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme

enum class ListStyle(val label: String, val icon: ImageVector) {
    NONE("Без списка", Icons.Outlined.FormatClear),
    BULLET("Список с маркерами", Icons.AutoMirrored.Outlined.FormatListBulleted),
    NUMBERED("Список с номерами", Icons.Outlined.FormatListNumbered),
    CHECKLIST("Чек-лист", Icons.Outlined.CheckBoxOutlineBlank),
    COLLAPSIBLE("Сворачиваемый блок", Icons.Outlined.UnfoldMore)
}

/**
 * Меню управления списками ≡ (макет media_1794253331200.png).
 */
@Composable
fun ListPopupMenu(
    selectedStyle: ListStyle,
    onSelectStyle: (ListStyle) -> Unit,
    onIndent: () -> Unit,
    onUnindent: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Box(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(16.dp), ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF232328))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            .padding(vertical = 6.dp)
            .width(220.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ListStyle.values().forEach { style ->
                val isSelected = (style == selectedStyle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onSelectStyle(style)
                            onDismiss()
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = style.icon,
                            contentDescription = style.label,
                            tint = if (isSelected) scheme.accent else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = style.label,
                            color = if (isSelected) scheme.accent else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = "Выбрано",
                            tint = scheme.accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = Color(0x33FFFFFF),
                thickness = 0.5.dp
            )

            // Отступ вправо
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onIndent()
                        onDismiss()
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.FormatIndentIncrease,
                    contentDescription = "Отступ вправо",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Отступ вправо",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            // Отменить отступ
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onUnindent()
                        onDismiss()
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.FormatIndentDecrease,
                    contentDescription = "Отменить отступ",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Отменить отступ",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Хелперы форматирования списков и отступов в тексте.
 */
object ListFormattingHelper {

    fun applyStyle(text: String, style: ListStyle): String {
        if (text.isEmpty()) {
            return when (style) {
                ListStyle.NONE -> ""
                ListStyle.BULLET -> "• "
                ListStyle.NUMBERED -> "1. "
                ListStyle.CHECKLIST -> "☐ "
                ListStyle.COLLAPSIBLE -> "||Сворачиваемый текст||\n"
            }
        }

        val lines = text.split("\n")
        return when (style) {
            ListStyle.NONE -> {
                lines.joinToString("\n") { line ->
                    line.replaceFirst(Regex("^(\\s*)([•\\-]|\\d+\\.|☐|☑)\\s*"), "$1")
                }
            }
            ListStyle.BULLET -> {
                lines.joinToString("\n") { line ->
                    val clean = line.replaceFirst(Regex("^(\\s*)([•\\-]|\\d+\\.|☐|☑)\\s*"), "$1")
                    if (clean.isBlank()) "• " else "• $clean"
                }
            }
            ListStyle.NUMBERED -> {
                var counter = 1
                lines.joinToString("\n") { line ->
                    val clean = line.replaceFirst(Regex("^(\\s*)([•\\-]|\\d+\\.|☐|☑)\\s*"), "$1")
                    if (clean.isBlank()) "${counter++}. " else "${counter++}. $clean"
                }
            }
            ListStyle.CHECKLIST -> {
                lines.joinToString("\n") { line ->
                    val clean = line.replaceFirst(Regex("^(\\s*)([•\\-]|\\d+\\.|☐|☑)\\s*"), "$1")
                    if (clean.isBlank()) "☐ " else "☐ $clean"
                }
            }
            ListStyle.COLLAPSIBLE -> {
                if (text.startsWith("||") && text.endsWith("||")) text
                else "||$text||"
            }
        }
    }

    fun indent(text: String): String {
        if (text.isEmpty()) return "    "
        return text.split("\n").joinToString("\n") { "    $it" }
    }

    fun unindent(text: String): String {
        return text.split("\n").joinToString("\n") { line ->
            when {
                line.startsWith("    ") -> line.substring(4)
                line.startsWith("\t") -> line.substring(1)
                line.startsWith("  ") -> line.substring(2)
                line.startsWith(" ") -> line.substring(1)
                else -> line
            }
        }
    }
}
