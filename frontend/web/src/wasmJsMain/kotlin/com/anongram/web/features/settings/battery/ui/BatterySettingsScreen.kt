package com.anongram.web.features.settings.battery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.features.settings.ui.SettingsCardDivider
import com.anongram.web.features.settings.ui.SettingsGroupCard
import com.anongram.web.features.settings.ui.SettingsSubScreenScaffold
import com.anongram.web.features.settings.ui.SettingsToggleRow
import com.anongram.web.platform.WebStorage
import kotlin.math.roundToInt

@Composable
fun BatterySettingsScreen(onBack: () -> Unit) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var powerSaving by remember { mutableStateOf(WebStorage.getString("anongram_battery_saving")?.toBooleanStrictOrNull() ?: false) }
    var threshold by remember { mutableStateOf(WebStorage.getString("anongram_battery_threshold")?.toIntOrNull() ?: 20) }
    var disableAutoplay by remember { mutableStateOf(WebStorage.getString("anongram_battery_autoplay")?.toBooleanStrictOrNull() ?: true) }
    var reduceBlur by remember { mutableStateOf(WebStorage.getString("anongram_battery_blur")?.toBooleanStrictOrNull() ?: true) }

    SettingsSubScreenScaffold(
        title = if (isRu) "Энергосбережение" else "Power Saving",
        onBack = onBack
    ) {
        SettingsGroupCard(title = if (isRu) "Режим экономии" else "Power Saving Mode") {
            SettingsToggleRow(
                title = if (isRu) "Энергосбережение" else "Power Saving",
                subtitle = if (isRu) "Снижает нагрузку на GPU и фоновые эффекты" else "Reduces GPU usage and background effects",
                checked = powerSaving,
                onCheckedChange = {
                    powerSaving = it
                    WebStorage.putString("anongram_battery_saving", it.toString())
                }
            )
            SettingsCardDivider()
            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(
                    text = if (isRu) "Порог авто-включения: $threshold%" else "Auto-enable threshold: $threshold%",
                    color = scheme.accent,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Slider(
                    value = threshold.toFloat(),
                    onValueChange = {
                        threshold = it.roundToInt()
                        WebStorage.putString("anongram_battery_threshold", threshold.toString())
                    },
                    valueRange = 5f..50f,
                    colors = SliderDefaults.colors(
                        thumbColor = scheme.accent,
                        activeTrackColor = scheme.accent,
                        inactiveTrackColor = scheme.divider,
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    )
                )
            }
        }

        SettingsGroupCard(title = if (isRu) "Ограничения при экономии" else "Power Saving Options") {
            SettingsToggleRow(
                title = if (isRu) "Отключить автовоспроизведение видео" else "Disable video autoplay",
                checked = disableAutoplay,
                onCheckedChange = {
                    disableAutoplay = it
                    WebStorage.putString("anongram_battery_autoplay", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Упростить размытие Glass" else "Simplify Glass blur",
                checked = reduceBlur,
                onCheckedChange = {
                    reduceBlur = it
                    WebStorage.putString("anongram_battery_blur", it.toString())
                }
            )
        }
    }
}
