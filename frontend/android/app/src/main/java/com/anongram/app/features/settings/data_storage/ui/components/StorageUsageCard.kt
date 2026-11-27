package com.anongram.app.features.settings.data_storage.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.settings.data_storage.data.StorageStats
import com.anongram.app.features.settings.data_storage.data.StorageUsageManager

/**
 * Карточка реальной статистики кэша, локальных данных и их очистки.
 */
@Composable
fun StorageUsageCard(
    stats: StorageStats,
    onClearCache: () -> Unit,
    onClearAllData: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    val cacheFormatted = StorageUsageManager.formatSize(stats.cacheBytes, isRu)
    val dataFormatted = StorageUsageManager.formatSize(stats.dataBytes, isRu)
    val cacheRatio = if (stats.totalBytes > 0L) {
        (stats.cacheBytes.toFloat() / stats.totalBytes.toFloat()).coerceIn(0.03f, 1f)
    } else {
        0.02f
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.card)
            .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRu) "Кэш медиа и файлов" else "Media & file cache",
                color = scheme.textPrimary,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = cacheFormatted,
                color = scheme.accent,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRu) "Локальная база и настройки" else "Local database & settings",
                color = scheme.textSecondary,
                fontSize = 13.5.sp
            )
            Text(
                text = dataFormatted,
                color = scheme.textSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LinearProgressIndicator(
            progress = { cacheRatio },
            color = scheme.accent,
            trackColor = scheme.surface,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onClearCache,
            colors = ButtonDefaults.buttonColors(
                containerColor = scheme.accent,
                contentColor = scheme.onAccent
            ),
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
        ) {
            Text(
                text = if (isRu) "Очистить кэш ($cacheFormatted)" else "Clear cache ($cacheFormatted)",
                fontWeight = FontWeight.SemiBold
            )
        }

        if (onClearAllData != null) {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = onClearAllData,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFFEF4444)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x66EF4444)),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Text(
                    text = if (isRu) "Очистить все локальные данные" else "Clear all local data",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
