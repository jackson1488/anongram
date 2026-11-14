package com.anongram.app.features.services.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.services.ai.data.AiReasoningLevel
import com.anongram.app.features.services.ai.data.AiSystemPromptPreset
import com.anongram.app.features.services.ai.data.LocalAiState
import com.anongram.app.features.services.ai.i18n.LocalAiI18nStrings
import com.anongram.app.features.settings.modules.ui.voice.formatMb
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalAiModelMenu(
    visible: Boolean,
    state: LocalAiState,
    strings: LocalAiI18nStrings,
    onDismiss: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectReasoning: (AiReasoningLevel) -> Unit,
    onChangeTemperature: (Float) -> Unit,
    onSelectPromptPreset: (AiSystemPromptPreset) -> Unit,
    onChangeCustomPrompt: (String) -> Unit,
    onSelectThreads: (Int) -> Unit,
    onSelectContext: (Int) -> Unit,
    onOpenModules: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background.copy(alpha = 0.62f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 48.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(initialScale = 0.96f) + fadeIn(),
                exit = scaleOut(targetScale = 0.96f) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 600.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(scheme.surface)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                ) {
                    // Заголовок
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.aiSettingsTitle,
                            color = scheme.textPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = null,
                                tint = scheme.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Активная модель
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.installedModelsHeader,
                                    color = scheme.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = strings.modulesTitle,
                                    color = scheme.accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            onDismiss()
                                            onOpenModules()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
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
                                    val selected = model.id == state.activeModelId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onSelectModel(model.id) }
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = model.displayName,
                                                color = scheme.textPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = formatMb(model.sizeBytes),
                                                color = scheme.textSecondary,
                                                fontSize = 11.5.sp
                                            )
                                        }
                                        if (selected) {
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

                        // 2. Уровень рассуждений
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.reasoningLevelTitle,
                                color = scheme.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(scheme.card)
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                AiReasoningLevel.entries.forEach { level ->
                                    val selected = state.reasoningLevel == level
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) scheme.accent else scheme.card)
                                            .clickable { onSelectReasoning(level) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = strings.reasoningLabel(level),
                                            color = if (selected) scheme.onAccent else scheme.textSecondary,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Температура
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val tempLabel = when {
                                state.temperature < 0.35f -> strings.tempPrecise
                                state.temperature < 0.85f -> strings.tempBalanced
                                else -> strings.tempCreative
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.temperatureTitle,
                                    color = scheme.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${"%.1f".format(state.temperature)} · $tempLabel",
                                    color = scheme.textPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Slider(
                                value = state.temperature,
                                onValueChange = { raw ->
                                    val snapped = (raw * 10f).roundToInt() / 10f
                                    onChangeTemperature(snapped.coerceIn(0f, 1.5f))
                                },
                                valueRange = 0f..1.5f,
                                colors = SliderDefaults.colors(
                                    thumbColor = scheme.accent,
                                    activeTrackColor = scheme.accent,
                                    inactiveTrackColor = scheme.divider
                                )
                            )
                        }

                        // 4. Потоки CPU и длина контекста
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.cpuThreadsTitle,
                                    color = scheme.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(2, 4, 6, 8).forEach { thr ->
                                        val selected = state.cpuThreads == thr
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) scheme.accent else scheme.card)
                                                .clickable { onSelectThreads(thr) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "$thr",
                                                color = if (selected) scheme.onAccent else scheme.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = strings.contextLengthTitle,
                                    color = scheme.textSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf(1024, 2048, 4096).forEach { ctx ->
                                        val selected = state.contextLength == ctx
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) scheme.accent else scheme.card)
                                                .clickable { onSelectContext(ctx) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = "$ctx",
                                                color = if (selected) scheme.onAccent else scheme.textPrimary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 5. Системный промпт
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = strings.systemPromptTitle,
                                color = scheme.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AiSystemPromptPreset.entries.forEach { preset ->
                                    val selected = state.systemPromptPreset == preset
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) scheme.accent else scheme.card)
                                            .clickable { onSelectPromptPreset(preset) }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = strings.promptPresetLabel(preset),
                                            color = if (selected) scheme.onAccent else scheme.textPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                            if (state.systemPromptPreset == AiSystemPromptPreset.CUSTOM) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(scheme.card)
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    BasicTextField(
                                        value = state.customSystemPrompt,
                                        onValueChange = onChangeCustomPrompt,
                                        textStyle = TextStyle(color = scheme.textPrimary, fontSize = 13.sp, lineHeight = 18.sp),
                                        cursorBrush = SolidColor(scheme.accent),
                                        modifier = Modifier.fillMaxWidth(),
                                        decorationBox = { inner ->
                                            if (state.customSystemPrompt.isEmpty()) {
                                                Text(
                                                    text = strings.systemPromptPlaceholder,
                                                    color = scheme.textMuted,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            inner()
                                        }
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
