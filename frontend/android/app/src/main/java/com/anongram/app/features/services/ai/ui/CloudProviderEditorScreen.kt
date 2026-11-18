package com.anongram.app.features.services.ai.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.anongram.app.features.services.ai.data.AiEngineMode
import com.anongram.app.features.services.ai.data.CloudModelInfo
import com.anongram.app.features.services.ai.data.CloudProviderProfile
import com.anongram.app.features.services.ai.data.LocalAiState
import com.anongram.app.features.services.ai.data.LocalAiStore
import com.anongram.app.features.services.ai.data.OpenAiCompatibleClient
import com.anongram.app.features.settings.common.ui.SettingsGlassScaffold
import kotlinx.coroutines.launch

/**
 * Полноэкранный редактор / добавление облачного OpenAI-совместимого провайдера
 * с шапкой на бесшовном Haze-блюре (SettingsGlassScaffold).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CloudProviderEditorScreen(
    initialProvider: CloudProviderProfile?,
    state: LocalAiState,
    isRu: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onBack)

    val presets = remember(state.cloudProviders) {
        state.cloudProviders.filter { it.isPreset }
    }
    val isNew = initialProvider == null

    var selectedPreset by remember(initialProvider?.id) {
        mutableStateOf(if (isNew) presets.firstOrNull() else initialProvider)
    }
    var name by remember(initialProvider?.id) {
        mutableStateOf(initialProvider?.name ?: presets.firstOrNull()?.name ?: "")
    }
    var baseUrl by remember(initialProvider?.id) {
        mutableStateOf(initialProvider?.baseUrl ?: presets.firstOrNull()?.baseUrl ?: "https://")
    }
    var apiKey by remember(initialProvider?.id) {
        mutableStateOf(initialProvider?.apiKey ?: "")
    }
    var models by remember(initialProvider?.id) {
        mutableStateOf(initialProvider?.models ?: presets.firstOrNull()?.models ?: emptyList())
    }
    var newModelId by remember { mutableStateOf("") }
    var isFetching by remember { mutableStateOf(false) }
    var fetchError by remember { mutableStateOf<String?>(null) }

    val activeCloudModelId = if (
        state.engineMode == AiEngineMode.CLOUD_OPENAI &&
        initialProvider != null &&
        state.activeCloudProviderId == initialProvider.id
    ) {
        state.activeCloudModelId
    } else null

    fun buildSavedProfile(): CloudProviderProfile {
        val cleanId = initialProvider?.id
            ?: selectedPreset?.id
            ?: name.lowercase().replace(Regex("[^a-z0-9_-]"), "_").ifBlank {
                "custom_${System.currentTimeMillis()}"
            }
        val finalModels = if (models.isNotEmpty()) {
            models
        } else {
            listOf(CloudModelInfo("gpt-4o-mini", "gpt-4o-mini", supportsVision = true))
        }
        return CloudProviderProfile(
            id = cleanId,
            name = name.trim().ifBlank { selectedPreset?.name ?: "Custom API" },
            baseUrl = baseUrl.trim().trimEnd('/'),
            apiKey = apiKey.trim(),
            models = finalModels,
            isPreset = initialProvider?.isPreset ?: (selectedPreset != null)
        )
    }

    SettingsGlassScaffold(
        title = if (isNew) {
            if (isRu) "Добавить провайдера" else "Add provider"
        } else {
            initialProvider?.name ?: if (isRu) "Провайдер" else "Provider"
        },
        onBack = onBack,
        contentHorizontalPadding = 16.dp,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isNew && presets.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isRu) "Пресет" else "Preset",
                        color = scheme.textSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            val active = selectedPreset?.id == preset.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (active) scheme.accent else scheme.card)
                                    .border(1.dp, if (active) scheme.accent else scheme.islandBorder, RoundedCornerShape(20.dp))
                                    .clickable {
                                        selectedPreset = preset
                                        name = preset.name
                                        baseUrl = preset.baseUrl
                                        models = preset.models
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = preset.name,
                                    color = if (active) scheme.onAccent else scheme.textPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        val isCustom = selectedPreset == null
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isCustom) scheme.accent else scheme.card)
                                .border(1.dp, if (isCustom) scheme.accent else scheme.islandBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    selectedPreset = null
                                    name = ""
                                    baseUrl = "https://"
                                    models = emptyList()
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (isRu) "Свой URL" else "Custom",
                                color = if (isCustom) scheme.onAccent else scheme.textPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EditorInputField(
                    label = if (isRu) "Название" else "Name",
                    value = name,
                    placeholder = "Iishko / OpenAI / My Router",
                    onValueChange = { name = it }
                )
                EditorInputField(
                    label = "Base URL",
                    value = baseUrl,
                    placeholder = "https://api.openai.com/v1",
                    onValueChange = { baseUrl = it }
                )
                EditorInputField(
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
                            .clip(RoundedCornerShape(20.dp))
                            .background(scheme.surface)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                            .clickable(enabled = !isFetching && baseUrl.isNotBlank()) {
                                isFetching = true
                                fetchError = null
                                scope.launch {
                                    OpenAiCompatibleClient.fetchModels(baseUrl.trim(), apiKey.trim())
                                        .onSuccess { fetched ->
                                            if (fetched.isNotEmpty()) {
                                                val existingMap = models.associateBy { it.id }
                                                models = fetched.map { f ->
                                                    existingMap[f.id]?.let { ex ->
                                                        f.copy(
                                                            displayName = ex.displayName,
                                                            supportsVision = ex.supportsVision || f.supportsVision
                                                        )
                                                    } ?: f
                                                }
                                            }
                                            isFetching = false
                                        }
                                        .onFailure { err ->
                                            fetchError = err.message ?: "Error"
                                            isFetching = false
                                        }
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            tint = scheme.accent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isFetching) {
                                if (isRu) "Загрузка..." else "Loading..."
                            } else {
                                if (isRu) "Загрузить /v1/models" else "Fetch /v1/models"
                            },
                            color = scheme.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (!fetchError.isNullOrBlank()) {
                    Text(text = fetchError.orEmpty(), color = scheme.textMuted, fontSize = 11.5.sp)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (isRu) "Модели (${models.size})" else "Models (${models.size})",
                    color = scheme.textPrimary,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        EditorInputField(
                            label = if (isRu) "Добавить модель по ID" else "Add model ID",
                            value = newModelId,
                            placeholder = "gpt-4o / gemini-auto",
                            onValueChange = { newModelId = it }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (newModelId.isNotBlank()) scheme.accent else scheme.surface)
                            .clickable(enabled = newModelId.isNotBlank()) {
                                val id = newModelId.trim()
                                models = (models + CloudModelInfo(id, id, supportsVision = true)).distinctBy { it.id }
                                newModelId = ""
                            }
                            .padding(horizontal = 16.dp, vertical = 11.dp)
                    ) {
                        Text(
                            text = "+",
                            color = if (newModelId.isNotBlank()) scheme.onAccent else scheme.textMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                models.forEach { m ->
                    val isActiveModel = activeCloudModelId == m.id
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isActiveModel) scheme.accent.copy(alpha = 0.14f) else scheme.surface)
                            .border(
                                width = if (isActiveModel) 1.dp else 0.5.dp,
                                color = if (isActiveModel) scheme.accent else scheme.islandBorder,
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                val saved = buildSavedProfile()
                                LocalAiStore.upsertCloudProvider(context, saved, activate = false)
                                LocalAiStore.selectCloudModel(context, saved.id, m.id)
                                onBack()
                            }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = m.displayName,
                                color = scheme.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = if (isActiveModel) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = m.id,
                                color = scheme.textMuted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (m.supportsVision) scheme.accent.copy(alpha = 0.18f) else scheme.card)
                                .clickable {
                                    models = models.map {
                                        if (it.id == m.id) it.copy(supportsVision = !it.supportsVision) else it
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Vision",
                                color = if (m.supportsVision) scheme.accent else scheme.textMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        if (isActiveModel) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = scheme.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = scheme.textMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable {
                                    models = models.filterNot { it.id == m.id }
                                }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!isNew) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                            .clickable {
                                val target = initialProvider!!
                                if (target.isPreset) {
                                    LocalAiStore.upsertCloudProvider(context, target.copy(apiKey = ""), activate = false)
                                } else {
                                    LocalAiStore.deleteCloudProvider(context, target.id)
                                }
                                onBack()
                            }
                            .padding(vertical = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRu) "Отключить" else "Remove",
                            color = scheme.textSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (baseUrl.isNotBlank() && apiKey.isNotBlank()) scheme.accent else scheme.card)
                        .clickable(enabled = baseUrl.isNotBlank() && apiKey.isNotBlank()) {
                            val saved = buildSavedProfile()
                            LocalAiStore.upsertCloudProvider(context, saved, activate = true)
                            onBack()
                        }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isRu) "Сохранить" else "Save",
                        color = if (baseUrl.isNotBlank() && apiKey.isNotBlank()) scheme.onAccent else scheme.textMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorInputField(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = scheme.textSecondary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surface)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp),
                cursorBrush = SolidColor(scheme.accent),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(text = placeholder, color = scheme.textMuted, fontSize = 14.sp)
                    }
                    inner()
                }
            )
        }
    }
}
