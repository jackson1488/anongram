package com.anongram.web.features.settings.animations.ui.messages

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.web.features.settings.animations.data.MessageAppearStyle
import com.anongram.web.features.settings.animations.data.MessageDeleteStyle
import com.anongram.web.features.settings.animations.data.TokenStreamSpeed
import com.anongram.web.features.settings.ui.SettingsCardDivider
import com.anongram.web.features.settings.ui.SettingsChoiceRow
import com.anongram.web.features.settings.ui.SettingsGroupCard
import com.anongram.web.features.settings.ui.SettingsSubScreenScaffold
import com.anongram.web.features.settings.ui.SettingsToggleRow

@Composable
fun MessageAnimationsSettingsScreen(onBack: () -> Unit) {
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU
    val state by AnimationsSettingsStore.state.collectAsState()

    SettingsSubScreenScaffold(
        title = if (isRu) "Анимация сообщений" else "Message Animations",
        onBack = onBack
    ) {
        SettingsGroupCard {
            SettingsToggleRow(
                title = if (isRu) "Анимация появления сообщений" else "Message appear animation",
                checked = state.messageAppearEnabled,
                onCheckedChange = { v -> AnimationsSettingsStore.update { it.copy(messageAppearEnabled = v) } }
            )
        }

        if (state.messageAppearEnabled) {
            SettingsGroupCard(title = if (isRu) "Стиль появления" else "Appear Style") {
                val styles = listOf(
                    MessageAppearStyle.TOKEN_STREAM to (if (isRu) "Потоковая печать" else "Token Stream"),
                    MessageAppearStyle.SPRING_FLY to (if (isRu) "Пружинный полёт" else "Spring Fly"),
                    MessageAppearStyle.SLIDE_FADE to (if (isRu) "Плавный слайд" else "Slide & Fade"),
                    MessageAppearStyle.CORNER_POP to (if (isRu) "Вылет из угла" else "Corner Pop")
                )
                styles.forEachIndexed { idx, (style, label) ->
                    SettingsChoiceRow(
                        title = label,
                        selected = state.messageAppearStyle == style,
                        onClick = { AnimationsSettingsStore.update { it.copy(messageAppearStyle = style) } }
                    )
                    if (idx < styles.lastIndex) SettingsCardDivider()
                }
            }

            if (state.messageAppearStyle == MessageAppearStyle.TOKEN_STREAM) {
                SettingsGroupCard(title = if (isRu) "Скорость потока" else "Token Stream Speed") {
                    TokenStreamSpeed.entries.forEachIndexed { idx, spd ->
                        SettingsChoiceRow(
                            title = spd.id.uppercase(),
                            subtitle = "${spd.tokenDelayMs} ms / token",
                            selected = state.tokenStreamSpeed == spd,
                            onClick = { AnimationsSettingsStore.update { it.copy(tokenStreamSpeed = spd) } }
                        )
                        if (idx < TokenStreamSpeed.entries.lastIndex) SettingsCardDivider()
                    }
                }
            }
        }

        SettingsGroupCard(title = if (isRu) "Эффект удаления сообщения" else "Message Delete Effect") {
            val delStyles = listOf(
                MessageDeleteStyle.PARTICLES to (if (isRu) "Рассыпание в частицы" else "Particle Dissolve"),
                MessageDeleteStyle.PUZZLE to (if (isRu) "Сжатие пазла" else "Puzzle Collapse"),
                MessageDeleteStyle.SLIDE_OUT to (if (isRu) "Выезд за экран" else "Slide Out"),
                MessageDeleteStyle.NONE to (if (isRu) "Без эффекта" else "None")
            )
            delStyles.forEachIndexed { idx, (ds, label) ->
                SettingsChoiceRow(
                    title = label,
                    selected = state.messageDeleteStyle == ds,
                    onClick = { AnimationsSettingsStore.update { it.copy(messageDeleteStyle = ds) } }
                )
                if (idx < delStyles.lastIndex) SettingsCardDivider()
            }
        }
    }
}
