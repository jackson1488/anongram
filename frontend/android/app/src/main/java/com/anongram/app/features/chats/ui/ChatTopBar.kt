package com.anongram.app.features.chats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.state.SelfDestruct

/**
 * Верхняя панель чата — плавающие «таблетки»: назад · аватар + имя + статус
 * + метка шифрования + иконка таймера самоуничтожения · звонок · три точки.
 * Меню точек: Уведомления (подменю), Видеозвонок, Поиск, Изменить обои,
 * Ключи шифрования, Самоуничтожение, Очистить историю, Удалить чат (красным).
 */
@Composable
internal fun ChatTopBar(
    session: IChatSession,
    isPeerTyping: Boolean,
    peerActivity: String?,
    selfDestruct: SelfDestruct,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit,
    onCycleWallpaper: () -> Unit,
    onOpenKeyCheck: () -> Unit,
    onSelfDestruct: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteChat: () -> Unit,
    onOpenNotifications: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TopPill {
            CompactIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = "Назад",
                tint = TextSecondary,
                onClick = onBack
            )
        }

        TopPill(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeerAvatar(title = session.title, accent = scheme.accent)
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = session.title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Сквозное шифрование",
                            tint = scheme.accent,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = statusLine(isPeerTyping, peerActivity, session),
                            color = if (isPeerTyping || peerActivity != null) scheme.accent else TextMuted,
                            fontSize = 10.sp
                        )
                        if (selfDestruct != SelfDestruct.OFF) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = "Таймер самоуничтожения: ${selfDestruct.label}",
                                tint = FlameOrange,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = selfDestruct.label,
                                color = FlameOrange,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
        }

        TopPill {
            CompactIconButton(
                icon = Icons.Outlined.Call,
                description = "Видеозвонок",
                tint = TextSecondary,
                onClick = { /* звонки поедут вместе с сетевым слоем */ }
            )
        }

        TopPill {
            ChatMenu(
                onOpenSearch = onOpenSearch,
                onCycleWallpaper = onCycleWallpaper,
                onOpenKeyCheck = onOpenKeyCheck,
                onSelfDestruct = onSelfDestruct,
                onClearHistory = onClearHistory,
                onDeleteChat = onDeleteChat,
                onOpenNotifications = onOpenNotifications
            )
        }
    }
}

private fun statusLine(isPeerTyping: Boolean, peerActivity: String?, session: IChatSession): String =
    when {
        peerActivity != null -> peerActivity
        isPeerTyping -> "печатает…"
        session.isOnline -> "в сети"
        else -> "был(а) недавно"
    }

@Composable
private fun ChatMenu(
    onOpenSearch: () -> Unit,
    onCycleWallpaper: () -> Unit,
    onOpenKeyCheck: () -> Unit,
    onSelfDestruct: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteChat: () -> Unit,
    onOpenNotifications: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var notificationsSubmenu by remember { mutableStateOf(false) }

    Box {
        CompactIconButton(
            icon = Icons.Outlined.MoreVert,
            description = "Меню чата",
            tint = TextSecondary,
            onClick = {
                notificationsSubmenu = false
                menuOpen = true
            }
        )
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = {
                menuOpen = false
                notificationsSubmenu = false
            },
            containerColor = CardDark
        ) {
            if (notificationsSubmenu) {
                MenuAction(Icons.AutoMirrored.Outlined.ArrowBack, "Назад") {
                    notificationsSubmenu = false
                }
                MenuAction(Icons.Outlined.VolumeOff, "Выключить звук") { menuOpen = false }
                MenuAction(Icons.Outlined.Schedule, "Выключить на время…") { menuOpen = false }
                MenuAction(Icons.Outlined.Tune, "Настроить") {
                    menuOpen = false
                    onOpenNotifications()
                }
                MenuAction(Icons.Outlined.VolumeOff, "Выключить уведомления", tint = FlameOrange) {
                    menuOpen = false
                }
            } else {
                MenuAction(Icons.Outlined.Timer, "Уведомления") {
                    notificationsSubmenu = true
                }
                MenuAction(Icons.Outlined.Call, "Видеозвонок") { menuOpen = false }
                MenuAction(
                    Icons.Outlined.MoreVert,
                    "Поиск"
                ) {
                    menuOpen = false
                    onOpenSearch()
                }
                MenuAction(Icons.Outlined.Lock, "Ключи шифрования") {
                    menuOpen = false
                    onOpenKeyCheck()
                }
                MenuAction(Icons.Outlined.Timer, "Изменить обои") {
                    menuOpen = false
                    onCycleWallpaper()
                }
                MenuAction(Icons.Outlined.Schedule, "Самоуничтожение") {
                    menuOpen = false
                    onSelfDestruct()
                }
                MenuAction(Icons.Outlined.Tune, "Очистить историю") {
                    menuOpen = false
                    onClearHistory()
                }
                MenuAction(Icons.Outlined.VolumeOff, "Удалить чат", tint = FlameOrange) {
                    menuOpen = false
                    onDeleteChat()
                }
            }
        }
    }
}

@Composable
private fun MenuAction(
    icon: ImageVector,
    label: String,
    tint: Color = TextPrimary,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(text = label, color = tint, fontSize = 13.sp)
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (tint == TextPrimary) TextSecondary else tint,
                modifier = Modifier.size(17.dp)
            )
        },
        onClick = onClick
    )
}

@Composable
private fun TopPill(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(IslandBackground)
            .border(width = 1.dp, color = CardBorder, shape = RoundedCornerShape(50))
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun CompactIconButton(
    icon: ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun PeerAvatar(title: String, accent: Color) {
    val monogram = title.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
