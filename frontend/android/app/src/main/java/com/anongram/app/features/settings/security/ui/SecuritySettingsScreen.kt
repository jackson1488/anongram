package com.anongram.app.features.settings.security.ui

import android.app.Activity
import android.content.Context
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.settings.security.data.PasscodeStore
import com.anongram.app.features.settings.security.ui.components.PasscodeLockOverlay
import com.anongram.app.features.settings.security.ui.components.PasscodeScreenMode
import kotlinx.coroutines.launch
import kotlin.math.abs

private enum class SecuritySubScreen {
    MAIN,
    PASSCODE_SETTINGS
}

/**
 * Экран «Безопасность» (`features/settings/security/`) и подэкран «Код-пароль» в стиле Telegram.
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
    var autoLockOption by remember { mutableStateOf("через 1 мин.") }
    var showInRecents by remember { mutableStateOf(true) }
    var twoFactorEnabled by remember { mutableStateOf(false) }

    var subScreen by remember { mutableStateOf(SecuritySubScreen.MAIN) }
    var isAutoLockDialogOpen by remember { mutableStateOf(false) }
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
        autoLockOption = prefs.getString("key_autolock_option", if (isRu) "через 1 мин." else "in 1 min")
            ?: if (isRu) "через 1 мин." else "in 1 min"
        showInRecents = prefs.getBoolean("key_show_in_recents", true)
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
        // Возврат по системному жесту Назад именно на экран «Безопасность», а не в корень настроек
        BackHandler(enabled = true) {
            subScreen = SecuritySubScreen.MAIN
        }

        val switchColors = SwitchDefaults.colors(
            checkedThumbColor = scheme.onAccent,
            checkedTrackColor = scheme.accent,
            uncheckedThumbColor = scheme.textSecondary,
            uncheckedTrackColor = scheme.surface,
            uncheckedBorderColor = scheme.cardBorder
        )

        fun confirmFingerprintToEnable() {
            val activity = context as? FragmentActivity
            val canAuth = BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
            if (activity == null || !canAuth) {
                Toast.makeText(
                    context,
                    if (isRu) "Сканер отпечатка недоступен на устройстве" else "Biometrics unavailable",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        biometricsEnabled = true
                        prefs.edit().putBoolean("key_biometrics_enabled", true).apply()
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(if (isRu) "Подтвердите отпечаток" else "Confirm Fingerprint")
                .setSubtitle(
                    if (isRu) "Приложите палец, чтобы включить вход по биометрии"
                    else "Touch the sensor to enable biometric unlock"
                )
                .setNegativeButtonText(if (isRu) "Отмена" else "Cancel")
                .build()
            prompt.authenticate(info)
        }

        com.anongram.app.features.settings.common.ui.SettingsGlassScaffold(
            title = if (isRu) "Код-пароль" else "Passcode Lock",
            onBack = { subScreen = SecuritySubScreen.MAIN },
            modifier = modifier
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(scheme.accentSoft)
                        .border(0.8.dp, scheme.islandBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isRu) {
                        "Для блокировки и разблокировки приложения используйте код-пароль или отпечаток пальца."
                    } else {
                        "Use your passcode or fingerprint to lock and unlock AnonGram."
                    },
                    color = scheme.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // Карточка 1: Сменить код-пароль, Отпечаток, Автоблокировка
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pendingPasscodeMode = PasscodeScreenMode.CREATE }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Сменить код-пароль" else "Change Passcode",
                        color = scheme.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (!biometricsEnabled) {
                                confirmFingerprintToEnable()
                            } else {
                                biometricsEnabled = false
                                prefs.edit().putBoolean("key_biometrics_enabled", false).apply()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Разблокировка отпечатком пальца" else "Unlock with Fingerprint",
                        color = scheme.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = biometricsEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                confirmFingerprintToEnable()
                            } else {
                                biometricsEnabled = false
                                prefs.edit().putBoolean("key_biometrics_enabled", false).apply()
                            }
                        },
                        colors = switchColors
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isAutoLockDialogOpen = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Автоблокировка" else "Auto-Lock",
                        color = scheme.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = autoLockOption,
                        color = scheme.accent,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Text(
                text = if (isRu) {
                    "Запрашивать код-пароль, если какое-то время не было активности."
                } else {
                    "Require a passcode if away for a time."
                },
                color = scheme.textSecondary,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Карточка 2: Просмотр при переключении задач
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (isRu) "Просмотр при переключении задач" else "Show App Content in Task Switcher",
                    color = scheme.accent,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Показывать" else "Show",
                        color = scheme.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = showInRecents,
                        onCheckedChange = { enabled ->
                            showInRecents = enabled
                            prefs.edit().putBoolean("key_show_in_recents", enabled).apply()
                            val window = (context as? Activity)?.window
                            if (enabled) {
                                window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            } else {
                                window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                            }
                        },
                        colors = switchColors
                    )
                }
            }

            Text(
                text = if (isRu) {
                    "Если выключено, чаты не будут видны в окне переключения задач, но Вы не сможете делать скриншоты приложения."
                } else {
                    "When disabled, chats will be hidden in the task switcher, and screenshots will be blocked."
                },
                color = scheme.textSecondary,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Карточка 3: Выключить код-пароль (красная кнопка)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                    .clickable {
                        PasscodeStore.clearPasscode(context)
                        passcodeEnabled = false
                        biometricsEnabled = false
                        prefs.edit().putBoolean("key_biometrics_enabled", false).apply()
                        subScreen = SecuritySubScreen.MAIN
                    }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isRu) "Выключить код-пароль" else "Turn Passcode Off",
                    color = Color(0xFFEF4444),
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        if (isAutoLockDialogOpen) {
            AutoLockWheelDialog(
                current = autoLockOption,
                isRu = isRu,
                onDismiss = { isAutoLockDialogOpen = false },
                onConfirm = { chosen ->
                    autoLockOption = chosen
                    prefs.edit().putString("key_autolock_option", chosen).apply()
                    isAutoLockDialogOpen = false
                }
            )
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
private fun AutoLockWheelDialog(
    current: String,
    isRu: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val options = remember(isRu) {
        if (isRu) {
            listOf("выключена", "через 1 мин.", "через 5 мин.", "через 1 час", "через 5 часов")
        } else {
            listOf("Disabled", "in 1 min", "in 5 min", "in 1 hour", "in 5 hours")
        }
    }
    val initialIdx = options.indexOf(current).coerceAtLeast(1)
    val totalVirtual = Int.MAX_VALUE
    val startVirtual = remember(options.size, initialIdx) {
        val mid = totalVirtual / 2
        mid - (mid % options.size) + initialIdx
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startVirtual)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val itemHeightDp = 42.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeightDp.toPx() }

    val centerVirtual by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val visible = layout.visibleItemsInfo
            if (visible.isEmpty()) startVirtual
            else {
                val vc = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                visible.minByOrNull { abs((it.offset + it.size / 2) - vc) }?.index ?: startVirtual
            }
        }
    }

    var lastTick by remember { mutableIntStateOf(startVirtual) }
    LaunchedEffect(centerVirtual) {
        if (centerVirtual != lastTick) {
            lastTick = centerVirtual
            if (listState.isScrollInProgress) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val selectedIdx = ((centerVirtual % options.size) + options.size) % options.size

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.card)
                .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .padding(top = 20.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            Text(
                text = if (isRu) "Автоблокировка" else "Auto-Lock",
                color = scheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(168.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    HorizontalDivider(color = scheme.accent, thickness = 1.dp)
                    HorizontalDivider(color = scheme.accent, thickness = 1.dp)
                }

                LazyColumn(
                    state = listState,
                    flingBehavior = flingBehavior,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 63.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(count = totalVirtual, key = { it }) { virtualIdx ->
                        val realIdx = ((virtualIdx % options.size) + options.size) % options.size
                        val isCenter = virtualIdx == centerVirtual
                        val dist by remember(virtualIdx) {
                            derivedStateOf {
                                val layout = listState.layoutInfo
                                val info = layout.visibleItemsInfo.firstOrNull { it.index == virtualIdx }
                                if (info == null || itemHeightPx <= 0f) {
                                    if (isCenter) 0f else 1.5f
                                } else {
                                    val vc = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                                    abs((info.offset + info.size / 2f) - vc) / itemHeightPx
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeightDp)
                                .graphicsLayer {
                                    val clamped = dist.coerceIn(0f, 2f)
                                    val scale = (1f - 0.12f * clamped).coerceAtLeast(0.76f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = (1f - 0.36f * clamped).coerceIn(0.2f, 1f)
                                }
                                .clickable { scope.launch { listState.animateScrollToItem(virtualIdx) } },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = options[realIdx],
                                color = if (isCenter) scheme.textPrimary else scheme.textMuted,
                                fontSize = if (isCenter) 17.sp else 15.sp,
                                fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { onConfirm(options[selectedIdx]) }) {
                    Text(
                        text = if (isRu) "Готово" else "Done",
                        color = scheme.accent,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
