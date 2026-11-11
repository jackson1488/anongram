package com.anongram.app.features.settings.search.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.navigation.SettingsRoute
import com.anongram.app.features.settings.search.domain.SettingsSearchEngine
import com.anongram.app.features.settings.search.i18n.SearchI18n
import com.anongram.app.features.settings.search.ui.components.SettingsSearchResultRow
import com.anongram.app.features.settings.search.ui.components.SettingsSearchTopBar

/**
 * Экран поиска по настройкам AnonGram (`features/settings/search/`).
 * Обеспечивает быстрый доступ ко всем подразделам с ранжированием и поддержкой i18n.
 */
@Composable
fun SettingsSearchScreen(
    onNavigate: (SettingsRoute) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val language by LanguageSettingsStore.language.collectAsState()
    val strings = remember(language) { SearchI18n.forLanguage(language) }

    var query by remember { mutableStateOf("") }

    val allItems = remember(language) { SettingsSearchEngine.getAllItems(language) }
    val searchResults = remember(query, language) {
        if (query.isBlank()) {
            emptyList()
        } else {
            SettingsSearchEngine.search(query, language)
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050508))
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
        // Верхняя панель поиска (по скриншоту)
        SettingsSearchTopBar(
            query = query,
            onQueryChange = { query = it },
            placeholder = strings.searchPlaceholder,
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            if (query.isNotBlank()) {
                if (searchResults.isEmpty()) {
                    // Пустое состояние
                    Spacer(modifier = Modifier.height(60.dp))
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = Color(0xFF8E99A8),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = strings.emptyTitle,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = strings.emptySubtitle,
                        color = Color(0xFF8E99A8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Список найденных результатов
                    Text(
                        text = strings.settingsCategory,
                        color = Color(0xFF8E99A8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF141318))
                    ) {
                        searchResults.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = Color.White.copy(alpha = 0.06f),
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                            SettingsSearchResultRow(
                                item = item,
                                onClick = { onNavigate(item.targetRoute) }
                            )
                        }
                    }
                }
            } else {
                // Список всех разделов при пустом вводе
                Text(
                    text = strings.recentOrPopular,
                    color = Color(0xFF8E99A8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 4.dp)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141318))
                ) {
                    allItems.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.06f),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                        SettingsSearchResultRow(
                            item = item,
                            onClick = { onNavigate(item.targetRoute) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
