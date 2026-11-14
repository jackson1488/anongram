package com.anongram.app.features.settings.animations.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.animations.data.MessageAppearStyle
import com.anongram.app.features.settings.animations.i18n.AnimationsI18n
import com.anongram.app.features.settings.animations.ui.components.AnimationsEffectsCard
import com.anongram.app.features.settings.animations.ui.components.AnimationsPerformanceCard
import com.anongram.app.features.settings.animations.ui.components.AnimationsTopBar
import com.anongram.app.features.settings.animations.ui.transitions.resolveTransitionLabels

/**
 * Экран «Анимации» (`features/settings/animations/`).
 * Плоский список разделов в стиле Telegram: переход в «Сообщения в чате», «Переходы экранов», визуальные эффекты и производительность.
 */
@Composable
fun AnimationsSettingsScreen(
    onOpenMessagesAnimations: () -> Unit,
    onOpenTransitionsAnimations: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = AnimationsI18n.current
    val state by AnimationsSettingsStore.state.collectAsState()

    LaunchedEffect(Unit) {
        AnimationsSettingsStore.ensureInitialized(context)
    }

    BackHandler(onBack = onBack)

    val activeStyleLabel = if (!state.messageAppearEnabled) {
        strings.messagesRowDisabled
    } else {
        when (state.messageAppearStyle) {
            MessageAppearStyle.SPRING_FLY -> strings.styleSpringTitle
            MessageAppearStyle.SLIDE_FADE -> strings.styleSlideTitle
            MessageAppearStyle.CORNER_POP -> strings.stylePopTitle
            MessageAppearStyle.TOKEN_STREAM -> strings.styleStreamTitle
        }
    }

    val activeTransitionLabel = if (!state.screenTransitionsEnabled || state.reduceMotionEnabled) {
        strings.messagesRowDisabled
    } else {
        resolveTransitionLabels(state.screenTransitionType, strings).first
    }

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
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenMessagesAnimations)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.messagesSectionTitle,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeStyleLabel,
                            color = scheme.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = scheme.textMuted
                    )
                }

                HorizontalDivider(color = scheme.divider)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenTransitionsAnimations)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = strings.transitionsSectionTitle,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activeTransitionLabel,
                            color = scheme.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = scheme.textMuted
                    )
                }

                HorizontalDivider(color = scheme.divider)

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

                HorizontalDivider(color = scheme.divider)

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
