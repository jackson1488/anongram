package com.anongram.app.features.profile.ui

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.CyberCyan
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.auth.data.crypto.KeystoreVault

/**
 * Раздел «Крипто-Профиль»: отображает локальную криптографическую идентичность,
 * деривированный Public ID, статус ключей Ed25519 и параметры Zero-Knowledge сессии.
 *
 * @param onLogout Колбэк при выходе из профиля со стиранием ключей.
 * @param modifier Модификатор макета.
 */
@Composable
fun ProfileScreen(
    onLogout: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vault = remember { KeystoreVault(context) }
    val identity = remember { vault.loadIdentity() }

    val nickname = identity?.nickname ?: "AnonUser"
    val displayPublicId = if (identity != null && identity.publicId.length >= 16) {
        "0x${identity.publicId.take(6)}…${identity.publicId.takeLast(6)}"
    } else {
        "0x4D9F…88E1"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Крипто-Профиль",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 16.sp
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(SurfaceDark)
                .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(CyberCyan.copy(alpha = 0.15f))
                    .border(1.dp, CyberCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_stat_anongram),
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = nickname,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$displayPublicId (Ed25519)",
                    color = CyberCyan,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        ProfileRow(title = "Seed-фраза", value = "12 слов · BIP-39 · 100% Offline")
        ProfileRow(title = "Квантовая защита", value = "Гибридный бандл ML-DSA-87")
        ProfileRow(title = "Верификация", value = "Blind Relay Hash · SHA-256")
        ProfileRow(title = "Аккаунт", value = "Без номера телефона, email и паролей")

        if (onLogout != null) {
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF241017))
                    .border(1.dp, CrimsonKillSwitch.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    .clickable { onLogout() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Выйти из аккаунта (стереть ключи)",
                    color = CrimsonKillSwitch,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileRow(title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, CardBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column {
            Text(text = title, fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
