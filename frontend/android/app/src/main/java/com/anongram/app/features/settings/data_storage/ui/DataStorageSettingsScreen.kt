package com.anongram.app.features.settings.data_storage.ui

import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.settings.data_storage.data.StorageStats
import com.anongram.app.features.settings.data_storage.data.StorageUsageManager
import com.anongram.app.features.settings.data_storage.ui.components.StorageUsageCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Экран «Данные и память» (`features/settings/data_storage/`).
 */
@Composable
fun DataStorageSettingsScreen(
    onBack: () -> Unit,
    allowClearAllLocalData: Boolean = false,
    onAllLocalDataCleared: () -> Unit = {}
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val scope = rememberCoroutineScope()
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var stats by remember { mutableStateOf(StorageStats(cacheBytes = 0L, dataBytes = 0L)) }
    var isConfirmClearAllOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        stats = withContext(Dispatchers.IO) {
            StorageUsageManager.calculateUsage(context)
        }
    }

    com.anongram.app.features.settings.common.ui.SettingsGlassScaffold(
        title = if (isRu) "Данные и память" else "Data and Storage",
        onBack = onBack
    ) {
        StorageUsageCard(
            stats = stats,
            onClearCache = {
                scope.launch {
                    stats = withContext(Dispatchers.IO) {
                        StorageUsageManager.clearCache(context)
                    }
                    Toast.makeText(
                        context,
                        if (isRu) "Кэш медиафайлов очищен" else "Media cache cleared",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onClearAllData = if (allowClearAllLocalData) {
                { isConfirmClearAllOpen = true }
            } else null
        )
    }

    if (isConfirmClearAllOpen) {
        AlertDialog(
            onDismissRequest = { isConfirmClearAllOpen = false },
            containerColor = scheme.card,
            titleContentColor = scheme.textPrimary,
            textContentColor = scheme.textSecondary,
            title = {
                Text(
                    text = if (isRu) "Очистить все локальные данные?" else "Clear all local data?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isRu) {
                        "Кэш, локальная база, чаты и настройки на этом устройстве будут удалены."
                    } else {
                        "Cache, local database, chats and settings on this device will be deleted."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isConfirmClearAllOpen = false
                        scope.launch {
                            stats = withContext(Dispatchers.IO) {
                                StorageUsageManager.clearAllLocalData(context)
                            }
                            Toast.makeText(
                                context,
                                if (isRu) "Локальные данные очищены" else "Local data cleared",
                                Toast.LENGTH_SHORT
                            ).show()
                            onAllLocalDataCleared()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text(if (isRu) "Очистить" else "Clear", fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isConfirmClearAllOpen = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = scheme.textPrimary)
                ) {
                    Text(if (isRu) "Отмена" else "Cancel")
                }
            }
        )
    }
}
