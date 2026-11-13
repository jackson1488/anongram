package com.anongram.app.features.settings.modules.ui.voice

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
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
 * Подмодуль настройки расшифровки ГС (`voice/`) внутри Настройки -> Модули.
 * Позволяет выбрать движок (Whisper, Vosk, Системный/Google), скачать или удалить языковые модели.
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(scheme.card)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(scheme.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.GraphicEq,
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = strings.voiceSectionTitle,
                    color = scheme.textPrimary,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = strings.voiceSectionHint,
                    color = scheme.textSecondary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Выбор движка
        EngineOptionCard(
            title = strings.engineWhisperTitle,
            description = strings.engineWhisperDesc,
            badgeText = strings.recommendedBadge,
            isRecommended = true,
            selected = settings.voiceEngine == VoiceSttEngine.WHISPER,
            onClick = { ModulesSettingsStore.setVoiceEngine(context, VoiceSttEngine.WHISPER) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        EngineOptionCard(
            title = strings.engineVoskTitle,
            description = strings.engineVoskDesc,
            badgeText = strings.recommendedBadge,
            isRecommended = true,
            selected = settings.voiceEngine == VoiceSttEngine.VOSK,
            onClick = { ModulesSettingsStore.setVoiceEngine(context, VoiceSttEngine.VOSK) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        EngineOptionCard(
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

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Детальные настройки выбранного движка
        when (settings.voiceEngine) {
            VoiceSttEngine.WHISPER -> {
                LanguageChipsRow(
                    title = strings.recognitionLanguageLabel,
                    options = listOf(
                        "auto" to strings.langAuto,
                        "ru" to strings.langRu,
                        "en" to strings.langEn
                    ),
                    selectedCode = settings.whisperLanguage,
                    onSelect = { ModulesSettingsStore.setWhisperLanguage(context, it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ModuleAssetManifest.allVoiceWhisper.forEachIndexed { index, spec ->
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    AssetPackRow(
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
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    AssetPackRow(
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isApi33Plus) scheme.surface else CrimsonKillSwitch.copy(alpha = 0.12f)
                        )
                        .border(
                            1.dp,
                            if (isApi33Plus) scheme.cardBorder else CrimsonKillSwitch.copy(alpha = 0.35f),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isApi33Plus) strings.systemAvailableApi33 else strings.systemUnavailableBelowApi33,
                        color = if (isApi33Plus) scheme.textSecondary else CrimsonKillSwitch,
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                LanguageChipsRow(
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
    }
}

@Composable
private fun EngineOptionCard(
    title: String,
    description: String,
    badgeText: String,
    isRecommended: Boolean,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val borderColor = when {
        selected && !isRecommended -> CrimsonKillSwitch
        selected -> scheme.accent
        else -> scheme.cardBorder
    }
    val bgColor = when {
        selected && !isRecommended -> CrimsonKillSwitch.copy(alpha = 0.08f)
        selected -> scheme.accent.copy(alpha = 0.09f)
        else -> scheme.surface
    }
    val badgeBg = if (isRecommended) scheme.accent.copy(alpha = 0.15f) else CrimsonKillSwitch.copy(alpha = 0.15f)
    val badgeFg = if (isRecommended) scheme.accent else CrimsonKillSwitch

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(if (selected) 1.4.dp else 1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (selected) borderColor else scheme.textMuted,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(badgeBg)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
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
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun LanguageChipsRow(
    title: String,
    options: List<Pair<String, String>>,
    selectedCode: String,
    onSelect: (String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = scheme.textSecondary,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (code, label) ->
                val isSelected = selectedCode == code
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) scheme.accent else scheme.surface)
                        .border(
                            1.dp,
                            if (isSelected) scheme.accent else scheme.cardBorder,
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelect(code) }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) scheme.onAccent else scheme.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun AssetPackRow(
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
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) scheme.accent.copy(alpha = 0.08f) else scheme.surface)
            .border(
                1.dp,
                if (isSelected) scheme.accent else scheme.cardBorder,
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onSelect)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isSelected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) scheme.accent else scheme.textMuted,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
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
                    fontSize = 11.5.sp
                )
            }

            when (downloadState) {
                is AssetDownloadState.NotInstalled, is AssetDownloadState.Error -> {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(scheme.accent)
                            .clickable(onClick = onDownload)
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                            modifier = Modifier.size(18.dp)
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
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = scheme.accent,
                trackColor = scheme.cardBorder
            )
        } else if (downloadState is AssetDownloadState.Extracting) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = scheme.accent,
                trackColor = scheme.cardBorder
            )
        }
    }
}

internal fun formatMb(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 0.1) "0 МБ" else "%.1f МБ".format(mb)
}
