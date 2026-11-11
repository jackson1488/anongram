package com.anongram.app.features.chats.ui.input.banner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.features.chats.ui.input.i18n.InputI18nStrings
import com.anongram.app.features.chats.ui.input.i18n.LocalInputStrings
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ChatActionBannerData(
    val id: Long,
    val resolveText: (InputI18nStrings) -> String,
    val icon: ImageVector,
    val isError: Boolean = false
)

/**
 * Единая шина локализованных кастомных уведомлений чата (i18n RU/EN через LocalInputStrings).
 */
object ChatActionBannerBus {
    private val _state = MutableStateFlow<ChatActionBannerData?>(null)
    val state: StateFlow<ChatActionBannerData?> = _state.asStateFlow()

    fun show(
        text: String,
        icon: ImageVector = Icons.Outlined.CheckCircle,
        isError: Boolean = false
    ) {
        show(icon = icon, isError = isError) { text }
    }

    fun show(
        icon: ImageVector = Icons.Outlined.CheckCircle,
        isError: Boolean = false,
        resolveText: (InputI18nStrings) -> String
    ) {
        _state.value = ChatActionBannerData(
            id = System.nanoTime(),
            resolveText = resolveText,
            icon = icon,
            isError = isError
        )
    }

    fun dismiss() {
        _state.value = null
    }
}

@Composable
fun BoxScope.ChatActionBannerHost(
    bottomOffsetDp: Dp,
    modifier: Modifier = Modifier
) {
    val banner by ChatActionBannerBus.state.collectAsState()
    val scheme = LocalAnonGramScheme.current
    val strings = LocalInputStrings.current

    LaunchedEffect(banner?.id) {
        if (banner != null) {
            delay(2400L)
            ChatActionBannerBus.dismiss()
        }
    }

    DisposableEffect(Unit) {
        onDispose { ChatActionBannerBus.dismiss() }
    }

    AnimatedVisibility(
        visible = banner != null,
        enter = fadeIn(tween(180)) + slideInVertically(
            animationSpec = tween(200),
            initialOffsetY = { it / 2 }
        ),
        exit = fadeOut(tween(180)) + slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { it / 2 }
        ),
        modifier = modifier
            .zIndex(20f)
            .align(Alignment.BottomCenter)
            .padding(bottom = bottomOffsetDp + 8.dp, start = 16.dp, end = 16.dp)
    ) {
        banner?.let { item ->
            val tintColor = if (item.isError) CrimsonKillSwitch else scheme.accent
            Box(
                modifier = Modifier
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.bannerBg)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                    .clickable { ChatActionBannerBus.dismiss() }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = item.resolveText(strings),
                        color = scheme.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.SelectionLimitBannerHost(
    warningText: String?,
    bottomOffsetDp: Dp,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    AnimatedVisibility(
        visible = warningText != null,
        enter = fadeIn(tween(180)) + slideInVertically(
            animationSpec = tween(200),
            initialOffsetY = { it / 2 }
        ),
        exit = fadeOut(tween(180)) + slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { it / 2 }
        ),
        modifier = modifier
            .zIndex(19f)
            .align(Alignment.BottomCenter)
            .padding(bottom = bottomOffsetDp + 8.dp, start = 20.dp, end = 20.dp)
    ) {
        warningText?.let { text ->
            Box(
                modifier = Modifier
                    .shadow(elevation = 8.dp, shape = RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.bannerBg)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(14.dp))
                    .clickable { onDismiss() }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ErrorOutline,
                        contentDescription = null,
                        tint = CrimsonKillSwitch,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = text,
                        color = scheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
