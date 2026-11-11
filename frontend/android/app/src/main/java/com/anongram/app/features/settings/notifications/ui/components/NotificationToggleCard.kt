package com.anongram.app.features.settings.notifications.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Карточка переключателей уведомлений (ЛС, группы, скрытие текста в пушах).
 * Изолированный компонент модуля `features/settings/notifications/`.
 */
@Composable
fun NotificationToggleCard(
    privateChatsEnabled: Boolean,
    onPrivateChatsChange: (Boolean) -> Unit,
    groupsEnabled: Boolean,
    onGroupsChange: (Boolean) -> Unit,
    hidePreviewContent: Boolean,
    onHidePreviewContentChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1B2331))
            .padding(16.dp)
    ) {
        NotificationRow(
            title = "Личные чаты",
            subtitle = "Мгновенные зашифрованные уведомления",
            checked = privateChatsEnabled,
            onCheckedChange = onPrivateChatsChange
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(12.dp))

        NotificationRow(
            title = "Группы и каналы",
            subtitle = "Уведомления из подписок и сообществ",
            checked = groupsEnabled,
            onCheckedChange = onGroupsChange
        )
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(12.dp))

        NotificationRow(
            title = "Скрывать текст сообщений на экране блокировки",
            subtitle = "Показывать только «Новое сообщение AnonGram»",
            checked = hidePreviewContent,
            onCheckedChange = onHidePreviewContentChange
        )
    }
}

@Composable
private fun NotificationRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color(0xFF8E99A8),
                fontSize = 12.sp
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF8B5CF6)
            )
        )
    }
}
