package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary

/**
 * Диалог удаления сообщений (Telegram-style):
 * - Заголовок: «Удалить сообщение» / «Удалить N сообщений»
 * - Пояснение: «Вы точно хотите удалить это сообщение?»
 * - Чекбокс: «Также удалить для {Имя собеседника}»
 * - Кнопки: «Отмена» и «Удалить»
 */
@Composable
internal fun DeleteMessageDialog(
    count: Int,
    peerName: String = "",
    onConfirm: (forEveryone: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var forEveryone by remember { mutableStateOf(false) }
    val scheme = LocalAnonGramScheme.current

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(CardDark)
                .padding(20.dp)
        ) {
            Text(
                text = if (count > 1) "Удалить $count сообщений" else "Удалить сообщение",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (count > 1) "Вы точно хотите удалить эти сообщения?" else "Вы точно хотите удалить это сообщение?",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            val checkboxLabel = if (peerName.isNotBlank()) "Также удалить для $peerName" else "Также удалить для всех"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { forEveryone = !forEveryone }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = forEveryone,
                    onCheckedChange = { forEveryone = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = scheme.accent,
                        uncheckedColor = TextSecondary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = checkboxLabel,
                    color = TextPrimary,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(text = "Отмена", color = TextSecondary, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = { onConfirm(forEveryone) }) {
                    Text(
                        text = "Удалить",
                        color = Color(0xFFFF5252),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
