package com.anongram.app.features.settings.modules.ui.ai

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.anongram.app.features.services.ai.data.AiModelCategory
import com.anongram.app.features.services.ai.data.HfModelFile
import com.anongram.app.features.services.ai.data.HfRepoSummary
import com.anongram.app.features.services.ai.data.HuggingFaceClient
import com.anongram.app.features.services.ai.data.LocalAiStore
import com.anongram.app.features.settings.common.ui.SettingsGlassScaffold
import com.anongram.app.features.settings.common.ui.settingsGlassPill
import com.anongram.app.features.settings.modules.i18n.ModulesI18n
import com.anongram.app.features.settings.modules.ui.voice.formatMb
import kotlinx.coroutines.launch

@Composable
fun LocalAiModuleSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val modStrings = ModulesI18n.current
    val state by LocalAiStore.state.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        LocalAiStore.ensureInitialized(context)
        LocalAiStore.refreshInstalledModels(context)
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AiModelCategory.ALL) }
    var hfLoading by remember { mutableStateOf(false) }
    var hfError by remember { mutableStateOf<String?>(null) }
    var hfRepos by remember { mutableStateOf<List<HfRepoSummary>>(emptyList()) }
    var selectedRepoId by remember { mutableStateOf<String?>(null) }
    var hfFiles by remember { mutableStateOf<List<HfModelFile>>(emptyList()) }

    val importPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            LocalAiStore.importLocalGguf(context, uri)
        }
    }

    val isRu = modStrings.isRussian

    fun runHfSearch(query: String) {
        val q = query.trim()
        if (q.isEmpty()) {
            hfRepos = emptyList()
            selectedRepoId = null
            hfFiles = emptyList()
            hfError = null
            return
        }
        scope.launch {
            hfLoading = true
            hfError = null
            selectedRepoId = null
            hfFiles = emptyList()
            if (q.contains("/") && !q.contains(" ")) {
                selectedRepoId = q
                HuggingFaceClient.fetchRepoGgufFiles(q)
                    .onSuccess { hfFiles = it }
                    .onFailure { hfError = it.message ?: "Error" }
            } else {
                HuggingFaceClient.searchGgufRepos(q)
                    .onSuccess { hfRepos = it }
                    .onFailure { hfError = it.message ?: "Error" }
            }
            hfLoading = false
        }
    }

    SettingsGlassScaffold(
        title = if (isRu) "Модели ИИ" else "AI Models",
        onBack = onBack,
        backContentDescription = modStrings.back,
        contentHorizontalPadding = 16.dp,
        modifier = modifier,
        trailingAction = { hazeState ->
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .settingsGlassPill(hazeState = hazeState)
                    .clickable { importPicker.launch(arrayOf("*/*")) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Поиск сверху по всему Hugging Face
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(scheme.card)
                        .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = scheme.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine = true,
                        textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp),
                        cursorBrush = SolidColor(scheme.accent),
                        modifier = Modifier.weight(1f),
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (isRu) "Поиск моделей на Hugging Face..." else "Search models on Hugging Face...",
                                    color = scheme.textMuted,
                                    fontSize = 14.sp
                                )
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = null,
                            tint = scheme.textMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    searchQuery = ""
                                    hfRepos = emptyList()
                                    selectedRepoId = null
                                    hfFiles = emptyList()
                                    hfError = null
                                }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (searchQuery.isNotBlank()) scheme.accent else scheme.surface)
                            .clickable(enabled = !hfLoading && searchQuery.isNotBlank()) {
                                runHfSearch(searchQuery)
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = if (isRu) "Найти" else "Search",
                            color = if (searchQuery.isNotBlank()) scheme.onAccent else scheme.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Категории под поиском
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AiModelCategory.entries) { cat ->
                        val active = selectedCategory == cat
                        val label = when (cat) {
                            AiModelCategory.ALL -> if (isRu) "Все" else "All"
                            AiModelCategory.GEMMA -> "Gemma"
                            AiModelCategory.DEEPSEEK -> "DeepSeek"
                            AiModelCategory.GPT_OSS -> "GPT-OSS"
                            AiModelCategory.UNCENSORED -> if (isRu) "Без цензуры" else "Uncensored"
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (active) scheme.accent else scheme.card)
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (active) scheme.onAccent else scheme.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }

                if (hfLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.5.dp),
                        color = scheme.accent,
                        trackColor = scheme.divider
                    )
                }

                if (hfError != null || state.downloadError != null) {
                    Text(
                        text = hfError ?: state.downloadError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.5.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }

                // Результаты глобального поиска Hugging Face
                if (selectedRepoId != null && hfFiles.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedRepoId ?: "",
                                color = scheme.textSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            if (hfRepos.isNotEmpty()) {
                                Text(
                                    text = if (isRu) "Назад к списку" else "Back to results",
                                    color = scheme.accent,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable {
                                        selectedRepoId = null
                                        hfFiles = emptyList()
                                    }
                                )
                            }
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(scheme.card)
                        ) {
                            hfFiles.take(15).forEachIndexed { idx, hfFile ->
                                if (idx > 0) HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                                val isDownloading = state.downloadingModelId == "hf_${hfFile.filename}"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = hfFile.filename,
                                            color = scheme.textPrimary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (hfFile.sizeBytes > 0L) {
                                            Text(
                                                text = formatMb(hfFile.sizeBytes),
                                                color = scheme.textSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                    if (isDownloading) {
                                        Text(
                                            text = "${(state.downloadProgress * 100).toInt()}%",
                                            color = scheme.accent,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    } else {
                                        IconButton(
                                            onClick = {
                                                LocalAiStore.downloadHfFile(
                                                    context,
                                                    selectedRepoId.orEmpty(),
                                                    hfFile
                                                )
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Download,
                                                contentDescription = null,
                                                tint = scheme.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (hfRepos.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isRu) "Результаты Hugging Face" else "Hugging Face results",
                            color = scheme.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(scheme.card)
                        ) {
                            hfRepos.forEachIndexed { idx, repo ->
                                if (idx > 0) HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch {
                                                hfLoading = true
                                                hfError = null
                                                selectedRepoId = repo.repoId
                                                HuggingFaceClient.fetchRepoGgufFiles(repo.repoId)
                                                    .onSuccess { hfFiles = it }
                                                    .onFailure { hfError = it.message ?: "Error" }
                                                hfLoading = false
                                            }
                                        }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = repo.repoId,
                                            color = scheme.textPrimary,
                                            fontSize = 13.5.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "${repo.downloads} ↓ · ${repo.likes} ♥",
                                            color = scheme.textSecondary,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Установленные модели
                if (state.installedModels.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isRu) "Установленные" else "Installed",
                            color = scheme.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(scheme.card)
                        ) {
                            state.installedModels.forEachIndexed { idx, model ->
                                if (idx > 0) {
                                    HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                                }
                                val isActive = model.id == state.activeModelId
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { LocalAiStore.selectModel(context, model.id) }
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = model.displayName,
                                            color = scheme.textPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = formatMb(model.sizeBytes),
                                            color = scheme.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (isActive) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
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
                                            contentDescription = null,
                                            tint = scheme.textMuted,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Каталог моделей (по выбранной категории)
                val catalogItems = remember(selectedCategory) {
                    if (selectedCategory == AiModelCategory.ALL) AiCatalog.presets
                    else AiCatalog.presets.filter { it.category == selectedCategory }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isRu) "Каталог моделей" else "Model catalog",
                        color = scheme.textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.card)
                    ) {
                        catalogItems.forEachIndexed { idx, preset ->
                            if (idx > 0) {
                                HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
                            }
                            val installed = state.installedModels.any {
                                it.filename.equals(preset.filename, ignoreCase = true)
                            }
                            val isDownloading = state.downloadingModelId == preset.id
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = preset.displayName,
                                            color = scheme.textPrimary,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${preset.sizeMb} MB · ${preset.ramHint}",
                                            color = scheme.textSecondary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    when {
                                        installed -> {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = scheme.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        isDownloading -> {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "${(state.downloadProgress * 100).toInt()}%",
                                                    color = scheme.accent,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(
                                                    onClick = { LocalAiStore.cancelDownload() },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Outlined.Close,
                                                        contentDescription = null,
                                                        tint = scheme.textSecondary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            IconButton(
                                                onClick = { LocalAiStore.downloadPreset(context, preset) },
                                                modifier = Modifier.size(34.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Download,
                                                    contentDescription = null,
                                                    tint = scheme.accent,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (isDownloading) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { state.downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = scheme.accent,
                                        trackColor = scheme.divider
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
