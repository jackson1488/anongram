package com.anongram.app.features.settings.language.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.features.settings.language.i18n.LanguageI18n
import com.anongram.app.features.settings.language.model.LanguageCatalog
import com.anongram.app.features.settings.language.ui.components.LanguageListCard
import com.anongram.app.features.settings.language.ui.components.LanguageTopBar
import com.anongram.app.features.settings.language.ui.components.LanguageTranslationCard
import com.anongram.app.features.settings.language.ui.components.TelegramRadioButton

/**
 * Полноэкранный экран «Язык» в стиле Telegram:
 * 1. Верхний бар с круглой кнопкой Назад и центрированным заголовком «Язык».
 * 2. Карточка «Перевод сообщений» с тумблерами и анимированной строкой «Не переводить».
 * 3. Карточка «Язык» с радиокнопками слева и мгновенным переключением (Русский / English).
 */
@Composable
fun LanguageSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLanguage by LanguageSettingsStore.language.collectAsState()
    val showTranslateButton by LanguageSettingsStore.showTranslateButton.collectAsState()
    val translateEntireChats by LanguageSettingsStore.translateEntireChats.collectAsState()
    val doNotTranslateLanguage by LanguageSettingsStore.doNotTranslateLanguage.collectAsState()

    val strings = remember(currentLanguage) { LanguageI18n.forLanguage(currentLanguage) }

    var showDoNotTranslateDialog by remember { mutableStateOf(false) }

    // Обработка системного «Назад»
    BackHandler(onBack = onBack)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя панель в стиле Telegram
            LanguageTopBar(
                title = strings.screenTitle,
                onBack = onBack
            )

            // Основной скроллируемый контент
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Карточка 1: Перевод сообщений
                LanguageTranslationCard(
                    strings = strings,
                    showTranslateButton = showTranslateButton,
                    translateEntireChats = translateEntireChats,
                    doNotTranslateLanguage = doNotTranslateLanguage,
                    onShowTranslateButtonChange = {
                        LanguageSettingsStore.setShowTranslateButton(context, it)
                    },
                    onTranslateEntireChatsChange = {
                        LanguageSettingsStore.setTranslateEntireChats(context, it)
                    },
                    onDoNotTranslateClick = {
                        showDoNotTranslateDialog = true
                    }
                )

                // Карточка 2: Список языков с радиокнопками слева (Русский и English)
                LanguageListCard(
                    languages = LanguageCatalog.languages,
                    selectedLanguageCode = currentLanguage.code,
                    strings = strings,
                    onSelectLanguage = { lang ->
                        when (lang.code.lowercase()) {
                            "ru" -> LanguageSettingsStore.setLanguage(context, AppLanguage.RU)
                            "en" -> LanguageSettingsStore.setLanguage(context, AppLanguage.EN)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(40.dp))
            }
        }

        // Диалог выбора языка исключения («Не переводить»)
        if (showDoNotTranslateDialog) {
            val options = LanguageCatalog.languages.map { it.nativeName }
            AlertDialog(
                onDismissRequest = { showDoNotTranslateDialog = false },
                containerColor = CardDark,
                title = {
                    Text(
                        text = strings.doNotTranslateTitle,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        options.forEach { opt ->
                            val isSelected = opt.equals(doNotTranslateLanguage, ignoreCase = true)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        LanguageSettingsStore.setDoNotTranslateLanguage(context, opt)
                                        showDoNotTranslateDialog = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TelegramRadioButton(
                                    selected = isSelected,
                                    activeColor = Color(0xFFB388FF)
                                )
                                Text(
                                    text = opt,
                                    color = TextPrimary,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 14.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showDoNotTranslateDialog = false }) {
                        Text(text = strings.close, color = Color(0xFFB388FF))
                    }
                }
            )
        }
    }
}
