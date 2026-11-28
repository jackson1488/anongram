package com.anongram.app.features.settings.security.ui.components

import android.app.Activity
import android.content.Context
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.ui.draw.blur
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.anongram.app.core.db.AnonGramEncryptedDb
import com.anongram.app.core.db.KeystoreDbKeyProvider
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.auth.ui.welcome.WelcomeShieldAnimation
import com.anongram.app.features.settings.security.data.PasscodeStore
import com.anongram.app.features.settings.security.data.PasscodeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.sin

enum class PasscodeScreenMode {
    UNLOCK,
    CREATE
}

/**
 * Кастомный экран ввода или создания код-пароля:
 * - Полная изоляция касаний (клики по фону не проходят к экранам под ним)
 * - Базовый FLAG_SECURE только на время показа экрана код-пароля
 * - Поднятая цифровая клавиатура (⌫ | 0 | Отпечаток при включённой биометрии)
 * - Для текстового пароля: клавиатура открыта сразу и не опускается + синяя пульсирующая кнопка биометрии слева
 */
@Composable
fun PasscodeLockOverlay(
    mode: PasscodeScreenMode,
    subtitleOverride: String? = null,
    allowBack: Boolean = true,
    hasWindowFocus: Boolean = true,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    BackHandler(enabled = true) {
        if (allowBack) {
            onBack()
        } else {
            (context as? Activity)?.moveTaskToBack(true)
        }
    }

    // Запрет скриншотов действует только пока экран код-пароля в фокусе (в Recents снимаем, чтобы работал блюр без серого экрана)
    DisposableEffect(hasWindowFocus) {
        val window = (context as? Activity)?.window
        if (hasWindowFocus) {
            window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    val biometricsEnabled = remember(mode) {
        mode == PasscodeScreenMode.UNLOCK &&
            context.applicationContext
                .getSharedPreferences("anongram_security_settings", Context.MODE_PRIVATE)
                .getBoolean("key_biometrics_enabled", false) &&
            BiometricManager.from(context).canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK
            ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    var selectedType by remember(mode) {
        mutableStateOf(
            if (mode == PasscodeScreenMode.UNLOCK) PasscodeStore.getPasscodeType(context)
            else PasscodeType.PIN
        )
    }
    var firstStepPasscode by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    val bruteForceEnabled = remember(mode) {
        mode == PasscodeScreenMode.UNLOCK && PasscodeStore.isBruteForceEnabled(context)
    }
    val expectedLength = remember(mode) {
        if (mode == PasscodeScreenMode.UNLOCK) PasscodeStore.getPasscodeLength(context) else 0
    }
    var lockoutRemainingSec by remember(mode) {
        mutableLongStateOf(
            if (bruteForceEnabled) PasscodeStore.getLockoutRemainingSeconds(context) else 0L
        )
    }

    LaunchedEffect(lockoutRemainingSec, bruteForceEnabled) {
        if (!bruteForceEnabled || lockoutRemainingSec <= 0L) return@LaunchedEffect
        while (lockoutRemainingSec > 0L) {
            delay(1000)
            lockoutRemainingSec = PasscodeStore.getLockoutRemainingSeconds(context)
        }
    }

    fun openDbInBackground(passcode: String) {
        val appCtx = context.applicationContext
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val dbKey = PasscodeStore.deriveDbKeyBytes(appCtx, passcode)
                try {
                    AnonGramEncryptedDb.openWithKey(appCtx, dbKey)
                    KeystoreDbKeyProvider.saveBioDbKey(appCtx, dbKey)
                } finally {
                    dbKey.fill(0)
                }
            }
        }
    }

    fun openDbWithBiometricsInBackground() {
        val appCtx = context.applicationContext
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val dbKey = KeystoreDbKeyProvider.getBioDbKeyBytes(appCtx) ?: return@launch
                try {
                    AnonGramEncryptedDb.openWithKey(appCtx, dbKey)
                } finally {
                    dbKey.fill(0)
                }
            }
        }
    }

    fun rekeyDbInBackground(passcode: String) {
        val appCtx = context.applicationContext
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                if (!AnonGramEncryptedDb.isUnlocked() && KeystoreDbKeyProvider.hasWrappedKey(appCtx)) {
                    val ksKey = KeystoreDbKeyProvider.getOrCreateKeyBytes(appCtx)
                    try {
                        AnonGramEncryptedDb.openWithKey(appCtx, ksKey)
                    } finally {
                        ksKey.fill(0)
                    }
                }
                val dbKey = PasscodeStore.deriveDbKeyBytes(appCtx, passcode)
                try {
                    AnonGramEncryptedDb.rekey(appCtx, dbKey)
                    KeystoreDbKeyProvider.saveBioDbKey(appCtx, dbKey)
                } finally {
                    dbKey.fill(0)
                }
                KeystoreDbKeyProvider.clearKey(appCtx)
            }
        }
    }

    fun launchBiometricPrompt() {
        if (lockoutRemainingSec > 0L) return
        val activity = context as? FragmentActivity ?: return
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    PasscodeStore.resetFailedAttempts(context)
                    openDbWithBiometricsInBackground()
                    onSuccess()
                }
            }
        )
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(if (isRu) "Разблокировка AnonGram" else "Unlock AnonGram")
            .setSubtitle(if (isRu) "Подтвердите личность биометрией" else "Verify with biometrics")
            .setNegativeButtonText(if (isRu) "Ввести пароль" else "Use passcode")
            .build()
        prompt.authenticate(promptInfo)
    }

    // Автоматический вызов биометрии при входе на экран блокировки, если включена
    LaunchedEffect(mode, biometricsEnabled) {
        if (mode == PasscodeScreenMode.UNLOCK && biometricsEnabled && lockoutRemainingSec <= 0L) {
            delay(180)
            launchBiometricPrompt()
        }
    }

    // Держим системную клавиатуру открытой для текстового пароля
    LaunchedEffect(mode, selectedType, lockoutRemainingSec) {
        if (selectedType == PasscodeType.PASSWORD && lockoutRemainingSec <= 0L) {
            delay(80)
            runCatching {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        }
    }

    // Мгновенная авторазблокировка в режиме UNLOCK, проверка защиты от перебора,
    // и мгновенное подтверждение / сброс на шаг 1 при создании код-пароля
    LaunchedEffect(input, mode, lockoutRemainingSec, firstStepPasscode) {
        if (lockoutRemainingSec > 0L || input.isEmpty()) return@LaunchedEffect
        if (mode == PasscodeScreenMode.UNLOCK) {
            val candidate = input
            val ok = withContext(Dispatchers.Default) {
                PasscodeStore.verifyPasscode(context, candidate)
            }
            if (ok && input == candidate) {
                PasscodeStore.resetFailedAttempts(context)
                input = ""
                errorText = null
                openDbInBackground(candidate)
                onSuccess()
            } else if (!ok && input == candidate && bruteForceEnabled && expectedLength > 0 && candidate.length >= expectedLength) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                PasscodeStore.recordFailedAttempt(context)
                val rem = PasscodeStore.getLockoutRemainingSeconds(context)
                if (rem > 0L) {
                    lockoutRemainingSec = rem
                }
                input = ""
                errorText = if (isRu) "Неверный код-пароль" else "Incorrect passcode"
            }
        } else if (mode == PasscodeScreenMode.CREATE && firstStepPasscode != null) {
            val target = firstStepPasscode!!
            if (input == target) {
                val chosenType = selectedType
                PasscodeStore.setPasscode(context, input, chosenType)
                rekeyDbInBackground(input)
                input = ""
                firstStepPasscode = null
                errorText = null
                onSuccess()
            } else if (input.length >= target.length) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                input = ""
                firstStepPasscode = null
                errorText = if (isRu) "Не совпадает, введите заново" else "Does not match, enter again"
            }
        }
    }

    val title = when {
        mode == PasscodeScreenMode.UNLOCK -> if (isRu) "Введите код-пароль" else "Enter Passcode"
        firstStepPasscode == null -> if (isRu) "Создайте код-пароль" else "Create Passcode"
        else -> if (isRu) "Повторите код-пароль" else "Confirm Passcode"
    }
    val subtitle: String? = subtitleOverride ?: when {
        mode == PasscodeScreenMode.UNLOCK -> null
        firstStepPasscode == null ->
            if (isRu) "Любая длина без ограничений" else "Any length, no character limit"
        else ->
            if (isRu) "Введите комбинацию ещё раз для подтверждения" else "Re-enter to confirm"
    }

    fun submitCreateOrDone() {
        val current = input
        if (mode == PasscodeScreenMode.UNLOCK) {
            if (current.isEmpty()) return
            scope.launch {
                val ok = withContext(Dispatchers.Default) {
                    PasscodeStore.verifyPasscode(context, current)
                }
                if (ok) {
                    input = ""
                    errorText = null
                    openDbInBackground(current)
                    onSuccess()
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    input = ""
                    errorText = if (isRu) "Неверный код-пароль" else "Incorrect passcode"
                }
            }
            return
        }

        if (firstStepPasscode == null) {
            if (current.isEmpty()) {
                errorText = if (isRu) "Введите хотя бы 1 символ" else "Enter at least 1 character"
                return
            }
            firstStepPasscode = current
            input = ""
            errorText = null
        } else {
            if (current == firstStepPasscode) {
                val chosenType = selectedType
                PasscodeStore.setPasscode(context, current, chosenType)
                rekeyDbInBackground(current)
                input = ""
                firstStepPasscode = null
                errorText = null
                onSuccess()
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                input = ""
                firstStepPasscode = null
                errorText = if (isRu) "Не совпадает, попробуйте снова" else "Does not match, try again"
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            // Перехват всех тапов по пустой области, чтобы клики не доходили до чатов под экраном
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (selectedType == PasscodeType.PASSWORD) {
                    runCatching {
                        focusRequester.requestFocus()
                        keyboardController?.show()
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if ((lockoutRemainingSec > 0L || !hasWindowFocus) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(38.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            AnimatedLockShaderBackground(
                accent = scheme.accent,
                accentSoft = scheme.accentSoft,
                background = scheme.background
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                .safeStatusBarPadding()
                .safeNavigationBarPadding()
                .then(if (selectedType == PasscodeType.PASSWORD) Modifier.imePadding() else Modifier),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (allowBack) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isRu) "Назад" else "Back",
                            tint = scheme.textPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.4f))

            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WelcomeShieldAnimation(size = 76.dp)

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = scheme.textSecondary,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                if (mode == PasscodeScreenMode.CREATE && firstStepPasscode == null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.card.copy(alpha = 0.78f))
                            .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        PasscodeTypeChip(
                            label = if (isRu) "Цифровой" else "Numeric",
                            selected = selectedType == PasscodeType.PIN,
                            onClick = {
                                selectedType = PasscodeType.PIN
                                input = ""
                                errorText = null
                            }
                        )
                        PasscodeTypeChip(
                            label = if (isRu) "Пароль" else "Password",
                            selected = selectedType == PasscodeType.PASSWORD,
                            onClick = {
                                selectedType = PasscodeType.PASSWORD
                                input = ""
                                errorText = null
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedType == PasscodeType.PIN) {
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .animateContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (input.isNotEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val visibleDots = input.length.coerceAtMost(14)
                                repeat(visibleDots) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(scheme.accent)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (biometricsEnabled) {
                            AnimatedBlueBiometricButton(
                                onClick = { launchBiometricPrompt() }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(scheme.card.copy(alpha = 0.85f))
                                .border(
                                    width = 1.dp,
                                    color = if (errorText != null) Color(0xFFEF4444) else scheme.islandBorder,
                                    shape = RoundedCornerShape(22.dp)
                                )
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (input.isEmpty()) {
                                Text(
                                    text = if (isRu) "Введите пароль" else "Enter password",
                                    color = scheme.textMuted,
                                    fontSize = 15.sp
                                )
                            }
                            BasicTextField(
                                value = input,
                                onValueChange = {
                                    input = it
                                    errorText = null
                                },
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { submitCreateOrDone() }),
                                cursorBrush = SolidColor(scheme.accent),
                                textStyle = TextStyle(
                                    color = scheme.textPrimary,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(focusRequester)
                            )
                        }
                    }
                }

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.45f))

            if (selectedType == PasscodeType.PIN) {
                CustomNumericKeypad(
                    showConfirmButton = mode == PasscodeScreenMode.CREATE,
                    showBiometricsInCenterRight = mode == PasscodeScreenMode.UNLOCK && biometricsEnabled,
                    onDigit = { digit ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        input += digit
                        errorText = null
                    },
                    onBackspace = {
                        if (input.isNotEmpty()) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            input = input.dropLast(1)
                        }
                    },
                    onConfirm = { submitCreateOrDone() },
                    onBiometrics = { launchBiometricPrompt() },
                    canConfirm = input.isNotEmpty(),
                    modifier = Modifier.padding(bottom = 68.dp)
                )
            } else if (mode == PasscodeScreenMode.CREATE) {
                Button(
                    onClick = { submitCreateOrDone() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.accent,
                        contentColor = scheme.onAccent
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 24.dp)
                        .height(48.dp)
                ) {
                    Text(
                        text = if (firstStepPasscode == null) {
                            if (isRu) "Далее" else "Next"
                        } else {
                            if (isRu) "Сохранить" else "Save"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        }

        if (!hasWindowFocus && lockoutRemainingSec <= 0L) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.background.copy(alpha = 0.36f))
            )
        }

        if (lockoutRemainingSec > 0L) {
            val hrs = lockoutRemainingSec / 3600L
            val mins = (lockoutRemainingSec % 3600L) / 60L
            val secs = lockoutRemainingSec % 60L
            val timeFormatted = if (hrs > 0L) {
                String.format(java.util.Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format(java.util.Locale.US, "%02d:%02d", mins, secs)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.62f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRu) "Введён неверный пароль" else "Incorrect passcode",
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = timeFormatted,
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedBlueBiometricButton(
    onClick: () -> Unit
) {
    val bioBlue = Color(0xFF38BDF8)
    val transition = rememberInfiniteTransition(label = "bio_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bio_alpha"
    )

    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(CircleShape)
            .background(bioBlue.copy(alpha = 0.16f + pulse * 0.12f))
            .border(1.2.dp, bioBlue.copy(alpha = 0.45f + pulse * 0.45f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Fingerprint,
            contentDescription = "Biometrics",
            tint = bioBlue,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
private fun AnimatedLockShaderBackground(
    accent: Color,
    accentSoft: Color,
    background: Color
) {
    val transition = rememberInfiniteTransition(label = "lock_bg")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2.0).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "lock_phase"
    )
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(color = background)
        val c1 = Offset(size.width * (0.3f + 0.18f * cos(phase)), size.height * (0.25f + 0.12f * sin(phase)))
        val c2 = Offset(size.width * (0.72f - 0.16f * sin(phase)), size.height * (0.72f - 0.14f * cos(phase)))
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                center = c1,
                radius = size.minDimension * 0.72f
            ),
            center = c1,
            radius = size.minDimension * 0.72f
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentSoft.copy(alpha = 0.55f), Color.Transparent),
                center = c2,
                radius = size.minDimension * 0.65f
            ),
            center = c2,
            radius = size.minDimension * 0.65f
        )
    }
}

@Composable
private fun PasscodeTypeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) scheme.accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) scheme.onAccent else scheme.textSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CustomNumericKeypad(
    showConfirmButton: Boolean,
    showBiometricsInCenterRight: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
    onBiometrics: () -> Unit,
    canConfirm: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9")
    )

    Column(
        modifier = modifier
            .widthIn(max = 312.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { digit ->
                    KeypadButton(
                        onClick = { onDigit(digit) }
                    ) {
                        Text(
                            text = digit,
                            color = scheme.textPrimary,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            if (showConfirmButton) {
                KeypadButton(onClick = onBackspace) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Backspace",
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                KeypadButton(onClick = { onDigit("0") }) {
                    Text(
                        text = "0",
                        color = scheme.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                KeypadButton(
                    containerColor = if (canConfirm) scheme.accent else scheme.card.copy(alpha = 0.55f),
                    onClick = onConfirm
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Confirm",
                        tint = if (canConfirm) scheme.onAccent else scheme.textMuted,
                        modifier = Modifier.size(26.dp)
                    )
                }
            } else {
                // Как в Telegram: слева ⌫, по центру 0, справа Отпечаток (если включён)
                KeypadButton(onClick = onBackspace) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.Backspace,
                        contentDescription = "Backspace",
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                KeypadButton(onClick = { onDigit("0") }) {
                    Text(
                        text = "0",
                        color = scheme.textPrimary,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                if (showBiometricsInCenterRight) {
                    KeypadButton(
                        containerColor = scheme.accentSoft,
                        onClick = onBiometrics
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "Biometrics",
                            tint = scheme.accent,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(78.dp))
                }
            }
        }
    }
}

@Composable
private fun KeypadButton(
    containerColor: Color? = null,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val bg = containerColor ?: scheme.card.copy(alpha = 0.72f)
    Box(
        modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .background(bg)
            .border(0.7.dp, scheme.islandBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
