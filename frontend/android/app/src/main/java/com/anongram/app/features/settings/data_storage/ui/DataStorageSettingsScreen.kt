package com.anongram.app.features.settings.data_storage.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.data_storage.ui.components.StorageUsageCard

/**
 * Экран «Данные и память» (`features/settings/data_storage/`).
 */
@Composable
fun DataStorageSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    var cacheSizeMb by remember { mutableIntStateOf(42) }

    com.anongram.app.features.settings.common.ui.SettingsGlassScaffold(
        title = "Данные и память",
        onBack = onBack
    ) {
        StorageUsageCard(
            cacheSizeMb = cacheSizeMb,
            onClearCache = {
                cacheSizeMb = 0
                Toast.makeText(context, "Кэш медиафайлов очищен", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
