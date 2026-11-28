package com.anongram.app.features.settings.account.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.auth.data.crypto.KeystoreVault
import com.anongram.app.features.profile.i18n.ProfileI18n
import com.anongram.app.features.settings.account.ui.components.BirthdayWheelSheet
import com.anongram.app.features.settings.account.ui.components.formatBirthdayWithAge
import kotlinx.coroutines.delay

/**
 * Экран редактирования аккаунта (чистый дизайн без рамок полей, по референсу):
 * - «Ваше имя» (Имя + Фамилия);
 * - «О себе» со счётчиком символов;
 * - «Информация о Вас» (AnonGram ID, @username -> отдельный экран, День рождения -> шторка с 3D-барабаном);
 * - Красная кнопка «Выход» внизу с подтверждением.
 */
@Composable
fun AccountSettingsScreen(
    initialDisplayName: String,
    initialHandle: String,
    onSaveAccount: (displayName: String, handle: String) -> Unit,
    onBack: () -> Unit,
    onLogout: (() -> Unit)? = null,
    initialBio: String? = null,
    onProfileMetaSaved: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    val appLanguage by LanguageSettingsStore.language.collectAsState()
    val isRu = appLanguage == AppLanguage.RU

    val prefs = remember {
        context.applicationContext.getSharedPreferences("anongram_profile_meta", Context.MODE_PRIVATE)
    }
    val identity = remember { KeystoreVault(context).loadIdentity() }
    val rawPublicId = identity?.publicId ?: "0x4D9F88E1A7C2"
    val displayPublicId = if (rawPublicId.length >= 14) {
        "0x${rawPublicId.take(6)}…${rawPublicId.takeLast(6)}"
    } else {
        rawPublicId
    }

    val savedFullName = remember(initialDisplayName) {
        prefs.getString("display_name", null) ?: initialDisplayName
    }
    val initialParts = remember(savedFullName) {
        val parts = savedFullName.trim().split(" ", limit = 2)
        parts.getOrElse(0) { "" } to parts.getOrElse(1) { "" }
    }

    var firstName by remember { mutableStateOf(prefs.getString("first_name", null) ?: initialParts.first) }
    var lastName by remember { mutableStateOf(prefs.getString("last_name", null) ?: initialParts.second) }
    var handle by remember {
        mutableStateOf((prefs.getString("handle", null) ?: initialHandle).removePrefix("@"))
    }
    var bio by remember {
        mutableStateOf(prefs.getString("bio", null) ?: initialBio ?: "")
    }

    var bDay by remember { mutableStateOf(prefs.getInt("birthday_day", 0).takeIf { it > 0 }) }
    var bMonth by remember { mutableStateOf(prefs.getInt("birthday_month", 0).takeIf { it > 0 }) }
    var bYear by remember { mutableStateOf(prefs.getInt("birthday_year", 0).takeIf { it > 0 }) }

    var showUsernameEditor by remember { mutableStateOf(false) }
    var showBirthdaySheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var bannerText by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(bannerText) {
        if (bannerText != null) {
            delay(2000)
            bannerText = null
        }
    }

    BackHandler {
        when {
            showBirthdaySheet -> showBirthdaySheet = false
            showUsernameEditor -> showUsernameEditor = false
            else -> onBack()
        }
    }

    AnonGramScreenSurface(
        applySystemBarsPadding = false,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeStatusBarPadding()
            ) {
                // Верхний бар: ← | Аккаунт | Сохранить
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(scheme.card)
                            .border(0.8.dp, scheme.cardBorder, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = strings.back,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = strings.accountTitle,
                        color = scheme.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(scheme.accent)
                            .clickable {
                                val cleanFirst = firstName.trim().ifBlank { initialParts.first.ifBlank { "AnonUser" } }
                                val cleanLast = lastName.trim()
                                val combinedName = if (cleanLast.isNotBlank()) "$cleanFirst $cleanLast" else cleanFirst
                                val cleanHandle = handle.trim().removePrefix("@").ifBlank { "ANONUSER" }
                                val cleanBio = bio.trim()
                                val birthdayFormatted = if (bDay != null && bMonth != null && bYear != null) {
                                    formatBirthdayWithAge(bDay!!, bMonth!!, bYear!!, isRu)
                                } else {
                                    strings.defaultBirthday
                                }

                                val normalizedHandle = "@${cleanHandle.uppercase()}"
                                val storedBirthday = if (bDay != null && bMonth != null && bYear != null) birthdayFormatted else ""
                                com.anongram.app.features.profile.data.ProfileMetaStore.saveFields(
                                    context = context,
                                    displayName = combinedName,
                                    username = normalizedHandle,
                                    bio = cleanBio,
                                    birthday = storedBirthday
                                )

                                prefs.edit().apply {
                                    putString("first_name", cleanFirst)
                                    putString("last_name", cleanLast)
                                    if (bDay != null && bMonth != null && bYear != null) {
                                        putInt("birthday_day", bDay!!)
                                        putInt("birthday_month", bMonth!!)
                                        putInt("birthday_year", bYear!!)
                                    } else {
                                        remove("birthday_day")
                                        remove("birthday_month")
                                        remove("birthday_year")
                                    }
                                    apply()
                                }

                                onSaveAccount(combinedName, cleanHandle)
                                onProfileMetaSaved?.invoke()
                                onBack()
                            }
                            .padding(horizontal = 16.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = strings.save,
                            color = scheme.onAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp)
                        .safeNavigationBarPadding()
                        .padding(bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Карточка «Ваше имя» (чистые поля без рамок)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.card)
                            .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Text(
                            text = strings.yourNameSection,
                            color = scheme.accent,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        BorderlessAccountInput(
                            value = firstName,
                            onValueChange = { firstName = it },
                            placeholder = strings.firstNameHint
                        )
                        HorizontalDivider(
                            color = scheme.divider,
                            thickness = 0.6.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                        BorderlessAccountInput(
                            value = lastName,
                            onValueChange = { lastName = it },
                            placeholder = strings.lastNameHint
                        )
                    }

                    // 2. Карточка «О себе» со счётчиком символов
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.card)
                                .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BorderlessAccountInput(
                                    value = bio,
                                    onValueChange = { if (it.length <= 70) bio = it },
                                    placeholder = strings.bioLabel,
                                    singleLine = false
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = (70 - bio.length).toString(),
                                color = scheme.textMuted,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.bioFooter,
                            color = scheme.textMuted,
                            fontSize = 12.5.sp,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }

                    // 3. Карточка «Информация о Вас»
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.card)
                            .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = strings.infoAboutYouSection,
                            color = scheme.accent,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        AccountInfoRow(
                            icon = Icons.Outlined.Key,
                            iconBg = Color(0xFF34C759),
                            title = displayPublicId,
                            subtitle = strings.accountIdSubtitle,
                            onClick = {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("AnonGram ID", rawPublicId))
                                bannerText = strings.copiedToClipboard
                            }
                        )

                        AccountInfoRow(
                            icon = Icons.Outlined.AlternateEmail,
                            iconBg = Color(0xFFFF9F0A),
                            title = "@${handle.uppercase()}",
                            subtitle = strings.tapToChangeUsername,
                            onClick = { showUsernameEditor = true }
                        )

                        val birthdayDisplay = if (bDay != null && bMonth != null && bYear != null) {
                            formatBirthdayWithAge(bDay!!, bMonth!!, bYear!!, isRu)
                        } else {
                            strings.defaultBirthday
                        }
                        AccountInfoRow(
                            icon = Icons.Outlined.Cake,
                            iconBg = Color(0xFF0A84FF),
                            title = birthdayDisplay,
                            subtitle = strings.tapToChangeBirthday,
                            onClick = { showBirthdaySheet = true }
                        )
                    }

                    // 4. Кнопка «Выход» внизу
                    if (onLogout != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.card)
                                .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                                .clickable { showLogoutDialog = true }
                                .padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                tint = CrimsonKillSwitch,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = strings.logout,
                                color = CrimsonKillSwitch,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Отдельный экран редактирования @username
            AnimatedVisibility(
                visible = showUsernameEditor,
                enter = slideInVertically { it / 4 } + fadeIn(),
                exit = slideOutVertically { it / 6 } + fadeOut()
            ) {
                UsernameEditorScreen(
                    initialUsername = handle,
                    onSave = { newHandle ->
                        handle = newHandle
                        showUsernameEditor = false
                    },
                    onBack = { showUsernameEditor = false }
                )
            }

            // Шторка 3D-барабана дня рождения
            if (showBirthdaySheet) {
                BirthdayWheelSheet(
                    initialDay = bDay,
                    initialMonth = bMonth,
                    initialYear = bYear,
                    onSave = { d, m, y ->
                        bDay = d
                        bMonth = m
                        bYear = y
                        showBirthdaySheet = false
                    },
                    onDelete = if (bDay != null) {
                        {
                            bDay = null
                            bMonth = null
                            bYear = null
                            showBirthdaySheet = false
                        }
                    } else null,
                    onDismiss = { showBirthdaySheet = false }
                )
            }

            // Диалог подтверждения выхода
            if (showLogoutDialog && onLogout != null) {
                AlertDialog(
                    onDismissRequest = { showLogoutDialog = false },
                    containerColor = scheme.card,
                    title = {
                        Text(
                            text = if (isRu) "Выйти из аккаунта?" else "Log out?",
                            color = scheme.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Text(
                            text = if (isRu) {
                                "Локальные криптографические ключи и сеанс на этом устройстве будут очищены."
                            } else {
                                "Local cryptographic keys and session on this device will be cleared."
                            },
                            color = scheme.textSecondary,
                            fontSize = 14.5.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showLogoutDialog = false
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CrimsonKillSwitch,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(text = strings.logout, fontWeight = FontWeight.SemiBold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { showLogoutDialog = false },
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Text(text = strings.cancel, color = scheme.textPrimary)
                        }
                    }
                )
            }

            // Кастомное тематическое уведомление снизу
            AnimatedVisibility(
                visible = bannerText != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeNavigationBarPadding()
                    .padding(horizontal = 20.dp, vertical = 22.dp)
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.card)
                        .border(0.9.dp, scheme.cardBorder, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircleOutline,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = bannerText.orEmpty(),
                        color = scheme.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun BorderlessAccountInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean = true
) {
    val scheme = LocalAnonGramScheme.current
    Box(modifier = Modifier.fillMaxWidth()) {
        if (value.isEmpty()) {
            Text(
                text = placeholder,
                color = scheme.textMuted,
                fontSize = 15.5.sp
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            cursorBrush = SolidColor(scheme.accent),
            textStyle = TextStyle(
                color = scheme.textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Normal
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AccountInfoRow(
    icon: ImageVector,
    iconBg: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = scheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = scheme.textMuted,
                fontSize = 12.5.sp
            )
        }
    }
}
