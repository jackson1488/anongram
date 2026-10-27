package com.anongram.app.features.chats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary

/**
 * Панель режима выделения сообщений (шаг 10): счётчик «Выбрано: N»,
 * Копировать, Переслать, Удалить, Отмена.
 */
@Composable
internal fun SelectionPanel(
    selectedCount: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(50))
            .background(IslandBackground)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "Отмена выделения",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        Text(
            text = "Выбрано: $selectedCount",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 2.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Копировать",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onForward) {
            Icon(
                imageVector = Icons.Outlined.Reply,
                contentDescription = "Переслать",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Outlined.Delete,
                contentDescription = "Удалить",
                tint = FlameOrange,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
