package com.anongram.web.features.profile.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.CrimsonKillSwitch
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.theme.NeonEmerald
import com.anongram.web.core.ui.AnonGramIslandCard

@Composable
fun ProfileScreen(
    nickname: String = "AnonUser",
    publicId: String = "0x4D9F…88E1",
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val language by LanguageSettingsStore.language.collectAsState()
    val isRu = language == AppLanguage.RU

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isRu) "Крипто-Профиль" else "Crypto Profile",
            fontWeight = FontWeight.Bold,
            color = scheme.textPrimary,
            fontSize = 16.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.card)
                .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.accentSoft)
                    .border(1.dp, scheme.accent.copy(alpha = 0.7f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Shield, contentDescription = null, tint = scheme.accent, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(nickname, color = scheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Public ID: $publicId", color = scheme.accent, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text("Ed25519 · ML-KEM-1024", color = NeonEmerald, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        AnonGramIslandCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Key, contentDescription = null, tint = scheme.accent, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(if (isRu) "Ключи устройства" else "Device Keys", color = scheme.textPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Text(
                    text = if (isRu) "Приватные ключи не покидают локальное хранилище. Нулевая метаинформация." else "Private keys never leave local storage. Zero metadata.",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VerifiedUser, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Zero-Knowledge Session Active", color = NeonEmerald, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (onLogout != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(CrimsonKillSwitch.copy(alpha = 0.12f))
                    .border(1.dp, CrimsonKillSwitch.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                    .clickable(onClick = onLogout)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Outlined.Logout, contentDescription = null, tint = CrimsonKillSwitch, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRu) "Стереть ключи и выйти" else "Wipe Keys & Logout",
                    color = CrimsonKillSwitch,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}
