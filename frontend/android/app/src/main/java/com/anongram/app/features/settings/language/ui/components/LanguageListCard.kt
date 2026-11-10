package com.anongram.app.features.settings.language.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.features.settings.language.i18n.LanguageI18nStrings
import com.anongram.app.features.settings.language.model.LanguageItem

/**
 * Карточка со списком языков Telegram:
 * - Telegram-радиокнопки СЛЕВА у каждого пункта
 * - Нативное название (16sp, белое) и английское название (13sp, серое)
 * - Тонкие разделители между строками
 */
@Composable
fun LanguageListCard(
    languages: List<LanguageItem>,
    selectedLanguageCode: String,
    isCustomLanguageSelected: Boolean,
    strings: LanguageI18nStrings,
    onSelectLanguage: (LanguageItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val accentPurple = Color(0xFFB388FF)
    val cardBackground = Color(0xFF141318)
    val dividerColor = Color(0x1AFFFFFF)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(vertical = 6.dp)
    ) {
        if (languages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = strings.searchEmptyNotice,
                    color = TextMuted,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            languages.forEachIndexed { index, lang ->
                val isSelected = !isCustomLanguageSelected && (lang.code.equals(selectedLanguageCode, ignoreCase = true))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectLanguage(lang) }
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TelegramRadioButton(
                        selected = isSelected,
                        activeColor = accentPurple
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = lang.nativeName,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = lang.englishName,
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }

                if (index < languages.lastIndex) {
                    HorizontalDivider(
                        color = dividerColor,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 54.dp, end = 16.dp)
                    )
                }
            }
        }
    }
}
