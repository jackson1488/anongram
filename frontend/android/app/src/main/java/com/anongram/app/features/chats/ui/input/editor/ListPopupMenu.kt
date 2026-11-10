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
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.ui.input.i18n.InputI18nStrings
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings

enum class ListStyle(val label: String, val icon: ImageVector) {
    NONE("Без списка", Icons.Outlined.FormatClear),
    BULLET("Список с маркерами", Icons.AutoMirrored.Outlined.FormatListBulleted),
    NUMBERED("Список с номерами", Icons.Outlined.FormatListNumbered),
    CHECKLIST("Чек-лист", Icons.Outlined.CheckBoxOutlineBlank),
    COLLAPSIBLE("Сворачиваемый блок", Icons.Outlined.UnfoldMore)
}

fun ListStyle.getLabel(strings: InputI18nStrings): String = when (this) {
    ListStyle.NONE -> strings.listNone
    ListStyle.BULLET -> strings.listBullet
    ListStyle.NUMBERED -> strings.listNumbered
    ListStyle.CHECKLIST -> strings.listChecklist
    ListStyle.COLLAPSIBLE -> strings.listCollapsible
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
    val strings = LocalInputStrings.current

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
                            val newStyle = if (isSelected) ListStyle.NONE else style
                            onSelectStyle(newStyle)
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
                            contentDescription = style.getLabel(strings),
                            tint = if (isSelected) scheme.accent else Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = style.getLabel(strings),
                            color = if (isSelected) scheme.accent else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = strings.selected,
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
                    contentDescription = strings.indentRight,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = strings.indentRight,
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
                    contentDescription = strings.outdent,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = strings.outdent,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Хелперы форматирования списков, отступов и умного Enter в тексте.
 */
object ListFormattingHelper {

    fun applyStyle(
        text: String,
        style: ListStyle,
        templateTitle: String = "Заголовок",
        templateBody: String = "Введите текст..."
    ): String {
        if (text.isEmpty()) {
            return when (style) {
                ListStyle.NONE -> ""
                ListStyle.BULLET -> "• "
                ListStyle.NUMBERED -> "1. "
                ListStyle.CHECKLIST -> "☐ "
                ListStyle.COLLAPSIBLE -> "^ $templateTitle\n$templateBody"
            }
        }

        if (style == ListStyle.COLLAPSIBLE) {
            return if (text.isBlank()) "^ $templateTitle\n$templateBody"
            else if (text.startsWith("^ ")) text
            else "^ $templateTitle\n$text"
        }

        val cleanText = if (text.startsWith("^ ")) text.removePrefix("^ ") else text
        val lines = cleanText.split("\n")
        return when (style) {
            ListStyle.NONE -> {
                lines.joinToString("\n") { line ->
                    line.replaceFirst(Regex("^(\\s*)([•\\-]|\\d+\\.|[☐☑]|\\[[ xX]\\]|-\\s*\\[[ xX]\\])\\s*"), "$1")
                }
            }
            ListStyle.BULLET -> {
                lines.joinToString("\n") { line ->
                    val clean = line.replaceFirst(Regex("^(\\s*)([•\\-]|\\d+\\.|[☐☑]|\\[[ xX]\\]|-\\s*\\[[ xX]\\])\\s*"), "$1")
                    if (clean.isBlank()) "• " else "• $clean"
                }
            }
            ListStyle.NUMBERED -> {
                var counter = 1
                lines.joinToString("\n") { line ->
                    val clean = line.replaceFirst(Regex("^(\\s*)([•\\-]|\\d+\\.|[☐☑]|\\[[ xX]\\]|-\\s*\\[[ xX]\\])\\s*"), "$1")
                    if (clean.isBlank()) "${counter++}. " else "${counter++}. $clean"
                }
            }
            ListStyle.CHECKLIST -> {
                lines.joinToString("\n") { line ->
                    val clean = line.replaceFirst(Regex("^(\\s*)([•\\-]|\\d+\\.|[☐☑]|\\[[ xX]\\]|-\\s*\\[[ xX]\\])\\s*"), "$1")
                    if (clean.isBlank()) "☐ " else "☐ $clean"
                }
            }
            ListStyle.COLLAPSIBLE -> text
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

/**
 * Обработчик умного Enter: автоматически продолжает маркеры, нумерацию и чек-боксы,
 * а на пустой строке стирает маркер и выходит из списка.
 */
object SmartListEnterHelper {
    fun processEnter(oldVal: TextFieldValue, newVal: TextFieldValue): TextFieldValue {
        val oldText = oldVal.text
        val newText = newVal.text
        if (newText.length != oldText.length + 1) return newVal

        val cursor = oldVal.selection.start
        if (cursor < 0 || cursor > oldText.length) return newVal
        if (newText.getOrNull(cursor) != '\n') return newVal

        // Определяем строку перед нажатием Enter
        val lineStart = oldText.lastIndexOf('\n', cursor - 1).let { if (it == -1) 0 else it + 1 }
        val lineBeforeCursor = oldText.substring(lineStart, cursor)

        // 1. Пустой маркер: пользователь нажал Enter на пустом пункте -> стереть маркер и выйти из списка
        val emptyBullet = lineBeforeCursor.matches(Regex("^(\\s*)•\\s*$"))
        val emptyNumber = lineBeforeCursor.matches(Regex("^(\\s*)\\d+\\.\\s*$"))
        val emptyCheck = lineBeforeCursor.matches(Regex("^(\\s*)([☐☑]|\\[[ xX]\\]|-\\s*\\[[ xX]\\])\\s*$"))

        if (emptyBullet || emptyNumber || emptyCheck) {
            val updatedText = oldText.substring(0, lineStart) + oldText.substring(cursor)
            return TextFieldValue(text = updatedText, selection = TextRange(lineStart))
        }

        // 2. Список с маркерами (• )
        val bulletMatch = Regex("^(\\s*)•\\s+").find(lineBeforeCursor)
        if (bulletMatch != null) {
            val indent = bulletMatch.groupValues[1]
            val continuation = "\n${indent}• "
            val updatedText = oldText.substring(0, cursor) + continuation + oldText.substring(cursor)
            val newCursor = cursor + continuation.length
            return TextFieldValue(text = updatedText, selection = TextRange(newCursor))
        }

        // 3. Список с номерами (1., 2., 3.)
        val numberMatch = Regex("^(\\s*)(\\d+)\\.\\s+").find(lineBeforeCursor)
        if (numberMatch != null) {
            val indent = numberMatch.groupValues[1]
            val num = numberMatch.groupValues[2].toIntOrNull() ?: 1
            val continuation = "\n${indent}${num + 1}. "
            val updatedText = oldText.substring(0, cursor) + continuation + oldText.substring(cursor)
            val newCursor = cursor + continuation.length
            return TextFieldValue(text = updatedText, selection = TextRange(newCursor))
        }

        // 4. Чек-лист (☐ / ☑ / [ ] / [x] / - [ ])
        val checkMatch = Regex("^(\\s*)([☐☑]|\\[[ xX]\\]|-\\s*\\[[ xX]\\])\\s+").find(lineBeforeCursor)
        if (checkMatch != null) {
            val indent = checkMatch.groupValues[1]
            val continuation = "\n${indent}☐ "
            val updatedText = oldText.substring(0, cursor) + continuation + oldText.substring(cursor)
            val newCursor = cursor + continuation.length
            return TextFieldValue(text = updatedText, selection = TextRange(newCursor))
        }

        return newVal
    }
}
