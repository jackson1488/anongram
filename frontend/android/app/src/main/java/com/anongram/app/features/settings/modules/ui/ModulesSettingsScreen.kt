package com.anongram.app.features.settings.modules.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.assets.ExternalAssetManager
import com.anongram.app.core.assets.ModuleAssetManifest
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.chats.tools.translate.LocalMessageTranslator
import com.anongram.app.features.settings.modules.data.ModulesSettingsStore
import com.anongram.app.features.settings.modules.data.VoiceSttEngine
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import com.anongram.app.features.settings.modules.ui.translation.TranslationModuleSection
import com.anongram.app.features.settings.modules.ui.voice.formatMb

/**
 * Экран-хаб «Модули» (`features/settings/modules/`):
 * Чистый плоский список без баблов и кружков. У каждого модуля отображается его собственная память и настройки.
 */
@Composable
fun ModulesSettingsScreen(
    onOpenVoiceModule: () -> Unit,
    onOpenTranslationModule: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = ModulesI18n.current

    val assetManager = remember { ExternalAssetManager.getInstance(context) }
    val assetStates by assetManager.states.collectAsState()
    val settings by ModulesSettingsStore.state.collectAsState()
    val downloadedTranslatePacks by LocalMessageTranslator.downloadedLanguages.collectAsState()

    LaunchedEffect(Unit) {
        ModulesSettingsStore.ensureInitialized(context)
        assetManager.refreshAllStates()
        LocalMessageTranslator.refreshDownloadedModels()
    }

    BackHandler(onBack = onBack)

    AnonGramScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Чистая плоская шапка
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(scheme.surface)
                    .height(56.dp)
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = strings.back,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = strings.modulesTitle,
                    color = scheme.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. Расшифровка голосовых (ГС) — активный движок + своя память
                val activeEngineLabel = when (settings.voiceEngine) {
                    VoiceSttEngine.WHISPER -> strings.engineWhisperTitle
                    VoiceSttEngine.VOSK -> strings.engineVoskTitle
                    VoiceSttEngine.SYSTEM_GOOGLE -> strings.engineSystemTitle
                }
                val isRecommended = settings.voiceEngine != VoiceSttEngine.SYSTEM_GOOGLE
                val voiceBytes = remember(assetStates) {
                    assetManager.installedBytesForSpecs(ModuleAssetManifest.allVoice)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenVoiceModule)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = null,
                            tint = scheme.accent,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.voiceSectionTitle,
                                color = scheme.textPrimary,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$activeEngineLabel • ${strings.moduleMemoryLabel(formatMb(voiceBytes))}",
                                color = if (isRecommended) scheme.textSecondary else CrimsonKillSwitch,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = scheme.textMuted,
                        modifier = Modifier.size(22.dp)
                    )
                }

                HorizontalDivider(
                    color = scheme.divider,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 56.dp)
                )

                // 2. Локальный перевод сообщений — статус и переход в настройки
                TranslationModuleSection(
                    isEnabled = settings.showTranslateButton || settings.autoTranslateIncoming,
                    downloadedCount = downloadedTranslatePacks.size,
                    onClick = onOpenTranslationModule
                )

                HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
            }
        }
    }
}
