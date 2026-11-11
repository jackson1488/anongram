package com.anongram.app.features.settings.search.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.anongram.app.features.settings.navigation.SettingsRoute

/**
 * Модель элемента результатов умного поиска по настройкам AnonGram.
 */
data class SettingsSearchItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetRoute: SettingsRoute,
    val category: String,
    val icon: ImageVector,
    val iconBackgroundColor: Color,
    val keywords: List<String> = emptyList()
)
