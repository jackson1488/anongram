package com.anongram.app.features.settings.animations.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.anongram.app.features.settings.animations.ui.components.AnimationsEffectsCard
import com.anongram.app.features.settings.animations.ui.components.AnimationsMessagesCard
import com.anongram.app.features.settings.animations.ui.components.AnimationsPerformanceCard
import com.anongram.app.features.settings.animations.ui.components.AnimationsTopBar

/**
 * Экран «Анимации» (`features/settings/animations/`).
 * Изолированный модуль управления анимациями сообщений (включая AI-стриминг), эффектами и производительностью.
 */
@Composable
fun AnimationsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val strings = AnimationsI18n.current
    val state by AnimationsSettingsStore.state.collectAsState()

    LaunchedEffect(Unit) {
        AnimationsSettingsStore.ensureInitialized(context)
    }

    BackHandler(onBack = onBack)

    AnonGramScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimationsTopBar(
                title = strings.screenTitle,
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Карточка анимаций отправки/получения сообщений + AI-стриминг + живой предпросмотр
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

                Spacer(modifier = Modifier.height(14.dp))

                // Карточка визуальных эффектов
                AnimationsEffectsCard(
                    screenTransitionsEnabled = state.screenTransitionsEnabled,
                    onScreenTransitionsChange = {
                        AnimationsSettingsStore.setScreenTransitions(context, it)
                    },
                    glassBlurEnabled = state.glassBlurEnabled,
                    onGlassBlurChange = {
                        AnimationsSettingsStore.setGlassBlur(context, it)
                    },
                    stickersLoopEnabled = state.stickersLoopEnabled,
                    onStickersLoopChange = {
                        AnimationsSettingsStore.setStickersLoop(context, it)
                    },
                    reactionsEffectsEnabled = state.reactionsEffectsEnabled,
                    onReactionsEffectsChange = {
                        AnimationsSettingsStore.setReactionsEffects(context, it)
                    },
                    animatedBackgroundsEnabled = state.animatedBackgroundsEnabled,
                    onAnimatedBackgroundsChange = {
                        AnimationsSettingsStore.setAnimatedBackgrounds(context, it)
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Карточка производительности
                AnimationsPerformanceCard(
                    reduceMotionEnabled = state.reduceMotionEnabled,
                    onReduceMotionChange = {
                        AnimationsSettingsStore.setReduceMotion(context, it)
                    },
                    animationSpeedOption = state.animationSpeedOption,
                    onAnimationSpeedOptionChange = {
                        AnimationsSettingsStore.setAnimationSpeedOption(context, it)
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
