package com.anongram.app.features.settings.modules.ui.ai

import android.widget.Toast
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.services.ai.data.AiEngineMode
import com.anongram.app.features.services.ai.data.CloudModelInfo
import com.anongram.app.features.services.ai.data.CloudProviderProfile
import com.anongram.app.features.services.ai.data.LocalAiState
import com.anongram.app.features.services.ai.data.LocalAiStore
import com.anongram.app.features.services.ai.i18n.LocalAiI18n

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloudAiProvidersSection(
    state: LocalAiState,
    isRu: Boolean,
    onOpenAddProvider: () -> Unit = {},
    onOpenEditProvider: (CloudProviderProfile) -> Unit = {}
) {
    val scheme = LocalAnonGramScheme.current
    val aiStrings = LocalAiI18n.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(scheme.card)
            .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.CloudQueue,
                contentDescription = null,
                tint = scheme.accent,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = aiStrings.cloudProvidersTitle,
                color = scheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.accent)
                .clickable { onOpenAddProvider() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                tint = scheme.onAccent,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = aiStrings.addCustomProvider,
                color = scheme.onAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }

        val activeProviders = state.cloudProviders.filter { it.apiKey.isNotBlank() && it.baseUrl.isNotBlank() }
        if (activeProviders.isEmpty()) {
            Text(
                text = if (isRu) {
                    "Нет активных провайдеров. Нажмите «+ Добавить провайдера»."
                } else {
                    "No active providers yet. Tap «+ Add provider»."
                },
                color = scheme.textMuted,
                fontSize = 12.5.sp
            )
        }
        activeProviders.forEach { provider ->
            val isActiveProvider = state.engineMode == AiEngineMode.CLOUD_OPENAI &&
                state.activeCloudProviderId == provider.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface)
                    .border(
                        width = if (isActiveProvider) 1.2.dp else 0.7.dp,
                        color = if (isActiveProvider) scheme.accent else scheme.islandBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onOpenEditProvider(provider) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = provider.name,
                            color = scheme.textPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isActiveProvider) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRu) "Активен" else "Active",
                                color = scheme.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Text(
                        text = "${provider.baseUrl} · ${provider.models.size} ${if (isRu) "мод." else "models"}",
                        color = scheme.textSecondary,
                        fontSize = 11.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isActiveProvider) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EditCloudProviderDialog(
    provider: CloudProviderProfile,
    isRu: Boolean,
    isFetchingModels: Boolean,
    fetchError: String?,
    activeModelId: String?,
    onDismiss: () -> Unit,
    onSave: (CloudProviderProfile) -> Unit,
    onFetchModels: (CloudProviderProfile) -> Unit,
    onSelectModel: (String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val scheme = LocalAnonGramScheme.current
    var baseUrl by remember(provider.id) { mutableStateOf(provider.baseUrl) }
    var apiKey by remember(provider.id) { mutableStateOf(provider.apiKey) }
    var customModelId by remember(provider.id) { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = provider.name,
                    color = scheme.textPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (onDelete != null) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        tint = scheme.textMuted,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(onClick = onDelete)
                    )
                }
            }

            ThemedInputBox(
                label = "Base URL",
                value = baseUrl,
                placeholder = "https://api.openai.com/v1",
                onValueChange = { baseUrl = it }
            )
            ThemedInputBox(
                label = "API Key",
                value = apiKey,
                placeholder = "sk-...",
                onValueChange = { apiKey = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.surface)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
                        .clickable(enabled = !isFetchingModels) {
                            onFetchModels(provider.copy(baseUrl = baseUrl.trim().trimEnd('/'), apiKey = apiKey.trim()))
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        tint = scheme.accent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isFetchingModels) {
                            if (isRu) "Загрузка..." else "Loading..."
                        } else {
                            if (isRu) "Обновить /v1/models" else "Fetch /v1/models"
                        },
                        color = scheme.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (!fetchError.isNullOrBlank()) {
                Text(text = fetchError, color = scheme.textMuted, fontSize = 11.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    ThemedInputBox(
                        label = if (isRu) "Добавить модель вручную" else "Add model ID",
                        value = customModelId,
                        placeholder = "model-id",
                        onValueChange = { customModelId = it }
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(scheme.accent)
                        .clickable(enabled = customModelId.isNotBlank()) {
                            val id = customModelId.trim()
                            val nextModels = (provider.models + CloudModelInfo(id, id, supportsVision = true)).distinctBy { it.id }
                            onSave(provider.copy(baseUrl = baseUrl.trim().trimEnd('/'), apiKey = apiKey.trim(), models = nextModels))
                        }
                        .padding(horizontal = 12.dp, vertical = 9.dp)
                ) {
                    Text(text = "+", color = scheme.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (provider.models.isNotEmpty()) {
                HorizontalDivider(color = scheme.divider)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 170.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    provider.models.forEach { m ->
                        val isAct = activeModelId == m.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isAct) scheme.accent.copy(alpha = 0.16f) else scheme.surface)
                                .clickable {
                                    onSave(provider.copy(baseUrl = baseUrl.trim().trimEnd('/'), apiKey = apiKey.trim()))
                                    onSelectModel(m.id)
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = m.displayName,
                                color = scheme.textPrimary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (m.supportsVision) {
                                Text(text = "Vision", color = scheme.accent, fontSize = 10.5.sp)
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (isRu) "Отмена" else "Cancel",
                    color = scheme.textSecondary,
                    fontSize = 13.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.accent)
                        .clickable {
                            onSave(provider.copy(baseUrl = baseUrl.trim().trimEnd('/'), apiKey = apiKey.trim()))
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isRu) "Сохранить" else "Save",
                        color = scheme.onAccent,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ImportOpenCodeJsonDialog(
    isRu: Boolean,
    onDismiss: () -> Unit,
    onPickFile: () -> Unit,
    onImportText: (String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var rawJson by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isRu) "Импорт OpenCode JSON" else "Import OpenCode JSON",
                color = scheme.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isRu) {
                    "Вставьте JSON провайдера (как в opencode.json) или выберите .json файл. Ключи сохранятся только локально."
                } else {
                    "Paste OpenCode provider JSON or pick a .json file. Keys are stored locally only."
                },
                color = scheme.textSecondary,
                fontSize = 12.5.sp
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(14.dp))
                    .padding(10.dp)
            ) {
                BasicTextField(
                    value = rawJson,
                    onValueChange = { rawJson = it },
                    textStyle = TextStyle(
                        color = scheme.textPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    ),
                    cursorBrush = SolidColor(scheme.accent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (rawJson.isEmpty()) {
                            Text(
                                text = "{\n  \"provider\": {\n    \"my_api\": {\n      \"options\": { \"baseURL\": \"...\", \"apiKey\": \"...\" },\n      \"models\": { ... }\n    }\n  }\n}",
                                color = scheme.textMuted,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        inner()
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.surface)
                        .border(1.dp, scheme.islandBorder, RoundedCornerShape(18.dp))
                        .clickable(onClick = onPickFile)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isRu) "Выбрать .json" else "Pick .json",
                        color = scheme.textPrimary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row {
                    Text(
                        text = if (isRu) "Отмена" else "Cancel",
                        color = scheme.textSecondary,
                        fontSize = 13.5.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onDismiss)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (rawJson.isNotBlank()) scheme.accent else scheme.surface)
                            .clickable(enabled = rawJson.isNotBlank()) { onImportText(rawJson) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isRu) "Импорт" else "Import",
                            color = if (rawJson.isNotBlank()) scheme.onAccent else scheme.textMuted,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomProviderDialog(
    presets: List<CloudProviderProfile>,
    isRu: Boolean,
    onDismiss: () -> Unit,
    onAdd: (selectedPreset: CloudProviderProfile?, name: String, baseUrl: String, apiKey: String, modelId: String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var selectedPreset by remember { mutableStateOf<CloudProviderProfile?>(presets.firstOrNull()) }
    var name by remember { mutableStateOf(presets.firstOrNull()?.name ?: "") }
    var baseUrl by remember { mutableStateOf(presets.firstOrNull()?.baseUrl ?: "https://") }
    var apiKey by remember { mutableStateOf(presets.firstOrNull()?.apiKey ?: "") }
    var modelId by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isRu) "Добавить провайдера" else "Add provider",
                color = scheme.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { preset ->
                    val active = selectedPreset?.id == preset.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (active) scheme.accent else scheme.surface)
                            .border(1.dp, if (active) scheme.accent else scheme.islandBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                selectedPreset = preset
                                name = preset.name
                                baseUrl = preset.baseUrl
                                apiKey = preset.apiKey
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = preset.name,
                            color = if (active) scheme.onAccent else scheme.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                val isCustom = selectedPreset == null
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isCustom) scheme.accent else scheme.surface)
                        .border(1.dp, if (isCustom) scheme.accent else scheme.islandBorder, RoundedCornerShape(16.dp))
                        .clickable {
                            selectedPreset = null
                            name = ""
                            baseUrl = "https://"
                            apiKey = ""
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (isRu) "Свой URL" else "Custom",
                        color = if (isCustom) scheme.onAccent else scheme.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            ThemedInputBox(
                label = if (isRu) "Название" else "Name",
                value = name,
                placeholder = "My Router",
                onValueChange = { name = it }
            )
            ThemedInputBox(
                label = "Base URL",
                value = baseUrl,
                placeholder = "https://api.example.com/v1",
                onValueChange = { baseUrl = it }
            )
            ThemedInputBox(
                label = "API Key",
                value = apiKey,
                placeholder = "sk-...",
                onValueChange = { apiKey = it }
            )
            ThemedInputBox(
                label = if (isRu) "ID модели (необязательно для пресета)" else "Model ID (optional for preset)",
                value = modelId,
                placeholder = selectedPreset?.models?.firstOrNull()?.id ?: "gpt-4o-mini",
                onValueChange = { modelId = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = if (isRu) "Отмена" else "Cancel",
                    color = scheme.textSecondary,
                    fontSize = 13.5.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (baseUrl.isNotBlank() && apiKey.isNotBlank()) scheme.accent else scheme.surface)
                        .clickable(enabled = baseUrl.isNotBlank() && apiKey.isNotBlank()) {
                            onAdd(selectedPreset, name, baseUrl, apiKey, modelId)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (isRu) "Добавить" else "Add",
                        color = if (baseUrl.isNotBlank() && apiKey.isNotBlank()) scheme.onAccent else scheme.textMuted,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemedInputBox(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = label, color = scheme.textSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.surface)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = scheme.textPrimary, fontSize = 13.5.sp),
                cursorBrush = SolidColor(scheme.accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = scheme.textMuted, fontSize = 13.5.sp)
                    }
                    inner()
                }
            )
        }
    }
}
