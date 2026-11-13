package com.anongram.app.features.settings.modules.ui.voice

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.assets.AssetDownloadState
import com.anongram.app.core.assets.ExternalAssetManager
import com.anongram.app.core.assets.ModuleAssetManifest
import com.anongram.app.core.assets.ModuleAssetSpec
import com.anongram.app.core.theme.CrimsonKillSwitch
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.settings.modules.data.ModulesSettingsState
import com.anongram.app.features.settings.modules.data.ModulesSettingsStore
import com.anongram.app.features.settings.modules.data.VoiceSttEngine
import com.anongram.app.features.settings.modules.i18n.ModulesI18n

/**
 * Плоский чистый экран настроек модуля ГС (без баблов и кружков):
 * 1) Движок (Whisper / Vosk / Системный-Google с предупреждением)
 * 2) Собственная память ГС + язык + список моделей с кнопкой удаления справа у каждого скачанного пака.
 */
@Composable
fun VoiceModuleSection(
    settings: ModulesSettingsState,
    assetStates: Map<String, AssetDownloadState>,
    assetManager: ExternalAssetManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = ModulesI18n.current
    var showGoogleWarning by remember { mutableStateOf(false) }

    if (showGoogleWarning) {
        GooglePrivacyWarningDialog(
            onKeepLocal = { showGoogleWarning = false },
            onAcceptAnyway = {
                showGoogleWarning = false
                ModulesSettingsStore.setVoiceEngine(context, VoiceSttEngine.SYSTEM_GOOGLE)
            }
        )
    }

    val voiceBytes = remember(assetStates) {
        assetManager.installedBytesForSpecs(ModuleAssetManifest.allVoice)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Секция 1: Движок распознавания
        Text(
            text = strings.engineHeader,
            color = scheme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )

        EngineFlatRow(
            title = strings.engineWhisperTitle,
            description = strings.engineWhisperDesc,
            badgeText = strings.recommendedBadge,
            isRecommended = true,
            selected = settings.voiceEngine == VoiceSttEngine.WHISPER,
            onClick = { ModulesSettingsStore.setVoiceEngine(context, VoiceSttEngine.WHISPER) }
        )
        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

        EngineFlatRow(
            title = strings.engineVoskTitle,
            description = strings.engineVoskDesc,
            badgeText = strings.recommendedBadge,
            isRecommended = true,
            selected = settings.voiceEngine == VoiceSttEngine.VOSK,
            onClick = { ModulesSettingsStore.setVoiceEngine(context, VoiceSttEngine.VOSK) }
        )
        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

        EngineFlatRow(
            title = strings.engineSystemTitle,
            description = strings.engineSystemDesc,
            badgeText = strings.notRecommendedBadge,
            isRecommended = false,
            selected = settings.voiceEngine == VoiceSttEngine.SYSTEM_GOOGLE,
            onClick = {
                if (settings.voiceEngine != VoiceSttEngine.SYSTEM_GOOGLE) {
                    showGoogleWarning = true
                }
            }
        )
        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

        Spacer(modifier = Modifier.height(8.dp))

        // Секция 2: Модели, языки и собственная память раздела ГС
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.modelsAndLanguagesHeader,
                color = scheme.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = strings.moduleMemoryLabel(formatMb(voiceBytes)),
                color = scheme.textSecondary,
                fontSize = 12.sp
            )
        }

        when (settings.voiceEngine) {
            VoiceSttEngine.WHISPER -> {
                LanguageFlatSelector(
                    title = strings.recognitionLanguageLabel,
                    options = listOf(
                        "auto" to strings.langAuto,
                        "ru" to strings.langRu,
                        "en" to strings.langEn
                    ),
                    selectedCode = settings.whisperLanguage,
                    onSelect = { ModulesSettingsStore.setWhisperLanguage(context, it) }
                )
                HorizontalDivider(color = scheme.divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                ModuleAssetManifest.allVoiceWhisper.forEachIndexed { index, spec ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = scheme.divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    AssetPackFlatRow(
                        spec = spec,
                        downloadState = assetStates[spec.id] ?: AssetDownloadState.NotInstalled,
                        isSelected = settings.whisperModelId == spec.id,
                        onSelect = { ModulesSettingsStore.setWhisperModelId(context, spec.id) },
                        onDownload = { assetManager.startDownload(spec) },
                        onCancel = { assetManager.cancelDownload(spec.id) },
                        onDelete = { assetManager.deleteAsset(spec.id) }
                    )
                }
            }

            VoiceSttEngine.VOSK -> {
                ModuleAssetManifest.allVoiceVosk.forEachIndexed { index, spec ->
                    if (index > 0) {
                        HorizontalDivider(
                            color = scheme.divider,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    AssetPackFlatRow(
                        spec = spec,
                        downloadState = assetStates[spec.id] ?: AssetDownloadState.NotInstalled,
                        isSelected = settings.voskAssetId == spec.id,
                        onSelect = { ModulesSettingsStore.setVoskAssetId(context, spec.id) },
                        onDownload = { assetManager.startDownload(spec) },
                        onCancel = { assetManager.cancelDownload(spec.id) },
                        onDelete = { assetManager.deleteAsset(spec.id) }
                    )
                }
            }

            VoiceSttEngine.SYSTEM_GOOGLE -> {
                val isApi33Plus = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                Text(
                    text = if (isApi33Plus) strings.systemAvailableApi33 else strings.systemUnavailableBelowApi33,
                    color = if (isApi33Plus) scheme.textSecondary else CrimsonKillSwitch,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
                HorizontalDivider(color = scheme.divider, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 16.dp))

                LanguageFlatSelector(
                    title = strings.recognitionLanguageLabel,
                    options = listOf(
                        "ru-RU" to strings.langRu,
                        "en-US" to strings.langEn
                    ),
                    selectedCode = settings.systemLanguage,
                    onSelect = { ModulesSettingsStore.setSystemLanguage(context, it) }
                )
            }
        }

        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
    }
}

@Composable
private fun EngineFlatRow(
    title: String,
    description: String,
    badgeText: String,
    isRecommended: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val badgeBg = if (isRecommended) scheme.accent.copy(alpha = 0.14f) else CrimsonKillSwitch.copy(alpha = 0.14f)
    val badgeFg = if (isRecommended) scheme.accent else CrimsonKillSwitch

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(badgeBg)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = badgeFg,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = scheme.textSecondary,
                fontSize = 12.5.sp,
                lineHeight = 17.sp
            )
        }

        if (selected) {
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = null,
                tint = if (isRecommended) scheme.accent else CrimsonKillSwitch,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun LanguageFlatSelector(
    title: String,
    options: List<Pair<String, String>>,
    selectedCode: String,
    onSelect: (String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            color = scheme.textSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            options.forEach { (code, label) ->
                val isSelected = selectedCode == code
                Text(
                    text = label,
                    color = if (isSelected) scheme.accent else scheme.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier
                        .clickable { onSelect(code) }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun AssetPackFlatRow(
    spec: ModuleAssetSpec,
    downloadState: AssetDownloadState,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ModulesI18n.current
    val title = if (strings.isRussian) spec.titleRu else spec.titleEn
    val subtitle = if (strings.isRussian) spec.subtitleRu else spec.subtitleEn

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) scheme.accent else scheme.textPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            tint = scheme.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                val statusText = when (downloadState) {
                    is AssetDownloadState.NotInstalled -> subtitle
                    is AssetDownloadState.Downloading ->
                        strings.downloadingProgress((downloadState.progress * 100).toInt())
                    is AssetDownloadState.Extracting -> strings.extractingLabel
                    is AssetDownloadState.Installed ->
                        strings.installedLabel(formatMb(downloadState.sizeBytes))
                    is AssetDownloadState.Error -> strings.downloadErrorRetry
                }
                Text(
                    text = statusText,
                    color = when (downloadState) {
                        is AssetDownloadState.Error -> CrimsonKillSwitch
                        is AssetDownloadState.Installed -> scheme.accent
                        else -> scheme.textSecondary
                    },
                    fontSize = 12.sp
                )
            }

            when (downloadState) {
                is AssetDownloadState.NotInstalled, is AssetDownloadState.Error -> {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(scheme.accent)
                            .clickable(onClick = onDownload)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Download,
                            contentDescription = strings.downloadBtn,
                            tint = scheme.onAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.downloadBtn,
                            color = scheme.onAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                is AssetDownloadState.Downloading, is AssetDownloadState.Extracting -> {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = strings.cancelBtn,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                is AssetDownloadState.Installed -> {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = strings.deleteBtn,
                            tint = CrimsonKillSwitch,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        if (downloadState is AssetDownloadState.Downloading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { downloadState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = scheme.accent,
                trackColor = scheme.divider
            )
        } else if (downloadState is AssetDownloadState.Extracting) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = scheme.accent,
                trackColor = scheme.divider
            )
        }
    }
}

internal fun formatMb(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 0.1) "0 МБ" else "%.1f МБ".format(mb)
}
