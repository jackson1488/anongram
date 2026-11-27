package com.anongram.app.features.settings.security.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.settings.security.data.PasscodeStore
import com.anongram.app.features.settings.security.ui.components.PasscodeLockOverlay
import com.anongram.app.features.settings.security.ui.components.PasscodeScreenMode

private enum class SecuritySubScreen {
    MAIN,
    PASSCODE_SETTINGS
}

/**
 * Экран «Безопасность» (`features/settings/security/`) в стиле Telegram:
 * чистые строки со статусом Вкл./Выкл. справа, без нижней карточки.
 */
@Composable
fun SecuritySettingsScreen(
    onBack: () -> Unit,
    openPasscodeCreateDirectly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    val prefs = remember {
        context.applicationContext.getSharedPreferences("anongram_security_settings", Context.MODE_PRIVATE)
    }

    var passcodeEnabled by remember { mutableStateOf(PasscodeStore.isPasscodeEnabled(context)) }
    var biometricsEnabled by remember { mutableStateOf(false) }
    var autoLockOption by remember { mutableStateOf("1 мин") }
    var screenProtectionEnabled by remember { mutableStateOf(false) }
    var twoFactorEnabled by remember { mutableStateOf(false) }

    var subScreen by remember { mutableStateOf(SecuritySubScreen.MAIN) }
    var pendingPasscodeMode by remember {
        mutableStateOf(
            if (openPasscodeCreateDirectly && !PasscodeStore.isPasscodeEnabled(context)) {
                PasscodeScreenMode.CREATE
            } else {
                null
            }
        )
    }
    var unlockAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    LaunchedEffect(Unit) {
        passcodeEnabled = PasscodeStore.isPasscodeEnabled(context)
        biometricsEnabled = prefs.getBoolean("key_biometrics_enabled", false)
        autoLockOption = prefs.getString("key_autolock_option", "1 мин") ?: "1 мин"
        screenProtectionEnabled = prefs.getBoolean("key_screen_protection", false)
        twoFactorEnabled = prefs.getBoolean("key_two_factor_enabled", false)
    }

    if (pendingPasscodeMode != null) {
        PasscodeLockOverlay(
            mode = pendingPasscodeMode!!,
            onSuccess = {
                val mode = pendingPasscodeMode
                pendingPasscodeMode = null
                if (mode == PasscodeScreenMode.CREATE) {
                    passcodeEnabled = true
                    subScreen = SecuritySubScreen.PASSCODE_SETTINGS
                } else {
                    unlockAction?.invoke()
                    unlockAction = null
                }
            },
            onBack = {
                pendingPasscodeMode = null
                unlockAction = null
                if (openPasscodeCreateDirectly && !passcodeEnabled) {
                    onBack()
                }
            }
        )
        return
    }

    val onText = if (isRu) "Вкл." else "On"
    val offText = if (isRu) "Выкл." else "Off"

    if (subScreen == SecuritySubScreen.PASSCODE_SETTINGS) {
        val switchColors = SwitchDefaults.colors(
            checkedThumbColor = scheme.onAccent,
            checkedTrackColor = scheme.accent,
            uncheckedThumbColor = scheme.textSecondary,
            uncheckedTrackColor = scheme.surface,
            uncheckedBorderColor = scheme.cardBorder
        )
        val autoLockOptions = if (isRu) {
            listOf("Сразу", "1 мин", "5 мин", "1 час")
        } else {
            listOf("Now", "1 min", "5 min", "1 hour")
        }

        com.anongram.app.features.settings.common.ui.SettingsGlassScaffold(
            title = if (isRu) "Код-пароль" else "Passcode Lock",
            onBack = { subScreen = SecuritySubScreen.MAIN },
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { pendingPasscodeMode = PasscodeScreenMode.CREATE }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Изменить код-пароль" else "Change Passcode",
                        color = scheme.accent,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Разблокировка отпечатком" else "Unlock with Biometrics",
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = biometricsEnabled,
                        onCheckedChange = { enabled ->
                            biometricsEnabled = enabled
                            prefs.edit().putBoolean("key_biometrics_enabled", enabled).apply()
                        },
                        colors = switchColors
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Защита от скриншотов" else "Screen Protection",
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = screenProtectionEnabled,
                        onCheckedChange = { enabled ->
                            screenProtectionEnabled = enabled
                            prefs.edit().putBoolean("key_screen_protection", enabled).apply()
                        },
                        colors = switchColors
                    )
                }

                Text(
                    text = if (isRu) "Автоблокировка" else "Auto-Lock",
                    color = scheme.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    autoLockOptions.forEach { opt ->
                        val isSelected = opt == autoLockOption
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) scheme.accent else scheme.surface)
                                .border(
                                    0.8.dp,
                                    if (isSelected) scheme.accent else scheme.islandBorder,
                                    RoundedCornerShape(18.dp)
                                )
                                .clickable {
                                    autoLockOption = opt
                                    prefs.edit().putString("key_autolock_option", opt).apply()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = opt,
                                color = if (isSelected) scheme.onAccent else scheme.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            PasscodeStore.clearPasscode(context)
                            passcodeEnabled = false
                            subScreen = SecuritySubScreen.MAIN
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Выключить код-пароль" else "Turn Passcode Off",
                        color = Color(0xFFEF4444),
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        return
    }

    com.anongram.app.features.settings.common.ui.SettingsGlassScaffold(
        title = if (isRu) "Безопасность" else "Privacy and Security",
        onBack = onBack,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.card)
                .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                .padding(vertical = 6.dp)
        ) {
            SecurityActionRow(
                icon = Icons.Outlined.Shield,
                title = if (isRu) "Облачный пароль" else "Two-Step Verification",
                status = if (twoFactorEnabled) onText else offText,
                statusAccent = twoFactorEnabled,
                onClick = {
                    twoFactorEnabled = !twoFactorEnabled
                    prefs.edit().putBoolean("key_two_factor_enabled", twoFactorEnabled).apply()
                }
            )
            SecurityActionRow(
                icon = Icons.Outlined.Timer,
                title = if (isRu) "Автоудаление сообщений" else "Auto-Delete Messages",
                status = offText,
                statusAccent = false,
                onClick = {}
            )
            SecurityActionRow(
                icon = Icons.Outlined.Lock,
                title = if (isRu) "Код-пароль" else "Passcode Lock",
                status = if (passcodeEnabled) onText else offText,
                statusAccent = passcodeEnabled,
                onClick = {
                    if (passcodeEnabled) {
                        unlockAction = { subScreen = SecuritySubScreen.PASSCODE_SETTINGS }
                        pendingPasscodeMode = PasscodeScreenMode.UNLOCK
                    } else {
                        pendingPasscodeMode = PasscodeScreenMode.CREATE
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SecurityActionRow(
    icon: ImageVector,
    title: String,
    status: String,
    statusAccent: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = scheme.textSecondary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            color = scheme.textPrimary,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = status,
            color = if (statusAccent) scheme.accent else scheme.textSecondary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
