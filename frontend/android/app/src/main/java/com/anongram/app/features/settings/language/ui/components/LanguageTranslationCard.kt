package com.anongram.app.features.settings.language.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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

/**
 * Карточка «Перевод сообщений» в стиле Telegram:
 * - Заголовок фиолетовым цветом: «Перевод сообщений»
 * - Тумблер «Показывать кнопку «Перевести»»
 * - Анимированная строка «Не переводить» (появляется только если тумблер включён)
 * - Тумблер «Переводить чаты целиком» с иконкой замка
 * - Сноска под карточкой
 */
@Composable
fun LanguageTranslationCard(
    strings: LanguageI18nStrings,
    showTranslateButton: Boolean,
    translateEntireChats: Boolean,
    doNotTranslateLanguage: String,
    onShowTranslateButtonChange: (Boolean) -> Unit,
    onTranslateEntireChatsChange: (Boolean) -> Unit,
    onDoNotTranslateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accentPurple = Color(0xFFB388FF)
    val cardBackground = Color(0xFF141318)
    val dividerColor = Color(0x1AFFFFFF)

    val switchColors = SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = accentPurple,
        uncheckedThumbColor = Color(0xFF888890),
        uncheckedTrackColor = Color(0xFF333338),
        checkedBorderColor = Color.Transparent,
        uncheckedBorderColor = Color.Transparent
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(cardBackground)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 12.dp)
        ) {
            // Фиолетовый заголовок секции
            Text(
                text = strings.translationSectionTitle,
                color = accentPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Строка 1: «Показывать кнопку «Перевести»»
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowTranslateButtonChange(!showTranslateButton) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.showTranslateButtonTitle,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = showTranslateButton,
                    onCheckedChange = onShowTranslateButtonChange,
                    colors = switchColors
                )
            }

            // Строка 2: «Не переводить» (плавно выезжает, когда включён тумблер 1)
            AnimatedVisibility(
                visible = showTranslateButton,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(
                        color = dividerColor,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onDoNotTranslateClick)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.doNotTranslateTitle,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = doNotTranslateLanguage,
                            color = accentPurple,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            HorizontalDivider(
                color = dividerColor,
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Строка 3: «Переводить чаты целиком»
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTranslateEntireChatsChange(!translateEntireChats) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.translateEntireChatsTitle,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )

                Switch(
                    checked = translateEntireChats,
                    onCheckedChange = onTranslateEntireChatsChange,
                    thumbContent = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (translateEntireChats) accentPurple else Color(0xFF44444A),
                            modifier = Modifier.size(12.dp)
                        )
                    },
                    colors = switchColors
                )
            }
        }

        // Подпись под карточкой (снаружи)
        Text(
            text = strings.translationFootnote,
            color = TextMuted,
            fontSize = 13.sp,
            lineHeight = 17.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
