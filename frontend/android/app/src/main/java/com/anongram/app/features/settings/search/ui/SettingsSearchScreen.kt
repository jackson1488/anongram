package com.anongram.app.features.settings.search.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.navigation.SettingsRoute
import com.anongram.app.features.settings.search.domain.SettingsSearchEngine
import com.anongram.app.features.settings.search.i18n.SearchI18n
import com.anongram.app.features.settings.search.ui.components.SettingsSearchResultRow
import com.anongram.app.features.settings.search.ui.components.SettingsSearchTopBar
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

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
    val scheme = LocalAnonGramScheme.current
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


    val animState by AnimationsSettingsStore.state.collectAsState()
    val hazeState = remember { HazeState() }

    AnonGramScreenSurface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(state = hazeState)
                    .background(scheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 68.dp, bottom = 16.dp)
                        .padding(horizontal = 14.dp)
                ) {
                if (query.isNotBlank()) {
                    if (searchResults.isEmpty()) {
                        // Пустое состояние
                        Spacer(modifier = Modifier.height(60.dp))
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(scheme.card)
                                .border(1.dp, scheme.cardBorder, CircleShape)
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = scheme.textSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = strings.emptyTitle,
                            color = scheme.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = strings.emptySubtitle,
                            color = scheme.textSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        // Список найденных результатов
                        Text(
                            text = strings.settingsCategory,
                            color = scheme.textSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(start = 6.dp, bottom = 8.dp)
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(scheme.card)
                                .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                        ) {
                            searchResults.forEachIndexed { index, item ->
                                if (index > 0) {
                                    HorizontalDivider(
                                        color = scheme.divider,
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
                        color = scheme.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 6.dp, bottom = 8.dp, top = 4.dp)
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                    ) {
                        allItems.forEachIndexed { index, item ->
                            if (index > 0) {
                                HorizontalDivider(
                                    color = scheme.divider,
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

            val topBarModifier = if (animState.glassBlurEnabled) {
                Modifier.hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = scheme.background,
                        tint = HazeTint(scheme.surface.copy(alpha = if (scheme.isDark) 0.34f else 0.44f)),
                        blurRadius = 22.dp,
                        noiseFactor = 0.02f
                    )
                ) {
                    progressive = HazeProgressive.verticalGradient(
                        startIntensity = 1f,
                        endIntensity = 0f
                    )
                }
            } else {
                Modifier.background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scheme.surface.copy(alpha = 0.92f),
                            scheme.background.copy(alpha = 0.65f),
                            Color.Transparent
                        )
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .then(topBarModifier)
            ) {
                SettingsSearchTopBar(
                    query = query,
                    onQueryChange = { query = it },
                    placeholder = strings.searchPlaceholder,
                    onBack = onBack
                )
                Spacer(modifier = Modifier.height(14.dp))
            }
        }
    }
}
