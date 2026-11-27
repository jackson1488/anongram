package com.anongram.app.features.settings.security.ui.components

import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.db.AnonGramEncryptedDb
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.security.data.PasscodeStore
import com.anongram.app.features.settings.security.data.PasscodeType
import kotlin.math.cos
import kotlin.math.sin

enum class PasscodeScreenMode {
    UNLOCK,
    CREATE
}

/**
 * Кастомный экран ввода или создания код-пароля без ограничения длины и без подсказок количества знаков:
 * - Шедевральный анимированный фон в цветах активной темы
 * - Кастомная цифровая клавиатура с кнопками 0-9, ⌫ и ✓ (без системной клавиатуры)
 * - Переключатель на текстовый пароль при создании
 */
@Composable
fun PasscodeLockOverlay(
    mode: PasscodeScreenMode,
    subtitleOverride: String? = null,
    allowBack: Boolean = true,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val haptic = LocalHapticFeedback.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    if (allowBack) {
        BackHandler(onBack = onBack)
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

    val title = when {
        mode == PasscodeScreenMode.UNLOCK -> if (isRu) "Введите код-пароль" else "Enter Passcode"
        firstStepPasscode == null -> if (isRu) "Создайте код-пароль" else "Create Passcode"
        else -> if (isRu) "Повторите код-пароль" else "Confirm Passcode"
    }
    val subtitle = subtitleOverride ?: when {
        mode == PasscodeScreenMode.UNLOCK ->
            if (isRu) "Введите комбинацию и нажмите ✓" else "Enter your combination and tap ✓"
        firstStepPasscode == null ->
            if (isRu) "Любая длина без ограничений" else "Any length, no character limit"
        else ->
            if (isRu) "Введите комбинацию ещё раз для подтверждения" else "Re-enter to confirm"
    }

    fun submit() {
        val current = input
        if (mode == PasscodeScreenMode.UNLOCK) {
            if (current.isEmpty()) {
                errorText = if (isRu) "Введите код-пароль" else "Enter passcode"
                return
            }
            if (PasscodeStore.verifyPasscode(context, current)) {
                runCatching {
                    val dbKey = PasscodeStore.deriveDbKeyBytes(context, current)
                    AnonGramEncryptedDb.openWithKey(context, dbKey)
                    dbKey.fill(0)
                }
                input = ""
                errorText = null
                onSuccess()
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                input = ""
                errorText = if (isRu) "Неверный код-пароль" else "Incorrect passcode"
            }
        } else {
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
                    PasscodeStore.setPasscode(context, current, selectedType)
                    runCatching {
                        val dbKey = PasscodeStore.deriveDbKeyBytes(context, current)
                        AnonGramEncryptedDb.rekey(context, dbKey)
                        dbKey.fill(0)
                    }
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
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
    ) {
        // Живой анимированный шейдерный фон (парящие градиентные орбиты)
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

            Spacer(modifier = Modifier.weight(0.5f))

            Column(
                modifier = Modifier
                    .widthIn(max = 360.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(scheme.accentSoft)
                        .border(0.8.dp, scheme.islandBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = title,
                        tint = scheme.accent,
                        modifier = Modifier.size(27.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = subtitle,
                    color = scheme.textSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

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
                    // Динамические точки только по факту ввода — без подсказки длины пароля
                    Box(
                        modifier = Modifier
                            .height(34.dp)
                            .animateContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (input.isEmpty()) {
                            Text(
                                text = "•  •  •",
                                color = scheme.textMuted.copy(alpha = 0.4f),
                                fontSize = 18.sp,
                                letterSpacing = 4.sp
                            )
                        } else {
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
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
                            keyboardActions = KeyboardActions(onDone = { submit() }),
                            cursorBrush = SolidColor(scheme.accent),
                            textStyle = TextStyle(
                                color = scheme.textPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
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

            Spacer(modifier = Modifier.weight(0.6f))

            if (selectedType == PasscodeType.PIN) {
                CustomNumericKeypad(
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
                    onConfirm = { submit() },
                    canConfirm = input.isNotEmpty(),
                    modifier = Modifier.padding(bottom = 28.dp)
                )
            } else {
                Button(
                    onClick = { submit() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.accent,
                        contentColor = scheme.onAccent
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .widthIn(max = 320.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 28.dp)
                        .height(48.dp)
                ) {
                    Text(
                        text = if (mode == PasscodeScreenMode.UNLOCK) {
                            if (isRu) "Разблокировать" else "Unlock"
                        } else if (firstStepPasscode == null) {
                            if (isRu) "Далее" else "Next"
                        } else {
                            if (isRu) "Сохранить" else "Save"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
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
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit,
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
            .widthIn(max = 280.dp)
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
                            fontSize = 24.sp,
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
            KeypadButton(onClick = onBackspace) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                    contentDescription = "Backspace",
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            KeypadButton(onClick = { onDigit("0") }) {
                Text(
                    text = "0",
                    color = scheme.textPrimary,
                    fontSize = 24.sp,
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
                    modifier = Modifier.size(24.dp)
                )
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
            .size(68.dp)
            .clip(CircleShape)
            .background(bg)
            .border(0.7.dp, scheme.islandBorder, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
