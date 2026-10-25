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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.*

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
                    elevation = 24.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = Color.Black,
                    spotColor = Color.Black.copy(alpha = 0.7f)
                )
                .clip(RoundedCornerShape(28.dp))
                .background(IslandBackground)
                .border(1.dp, IslandBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // 0. Chats
            NavTabItem(
                label = "Чаты",
                icon = Icons.Outlined.ChatBubbleOutline,
                isSelected = currentTab == 0,
                onClick = { onTabSelected(0) }
            )

            // 1. Services
            NavTabItem(
                label = "Сервисы",
                icon = Icons.Outlined.Shield,
                isSelected = currentTab == 1,
                onClick = { onTabSelected(1) }
            )

            // Spacer for center elevated stealth button
            Spacer(modifier = Modifier.width(56.dp))

            // 2. Settings
            NavTabItem(
                label = "Настройки",
                icon = Icons.Outlined.Settings,
                isSelected = currentTab == 2,
                onClick = { onTabSelected(2) }
            )

            // 3. Profile
            NavTabItem(
                label = "Профиль",
                icon = Icons.Outlined.Person,
                isSelected = currentTab == 3,
                onClick = { onTabSelected(3) }
            )
        }

        // Center Elevated Stealth QR Button
        Box(
            modifier = Modifier
                .offset(y = (-20).dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(22.dp),
                    spotColor = CyberCyan.copy(alpha = 0.5f)
                )
                .size(56.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF09111E),
                            Color(0xFF0F1E33)
                        )
                    )
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(CyberCyan, CyberTeal, NeonEmerald)
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
                tint = Color.White,
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
    onClick: () -> Unit
) {
    val contentColor = if (isSelected) CyberCyan else TextSecondary
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
                .background(if (isSelected) Color(0xFF1B2B3F) else Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
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
