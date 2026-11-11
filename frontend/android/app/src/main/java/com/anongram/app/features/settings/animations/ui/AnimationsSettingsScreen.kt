package com.anongram.app.features.settings.animations.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.animations.ui.components.AnimationsEffectsCard
import com.anongram.app.features.settings.animations.ui.components.AnimationsPerformanceCard
import com.anongram.app.features.settings.animations.ui.components.AnimationsTopBar

/**
 * Экран «Анимации» (`features/settings/animations/`).
 * Изолированный модуль управления визуальными эффектами, размытием Glass, стикерами и скоростью переходов.
 */
@Composable
fun AnimationsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val prefs = remember {
        context.applicationContext.getSharedPreferences("anongram_animations_settings", Context.MODE_PRIVATE)
    }

    var screenTransitionsEnabled by remember { mutableStateOf(true) }
    var glassBlurEnabled by remember { mutableStateOf(true) }
    var stickersLoopEnabled by remember { mutableStateOf(true) }
    var reactionsEffectsEnabled by remember { mutableStateOf(true) }
    var animatedBackgroundsEnabled by remember { mutableStateOf(true) }
    var reduceMotionEnabled by remember { mutableStateOf(false) }
    var animationSpeedOption by remember { mutableStateOf("1.0x") }

    LaunchedEffect(Unit) {
        screenTransitionsEnabled = prefs.getBoolean("key_screen_transitions", true)
        glassBlurEnabled = prefs.getBoolean("key_glass_blur", true)
        stickersLoopEnabled = prefs.getBoolean("key_stickers_loop", true)
        reactionsEffectsEnabled = prefs.getBoolean("key_reactions_effects", true)
        animatedBackgroundsEnabled = prefs.getBoolean("key_animated_backgrounds", true)
        reduceMotionEnabled = prefs.getBoolean("key_reduce_motion", false)
        animationSpeedOption = prefs.getString("key_animation_speed", "1.0x") ?: "1.0x"
    }

    // Обработка системного жеста «Назад»
    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050508))
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
        // Верхняя панель в стиле Telegram
        AnimationsTopBar(
            title = "Анимации",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // Карточка визуальных эффектов (переходы, blur, стикеры, реакции, живой фон)
            AnimationsEffectsCard(
                screenTransitionsEnabled = screenTransitionsEnabled,
                onScreenTransitionsChange = { enabled ->
                    screenTransitionsEnabled = enabled
                    prefs.edit().putBoolean("key_screen_transitions", enabled).apply()
                },
                glassBlurEnabled = glassBlurEnabled,
                onGlassBlurChange = { enabled ->
                    glassBlurEnabled = enabled
                    prefs.edit().putBoolean("key_glass_blur", enabled).apply()
                },
                stickersLoopEnabled = stickersLoopEnabled,
                onStickersLoopChange = { enabled ->
                    stickersLoopEnabled = enabled
                    prefs.edit().putBoolean("key_stickers_loop", enabled).apply()
                },
                reactionsEffectsEnabled = reactionsEffectsEnabled,
                onReactionsEffectsChange = { enabled ->
                    reactionsEffectsEnabled = enabled
                    prefs.edit().putBoolean("key_reactions_effects", enabled).apply()
                },
                animatedBackgroundsEnabled = animatedBackgroundsEnabled,
                onAnimatedBackgroundsChange = { enabled ->
                    animatedBackgroundsEnabled = enabled
                    prefs.edit().putBoolean("key_animated_backgrounds", enabled).apply()
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Карточка производительности (уменьшение движения, скорость анимаций)
            AnimationsPerformanceCard(
                reduceMotionEnabled = reduceMotionEnabled,
                onReduceMotionChange = { enabled ->
                    reduceMotionEnabled = enabled
                    prefs.edit().putBoolean("key_reduce_motion", enabled).apply()
                },
                animationSpeedOption = animationSpeedOption,
                onAnimationSpeedOptionChange = { opt ->
                    animationSpeedOption = opt
                    prefs.edit().putString("key_animation_speed", opt).apply()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
