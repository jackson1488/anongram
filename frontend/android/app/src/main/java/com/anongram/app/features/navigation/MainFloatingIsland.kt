package com.anongram.app.features.navigation

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.icons.FluentIcons
import com.anongram.app.features.auth.data.crypto.KeystoreVault
import com.anongram.app.features.profile.avatar.ProfileAvatar
import com.anongram.app.features.profile.avatar.ProfileAvatarStore
import com.anongram.app.features.profile.avatar.ProfileAvatarView

/**
 * Ergonomic Floating Navigation Island with prominent central elevated Stealth QR button
 */
@Composable
fun MainFloatingIsland(
    currentTab: Int,
    onTabSelected: (Int) -> Unit,
    onQrClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        ProfileAvatarStore.ensureLoaded(context)
    }
    val profileAvatar by ProfileAvatarStore.avatar.collectAsState()
    val profileName = remember {
        context.applicationContext
            .getSharedPreferences("anongram_profile_meta", android.content.Context.MODE_PRIVATE)
            .getString("display_name", null)
            ?: KeystoreVault(context).loadIdentity()?.nickname
            ?: "Anon"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Island Pill Capsule
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = if (scheme.isDark) 24.dp else 14.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black.copy(alpha = if (scheme.isDark) 1f else 0.25f),
                    spotColor = Color.Black.copy(alpha = if (scheme.isDark) 0.7f else 0.22f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(scheme.islandBg)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // 0. Chats (FluentUI ChatMultiple)
            NavTabItem(
                label = "Чаты",
                icon = FluentIcons.ChatMultiple,
                isSelected = currentTab == 0,
                activeColor = scheme.accent,
                activeBgColor = scheme.accentSoft,
                inactiveColor = scheme.textSecondary,
                onClick = { onTabSelected(0) }
            )

            // 1. Services (FluentUI Shield)
            NavTabItem(
                label = "Сервисы",
                icon = FluentIcons.Shield,
                isSelected = currentTab == 1,
                activeColor = scheme.accent,
                activeBgColor = scheme.accentSoft,
                inactiveColor = scheme.textSecondary,
                onClick = { onTabSelected(1) }
            )

            // Spacer for center elevated stealth button
            Spacer(modifier = Modifier.width(56.dp))

            // 2. Settings (FluentUI Settings)
            NavTabItem(
                label = "Настройки",
                icon = FluentIcons.Settings,
                isSelected = currentTab == 2,
                activeColor = scheme.accent,
                activeBgColor = scheme.accentSoft,
                inactiveColor = scheme.textSecondary,
                onClick = { onTabSelected(2) }
            )

            // 3. Profile (мини-аватар как в Telegram, видео НЕ воспроизводится)
            NavTabItem(
                label = "Профиль",
                icon = FluentIcons.Person,
                isSelected = currentTab == 3,
                activeColor = scheme.accent,
                activeBgColor = scheme.accentSoft,
                inactiveColor = scheme.textSecondary,
                onClick = { onTabSelected(3) },
                avatarContent = if (profileAvatar !is ProfileAvatar.None) {
                    {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .then(
                                    if (currentTab == 3) {
                                        Modifier.border(1.5.dp, scheme.accent, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                        ) {
                            ProfileAvatarView(
                                avatar = profileAvatar,
                                displayName = profileName,
                                monogramFontSize = 10.sp,
                                emojiFontSize = 12.sp,
                                playVideo = false,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    null
                }
            )
        }

        // Center Elevated Stealth QR Button
        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .shadow(
                    elevation = if (scheme.isDark) 20.dp else 12.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = scheme.accent.copy(alpha = if (scheme.isDark) 0.5f else 0.35f)
                )
                .size(56.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(scheme.card, scheme.surface)
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(scheme.accent, scheme.accentDim)
                    ),
                    shape = RoundedCornerShape(22.dp)
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onQrClick
                ),
            contentAlignment = Alignment.Center
        ) {
            // Canonical Stealth Shield Icon
            Icon(
                painter = painterResource(id = R.drawable.ic_stat_anongram),
                contentDescription = "Stealth QR",
                tint = scheme.textPrimary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun RowScope.NavTabItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    activeColor: Color,
    activeBgColor: Color,
    inactiveColor: Color,
    onClick: () -> Unit,
    avatarContent: (@Composable () -> Unit)? = null
) {
    val contentColor = if (isSelected) activeColor else inactiveColor
    val fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (isSelected) activeBgColor else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            if (avatarContent != null) {
                avatarContent()
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = fontWeight,
            color = contentColor
        )
    }
}
