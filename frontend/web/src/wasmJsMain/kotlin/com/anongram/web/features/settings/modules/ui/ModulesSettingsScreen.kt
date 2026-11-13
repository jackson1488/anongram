package com.anongram.web.features.settings.modules.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.features.settings.ui.SettingsCardDivider
import com.anongram.web.features.settings.ui.SettingsGroupCard
import com.anongram.web.features.settings.ui.SettingsNavRow
import com.anongram.web.features.settings.ui.SettingsSubScreenScaffold
import com.anongram.web.features.settings.ui.SettingsToggleRow
import com.anongram.web.platform.WebStorage

@Composable
fun ModulesSettingsScreen(
    onOpenVoice: () -> Unit,
    onOpenTranslation: () -> Unit,
    onBack: () -> Unit
) {
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var voiceEnabled by remember { mutableStateOf(WebStorage.getString("anongram_mod_voice")?.toBooleanStrictOrNull() ?: true) }
    var translationEnabled by remember { mutableStateOf(LanguageSettingsStore.showTranslateButton.value) }

    SettingsSubScreenScaffold(
        title = if (isRu) "Модули" else "Modules",
        onBack = onBack
    ) {
        SettingsGroupCard(title = if (isRu) "Активные модули" else "Active Modules") {
            SettingsNavRow(
                title = if (isRu) "Голосовые сообщения и транскрибация" else "Voice Notes & Transcription",
                value = if (voiceEnabled) (if (isRu) "Вкл" else "On") else (if (isRu) "Выкл" else "Off"),
                onClick = onOpenVoice
            )
            SettingsCardDivider()
            SettingsNavRow(
                title = if (isRu) "Перевод сообщений и чатов" else "Message & Chat Translation",
                value = if (translationEnabled) (if (isRu) "Вкл" else "On") else (if (isRu) "Выкл" else "Off"),
                onClick = onOpenTranslation
            )
        }

        SettingsGroupCard(title = if (isRu) "Быстрое управление" else "Quick Toggles") {
            SettingsToggleRow(
                title = if (isRu) "Модуль голосовых сообщений" else "Voice notes module",
                checked = voiceEnabled,
                onCheckedChange = {
                    voiceEnabled = it
                    WebStorage.putString("anongram_mod_voice", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Кнопка «Перевести»" else "Show Translate button",
                checked = translationEnabled,
                onCheckedChange = {
                    translationEnabled = it
                    LanguageSettingsStore.setShowTranslateButton(it)
                }
            )
        }
    }
}

@Composable
fun VoiceModuleSettingsScreen(onBack: () -> Unit) {
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var voiceEnabled by remember { mutableStateOf(WebStorage.getString("anongram_mod_voice")?.toBooleanStrictOrNull() ?: true) }
    var autoTranscribe by remember { mutableStateOf(WebStorage.getString("anongram_mod_voice_transcribe")?.toBooleanStrictOrNull() ?: true) }
    var noiseSuppression by remember { mutableStateOf(WebStorage.getString("anongram_mod_voice_denoise")?.toBooleanStrictOrNull() ?: true) }

    SettingsSubScreenScaffold(
        title = if (isRu) "Голосовые сообщения" else "Voice Notes",
        onBack = onBack
    ) {
        SettingsGroupCard {
            SettingsToggleRow(
                title = if (isRu) "Включить модуль" else "Enable module",
                checked = voiceEnabled,
                onCheckedChange = {
                    voiceEnabled = it
                    WebStorage.putString("anongram_mod_voice", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Локальная расшифровка в текст" else "On-device speech-to-text",
                checked = autoTranscribe,
                onCheckedChange = {
                    autoTranscribe = it
                    WebStorage.putString("anongram_mod_voice_transcribe", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Шумоподавление" else "Noise suppression",
                checked = noiseSuppression,
                onCheckedChange = {
                    noiseSuppression = it
                    WebStorage.putString("anongram_mod_voice_denoise", it.toString())
                }
            )
        }
    }
}

@Composable
fun TranslationModuleSettingsScreen(onBack: () -> Unit) {
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU
    val showBtn by LanguageSettingsStore.showTranslateButton.collectAsState()
    val translateAll by LanguageSettingsStore.translateEntireChats.collectAsState()

    SettingsSubScreenScaffold(
        title = if (isRu) "Перевод чатов" else "Chat Translation",
        onBack = onBack
    ) {
        SettingsGroupCard {
            SettingsToggleRow(
                title = if (isRu) "Показывать кнопку «Перевести»" else "Show Translate button",
                checked = showBtn,
                onCheckedChange = { LanguageSettingsStore.setShowTranslateButton(it) }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Переводить чаты целиком" else "Translate entire chats",
                checked = translateAll,
                onCheckedChange = { LanguageSettingsStore.setTranslateEntireChats(it) }
            )
        }
    }
}
