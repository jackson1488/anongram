package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material.icons.outlined.VolumeOff
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.core.ui.safeStatusBarPadding

/**
 * Панель режима выбора (Telegram-style): закрыть · счётчик · закрепить ·
 * без звука · корзина | архив/из архива · прочитано · очистить историю.
 */
@Composable
internal fun SelectionTopBar(
    selectedCount: Int,
    allPinned: Boolean,
    allMuted: Boolean,
    isArchiveMode: Boolean,
    onClose: () -> Unit,
    onTogglePin: () -> Unit,
    onMute: () -> Unit,
    onDelete: () -> Unit,
    onArchive: () -> Unit,
    onMarkRead: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .safeStatusBarPadding()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Закрыть режим выбора",
                tint = TextSecondary,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = selectedCount.toString(),
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        SelectionIcon(
            icon = Icons.Outlined.PushPin,
            description = if (allPinned) "Открепить" else "Закрепить",
            tint = if (allPinned) scheme.accent else TextSecondary,
            onClick = onTogglePin
        )
        SelectionIcon(
            icon = if (allMuted) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
            description = if (allMuted) "Включить звук" else "Без звука",
            tint = if (allMuted) scheme.accent else TextSecondary,
            onClick = onMute
        )
        SelectionIcon(
            icon = Icons.Outlined.Delete,
            description = "Удалить",
            tint = FlameOrange,
            onClick = onDelete
        )

        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .width(1.dp)
                .height(24.dp)
                .background(CardBorder)
        )

        SelectionIcon(
            icon = if (isArchiveMode) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
            description = if (isArchiveMode) "Убрать из архива" else "В архив",
            tint = TextSecondary,
            onClick = onArchive
        )
        SelectionIcon(
            icon = Icons.Outlined.DoneAll,
            description = "Отметить прочитанным",
            tint = TextSecondary,
            onClick = onMarkRead
        )
        SelectionIcon(
            icon = Icons.Outlined.CleaningServices,
            description = "Очистить историю",
            tint = TextSecondary,
            onClick = onClearHistory
        )
    }
}

@Composable
private fun SelectionIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}
