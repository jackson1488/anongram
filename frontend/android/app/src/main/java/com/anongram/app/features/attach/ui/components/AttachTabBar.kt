package com.anongram.app.features.attach.ui.components

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
import com.anongram.app.features.attach.ui.AttachTab
import com.anongram.app.features.attach.ui.theme.AttachBorder
import com.anongram.app.features.attach.ui.theme.AttachPurple
import com.anongram.app.features.attach.ui.theme.AttachTextMuted
import com.anongram.app.features.attach.ui.theme.AttachTextPrimary

/**
 * Парящая нижняя панель-таблетка вкладок (ТЗ, шаг 10):
 * - НЕ прижата к краю: отступ снизу = навигационная панель + 12dp, по бокам по 12dp.
 * - Приподнята выше с мягкой тенью и полупрозрачным фоном.
 */
@Composable
fun AttachTabBar(
    selectedTab: AttachTab,
    onSelectTab: (AttachTab) -> Unit,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
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
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(28.dp), ambientColor = Color.Black, spotColor = Color.Black)
                    .clip(RoundedCornerShape(28.dp))
                    .background(Color(0xE610192D))
                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
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
                            .background(if (isSelected) AttachPurple else Color.Transparent)
                            .clickable { onSelectTab(tab) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else AttachTextMuted,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tab.title,
                            color = if (isSelected) Color.White else AttachTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
