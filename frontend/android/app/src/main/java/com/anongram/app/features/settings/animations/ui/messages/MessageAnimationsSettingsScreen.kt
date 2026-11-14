package com.anongram.app.features.settings.animations.ui.messages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.animations.i18n.AnimationsI18n
import com.anongram.app.features.settings.animations.ui.components.AnimationsMessagesCard
import com.anongram.app.features.settings.animations.ui.components.AnimationsTopBar

/**
 * Отдельный плоский экран настройки анимаций сообщений в чате (`features/settings/animations/ui/messages/`).
 */
@Composable
fun MessageAnimationsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = AnimationsI18n.current
    val state by AnimationsSettingsStore.state.collectAsState()

    LaunchedEffect(Unit) {
        AnimationsSettingsStore.ensureInitialized(context)
    }


    AnonGramScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimationsTopBar(
                title = strings.messagesSectionTitle,
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                AnimationsMessagesCard(
                    messageAppearEnabled = state.messageAppearEnabled,
                    onMessageAppearEnabledChange = {
                        AnimationsSettingsStore.setMessageAppearEnabled(context, it)
                    },
                    appearStyle = state.messageAppearStyle,
                    onAppearStyleChange = {
                        AnimationsSettingsStore.setMessageAppearStyle(context, it)
                    },
                    tokenStreamSpeed = state.tokenStreamSpeed,
                    onTokenStreamSpeedChange = {
                        AnimationsSettingsStore.setTokenStreamSpeed(context, it)
                    },
                    deleteStyle = state.messageDeleteStyle,
                    onDeleteStyleChange = {
                        AnimationsSettingsStore.setMessageDeleteStyle(context, it)
                    },
                    listItemShiftEnabled = state.listItemShiftEnabled,
                    onListItemShiftEnabledChange = {
                        AnimationsSettingsStore.setListItemShiftEnabled(context, it)
                    },
                    statusCheckAnimEnabled = state.statusCheckAnimEnabled,
                    onStatusCheckAnimEnabledChange = {
                        AnimationsSettingsStore.setStatusCheckAnimEnabled(context, it)
                    },
                    speedMultiplier = state.speedMultiplier,
                    reduceMotionEnabled = state.reduceMotionEnabled
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
