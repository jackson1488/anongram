package com.anongram.app.features.settings.devices.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.devices.ui.components.DeviceSessionCard

/**
 * Экран «Устройства» (`features/settings/devices/`).
 */
@Composable
fun DevicesSettingsScreen(
    onOpenQrScanner: () -> Unit,
    onBack: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"

    com.anongram.app.features.settings.common.ui.SettingsGlassScaffold(
        title = "Устройства",
        onBack = onBack
    ) {
        DeviceSessionCard(
            deviceModel = deviceName,
            appVersion = "AnonGram Android 1.0",
            onLinkDesktopQr = onOpenQrScanner
        )
    }
}
