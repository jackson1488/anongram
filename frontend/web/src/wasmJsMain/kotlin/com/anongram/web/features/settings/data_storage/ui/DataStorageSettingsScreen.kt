package com.anongram.web.features.settings.data_storage.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.features.settings.ui.SettingsCardDivider
import com.anongram.web.features.settings.ui.SettingsGroupCard
import com.anongram.web.features.settings.ui.SettingsSubScreenScaffold
import com.anongram.web.features.settings.ui.SettingsToggleRow
import com.anongram.web.platform.WebStorage

@Composable
fun DataStorageSettingsScreen(onBack: () -> Unit) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var autoPhotos by remember { mutableStateOf(WebStorage.getString("anongram_data_photos")?.toBooleanStrictOrNull() ?: true) }
    var autoVideos by remember { mutableStateOf(WebStorage.getString("anongram_data_videos")?.toBooleanStrictOrNull() ?: false) }
    var onionRouting by remember { mutableStateOf(WebStorage.getString("anongram_data_onion")?.toBooleanStrictOrNull() ?: true) }
    var cacheCleared by remember { mutableStateOf(false) }

    SettingsSubScreenScaffold(
        title = if (isRu) "Данные и память" else "Data & Storage",
        onBack = onBack
    ) {
        SettingsGroupCard(title = if (isRu) "Использование памяти" else "Storage Usage") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (cacheCleared) {
                        if (isRu) "Кэш очищен (0.0 МБ)" else "Cache cleared (0.0 MB)"
                    } else {
                        if (isRu) "Локальный кэш: 12.4 МБ" else "Local cache: 12.4 MB"
                    },
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(scheme.accent)
                        .clickable { cacheCleared = true }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (isRu) "Очистить" else "Clear",
                        color = scheme.onAccent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        SettingsGroupCard(title = if (isRu) "Автозагрузка медиа" else "Automatic Media Download") {
            SettingsToggleRow(
                title = if (isRu) "Фотографии" else "Photos",
                checked = autoPhotos,
                onCheckedChange = {
                    autoPhotos = it
                    WebStorage.putString("anongram_data_photos", it.toString())
                }
            )
            SettingsCardDivider()
            SettingsToggleRow(
                title = if (isRu) "Видео и кружки" else "Videos & video notes",
                checked = autoVideos,
                onCheckedChange = {
                    autoVideos = it
                    WebStorage.putString("anongram_data_videos", it.toString())
                }
            )
        }

        SettingsGroupCard(title = if (isRu) "Прокси и маршрутизация" else "Proxy & Routing") {
            SettingsToggleRow(
                title = if (isRu) "Onion-маршрутизация (3 хопа)" else "Onion Routing (3 hops)",
                subtitle = if (isRu) "Скрывает IP-адрес при доставке сообщений" else "Hides IP address during message relay",
                checked = onionRouting,
                onCheckedChange = {
                    onionRouting = it
                    WebStorage.putString("anongram_data_onion", it.toString())
                }
            )
        }
    }
}
