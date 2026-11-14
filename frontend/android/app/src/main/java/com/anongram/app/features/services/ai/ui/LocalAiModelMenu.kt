package com.anongram.app.features.services.ai.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.services.ai.data.AiReasoningLevel
import com.anongram.app.features.services.ai.data.AiSystemPromptPreset
import com.anongram.app.features.services.ai.data.LocalAiState
import com.anongram.app.features.services.ai.i18n.LocalAiI18nStrings
import com.anongram.app.features.settings.modules.ui.voice.formatMb
import kotlin.math.roundToInt

/**
 * Компактное выпадающее меню выбора модели и уровня рассуждений (в стиле Gemini).
 */
@Composable
fun LocalAiQuickModelMenu(
    visible: Boolean,
    state: LocalAiState,
    strings: LocalAiI18nStrings,
    onDismiss: () -> Unit,
    onSelectModel: (String) -> Unit,
    onSelectReasoning: (AiReasoningLevel) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    var reasoningExpanded by remember(visible) { mutableStateOf(false) }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scheme.background.copy(alpha = 0.28f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 50.dp)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(
                    initialScale = 0.92f,
                    transformOrigin = TransformOrigin(0.25f, 0f)
                ) + fadeIn(),
                exit = scaleOut(
                    targetScale = 0.92f,
                    transformOrigin = TransformOrigin(0.25f, 0f)
                ) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(min = 250.dp, max = 310.dp)
                        .heightIn(max = 460.dp)
                        .shadow(16.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.surface)
                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp)
                ) {
                    state.installedModels.forEach { model ->
                        val selected = model.id == state.activeModelId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectModel(model.id)
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.width(24.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = scheme.textPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = model.displayName,
                                    color = scheme.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = buildString {
                                        append(formatMb(model.sizeBytes))
                                        if (model.supportsThinking) append(" · Think")
                                    },
                                    color = scheme.textSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = scheme.divider,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    // Раскрывающийся вниз пункт "Уровень рассуждений"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { reasoningExpanded = !reasoningExpanded }
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(modifier = Modifier.width(24.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = strings.reasoningLevelTitle,
                                color = scheme.textPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = strings.reasoningLabel(state.reasoningLevel),
                                color = scheme.textSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = if (reasoningExpanded) {
                                Icons.Outlined.KeyboardArrowUp
                            } else {
                                Icons.Outlined.KeyboardArrowDown
                            },
                            contentDescription = null,
                            tint = scheme.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = reasoningExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 40.dp, end = 16.dp, bottom = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            AiReasoningLevel.entries.forEach { level ->
                                val selected = state.reasoningLevel == level
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) scheme.card else scheme.surface)
                                        .clickable {
                                            onSelectReasoning(level)
                                            reasoningExpanded = false
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = strings.reasoningLabel(level),
                                        color = if (selected) scheme.accent else scheme.textPrimary,
                                        fontSize = 13.5.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (selected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = scheme.accent,
                                            modifier = Modifier.size(16.dp)
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
}

/**
 * Отдельный чистый экран настроек ИИ (системный промпт, температура, потоки, контекст).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalAiSettingsScreen(
    visible: Boolean,
    state: LocalAiState,
    strings: LocalAiI18nStrings,
    onDismiss: () -> Unit,
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
        AnonGramScreenSurface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.aiSettingsTitle,
                        color = scheme.textPrimary,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Системный промпт
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = strings.systemPromptTitle,
                            color = scheme.textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AiSystemPromptPreset.entries.forEach { preset ->
                                val selected = state.systemPromptPreset == preset
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) scheme.accent else scheme.card)
                                        .clickable { onSelectPromptPreset(preset) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        text = strings.promptPresetLabel(preset),
                                        color = if (selected) scheme.onAccent else scheme.textPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        if (state.systemPromptPreset == AiSystemPromptPreset.CUSTOM) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(scheme.card)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = state.customSystemPrompt,
                                    onValueChange = onChangeCustomPrompt,
                                    textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.sp, lineHeight = 20.sp),
                                    cursorBrush = SolidColor(scheme.accent),
                                    modifier = Modifier.fillMaxWidth(),
                                    decorationBox = { inner ->
                                        if (state.customSystemPrompt.isEmpty()) {
                                            Text(
                                                text = strings.systemPromptPlaceholder,
                                                color = scheme.textMuted,
                                                fontSize = 14.sp
                                            )
                                        }
                                        inner()
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                    // Температура
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${"%.1f".format(state.temperature)} · $tempLabel",
                                color = scheme.textPrimary,
                                fontSize = 13.5.sp,
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

                    HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                    // Потоки CPU и длина контекста
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.card)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.cpuThreadsTitle,
                                color = scheme.textPrimary,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(2, 4, 6, 8).forEach { thr ->
                                    val selected = state.cpuThreads == thr
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) scheme.accent else scheme.surface)
                                            .clickable { onSelectThreads(thr) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "$thr",
                                            color = if (selected) scheme.onAccent else scheme.textPrimary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.contextLengthTitle,
                                color = scheme.textPrimary,
                                fontSize = 14.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(1024, 2048, 4096).forEach { ctx ->
                                    val selected = state.contextLength == ctx
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) scheme.accent else scheme.surface)
                                            .clickable { onSelectContext(ctx) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "$ctx",
                                            color = if (selected) scheme.onAccent else scheme.textPrimary,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.card)
                            .clickable {
                                onDismiss()
                                onOpenModules()
                            }
                            .padding(horizontal = 14.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.modulesTitle,
                            color = scheme.accent,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
