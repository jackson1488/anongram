package com.anongram.app.features.chats.ui.input.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings

/**
 * Подменю уровней заголовков H1..H6 («заголовок 1 до 6 как в ворде»).
 */
@Composable
fun HeadingPickerSubmenu(
    currentLevel: Int?,
    onSelectHeading: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current

    val headingLevels = listOf(
        1 to strings.headingLevel(1, 24),
        2 to strings.headingLevel(2, 21),
        3 to strings.headingLevel(3, 18),
        4 to strings.headingLevel(4, 16),
        5 to strings.headingLevel(5, 14),
        6 to strings.headingLevel(6, 12)
    )

    Column(modifier = modifier.fillMaxWidth()) {
        // Кнопка возврата «← Назад»
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
                text = strings.backToStyles,
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

        headingLevels.forEach { (level, label) ->
            val isSelected = currentLevel == level
            val fontSizeSp = when (level) {
                1 -> 18.sp
                2 -> 16.sp
                3 -> 15.sp
                4 -> 14.sp
                5 -> 13.sp
                else -> 12.sp
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectHeading(level) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "H$level",
                        color = if (isSelected) scheme.accent else Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        color = if (isSelected) scheme.accent else Color.White,
                        fontSize = fontSizeSp,
                        fontWeight = if (level <= 3) FontWeight.Bold else FontWeight.SemiBold
                    )
                }

                if (isSelected) {
                    Text(
                        text = "✓",
                        color = scheme.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
