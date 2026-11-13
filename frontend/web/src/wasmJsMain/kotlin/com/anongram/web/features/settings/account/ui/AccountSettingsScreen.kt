package com.anongram.web.features.settings.account.ui

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.ChatTextSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.features.auth.domain.model.UserIdentity
import com.anongram.web.features.settings.ui.SettingsGroupCard
import com.anongram.web.features.settings.ui.SettingsSubScreenScaffold
import com.anongram.web.platform.WebStorage

@Composable
fun AccountSettingsScreen(
    identity: UserIdentity?,
    displayName: String,
    handle: String,
    onProfileSaved: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU
    val clipboard = LocalClipboardManager.current

    var nameInput by remember(displayName) { mutableStateOf(displayName) }
    var handleInput by remember(handle) { mutableStateOf(handle) }
    var bioInput by remember {
        mutableStateOf(
            WebStorage.getString("anongram_profile_bio")
                ?: if (isRu) "Пост-квантовая приватность ML-DSA-87" else "Post-quantum privacy ML-DSA-87"
        )
    }
    var savedBanner by remember { mutableStateOf(false) }
    var copiedId by remember { mutableStateOf(false) }

    val publicId = identity?.publicId ?: "ag1_8f91c2a0b47d12e8"
    val masterVk = identity?.masterVkHex?.take(24) ?: "8f91c2a0b47d12e8903fa12b"

    SettingsSubScreenScaffold(
        title = if (isRu) "Аккаунт" else "Account",
        onBack = onBack,
        trailingAction = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(scheme.accent)
                    .clickable {
                        val cleanName = nameInput.trim().ifBlank { displayName }
                        val cleanHandle = handleInput.trim().removePrefix("@").uppercase().ifBlank { handle }
                        WebStorage.putString("anongram_profile_bio", bioInput.trim())
                        WebStorage.putString("anongram_auth_nickname", cleanName)
                        ChatTextSettingsStore.setRegisteredUserName(cleanName)
                        onProfileSaved(cleanName, cleanHandle)
                        savedBanner = true
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = if (isRu) "Сохранить" else "Save",
                    tint = scheme.onAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(scheme.accent, scheme.accentDim))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = nameInput.take(1).uppercase().ifBlank { "A" },
                    color = scheme.onAccent,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "@${handleInput.removePrefix("@").uppercase()}",
                color = scheme.accent,
                fontSize = 13.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (savedBanner) {
            Text(
                text = if (isRu) "Профиль обновлён" else "Profile updated",
                color = scheme.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 6.dp)
            )
        }

        SettingsGroupCard(title = if (isRu) "Имя и псевдоним" else "Name & Username") {
            ProfileField(
                label = if (isRu) "Отображаемое имя" else "Display name",
                value = nameInput,
                onValueChange = { nameInput = it; savedBanner = false }
            )
            ProfileField(
                label = if (isRu) "Имя пользователя (@handle)" else "Username (@handle)",
                value = handleInput,
                onValueChange = { handleInput = it.filter { c -> c.isLetterOrDigit() || c == '_' }; savedBanner = false }
            )
            ProfileField(
                label = if (isRu) "О себе" else "Bio",
                value = bioInput,
                onValueChange = { bioInput = it; savedBanner = false }
            )
        }

        SettingsGroupCard(title = if (isRu) "Криптографическая идентичность" else "Cryptographic Identity") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        clipboard.setText(AnnotatedString(publicId))
                        copiedId = true
                    }
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Public ID",
                        color = scheme.textSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = publicId,
                        color = scheme.textPrimary,
                        fontSize = 13.5.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(
                    text = if (isRu) "Публичный ключ ML-DSA-87" else "ML-DSA-87 Master Key",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "$masterVk...",
                    color = scheme.accent,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (copiedId) {
                Text(
                    text = if (isRu) "Public ID скопирован" else "Public ID copied",
                    color = scheme.accent,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = scheme.textSecondary,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(scheme.surface)
                .border(1.dp, scheme.cardBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.5.sp),
                cursorBrush = SolidColor(scheme.accent),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
