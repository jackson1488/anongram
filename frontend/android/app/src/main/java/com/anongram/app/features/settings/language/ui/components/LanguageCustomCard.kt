package com.anongram.app.features.settings.language.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.features.settings.language.i18n.LanguageI18nStrings
import com.anongram.app.features.settings.language.model.LanguageCatalog

/**
 * Вторая карточка настроек: секция «Язык» со специальным пунктом SpermoGram (как на скриншоте).
 */
@Composable
fun LanguageCustomCard(
    strings: LanguageI18nStrings,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentPurple = Color(0xFFB388FF)
    val cardBackground = Color(0xFF141318)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardBackground)
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = strings.languageSectionTitle,
            color = accentPurple,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TelegramRadioButton(
                selected = isSelected,
                activeColor = accentPurple
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = LanguageCatalog.CUSTOM_EASTER_EGG_NAME,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = LanguageCatalog.CUSTOM_EASTER_EGG_NAME,
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}
