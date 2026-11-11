package com.anongram.app.features.settings.notifications.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.notifications.ui.components.NotificationToggleCard

/**
 * Экран «Уведомления» (`features/settings/notifications/`).
 */
@Composable
fun NotificationsSettingsScreen(
    onBack: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var privateChatsEnabled by remember { mutableStateOf(true) }
    var groupsEnabled by remember { mutableStateOf(true) }
    var hidePreviewContent by remember { mutableStateOf(true) }

    AnonGramScreenSurface(applySystemBarsPadding = false) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeNavigationBarPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surface)
                    .safeStatusBarPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = scheme.textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Уведомления и звуки",
                    color = scheme.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                NotificationToggleCard(
                    privateChatsEnabled = privateChatsEnabled,
                    onPrivateChatsChange = { privateChatsEnabled = it },
                    groupsEnabled = groupsEnabled,
                    onGroupsChange = { groupsEnabled = it },
                    hidePreviewContent = hidePreviewContent,
                    onHidePreviewContentChange = { hidePreviewContent = it }
                )
            }
        }
    }
}
