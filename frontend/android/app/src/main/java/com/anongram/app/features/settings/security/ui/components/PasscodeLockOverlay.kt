package com.anongram.app.features.settings.security.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.security.data.PasscodeStore

enum class PasscodeScreenMode {
    UNLOCK,
    CREATE
}

/**
 * Кастомный экран ввода или создания код-пароля в стиле текущей темы AnonGram.
 */
@Composable
fun PasscodeLockOverlay(
    mode: PasscodeScreenMode,
    subtitleOverride: String? = null,
    onSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    BackHandler(onBack = onBack)

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
            if (isRu) "Для доступа к защищённым данным введите ваш код-пароль"
            else "Enter your passcode to access protected data"
        firstStepPasscode == null ->
            if (isRu) "Минимум 4 символа (цифры или пароль)"
            else "At least 4 characters (PIN or password)"
        else ->
            if (isRu) "Введите код-пароль ещё раз для подтверждения"
            else "Re-enter your passcode to confirm"
    }
    val actionLabel = when {
        mode == PasscodeScreenMode.UNLOCK -> if (isRu) "Разблокировать" else "Unlock"
        firstStepPasscode == null -> if (isRu) "Далее" else "Next"
        else -> if (isRu) "Сохранить" else "Save"
    }

    fun submit() {
        val trimmed = input
        if (mode == PasscodeScreenMode.UNLOCK) {
            if (trimmed.isEmpty()) {
                errorText = if (isRu) "Введите код-пароль" else "Enter passcode"
                return
            }
            if (PasscodeStore.verifyPasscode(context, trimmed)) {
                input = ""
                errorText = null
                onSuccess()
            } else {
                input = ""
                errorText = if (isRu) "Неверный код-пароль" else "Incorrect passcode"
            }
        } else {
            if (firstStepPasscode == null) {
                if (trimmed.length < 4) {
                    errorText = if (isRu) "Минимум 4 символа" else "At least 4 characters"
                    return
                }
                firstStepPasscode = trimmed
                input = ""
                errorText = null
            } else {
                if (trimmed == firstStepPasscode) {
                    PasscodeStore.setPasscode(context, trimmed)
                    input = ""
                    firstStepPasscode = null
                    errorText = null
                    onSuccess()
                } else {
                    input = ""
                    firstStepPasscode = null
                    errorText = if (isRu) "Пароли не совпадают, попробуйте снова" else "Passcodes do not match, try again"
                }
            }
        }
    }

    AnonGramScreenSurface(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeStatusBarPadding()
                .safeNavigationBarPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = if (isRu) "Назад" else "Back",
                        tint = scheme.textPrimary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .widthIn(max = 360.dp)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(scheme.accentSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = title,
                        tint = scheme.accent,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = subtitle,
                    color = scheme.textSecondary,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.card)
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
                            text = if (isRu) "Код-пароль" else "Passcode",
                            color = scheme.textMuted,
                            fontSize = 16.sp
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

                if (errorText != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorText!!,
                        color = Color(0xFFEF4444),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { submit() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = scheme.accent,
                        contentColor = scheme.onAccent
                    ),
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
