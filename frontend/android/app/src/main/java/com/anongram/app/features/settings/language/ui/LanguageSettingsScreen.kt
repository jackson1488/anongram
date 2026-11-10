package com.anongram.app.features.settings.language.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.features.settings.language.i18n.LanguageI18n
import com.anongram.app.features.settings.language.model.LanguageCatalog
import com.anongram.app.features.settings.language.model.LanguageItem
import com.anongram.app.features.settings.language.ui.components.LanguageCustomCard
import com.anongram.app.features.settings.language.ui.components.LanguageListCard
import com.anongram.app.features.settings.language.ui.components.LanguageTopBar
import com.anongram.app.features.settings.language.ui.components.LanguageTranslationCard
import com.anongram.app.features.settings.language.ui.components.TelegramRadioButton

/**
 * Полноэкранный экран «Язык» в стиле Telegram:
 * 1. Верхний бар с круглыми кнопками (Назад, Поиск).
 * 2. Карточка «Перевод сообщений» с тумблерами и анимированной строкой «Не переводить».
 * 3. Карточка «Язык» со специальным пунктом SpermoGram.
 * 4. Карточка полного списка языков с Telegram-радиокнопками слева и мгновенным переключением.
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
    val isCustomLangActive by LanguageSettingsStore.isCustomLanguageActive.collectAsState()

    val strings = remember(currentLanguage) { LanguageI18n.forLanguage(currentLanguage) }

    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var showDoNotTranslateDialog by remember { mutableStateOf(false) }

    // Обработка системного «Назад»
    BackHandler {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            onBack()
        }
    }

    // Фильтрация списка языков по запросу
    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            LanguageCatalog.languages
        } else {
            val q = searchQuery.trim().lowercase()
            LanguageCatalog.languages.filter {
                it.nativeName.lowercase().contains(q) ||
                it.englishName.lowercase().contains(q) ||
                it.code.lowercase().contains(q)
            }
        }
    }

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
                searchQuery = searchQuery,
                isSearchActive = isSearchActive,
                searchPlaceholder = strings.searchPlaceholder,
                onBack = onBack,
                onSearchQueryChange = { searchQuery = it },
                onSearchActiveToggle = { active ->
                    isSearchActive = active
                    if (!active) searchQuery = ""
                }
            )

            // Основной скроллируемый контент
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (!isSearchActive) {
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

                    // Карточка 2: Кастомный язык (SpermoGram)
                    LanguageCustomCard(
                        strings = strings,
                        isSelected = isCustomLangActive,
                        onSelect = {
                            val nextState = !isCustomLangActive
                            LanguageSettingsStore.setCustomLanguageActive(context, nextState)
                            if (nextState) {
                                Toast.makeText(
                                    context,
                                    "SpermoGram активирован",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                // Карточка 3: Список языков с радиокнопками слева
                LanguageListCard(
                    languages = filteredLanguages,
                    selectedLanguageCode = currentLanguage.code,
                    isCustomLanguageSelected = isCustomLangActive,
                    strings = strings,
                    onSelectLanguage = { lang ->
                        when (lang.code.lowercase()) {
                            "ru" -> {
                                LanguageSettingsStore.setLanguage(context, AppLanguage.RU)
                            }
                            "en" -> {
                                LanguageSettingsStore.setLanguage(context, AppLanguage.EN)
                            }
                            else -> {
                                Toast.makeText(
                                    context,
                                    strings.languageAddedNotice(lang.nativeName),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Диалог выбора языка исключения («Не переводить»)
        if (showDoNotTranslateDialog) {
            val options = listOf("Русский", "English", "Любой")
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
                                Spacer(modifier = Modifier.height(0.dp))
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
                        Text(text = "Закрыть", color = Color(0xFFB388FF))
                    }
                }
            )
        }
    }
}
