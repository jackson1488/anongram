package com.anongram.app.features.services.ai.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.services.ai.data.AiReasoningLevel
import com.anongram.app.features.services.ai.data.AiSystemPromptPreset
import com.anongram.app.features.services.ai.data.LocalAiState
import com.anongram.app.features.services.ai.i18n.LocalAiI18nStrings

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocalAiModelMenuPopup(
    expanded: Boolean,
    state: LocalAiState,
    strings: LocalAiI18nStrings,
    onDismiss: () -> Unit,
    onSelectModel: (String) -> Unit,
    onOpenModules: () -> Unit,
    onReasoningChange: (AiReasoningLevel) -> Unit,
    onTemperatureChange: (Float) -> Unit,
    onPromptPresetChange: (AiSystemPromptPreset) -> Unit,
    onCustomPromptChange: (String) -> Unit
) {
    if (!expanded) return
    val scheme = LocalAnonGramScheme.current

    Popup(
        alignment = Alignment.TopStart,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true)
    ) {
        Column(
            modifier = Modifier
                .padding(top = 54.dp, start = 14.dp, end = 14.dp)
                .fillMaxWidth(0.92f)
                .heightIn(max = 520.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.card)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Скачанные модели + быстрый переход в Модули
            Text(
                text = strings.switchModelTitle,
                color = scheme.accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (state.installedModels.isEmpty()) {
                Text(
                    text = strings.noModelInstalledHint,
                    color = scheme.textSecondary,
                    fontSize = 12.5.sp
                )
            } else {
                state.installedModels.forEach { model ->
                    val selected = model.id == state.activeModelId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) scheme.accent.copy(alpha = 0.14f) else scheme.surface)
                            .clickable { onSelectModel(model.id) }
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = model.displayName,
                                color = scheme.textPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${(model.sizeBytes / (1024L * 1024L))} MB",
                                color = scheme.textMuted,
                                fontSize = 11.5.sp
                            )
                        }
                        if (selected) {
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surface)
                    .clickable {
                        onDismiss()
                        onOpenModules()
                    }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Download,
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = strings.openModulesToDownload,
                    color = scheme.accent,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

            // 2. Уровень рассуждений
            Text(
                text = strings.reasoningLevelTitle,
                color = scheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiReasoningLevel.entries.forEach { lvl ->
                    val selected = state.reasoningLevel == lvl
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) scheme.accent else scheme.surface)
                            .clickable { onReasoningChange(lvl) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = strings.reasoningLabel(lvl),
                            color = if (selected) scheme.onAccent else scheme.textPrimary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 3. Температура
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.temperatureTitle,
                    color = scheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "%.2f".format(state.temperature),
                    color = scheme.accent,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            AiSlimSlider(
                value = state.temperature,
                onValueChange = onTemperatureChange,
                valueRange = 0f..1.5f
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    strings.tempPrecise to 0.2f,
                    strings.tempBalanced to 0.7f,
                    strings.tempCreative to 1.0f
                ).forEach { (label, valTemp) ->
                    val isNear = kotlin.math.abs(state.temperature - valTemp) < 0.08f
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isNear) scheme.accent.copy(alpha = 0.16f) else scheme.surface)
                            .clickable { onTemperatureChange(valTemp) }
                            .padding(vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$label ${"%.1f".format(valTemp)}",
                            color = if (isNear) scheme.accent else scheme.textSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

            // 4. Системный промпт + варианты
            Text(
                text = strings.systemPromptTitle,
                color = scheme.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AiSystemPromptPreset.entries.forEach { preset ->
                    val selected = state.systemPromptPreset == preset
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) scheme.accent else scheme.surface)
                            .clickable { onPromptPresetChange(preset) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = strings.promptPresetLabel(preset),
                            color = if (selected) scheme.onAccent else scheme.textPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            val displayedPrompt = if (state.systemPromptPreset == AiSystemPromptPreset.CUSTOM) {
                state.customSystemPrompt
            } else {
                state.effectiveSystemPrompt(strings.isRussian)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp, max = 110.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.surface)
                    .padding(10.dp)
            ) {
                if (displayedPrompt.isEmpty()) {
                    Text(
                        text = strings.systemPromptPlaceholder,
                        color = scheme.textMuted,
                        fontSize = 12.5.sp
                    )
                }
                BasicTextField(
                    value = displayedPrompt,
                    onValueChange = onCustomPromptChange,
                    textStyle = TextStyle(color = scheme.textPrimary, fontSize = 12.5.sp),
                    cursorBrush = SolidColor(scheme.accent),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AiSlimSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>
) {
    val scheme = LocalAnonGramScheme.current
    val latestOnChange by rememberUpdatedState(onValueChange)
    var widthPx by remember { mutableIntStateOf(1) }
    val span = (valueRange.endInclusive - valueRange.start).coerceAtLeast(0.001f)
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    fun updateFromX(x: Float) {
        val pad = widthPx * 0.03f
        val usable = (widthPx - pad * 2f).coerceAtLeast(1f)
        val f = ((x - pad) / usable).coerceIn(0f, 1f)
        latestOnChange(valueRange.start + f * span)
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .onSizeChanged { widthPx = it.width.coerceAtLeast(1) }
            .pointerInput(valueRange) { detectTapGestures { updateFromX(it.x) } }
            .pointerInput(valueRange) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    updateFromX(change.position.x)
                }
            }
    ) {
        val trackH = 4.dp.toPx()
        val thumbR = 7.dp.toPx()
        val pad = thumbR
        val trackW = (size.width - pad * 2f).coerceAtLeast(1f)
        val cy = size.height * 0.5f
        val tx = pad + trackW * fraction

        drawRoundRect(
            color = scheme.divider,
            topLeft = Offset(pad, cy - trackH * 0.5f),
            size = Size(trackW, trackH),
            cornerRadius = CornerRadius(trackH * 0.5f, trackH * 0.5f)
        )
        drawRoundRect(
            color = scheme.accent,
            topLeft = Offset(pad, cy - trackH * 0.5f),
            size = Size(trackW * fraction, trackH),
            cornerRadius = CornerRadius(trackH * 0.5f, trackH * 0.5f)
        )
        drawCircle(color = scheme.card, radius = thumbR + 1.5.dp.toPx(), center = Offset(tx, cy))
        drawCircle(color = scheme.accent, radius = thumbR, center = Offset(tx, cy))
    }
}
