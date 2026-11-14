package com.anongram.app.features.services.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.NeonEmerald
import com.anongram.app.features.services.ai.data.LocalAiStore
import com.anongram.app.features.services.ai.i18n.LocalAiI18n

/**
 * Хаб «Сервисы» (секция 2 из 5): Локальный ИИ, Onion VPN, Kill-Switch, OTP-блокнот.
 */
@Composable
fun ServicesScreen(
    onOpenLocalAi: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val aiStrings = LocalAiI18n.current
    val aiState by LocalAiStore.state.collectAsState()

    LaunchedEffect(Unit) {
        LocalAiStore.ensureInitialized(context)
    }

    val onionAccent = if (scheme.isDark) NeonEmerald else Color(0xFF059669)
    val killSwitchAccent = if (scheme.isDark) CrimsonKillSwitch else Color(0xFFDC2626)
    val otpAccent = if (scheme.isDark) FlameOrange else Color(0xFFEA580C)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (aiStrings.isRussian) "Сервисы Приватности" else "Privacy Services",
            fontWeight = FontWeight.Bold,
            color = scheme.textPrimary,
            fontSize = 16.sp
        )

        ServiceCard(
            icon = { accent ->
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            },
            title = aiStrings.title,
            accent = scheme.accent,
            body = if (aiState.installedModels.isNotEmpty()) {
                "${aiState.activeModelDisplayName} • ${aiStrings.reasoningLabel(aiState.reasoningLevel)}"
            } else {
                aiStrings.emptyHeroSubtitle
            },
            detail = if (aiStrings.isRussian) {
                "Чат, мысли, системный промпт, фото и файлы (llama.cpp)"
            } else {
                "Chat, reasoning, system prompt, photos & files (llama.cpp)"
            },
            onClick = onOpenLocalAi
        )

        ServiceCard(
            icon = { accent ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_onion),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            },
            title = "AnonGram Onion VPN",
            accent = onionAccent,
            body = "Статус: Активен · 3 луковых прыжка",
            detail = "Трафик замаскирован под TLS 1.3"
        )

        ServiceCard(
            icon = { accent ->
                Icon(
                    imageVector = Icons.Outlined.PowerSettingsNew,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            },
            title = "Kill-Switch",
            accent = killSwitchAccent,
            body = "Взведён",
            detail = "При обрыве туннеля весь трафик блокируется мгновенно"
        )

        ServiceCard(
            icon = { accent ->
                Icon(
                    painter = painterResource(id = R.drawable.ic_flame),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            },
            title = "OTP Блокнот",
            accent = otpAccent,
            body = "Осталось 7 страниц",
            detail = "Каждая страница сгорает сразу после прочтения"
        )
    }
}

@Composable
private fun ServiceCard(
    icon: @Composable (Color) -> Unit,
    title: String,
    body: String,
    detail: String,
    accent: Color,
    onClick: (() -> Unit)? = null
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.card)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            icon(accent)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, fontWeight = FontWeight.Bold, color = accent, fontSize = 14.sp)
            Spacer(modifier = Modifier.size(4.dp))
            Text(text = body, color = scheme.textPrimary, fontSize = 12.sp)
            Spacer(modifier = Modifier.size(2.dp))
            Text(text = detail, color = scheme.textSecondary, fontSize = 11.sp)
        }
    }
}
