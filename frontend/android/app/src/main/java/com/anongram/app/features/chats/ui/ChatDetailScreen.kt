package com.anongram.app.features.chats.ui

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.core.theme.CyberCyan
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.common.IChatSession

/** Пузырь исходящего сообщения: тёмно-синий корпус в акцентной рамке стратегии. */
private val OutgoingBubbleBackground = Color(0xFF0C243B)

/**
 * Открытый чат. Единый для всех стратегий экран: шапка (имя, уровень шифрования,
 * отпечаток) строится полиморфно из IChatSession; специфика Direct/Group/OTP
 * рендерится их собственными компонентами в следующих итерациях.
 */
@Composable
fun ChatDetailScreen(
    session: IChatSession,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = session.accentColor

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .safeStatusBarPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = CyberCyan,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onBack)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${session.securityLabel} · ${session.fingerprint}",
                    color = accent,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
                contentAlignment = Alignment.BottomStart
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceDark)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "Сквозное шифрование активно · ${session.securityLabel}",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(RoundedCornerShape(16.dp))
                        .background(OutgoingBubbleBackground)
                        .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = session.lastMessagePreview,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
