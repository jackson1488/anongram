package com.anongram.app.features.settings.modules.ui.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.services.ai.data.AiCatalog
import com.anongram.app.features.services.ai.data.AiSystemPromptPreset
import com.anongram.app.features.services.ai.data.HfModelFile
import com.anongram.app.features.services.ai.data.HuggingFaceClient
import com.anongram.app.features.services.ai.data.LocalAiStore
import com.anongram.app.features.services.ai.i18n.LocalAiI18n
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import com.anongram.app.features.settings.modules.ui.voice.formatMb
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalAiModuleSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val modStrings = ModulesI18n.current
    val aiStrings = LocalAiI18n.current
    val state by LocalAiStore.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        LocalAiStore.ensureInitialized(context)
        LocalAiStore.refreshInstalledModels(context)
    }

    var hfRepoQuery by remember { mutableStateOf("Qwen/Qwen2.5-1.5B-Instruct-GGUF") }
    var hfLoading by remember { mutableStateOf(false) }
    var hfError by remember { mutableStateOf<String?>(null) }
    var hfFiles by remember { mutableStateOf<List<HfModelFile>>(emptyList()) }

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            LocalAiStore.importLocalGguf(context, uri)
        }
    }

    val isRu = modStrings.isRussian

    AnonGramScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                        contentDescription = modStrings.back,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRu) "Локальный ИИ (llama.cpp)" else "Local AI (llama.cpp)",
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
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Импорт локального .gguf
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(scheme.card)
                        .border(0.8.dp, scheme.islandBorder, RoundedCornerShape(14.dp))
                        .clickable { importPicker.launch(arrayOf("*/*")) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isRu) "Импортировать .gguf с телефона" else "Import .gguf from device",
                        color = scheme.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // 2. Прямая загрузка с Hugging Face
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.card)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isRu) "Hugging Face (прямой поиск GGUF)" else "Hugging Face (direct GGUF lookup)",
                        color = scheme.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surface)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = hfRepoQuery,
                            onValueChange = { hfRepoQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(color = scheme.textPrimary, fontSize = 13.5.sp),
                            cursorBrush = SolidColor(scheme.accent),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(scheme.accent)
                                .clickable(enabled = !hfLoading) {
                                    hfLoading = true
                                    hfError = null
                                    scope.launch {
                                        val res = HuggingFaceClient.fetchRepoGgufFiles(hfRepoQuery)
                                        hfLoading = false
                                        res.onSuccess { list ->
                                            hfFiles = list
                                            if (list.isEmpty()) {
                                                hfError = if (isRu) "В репозитории нет .gguf файлов" else "No .gguf files found"
                                            }
                                        }.onFailure { err ->
                                            hfError = err.message ?: "Error"
                                        }
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = scheme.onAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    if (hfLoading) {
                        LinearProgressIndicator(
                            color = scheme.accent,
                            trackColor = scheme.divider,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (hfError != null) {
                        Text(text = hfError!!, color = scheme.textSecondary, fontSize = 12.sp)
                    }
                    hfFiles.forEach { hf ->
                        val dlId = "hf_${hfRepoQuery}_${hf.filename}"
                        val isDownloadingThis = state.downloadingModelId == dlId
                        val isInstalled = state.installedModels.any { it.filename.equals(hf.filename, ignoreCase = true) }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(scheme.surface)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = hf.filename,
                                    color = scheme.textPrimary,
                                    fontSize = 12.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (hf.sizeBytes > 0L) {
                                    Text(
                                        text = formatMb(hf.sizeBytes),
                                        color = scheme.textMuted,
                                        fontSize = 11.5.sp
                                    )
                                }
                            }
                            if (isDownloadingThis) {
                                Text(
                                    text = "${(state.downloadProgress * 100).toInt()}%",
                                    color = scheme.accent,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                Text(
                                    text = modStrings.cancelBtn,
                                    color = scheme.textSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { LocalAiStore.cancelDownload() }
                                )
                            } else if (!isInstalled) {
                                IconButton(
                                    onClick = { LocalAiStore.downloadHfFile(context, hfRepoQuery, hf) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Download,
                                        contentDescription = modStrings.downloadBtn,
                                        tint = scheme.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = scheme.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 3. Установленные модели
                if (state.installedModels.isNotEmpty()) {
                    Text(
                        text = if (isRu) "Установленные модели (${formatMb(state.totalInstalledBytes)})" else "Installed models (${formatMb(state.totalInstalledBytes)})",
                        color = scheme.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    state.installedModels.forEach { model ->
                        val active = model.id == state.activeModelId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) scheme.accent.copy(alpha = 0.14f) else scheme.card)
                                .clickable { LocalAiStore.selectModel(context, model.id) }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.displayName,
                                    color = scheme.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${model.filename} • ${formatMb(model.sizeBytes)}",
                                    color = scheme.textMuted,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (active) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = scheme.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            IconButton(
                                onClick = { LocalAiStore.deleteInstalledModel(context, model) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = modStrings.deleteBtn,
                                    tint = scheme.textMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Каталог моделей Hugging Face
                Text(
                    text = if (isRu) "Рекомендуемые GGUF-модели" else "Recommended GGUF models",
                    color = scheme.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                AiCatalog.presets.forEach { preset ->
                    val isInstalled = state.installedModels.any { it.filename.equals(preset.filename, ignoreCase = true) }
                    val isDownloadingThis = state.downloadingModelId == preset.id
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(scheme.card)
                            .padding(horizontal = 14.dp, vertical = 11.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.displayName,
                                    color = scheme.textPrimary,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${preset.sizeMb} MB • ${preset.ramHint}",
                                    color = scheme.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            if (isDownloadingThis) {
                                Text(
                                    text = modStrings.cancelBtn,
                                    color = scheme.accent,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.clickable { LocalAiStore.cancelDownload() }
                                )
                            } else if (!isInstalled) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(scheme.accent.copy(alpha = 0.16f))
                                        .clickable { LocalAiStore.downloadPreset(context, preset) }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = modStrings.downloadBtn,
                                        color = scheme.accent,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = scheme.accent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (isDownloadingThis) {
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { state.downloadProgress },
                                color = scheme.accent,
                                trackColor = scheme.divider,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 5. Настройки движка и системного промпта по умолчанию
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.card)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isRu) "Параметры llama.cpp и системный промпт" else "llama.cpp & System Prompt",
                        color = scheme.accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = if (isRu) "Потоки CPU: ${state.cpuThreads}" else "CPU threads: ${state.cpuThreads}",
                        color = scheme.textPrimary,
                        fontSize = 13.5.sp
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(2, 4, 6, 8).forEach { th ->
                            val sel = state.cpuThreads == th
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) scheme.accent else scheme.surface)
                                    .clickable { LocalAiStore.setCpuThreads(context, th) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$th",
                                    color = if (sel) scheme.onAccent else scheme.textPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = if (isRu) "Контекст: ${state.contextLength} токенов" else "Context: ${state.contextLength} tokens",
                        color = scheme.textPrimary,
                        fontSize = 13.5.sp
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1024, 2048, 4096).forEach { ctx ->
                            val sel = state.contextLength == ctx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) scheme.accent else scheme.surface)
                                    .clickable { LocalAiStore.setContextLength(context, ctx) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "$ctx",
                                    color = if (sel) scheme.onAccent else scheme.textPrimary,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    Text(
                        text = aiStrings.systemPromptTitle,
                        color = scheme.textPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AiSystemPromptPreset.entries.forEach { preset ->
                            val sel = state.systemPromptPreset == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) scheme.accent else scheme.surface)
                                    .clickable { LocalAiStore.setSystemPromptPreset(context, preset) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = aiStrings.promptPresetLabel(preset),
                                    color = if (sel) scheme.onAccent else scheme.textPrimary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    val displayedPrompt = if (state.systemPromptPreset == AiSystemPromptPreset.CUSTOM) {
                        state.customSystemPrompt
                    } else {
                        state.effectiveSystemPrompt(isRu)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.surface)
                            .padding(10.dp)
                    ) {
                        BasicTextField(
                            value = displayedPrompt,
                            onValueChange = { LocalAiStore.setCustomSystemPrompt(context, it) },
                            textStyle = TextStyle(color = scheme.textPrimary, fontSize = 13.sp),
                            cursorBrush = SolidColor(scheme.accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
