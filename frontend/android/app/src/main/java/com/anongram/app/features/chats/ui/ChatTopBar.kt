package com.anongram.app.features.chats.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.NotificationsPaused
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import com.anongram.app.features.chats.ui.components.MuteDurationCustomBottomSheet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.messages.state.SelfDestruct

private val AccentViolet = Color(0xFF7A4DF3)

/**
 * Верхняя панель чата — парящие закругленные «таблетки»:
 * - Слева: кнопка назад
 * - По центру: аватар + имя + нежный статус «в сети»
 * - Справа: аудиозвонок и три точки ВМЕСТЕ в одной таблетке.
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
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Таблетка 1: Назад
        TopPill {
            CompactIconButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                description = "Назад",
                tint = TextSecondary,
                onClick = onBack
            )
        }

        // Таблетка 2: Аватар + Имя + Нежный статус
        TopPill(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
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
                            color = if (isPeerTyping || peerActivity != null || session.isOnline) {
                                scheme.accent.copy(alpha = 0.85f)
                            } else {
                                TextMuted
                            },
                            fontSize = 11.sp
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

        // Таблетка 3: Аудиозвонок и Три точки ВМЕСТЕ
        TopPill {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                CompactIconButton(
                    painter = painterResource(id = R.drawable.ic_phone),
                    description = "Аудиозвонок",
                    tint = TextSecondary,
                    onClick = { /* Аудиозвонок */ }
                )
                ChatMenu(
                    session = session,
                    accent = scheme.accent,
                    onOpenSearch = onOpenSearch,
                    onCycleWallpaper = onCycleWallpaper,
                    onOpenNotifications = onOpenNotifications,
                    onClearHistory = onClearHistory,
                    onDeleteChat = onDeleteChat
                )
            }
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

/**
 * Меню чата (три точки):
 * Раскрывается ПРЯМО ПОВЕРХ самой кнопки трёх точек (Telegram-style).
 */
@Composable
private fun ChatMenu(
    session: IChatSession,
    accent: Color,
    onOpenSearch: () -> Unit,
    onCycleWallpaper: () -> Unit,
    onOpenNotifications: () -> Unit,
    onClearHistory: () -> Unit,
    onDeleteChat: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var isNotificationsSubmenu by remember { mutableStateOf(false) }
    var showMuteDurationSheet by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showDeleteChatDialog by remember { mutableStateOf(false) }

    Box {
        CompactIconButton(
            icon = Icons.Outlined.MoreVert,
            description = "Меню чата",
            tint = TextSecondary,
            onClick = {
                isNotificationsSubmenu = false
                menuOpen = true
            }
        )

        // Меню перекрывает саму кнопку трёх точек (offset y = -44.dp)
        DropdownMenu(
            expanded = menuOpen,
            onDismissRequest = {
                menuOpen = false
                isNotificationsSubmenu = false
            },
            offset = DpOffset(x = 0.dp, y = (-44).dp),
            shape = RoundedCornerShape(16.dp),
            containerColor = Color(0xF2161E2D),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            if (!isNotificationsSubmenu) {
                // 1. Уведомления > (Фото 1)
                MenuAction(
                    icon = Icons.Outlined.VolumeUp,
                    label = "Уведомления",
                    trailing = Icons.AutoMirrored.Outlined.KeyboardArrowRight
                ) {
                    isNotificationsSubmenu = true
                }

                // 2. Видеозвонок
                MenuAction(
                    painter = painterResource(id = R.drawable.ic_video),
                    label = "Видеозвонок"
                ) {
                    menuOpen = false
                }

                // 3. Поиск
                MenuAction(
                    painter = painterResource(id = R.drawable.ic_search),
                    label = "Поиск"
                ) {
                    menuOpen = false
                    onOpenSearch()
                }

                // 4. Изменить обои
                MenuAction(
                    painter = painterResource(id = R.drawable.ic_gallery_horizontal_end),
                    label = "Изменить обои"
                ) {
                    menuOpen = false
                    onCycleWallpaper()
                }

                // 5. Очистить историю
                MenuAction(
                    painter = painterResource(id = R.drawable.ic_broom),
                    label = "Очистить историю"
                ) {
                    menuOpen = false
                    showClearHistoryDialog = true
                }

                // 6. Удалить чат
                MenuAction(
                    painter = painterResource(id = R.drawable.ic_trash),
                    label = "Удалить чат",
                    tint = Color(0xFFFF5252)
                ) {
                    menuOpen = false
                    showDeleteChatDialog = true
                }
            } else {
                // Подменю «Уведомления» (Фото 2)
                // 1. ← Назад
                MenuAction(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    label = "Назад"
                ) {
                    isNotificationsSubmenu = false
                }

                // 2. Выключить звук
                MenuAction(
                    icon = Icons.Outlined.VolumeOff,
                    label = "Выключить звук"
                ) {
                    menuOpen = false
                    isNotificationsSubmenu = false
                }

                // 3. Выключить на время... (Фото 3)
                MenuAction(
                    icon = Icons.Outlined.NotificationsPaused,
                    label = "Выключить на время…"
                ) {
                    menuOpen = false
                    isNotificationsSubmenu = false
                    showMuteDurationSheet = true
                }

                // 4. Настроить
                MenuAction(
                    icon = Icons.Outlined.Tune,
                    label = "Настроить"
                ) {
                    menuOpen = false
                    isNotificationsSubmenu = false
                    onOpenNotifications()
                }

                // 5. Выключить уведомления (красным)
                MenuAction(
                    icon = Icons.Outlined.NotificationsOff,
                    label = "Выключить уведомления",
                    tint = Color(0xFFFF5252)
                ) {
                    menuOpen = false
                    isNotificationsSubmenu = false
                }
            }
        }
    }

    // Bottom-sheet выбора времени отключения уведомлений (Фото 3)
    if (showMuteDurationSheet) {
        MuteDurationCustomBottomSheet(
            onConfirm = { _ ->
                showMuteDurationSheet = false
            },
            onDismiss = { showMuteDurationSheet = false }
        )
    }

    // Диалог подтверждения очистки истории
    if (showClearHistoryDialog) {
        ClearHistoryDialog(
            peerName = session.title,
            onConfirm = { _ ->
                showClearHistoryDialog = false
                onClearHistory()
            },
            onDismiss = { showClearHistoryDialog = false }
        )
    }

    // Диалог подтверждения удаления чата с аватаркой сверху
    if (showDeleteChatDialog) {
        DeleteChatDialog(
            peerName = session.title,
            accent = accent,
            onConfirm = { _ ->
                showDeleteChatDialog = false
                onDeleteChat()
            },
            onDismiss = { showDeleteChatDialog = false }
        )
    }
}

