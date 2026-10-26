package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.NeonEmerald
import com.anongram.app.core.theme.TextPrimary

private val OnionBadgeBackground = Color(0xFF0F261E)

/** Топбар главного меню «Чаты»: название приложения, onion-статус, кнопка поиска. */
@Composable
internal fun ChatsTopBar(
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .safeStatusBarPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "AnonGram",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(OnionBadgeBackground)
                    .border(1.dp, NeonEmerald.copy(alpha = 0.5f), CircleShape)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_onion),
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Onion v3",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = NeonEmerald
                    )
                }
            }
        }

        IconButton(onClick = onOpenSearch) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "Поиск",
                tint = scheme.accent,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
