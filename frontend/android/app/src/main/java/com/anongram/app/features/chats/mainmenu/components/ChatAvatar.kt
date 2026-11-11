package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.NeonEmerald
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.SessionGlyph

/**
 * Аватар сессии. В режиме выбора аватар остаётся на месте и слегка
 * притемняется, а состояние выбора показывает бейдж-галочку в нижнем
 * углу (как в Telegram), не заменяя собой аватар.
 */
@Composable
internal fun ChatAvatar(
    session: IChatSession,
    accent: Color,
    modifier: Modifier = Modifier,
    showSelectionBadge: Boolean = false,
    isSelected: Boolean = false
) {
    val scheme = LocalAnonGramScheme.current
    val checkTint = if (accent.luminance() > 0.45f) Color(0xFF0A0C10) else Color.White

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .alpha(if (showSelectionBadge) 0.45f else 1f)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (session.avatarGlyph) {
                SessionGlyph.MONOGRAM -> Text(
                    text = session.avatarMonogram ?: "··",
                    fontWeight = FontWeight.Bold,
                    color = scheme.textPrimary,
                    fontSize = 15.sp
                )

                SessionGlyph.GROUP -> Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )

                SessionGlyph.FLAME -> Icon(
                    painter = painterResource(id = R.drawable.ic_flame),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        if (session.isOnline && session.avatarGlyph == SessionGlyph.MONOGRAM && !showSelectionBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(if (scheme.isDark) NeonEmerald else Color(0xFF059669))
                    .border(2.dp, scheme.surface, CircleShape)
            )
        }

        if (showSelectionBadge) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) accent else scheme.background)
                    .border(2.dp, scheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Выбрано",
                        tint = checkTint,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}