/**
 * Диалог подтверждения очистки истории (без аватарки, только заголовок и имя).
 */
@Composable
private fun ClearHistoryDialog(
    peerName: String,
    onConfirm: (alsoDeleteForPeer: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var alsoDeleteForPeer by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF161E2D),
        title = {
            Text(
                text = "Очистить историю",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Вы точно хотите очистить историю сообщений с $peerName?",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { alsoDeleteForPeer = !alsoDeleteForPeer }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = alsoDeleteForPeer,
                        onCheckedChange = { alsoDeleteForPeer = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentViolet,
                            checkmarkColor = Color.White,
                            uncheckedColor = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Также очистить для $peerName",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(alsoDeleteForPeer) }) {
                Text(
                    text = "Очистить",
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена", color = TextSecondary)
            }
        }
    )
}

/**
 * Диалог удаления чата (сверху аватарка, затем имя, подтверждение и чекбокс).
 */
@Composable
private fun DeleteChatDialog(
    peerName: String,
    accent: Color,
    onConfirm: (alsoDeleteForPeer: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var alsoDeleteForPeer by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF161E2D),
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                PeerAvatar(title = peerName, accent = accent, size = 48.dp)
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Удалить чат",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Вы точно хотите удалить чат с $peerName?",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { alsoDeleteForPeer = !alsoDeleteForPeer }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = alsoDeleteForPeer,
                        onCheckedChange = { alsoDeleteForPeer = it },
                        colors = CheckboxDefaults.colors(
                            checkedColor = AccentViolet,
                            checkmarkColor = Color.White,
                            uncheckedColor = TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Также удалить для $peerName",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(alsoDeleteForPeer) }) {
                Text(
                    text = "Удалить",
                    color = Color(0xFFFF5252),
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Отмена", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun MenuAction(
    label: String,
    painter: Painter? = null,
    icon: ImageVector? = null,
    tint: Color = TextPrimary,
    trailing: ImageVector? = null,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(text = label, color = tint, fontSize = 14.sp, fontWeight = FontWeight.Normal)
        },
        leadingIcon = {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (tint == TextPrimary) TextSecondary else tint,
                    modifier = Modifier.size(19.dp)
                )
            } else if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    tint = if (tint == TextPrimary) TextSecondary else tint,
                    modifier = Modifier.size(19.dp)
                )
            }
        },
        trailingIcon = if (trailing != null) {
            {
                Icon(
                    imageVector = trailing,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null,
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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
private fun CompactIconButton(
    painter: Painter,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(38.dp)
    ) {
        Icon(
            painter = painter,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun PeerAvatar(title: String, accent: Color, size: Dp = 34.dp) {
    val monogram = title.split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercase() }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = monogram,
            color = TextPrimary,
            fontSize = if (size > 40.dp) 16.sp else 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
