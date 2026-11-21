package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.NeonEmerald
import com.anongram.app.features.chats.common.IChatSession
import com.anongram.app.features.chats.common.SessionGlyph
import com.anongram.app.features.chats.mainmenu.i18n.ChatsMenuI18n

/**
 * Аватар сессии. При выборе чата (`isSelected == true`) в правом нижнем
 * углу появляется маленькая точка-галочка (без пустых кружков у невыбранных).
 */
@Composable
internal fun ChatAvatar(
    session: IChatSession,
    accent: Color,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ChatsMenuI18n.current

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val resId = session.avatarResId
            if (resId != null) {
                androidx.compose.foundation.Image(
                    painter = painterResource(id = resId),
                    contentDescription = session.title,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.size(46.dp)
                )
            } else when (session.avatarGlyph) {
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

        if (session.isOnline && session.avatarGlyph == SessionGlyph.MONOGRAM && !isSelected) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(if (scheme.isDark) NeonEmerald else Color(0xFF059669))
                    .border(2.dp, scheme.background, CircleShape)
            )
        }

        AnimatedVisibility(
            visible = isSelected,
            enter = scaleIn(tween(150)) + fadeIn(tween(120)),
            exit = scaleOut(tween(120)) + fadeOut(tween(100)),
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(scheme.accent)
                    .border(2.dp, scheme.background, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = strings.selected,
                    tint = scheme.onAccent,
                    modifier = Modifier.size(11.dp)
                )
            }
        }
    }
}
