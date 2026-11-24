package com.anongram.app.features.settings.chat_settings.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.BubbleColorStyle
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ThemePolarity
import com.anongram.app.core.theme.ThemeSpec
import com.anongram.app.core.theme.ThemeTintMode
import com.anongram.app.features.chats.input.i18n.LocalInputStrings
import com.anongram.app.features.settings.qr.ui.components.QrThemePalette

/**
 * Выдвижная ровная 6-колоночная палитра из 42 цветов (как в разделе QR),
 * применяющая цвета в режиме [ThemeTintMode.FULL_OS] без Ч/Б базы.
 */
@Composable
fun FullOsColorPaletteSheet(
    currentSpec: ThemeSpec,
    onSelectSpec: (ThemeSpec) -> Unit,
    onDone: () -> Unit,
    onReset: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current
    val scrollState = rememberScrollState()
    val swatchColors = remember { QrThemePalette.items.take(42) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 380.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(scheme.islandBg.copy(alpha = 0.96f))
            .border(1.dp, scheme.islandBorder, RoundedCornerShape(32.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Переключатель полярности (Тёмная / Светлая Full OS) + Сброс + Готово
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        Triple(ThemePolarity.DARK, Icons.Outlined.DarkMode, strings.themePolarityDark),
                        Triple(ThemePolarity.LIGHT, Icons.Outlined.LightMode, strings.themePolarityLight)
                    ).forEach { (polarity, icon, label) ->
                        val active = currentSpec.polarity == polarity
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (active) scheme.accent else scheme.card)
                                .border(
                                    0.8.dp,
                                    if (active) scheme.accent else scheme.cardBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    onSelectSpec(
                                        currentSpec.copy(
                                            polarity = polarity,
                                            tintMode = ThemeTintMode.FULL_OS,
                                            bubbleStyle = BubbleColorStyle.GRADIENT
                                        )
                                    )
                                }
                                .padding(horizontal = 11.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (active) scheme.onAccent else scheme.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = label,
                                color = if (active) scheme.onAccent else scheme.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onReset != null) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(scheme.card)
                                .border(0.8.dp, scheme.cardBorder, CircleShape)
                                .clickable(onClick = onReset),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RestartAlt,
                                contentDescription = strings.chatThemeResetBtn,
                                tint = scheme.textSecondary,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = scheme.accent,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable(onClick = onDone)
                    ) {
                        Text(
                            text = strings.chatThemeDone,
                            color = scheme.onAccent,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ровная 6-колоночная сетка 42 палитр (как в QR)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                swatchColors.chunked(6).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        rowItems.forEach { item ->
                            val (hue, sat) = colorToHueSat(item.color)
                            val isSelected = kotlin.math.abs(currentSpec.hue - hue) < 6f &&
                                currentSpec.tintMode == ThemeTintMode.FULL_OS
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .then(
                                        if (isSelected) Modifier.border(2.dp, scheme.textPrimary, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        onSelectSpec(
                                            currentSpec.copy(
                                                tintMode = ThemeTintMode.FULL_OS,
                                                bubbleStyle = BubbleColorStyle.GRADIENT,
                                                hue = hue,
                                                saturation = sat.coerceAtLeast(0.55f)
                                            )
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isSelected) 32.dp else 38.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                        .border(0.8.dp, Color.White.copy(alpha = 0.22f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Outlined.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                        repeat(6 - rowItems.size) {
                            Spacer(modifier = Modifier.size(44.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun colorToHueSat(color: Color): Pair<Float, Float> {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0] to hsv[1]
}
