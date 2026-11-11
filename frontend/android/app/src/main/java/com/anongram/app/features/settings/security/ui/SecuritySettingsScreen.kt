package com.anongram.app.features.settings.security.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.security.ui.components.SecurityCryptoCard
import com.anongram.app.features.settings.security.ui.components.SecurityPasscodeCard
import com.anongram.app.features.settings.security.ui.components.SecurityTopBar

/**
 * Экран «Безопасность» (`features/settings/security/`).
 * Изолированный модуль управления кодом-паролем, биометрией, защитой экрана и квантовой криптографией.
 */
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.applicationContext.getSharedPreferences("anongram_security_settings", Context.MODE_PRIVATE)
    }

    var passcodeEnabled by remember { mutableStateOf(false) }
    var biometricsEnabled by remember { mutableStateOf(false) }
    var autoLockOption by remember { mutableStateOf("1 мин") }
    var screenProtectionEnabled by remember { mutableStateOf(false) }
    var twoFactorEnabled by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        passcodeEnabled = prefs.getBoolean("key_passcode_enabled", false)
        biometricsEnabled = prefs.getBoolean("key_biometrics_enabled", false)
        autoLockOption = prefs.getString("key_autolock_option", "1 мин") ?: "1 мин"
        screenProtectionEnabled = prefs.getBoolean("key_screen_protection", false)
        twoFactorEnabled = prefs.getBoolean("key_two_factor_enabled", false)
    }

    // Обработка системного жеста «Назад»
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050508))
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
        // Верхняя панель в стиле Telegram
        SecurityTopBar(
            title = "Безопасность",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Карточка блокировки приложения, биометрии и 2FA
            SecurityPasscodeCard(
                passcodeEnabled = passcodeEnabled,
                onPasscodeChange = { enabled ->
                    passcodeEnabled = enabled
                    prefs.edit().putBoolean("key_passcode_enabled", enabled).apply()
                },
                biometricsEnabled = biometricsEnabled,
                onBiometricsChange = { enabled ->
                    biometricsEnabled = enabled
                    prefs.edit().putBoolean("key_biometrics_enabled", enabled).apply()
                },
                autoLockOption = autoLockOption,
                onAutoLockOptionChange = { option ->
                    autoLockOption = option
                    prefs.edit().putString("key_autolock_option", option).apply()
                },
                screenProtectionEnabled = screenProtectionEnabled,
                onScreenProtectionChange = { enabled ->
                    screenProtectionEnabled = enabled
                    prefs.edit().putBoolean("key_screen_protection", enabled).apply()
                },
                twoFactorEnabled = twoFactorEnabled,
                onTwoFactorChange = { enabled ->
                    twoFactorEnabled = enabled
                    prefs.edit().putBoolean("key_two_factor_enabled", enabled).apply()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Карточка квантовой криптографии AnonGram
            SecurityCryptoCard()

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
