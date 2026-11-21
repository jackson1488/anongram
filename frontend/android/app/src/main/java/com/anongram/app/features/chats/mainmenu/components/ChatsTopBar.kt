package com.anongram.app.features.chats.mainmenu.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.mainmenu.i18n.ChatsMenuI18n
import com.anongram.app.features.settings.common.ui.settingsGlassPill
import dev.chrisbanes.haze.HazeState

/**
 * Топбар главного меню чатов (Telegram Liquid Glass):
 * название и отдельная стеклянная кнопка поиска.
 */
@Composable
internal fun ChatsTopBar(
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    glassBlurEnabled: Boolean = true
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ChatsMenuI18n.current

    val searchPillModifier = if (hazeState != null) {
        Modifier.settingsGlassPill(
            hazeState = hazeState,
            shape = CircleShape,
            glassBlurEnabled = glassBlurEnabled
        )
    } else {
        Modifier
            .clip(CircleShape)
            .background(scheme.islandBg.copy(alpha = 0.90f))
            .border(0.8.dp, scheme.islandBorder, CircleShape)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .safeStatusBarPadding()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "AnonGram",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = scheme.textPrimary,
            modifier = Modifier.weight(1f)
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .then(searchPillModifier)
                .clickable(onClick = onOpenSearch),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = strings.searchPlaceholder,
                tint = scheme.textPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
