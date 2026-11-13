package com.anongram.web.features.services.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.CrimsonKillSwitch
import com.anongram.web.core.theme.FlameOrange
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.theme.NeonEmerald

@Composable
fun ServicesScreen(modifier: Modifier = Modifier) {
    val scheme = LocalAnonGramScheme.current
    val language by LanguageSettingsStore.language.collectAsState()
    val isRu = language == AppLanguage.RU

    val onionAccent = if (scheme.isDark) NeonEmerald else Color(0xFF059669)
    val killSwitchAccent = if (scheme.isDark) CrimsonKillSwitch else Color(0xFFDC2626)
    val otpAccent = if (scheme.isDark) FlameOrange else Color(0xFFEA580C)

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isRu) "Сервисы Приватности" else "Privacy Services",
            fontWeight = FontWeight.Bold,
            color = scheme.textPrimary,
            fontSize = 16.sp
        )

        ServiceCard(
            icon = { accent ->
                Icon(Icons.Outlined.Hub, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            },
            title = "AnonGram Onion VPN",
            accent = onionAccent,
            body = if (isRu) "Статус: Активен · 3 луковых прыжка" else "Status: Active · 3 onion hops",
            detail = if (isRu) "Трафик замаскирован под TLS 1.3" else "Traffic disguised as TLS 1.3"
        )

        ServiceCard(
            icon = { accent ->
                Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            },
            title = "Kill-Switch",
            accent = killSwitchAccent,
            body = if (isRu) "Взведён" else "Armed",
            detail = if (isRu) "При обрыве туннеля весь трафик блокируется мгновенно" else "All traffic blocked immediately on tunnel drop"
        )

        ServiceCard(
            icon = { accent ->
                Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
            },
            title = if (isRu) "OTP-Блокнот (Physical Pad)" else "OTP Physical Pad",
            accent = otpAccent,
            body = if (isRu) "Идеальная секретность · 7 страниц" else "Perfect secrecy · 7 pages",
            detail = if (isRu) "Страницы сгорают сразу после прочтения" else "Pages burn immediately after reading"
        )
    }
}

@Composable
private fun ServiceCard(
    icon: @Composable (Color) -> Unit,
    title: String,
    accent: Color,
    body: String,
    detail: String
) {
    val scheme = LocalAnonGramScheme.current

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
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.14f))
                .border(1.dp, accent.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon(accent)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = scheme.textPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(body, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = scheme.textSecondary, fontSize = 11.sp)
        }
    }
}
