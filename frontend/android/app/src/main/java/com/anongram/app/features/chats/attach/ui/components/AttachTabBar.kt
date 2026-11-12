package com.anongram.app.features.chats.attach.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.attach.i18n.LocalAttachStrings
import com.anongram.app.features.chats.attach.ui.AttachTab

/**
 * Парящая нижняя панель-таблетка вкладок:
 * - НЕ прижата к краю: отступ снизу = навигационная панель + 12dp, по бокам по 12dp.
 * - Приподнята выше с мягкой тенью и фоном текущей темы.
 */
@Composable
fun AttachTabBar(
    selectedTab: AttachTab,
    onSelectTab: (AttachTab) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = LocalAttachStrings.current
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 12.dp

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(250)),
        exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(250)),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .padding(bottom = navBottom),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .shadow(
                        elevation = if (scheme.isDark) 16.dp else 10.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color.Black.copy(alpha = if (scheme.isDark) 1f else 0.25f),
                        spotColor = Color.Black.copy(alpha = if (scheme.isDark) 0.7f else 0.22f)
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(scheme.islandBg)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(28.dp))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                AttachTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(if (isSelected) scheme.accent else Color.Transparent)
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) scheme.onAccent else scheme.textMuted,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.label(strings),
                            color = if (isSelected) scheme.onAccent else scheme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
