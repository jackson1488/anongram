package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.CyberCyan
import com.anongram.app.core.theme.FlameOrange
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.features.chats.common.IChatSession

/** Хвостовые бейджи карточки: остаток страниц OTP-пада, мьют, непрочитанные. */
@Composable
internal fun ChatTrailingBadges(session: IChatSession, modifier: Modifier = Modifier) {
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
                    painter = painterResource(id = R.drawable.ic_flame),
                    contentDescription = "Осталось страниц OTP-пада",
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
                contentDescription = "Уведомления отключены",
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }

        if (session.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(CyberCyan),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (session.unreadCount > 99) "99+" else session.unreadCount.toString(),
                    color = ObsidianBlack,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}
