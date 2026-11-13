package com.anongram.app.features.settings.modules.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.assets.ExternalAssetManager
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.settings.modules.data.ModulesSettingsStore
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import com.anongram.app.features.settings.modules.ui.translation.TranslationModuleSection
import com.anongram.app.features.settings.modules.ui.voice.VoiceModuleSection
import com.anongram.app.features.settings.modules.ui.voice.formatMb

/**
 * Экран «Модули» (`features/settings/modules/`):
 * Единый центр управления внешними модулями (ГС: Whisper/Vosk/Системный,
 * локальный перевод сообщений, управление скачанными пакетами).
 */
@Composable
fun ModulesSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = ModulesI18n.current

    val assetManager = remember { ExternalAssetManager.getInstance(context) }
    val assetStates by assetManager.states.collectAsState()
    val settings by ModulesSettingsStore.state.collectAsState()

    LaunchedEffect(Unit) {
        ModulesSettingsStore.ensureInitialized(context)
        assetManager.refreshAllStates()
    }

    BackHandler(onBack = onBack)

    AnonGramScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Верхняя панель
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg)
                        .border(0.8.dp, scheme.islandBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = strings.modulesTitle,
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.size(40.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // 1. Расшифровка ГС
                VoiceModuleSection(
                    settings = settings,
                    assetStates = assetStates,
                    assetManager = assetManager
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 2. Локальный перевод
                TranslationModuleSection()

                Spacer(modifier = Modifier.height(14.dp))

                // 3. Память модулей
                val totalBytes = remember(assetStates) { assetManager.totalInstalledBytes() }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(scheme.card)
                        .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = strings.storageSectionTitle,
                        color = scheme.textPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = strings.storageUsedLabel(formatMb(totalBytes)),
                        color = scheme.textSecondary,
                        fontSize = 13.sp
                    )
                    if (totalBytes > 0L) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CrimsonKillSwitch.copy(alpha = 0.12f))
                                .border(1.dp, CrimsonKillSwitch.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .clickable { assetManager.clearAllAssets() },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.clearAllModulesBtn,
                                color = CrimsonKillSwitch,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
