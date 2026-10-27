package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary

/**
 * Мини-диалог управления уведомлениями чата (Telegram-style):
 * - Выключить / включить звук
 * - Без звука на 1 час, 8 часов, 2 дня
 * - Настроить (открывает полноэкранные «Особые уведомления»)
 */
@Composable
internal fun NotificationsMuteDialog(
    peerName: String,
    onMuteFor: (hours: Int) -> Unit,
    onOpenCustomSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardDark)
                .padding(vertical = 16.dp)
        ) {
            Text(
                text = "Уведомления",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            MuteOptionRow(
                icon = Icons.Outlined.NotificationsOff,
                title = "Выключить звук",
                onClick = {
                    onMuteFor(-1)
                    onDismiss()
                }
            )

            MuteOptionRow(
                icon = Icons.Outlined.Schedule,
                title = "Без звука на 1 час",
                onClick = {
                    onMuteFor(1)
                    onDismiss()
                }
            )

            MuteOptionRow(
                icon = Icons.Outlined.Schedule,
                title = "Без звука на 8 часов",
                onClick = {
                    onMuteFor(8)
                    onDismiss()
                }
            )

            MuteOptionRow(
                icon = Icons.Outlined.Schedule,
                title = "Без звука на 2 дня",
                onClick = {
                    onMuteFor(48)
                    onDismiss()
                }
            )

            HorizontalDivider(
                color = CardBorder,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            MuteOptionRow(
                icon = Icons.Outlined.Tune,
                title = "Настроить",
                subtitle = "Особые уведомления для $peerName",
                onClick = {
                    onOpenCustomSettings()
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Отмена", color = TextSecondary, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun MuteOptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = LocalAnonGramScheme.current.accent,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }
    }
}
