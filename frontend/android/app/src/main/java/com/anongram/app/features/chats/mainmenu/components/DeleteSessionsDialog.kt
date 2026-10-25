package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.direct.DirectChatSession
import com.anongram.app.features.chats.group.GroupChatSession
import com.anongram.app.features.chats.otp.OtpBurnChatSession

/**
 * Диалог подтверждения удаления (Telegram-style):
 * группа — «Покинуть чат», 1-на-1 — «Удалить чат» с галочкой
 * «Также удалить у собеседника», OTP — «Сжечь пад», мульти — «Удалить чаты».
 */
@Composable
internal fun DeleteSessionsDialog(
    sessions: List<IChatSession>,
    onConfirm: (alsoForPeer: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var alsoForPeer by remember { mutableStateOf(false) }

    val single = sessions.singleOrNull()
    val title = when {
        single != null -> "${single.deleteActionLabel}?"
        else -> "Удалить чаты?"
    }
    val message = when {
        single is GroupChatSession -> "Вы покинете этот чат. История останется у участников."
        single is OtpBurnChatSession -> "Неиспользованные страницы пада будут сожжены безвозвратно."
        single is DirectChatSession -> "Чат будет удалён только у вас."
        else -> "Выбрано чатов: ${sessions.size}"
    }
    val confirmLabel = when (single) {
        is GroupChatSession -> "Покинуть"
        is OtpBurnChatSession -> "Сжечь"
        else -> "Удалить"
    }
    val showPeerCheckbox = single is DirectChatSession

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = {
            Text(text = title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            androidx.compose.foundation.layout.Column {
                Text(text = message, color = TextSecondary, fontSize = 13.sp)
                if (showPeerCheckbox) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { alsoForPeer = !alsoForPeer }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = alsoForPeer,
                            onCheckedChange = { alsoForPeer = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = scheme.accent,
                                uncheckedColor = TextSecondary
                            )
                        )
                        Text(
                            text = "Также удалить у собеседника",
                            color = TextPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(alsoForPeer) }) {
                Text(text = confirmLabel, color = FlameOrange, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена", color = TextSecondary)
            }
        }
    )
}
