package com.anongram.app.features.settings.security.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.features.settings.security.data.PasscodeStore
import com.anongram.app.features.settings.security.ui.components.PasscodeLockOverlay
import com.anongram.app.features.settings.security.ui.components.PasscodeScreenMode
import kotlinx.coroutines.delay
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

        var bruteForceEnabled by remember { mutableStateOf(PasscodeStore.isBruteForceEnabled(context)) }
        var bruteForceMaxAttempts by remember { mutableIntStateOf(PasscodeStore.getBruteForceMaxAttempts(context)) }
        var bruteForceBaseSec by remember { mutableIntStateOf(PasscodeStore.getBruteForceBaseSec(context)) }
        var isAttemptsDialogOpen by remember { mutableStateOf(false) }
        var isTimerDialogOpen by remember { mutableStateOf(false) }
        var biometricFallbackMessage by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(biometricFallbackMessage) {
            if (biometricFallbackMessage != null) {
                delay(2800)
                biometricFallbackMessage = null
            }
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
                biometricsEnabled = false
                prefs.edit().putBoolean("key_biometrics_enabled", false).apply()
                biometricFallbackMessage = if (isRu) {
                    "Нельзя включить: у вас не добавлена или отключена биометрия в системе"
                } else {
                    "Cannot enable: biometrics not enrolled or unavailable"
                }
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

        Box(modifier = Modifier.fillMaxSize()) {
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
                        },
                        colors = switchColors
                    )
                }
            }

            Text(
                text = if (isRu) {
                    "Если выключено, экран приложения будет размываться в окне переключения задач."
                } else {
                    "When disabled, the app screen will be blurred in the task switcher."
                },
                color = scheme.textSecondary,
                fontSize = 12.5.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Карточка «Защита от перебора»
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
                        .clickable {
                            val next = !bruteForceEnabled
                            bruteForceEnabled = next
                            PasscodeStore.setBruteForceEnabled(context, next)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isRu) "Защита от перебора" else "Brute-Force Protection",
                        color = scheme.textPrimary,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = bruteForceEnabled,
                        onCheckedChange = { enabled ->
                            bruteForceEnabled = enabled
                            PasscodeStore.setBruteForceEnabled(context, enabled)
                        },
                        colors = switchColors
                    )
                }

                if (bruteForceEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAttemptsDialogOpen = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) "Попыток до блокировки" else "Attempts before lock",
                            color = scheme.textPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$bruteForceMaxAttempts",
                            color = scheme.accent,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isTimerDialogOpen = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRu) "Базовый таймер" else "Base timer",
                            color = scheme.textPrimary,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = formatDurationLabel(bruteForceBaseSec, isRu),
                            color = scheme.accent,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Text(
                text = if (isRu) {
                    "Внимание: при включении длина вашего код-пароля раскрывается при вводе (попытка засчитывается по точной длине)."
                } else {
                    "Warning: enabling this reveals your passcode length during entry (failed attempts trigger at exact length)."
                },
                color = Color(0xFFEF4444),
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

            AnimatedVisibility(
                visible = biometricFallbackMessage != null,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeNavigationBarPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
                    .zIndex(60f)
            ) {
                biometricFallbackMessage?.let { msg ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(scheme.islandBg)
                            .border(0.8.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                            .clickable { biometricFallbackMessage = null }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = msg,
                            color = scheme.textPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
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
        if (isAttemptsDialogOpen) {
            BruteForceAttemptsDialog(
                current = bruteForceMaxAttempts,
                isRu = isRu,
                onDismiss = { isAttemptsDialogOpen = false },
                onConfirm = { chosen ->
                    bruteForceMaxAttempts = chosen
                    PasscodeStore.setBruteForceMaxAttempts(context, chosen)
                    isAttemptsDialogOpen = false
                }
            )
        }
        if (isTimerDialogOpen) {
            BruteForceTimerDialog(
                currentSec = bruteForceBaseSec,
                isRu = isRu,
                onDismiss = { isTimerDialogOpen = false },
                onConfirm = { chosenSec ->
                    bruteForceBaseSec = chosenSec
                    PasscodeStore.setBruteForceBaseSec(context, chosenSec)
                    isTimerDialogOpen = false
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
            listOf(
                "выключена",
                "через 30 сек.",
                "через 1 мин.",
                "через 2 мин.",
                "через 3 мин.",
                "через 5 мин.",
                "через 10 мин.",
                "через 15 мин."
            )
        } else {
            listOf(
                "Disabled",
                "in 30 sec",
                "in 1 min",
                "in 2 min",
                "in 3 min",
                "in 5 min",
                "in 10 min",
                "in 15 min"
            )
        }
    }
    val initialIdx = options.indexOf(current).let { if (it >= 0) it else 2 }
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

private fun formatDurationLabel(sec: Int, isRu: Boolean): String {
    val s = sec.coerceIn(5, 86400)
    if (s < 60) {
        return if (isRu) "$s сек." else "${s}s"
    }
    if (s < 3600) {
        val m = s / 60
        val rem = s % 60
        return if (rem == 0) {
            if (isRu) "$m мин." else "$m min"
        } else {
            if (isRu) "$m мин. $rem сек." else "${m}m ${rem}s"
        }
    }
    val hours = s / 3600
    val mins = (s % 3600) / 60
    return if (mins == 0) {
        if (isRu) {
            val unit = when {
                hours == 1 || hours == 21 -> "час"
                hours in 2..4 || hours in 22..24 -> "часа"
                else -> "часов"
            }
            "$hours $unit"
        } else {
            if (hours == 1) "1 hour" else "$hours hours"
        }
    } else {
        if (isRu) "$hours ч. $mins мин." else "${hours}h ${mins}m"
    }
}

@Composable
private fun BruteForceAttemptsDialog(
    current: Int,
    isRu: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val options = remember { (1..10).toList() }
    val labels = remember(isRu) {
        options.map { n ->
            if (isRu) {
                val unit = when {
                    n == 1 -> "попытка"
                    n in 2..4 -> "попытки"
                    else -> "попыток"
                }
                "$n $unit"
            } else {
                if (n == 1) "1 attempt" else "$n attempts"
            }
        }
    }
    val selectedOptionIndex = options.indexOf(current).let { if (it >= 0) it else 2 }
    val itemHeightDp = 42.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeightDp.toPx() }
    val repeatCount = 2000
    val totalVirtual = repeatCount * options.size
    val initialVirtual = (repeatCount / 2) * options.size + selectedOptionIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialVirtual)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    val centerVirtual by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val visible = layout.visibleItemsInfo
            if (visible.isEmpty()) initialVirtual
            else {
                val vc = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                visible.minByOrNull { abs((it.offset + it.size / 2) - vc) }?.index ?: initialVirtual
            }
        }
    }
    val selectedIdx = ((centerVirtual % options.size) + options.size) % options.size

    var lastTickVirtual by remember { mutableIntStateOf(initialVirtual) }
    LaunchedEffect(centerVirtual) {
        if (centerVirtual != lastTickVirtual) {
            lastTickVirtual = centerVirtual
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Text(
                text = if (isRu) "Попыток до блокировки" else "Attempts before lock",
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
                                text = labels[realIdx],
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
private fun BruteForceTimerDialog(
    currentSec: Int,
    isRu: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val presets = remember {
        listOf(
            30, 60, 180, 300, 600, 900, 1800,
            3600, 7200, 10800, 18000, 28800, 36000,
            43200, 50400, 57600, 64800, 72000, 79200, 86400
        )
    }
    val labels = remember(isRu) { presets.map { formatDurationLabel(it, isRu) } }
    val presetIdx = presets.indexOf(currentSec)
    val selectedOptionIndex = if (presetIdx >= 0) presetIdx else 0
    val itemHeightDp = 42.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeightDp.toPx() }
    val repeatCount = 2000
    val totalVirtual = repeatCount * presets.size
    val initialVirtual = (repeatCount / 2) * presets.size + selectedOptionIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialVirtual)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    var customInput by remember {
        mutableStateOf(
            if (presetIdx >= 0) ""
            else if (currentSec % 60 == 0) (currentSec / 60).toString()
            else currentSec.toString()
        )
    }
    var customUnitIsMin by remember {
        mutableStateOf(presetIdx >= 0 || currentSec % 60 == 0)
    }

    val centerVirtual by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val visible = layout.visibleItemsInfo
            if (visible.isEmpty()) initialVirtual
            else {
                val vc = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                visible.minByOrNull { abs((it.offset + it.size / 2) - vc) }?.index ?: initialVirtual
            }
        }
    }
    val selectedIdx = ((centerVirtual % presets.size) + presets.size) % presets.size

    var lastTickVirtual by remember { mutableIntStateOf(initialVirtual) }
    LaunchedEffect(centerVirtual) {
        if (centerVirtual != lastTickVirtual) {
            lastTickVirtual = centerVirtual
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 20.dp)
        ) {
            Text(
                text = if (isRu) "Базовый таймер" else "Base timer",
                color = scheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        val realIdx = ((virtualIdx % presets.size) + presets.size) % presets.size
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
                                .clickable {
                                    customInput = ""
                                    scope.launch { listState.animateScrollToItem(virtualIdx) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = labels[realIdx],
                                color = if (isCenter && customInput.isBlank()) scheme.textPrimary else scheme.textMuted,
                                fontSize = if (isCenter) 17.sp else 15.sp,
                                fontWeight = if (isCenter) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customInput,
                    onValueChange = { raw ->
                        customInput = raw.filter { it.isDigit() }.take(5)
                    },
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = if (isRu) "Своё время" else "Custom time",
                            color = scheme.textMuted,
                            fontSize = 14.sp
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = scheme.textPrimary,
                        unfocusedTextColor = scheme.textPrimary,
                        focusedBorderColor = scheme.accent,
                        unfocusedBorderColor = scheme.islandBorder,
                        cursorColor = scheme.accent
                    ),
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(scheme.surface)
                        .border(1.dp, scheme.islandBorder, CircleShape)
                        .clickable { customUnitIsMin = !customUnitIsMin }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (customUnitIsMin) {
                            if (isRu) "мин" else "min"
                        } else {
                            if (isRu) "сек" else "sec"
                        },
                        color = scheme.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    val parsed = customInput.toIntOrNull()
                    val resultSec = if (parsed != null && parsed > 0) {
                        val rawSec = if (customUnitIsMin) parsed * 60 else parsed
                        rawSec.coerceIn(5, 86400)
                    } else {
                        presets[selectedIdx]
                    }
                    onConfirm(resultSec)
                }) {
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

