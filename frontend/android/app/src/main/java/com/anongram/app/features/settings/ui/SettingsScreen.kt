package com.anongram.app.features.settings.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.AnonGramThemes
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.settings.ui.components.ThemePickerDialog

/**
 * Раздел «Настройки» (секция 4 из 5). Тема оформления переключается
 * через ThemeController; остальные контроллеры приезжают своими модулями.
 */
@Composable
fun SettingsScreen(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var themePickerOpen by remember { mutableStateOf(false) }
    val activeThemeName = AnonGramThemes.byId(currentThemeId).displayName

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Настройки",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 16.sp
        )

        SettingsRow(
            title = "Тема оформления",
            value = activeThemeName,
            highlight = true,
            onClick = { themePickerOpen = true }
        )
        SettingsRow(title = "Переходы экранов", value = "11 стилей · активен SPRING_SWIPE")
        SettingsRow(title = "Крипто-ядро", value = "Rust · ML-KEM-1024 · ChaCha20-Poly1305")
        SettingsRow(title = "Жесты", value = "Полноэкранный свайп · Predictive Back")
    }

    if (themePickerOpen) {
        ThemePickerDialog(
            currentThemeId = currentThemeId,
            onSelect = { id ->
                onThemeSelect(id)
                themePickerOpen = false
            },
            onDismiss = { themePickerOpen = false }
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(
                1.dp,
                if (highlight) scheme.accent.copy(alpha = 0.45f) else CardBorder,
                RoundedCornerShape(18.dp)
            )
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = if (highlight) scheme.accent else TextPrimary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
