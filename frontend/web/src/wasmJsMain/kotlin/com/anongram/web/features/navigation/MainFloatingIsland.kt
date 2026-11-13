package com.anongram.web.features.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.icons.FluentIcons
import com.anongram.web.core.ui.safeNavigationBarPadding

@Composable
fun MainFloatingIsland(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenStealthQr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val language by LanguageSettingsStore.language.collectAsState()
    val isRu = language == AppLanguage.RU

    val chatsLabel = if (isRu) "Чаты" else "Chats"
    val servicesLabel = if (isRu) "Сервисы" else "Services"
    val settingsLabel = if (isRu) "Настройки" else "Settings"
    val profileLabel = if (isRu) "Профиль" else "Profile"

    Box(
        modifier = modifier
            .fillMaxWidth()
            .safeNavigationBarPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(28.dp),
            color = scheme.islandBg,
            shadowElevation = 16.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, scheme.islandBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NavTabItem(
                    icon = FluentIcons.ChatMultiple,
                    label = chatsLabel,
                    selected = selectedTab == 0,
                    onClick = { onSelectTab(0) },
                    modifier = Modifier.weight(1f)
                )
                NavTabItem(
                    icon = FluentIcons.Shield,
                    label = servicesLabel,
                    selected = selectedTab == 1,
                    onClick = { onSelectTab(1) },
                    modifier = Modifier.weight(1f)
                )

                // Место под центральную парящую кнопку QR
                Spacer(modifier = Modifier.width(64.dp))

                NavTabItem(
                    icon = FluentIcons.Settings,
                    label = settingsLabel,
                    selected = selectedTab == 2,
                    onClick = { onSelectTab(2) },
                    modifier = Modifier.weight(1f)
                )
                NavTabItem(
                    icon = FluentIcons.Person,
                    label = profileLabel,
                    selected = selectedTab == 3,
                    onClick = { onSelectTab(3) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Центральная приподнятая кнопка Stealth QR
        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .size(56.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(scheme.accent, scheme.accentSecondary)
                    )
                )
                .border(1.5.dp, scheme.background.copy(alpha = 0.8f), RoundedCornerShape(22.dp))
                .clickable(onClick = onOpenStealthQr),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.QrCode2,
                contentDescription = "Stealth QR",
                tint = scheme.onAccent,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun NavTabItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val tint by animateColorAsState(
        targetValue = if (selected) scheme.accent else scheme.textMuted
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.06f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
    )
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = tint,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
