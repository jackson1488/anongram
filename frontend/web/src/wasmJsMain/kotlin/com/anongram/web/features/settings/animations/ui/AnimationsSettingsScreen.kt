package com.anongram.web.features.settings.animations.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.web.features.settings.animations.data.MessageAppearStyle
import com.anongram.web.features.settings.ui.SettingsCardDivider
import com.anongram.web.features.settings.ui.SettingsGroupCard
import com.anongram.web.features.settings.ui.SettingsNavRow
import com.anongram.web.features.settings.ui.SettingsSubScreenScaffold
import com.anongram.web.features.settings.ui.SettingsToggleRow

@Composable
fun AnimationsSettingsScreen(
    onOpenMessagesAnimations: () -> Unit,
    onBack: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU
    val state by AnimationsSettingsStore.state.collectAsState()

    LaunchedEffect(Unit) {
        AnimationsSettingsStore.ensureInitialized()
    }

    val activeStyleLabel = if (!state.messageAppearEnabled) {
        if (isRu) "Отключено" else "Disabled"
    } else {
        when (state.messageAppearStyle) {
            MessageAppearStyle.SPRING_FLY -> if (isRu) "Пружинный полёт" else "Spring Fly"
            MessageAppearStyle.SLIDE_FADE -> if (isRu) "Плавный слайд" else "Slide & Fade"
            MessageAppearStyle.CORNER_POP -> if (isRu) "Вылет из угла" else "Corner Pop"
            MessageAppearStyle.TOKEN_STREAM -> if (isRu) "Потоковая печать" else "Token Stream"
        }
    }

    SettingsSubScreenScaffold(
        title = if (isRu) "Анимации" else "Animations",
        onBack = onBack
    ) {
        SettingsGroupCard(title = if (isRu) "Сообщения в чате" else "Chat Messages") {
            SettingsNavRow(
                title = if (isRu) "Анимация сообщений" else "Message animations",
                value = activeStyleLabel,
                subtitle = if (isRu) "Стиль появления, скорость потока и удаление" else "Appear style, token speed and deletion",
                onClick = onOpenMessagesAnimations
            )
        }

        SettingsGroupCard(title = if (isRu) "Визуальные эффекты" else "Visual Effects") {
            SettingsToggleRow(
                title = if (isRu) "Переходы экранов" else "Screen transitions",
                checked = state.screenTransitionsEnabled,
                onCheckedChange = { v -> AnimationsSettingsStore.update { it.copy(screenTransitionsEnabled = v) } }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Размытие Glass" else "Glass blur",
                checked = state.glassBlurEnabled,
                onCheckedChange = { v -> AnimationsSettingsStore.update { it.copy(glassBlurEnabled = v) } }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Зацикленные стикеры" else "Loop animated stickers",
                checked = state.stickersLoopEnabled,
                onCheckedChange = { v -> AnimationsSettingsStore.update { it.copy(stickersLoopEnabled = v) } }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Эффекты реакций" else "Reaction effects",
                checked = state.reactionsEffectsEnabled,
                onCheckedChange = { v -> AnimationsSettingsStore.update { it.copy(reactionsEffectsEnabled = v) } }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Анимированные фоны" else "Animated backgrounds",
                checked = state.animatedBackgroundsEnabled,
                onCheckedChange = { v -> AnimationsSettingsStore.update { it.copy(animatedBackgroundsEnabled = v) } }
            )
        }

        SettingsGroupCard(title = if (isRu) "Производительность" else "Performance") {
            SettingsToggleRow(
                title = if (isRu) "Уменьшение движения" else "Reduce motion",
                subtitle = if (isRu) "Отключает сложные 3D- и пружинные эффекты" else "Disables heavy 3D and spring effects",
                checked = state.reduceMotionEnabled,
                onCheckedChange = { v -> AnimationsSettingsStore.update { it.copy(reduceMotionEnabled = v) } }
            )
            SettingsCardDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("0.5x", "1.0x", "1.5x", "2.0x").forEach { opt ->
                    val selected = state.animationSpeedOption == opt
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) scheme.accent else scheme.surface)
                            .border(1.dp, if (selected) scheme.accent else scheme.cardBorder, RoundedCornerShape(10.dp))
                            .clickable { AnimationsSettingsStore.update { it.copy(animationSpeedOption = opt) } }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = opt,
                            color = if (selected) scheme.onAccent else scheme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
