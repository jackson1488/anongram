package com.anongram.app.features.chats.messages.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.common.IChatSession

/**
 * Тулбар чата (как в Telegram): назад, аватар, имя и статус
 * («в сети» / «был(а) недавно» / «печатает…»), меню с очисткой истории.
 */
@Composable
internal fun ChatTopBar(
    session: IChatSession,
    isPeerTyping: Boolean,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    var menuOpen by remember { mutableStateOf(false) }
    val statusText = when {
        isPeerTyping -> "печатает…"
        session.isOnline -> "в сети"
        else -> "был(а) недавно"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(com.anongram.app.core.theme.ObsidianBlack)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Назад",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        PeerAvatar(title = session.title, accent = scheme.accent)
        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = session.title,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = statusText,
                color = if (isPeerTyping) scheme.accent else TextMuted,
                fontSize = 11.sp
            )
        }

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "Меню чата",
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                containerColor = com.anongram.app.core.theme.CardDark
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Очистить историю",
                            color = FlameOrange,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.CleaningServices,
                            contentDescription = null,
                            tint = FlameOrange,
                            modifier = Modifier.size(17.dp)
                        )
                    },
                    onClick = {
                        menuOpen = false
                        onClearHistory()
                    }
                )
            }
        }
    }
}

@Composable
private fun PeerAvatar(title: String, accent: androidx.compose.ui.graphics.Color) {
    val monogram = title.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.75f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
