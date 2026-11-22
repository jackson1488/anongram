package com.anongram.app.features.chats.user_profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.user_profile.i18n.UserProfileStrings
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

@Composable
internal fun UserProfileActionsRow(
    isMuted: Boolean,
    strings: UserProfileStrings,
    onChatClick: () -> Unit,
    onMuteToggle: () -> Unit,
    onCallClick: () -> Unit,
    onVideoClick: () -> Unit,
    hazeState: HazeState? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ActionTile(
            icon = Icons.Outlined.ChatBubbleOutline,
            label = strings.actionChat,
            isActive = false,
            hazeState = hazeState,
            onClick = onChatClick,
            modifier = Modifier.weight(1f)
        )
        ActionTile(
            icon = if (isMuted) Icons.Outlined.NotificationsOff else Icons.Outlined.Notifications,
            label = if (isMuted) strings.actionMuted else strings.actionSound,
            isActive = isMuted,
            hazeState = hazeState,
            onClick = onMuteToggle,
            modifier = Modifier.weight(1f)
        )
        ActionTile(
            icon = Icons.Outlined.Call,
            label = strings.actionCall,
            isActive = false,
            hazeState = hazeState,
            onClick = onCallClick,
            modifier = Modifier.weight(1f)
        )
        ActionTile(
            icon = Icons.Outlined.Videocam,
            label = strings.actionVideo,
            isActive = false,
            hazeState = hazeState,
            onClick = onVideoClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ActionTile(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    hazeState: HazeState?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val shape = RoundedCornerShape(18.dp)
    val bgModifier = when {
        isActive -> Modifier.background(scheme.accentSoft)
        hazeState != null -> Modifier.hazeEffect(
            state = hazeState,
            style = HazeStyle(
                backgroundColor = scheme.card.copy(alpha = 0.45f),
                tint = HazeTint(color = scheme.surface.copy(alpha = 0.45f)),
                blurRadius = 20.dp,
                noiseFactor = 0.02f
            )
        )
        else -> Modifier.background(scheme.card.copy(alpha = 0.72f))
    }
    Column(
        modifier = modifier
            .clip(shape)
            .then(bgModifier)
            .border(0.6.dp, scheme.islandBorder, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive) scheme.accent else scheme.textPrimary,
            modifier = Modifier.size(21.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = if (isActive) scheme.accent else scheme.textPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
