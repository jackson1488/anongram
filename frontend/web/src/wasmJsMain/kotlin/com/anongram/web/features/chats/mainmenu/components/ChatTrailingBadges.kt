package com.anongram.web.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.FlameOrange
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.features.chats.common.IChatSession

@Composable
internal fun ChatTrailingBadges(session: IChatSession, modifier: Modifier = Modifier) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        session.padPagesLeft?.let { pages ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = "OTP pad pages",
                    tint = FlameOrange,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = pages.toString(),
                    color = FlameOrange,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        if (session.isMuted) {
            Icon(
                imageVector = Icons.Outlined.NotificationsOff,
                contentDescription = "Muted",
                tint = scheme.textMuted,
                modifier = Modifier.size(14.dp)
            )
        }

        if (session.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(scheme.accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (session.unreadCount > 99) "99+" else session.unreadCount.toString(),
                    color = scheme.onAccent,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
