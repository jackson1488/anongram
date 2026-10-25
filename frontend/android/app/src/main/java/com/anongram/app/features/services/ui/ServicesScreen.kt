package com.anongram.app.features.services.ui

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
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.NeonEmerald
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary

/**
 * Хаб «Сервисы» (секция 2 из 5): Onion VPN, Kill-Switch, OTP-блокнот.
 * Иконки — фирменные векторные ассеты (луковые слои, пламя OTP);
 * системные — Material. Управление туннелем и статистика трафика
 * приезжают отдельным VPN-модулем.
 */
@Composable
fun ServicesScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Сервисы Приватности",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 16.sp
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
            accent = NeonEmerald,
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
            accent = CrimsonKillSwitch,
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
            accent = FlameOrange,
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
    accent: Color
) {
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
            Text(text = body, color = TextPrimary, fontSize = 12.sp)
            Spacer(modifier = Modifier.size(2.dp))
            Text(text = detail, color = TextSecondary, fontSize = 11.sp)
        }
    }
}
