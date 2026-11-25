package com.anongram.app.features.chats.mediaeditor.ui.adjust

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorI18n
import com.anongram.app.features.chats.mediaeditor.i18n.MediaEditorStrings
import com.anongram.app.features.chats.mediaeditor.model.BlurEffectState
import com.anongram.app.features.chats.mediaeditor.model.BlurMode
import com.anongram.app.features.chats.mediaeditor.model.CurveChannel
import com.anongram.app.features.chats.mediaeditor.model.PhotoAdjustments
import com.anongram.app.features.chats.mediaeditor.model.ToneCurvesState
import kotlin.math.roundToInt

enum class AdjustSubTab {
    SLIDERS,
    BLUR,
    CURVES
}

private enum class SliderParam(val min: Float, val max: Float) {
    SMOOTH_SKIN(0f, 1f),
    ENHANCE(0f, 1f),
    EXPOSURE(-1f, 1f),
    CONTRAST(-1f, 1f),
    SATURATION(-1f, 1f),
    WARMTH(-1f, 1f),
    FADE(0f, 1f),
    HIGHLIGHTS(-1f, 1f),
    SHADOWS(-1f, 1f),
    VIGNETTE(0f, 1f),
    GRAIN(0f, 1f),
    SHARPEN(0f, 1f)
}

@Composable
fun AdjustmentsPanel(
    activeTab: AdjustSubTab,
    onTabChange: (AdjustSubTab) -> Unit,
    adjustments: PhotoAdjustments,
    onAdjustmentsChange: (PhotoAdjustments) -> Unit,
    blur: BlurEffectState,
    onBlurChange: (BlurEffectState) -> Unit,
    curves: ToneCurvesState,
    onCurvesChange: (ToneCurvesState) -> Unit,
    onCancel: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val strings = MediaEditorI18n.current
    var selectedSlider by remember { mutableStateOf(SliderParam.EXPOSURE) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(scheme.surface.copy(alpha = 0.96f))
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 8.dp)
    ) {
        when (activeTab) {
            AdjustSubTab.SLIDERS -> {
                val curVal = getParamValue(adjustments, selectedSlider)
                val displayNum = (curVal * 100f).roundToInt()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = labelForParam(selectedSlider, strings),
                        color = scheme.textPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (displayNum > 0) "+$displayNum" else "$displayNum",
                        color = if (displayNum != 0) scheme.accent else scheme.textMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = curVal,
                    onValueChange = { v -> onAdjustmentsChange(setParamValue(adjustments, selectedSlider, v)) },
                    valueRange = selectedSlider.min..selectedSlider.max,
                    colors = SliderDefaults.colors(
                        thumbColor = scheme.accent,
                        activeTrackColor = scheme.accent,
                        inactiveTrackColor = scheme.textMuted.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SliderParam.entries.forEach { param ->
                        val isSelected = param == selectedSlider
                        val valNonZero = kotlin.math.abs(getParamValue(adjustments, param)) > 0.01f
                        Text(
                            text = labelForParam(param, strings),
                            color = when {
                                isSelected -> scheme.onAccent
                                valNonZero -> scheme.accent
                                else -> scheme.textSecondary
                            },
                            fontSize = 12.sp,
                            fontWeight = if (isSelected || valNonZero) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(
                                    when {
                                        isSelected -> scheme.accent
                                        valNonZero -> scheme.accent.copy(alpha = 0.14f)
                                        else -> scheme.card
                                    }
                                )
                                .clickable { selectedSlider = param }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            AdjustSubTab.BLUR -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                ) {
                    listOf(
                        BlurMode.OFF to strings.blurOff,
                        BlurMode.RADIAL to strings.blurRadial,
                        BlurMode.LINEAR to strings.blurLinear
                    ).forEach { (mode, label) ->
                        val selected = blur.mode == mode
                        Text(
                            text = label,
                            color = if (selected) scheme.onAccent else scheme.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) scheme.accent else scheme.card)
                                .clickable { onBlurChange(blur.copy(mode = mode)) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                if (blur.mode != BlurMode.OFF) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.blurIntensity, color = scheme.textSecondary, fontSize = 12.sp)
                        Text("${(blur.intensity * 100).roundToInt()}%", color = scheme.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = blur.intensity,
                        onValueChange = { onBlurChange(blur.copy(intensity = it)) },
                        valueRange = 0.1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = scheme.accent,
                            activeTrackColor = scheme.accent,
                            inactiveTrackColor = scheme.textMuted.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            }

            AdjustSubTab.CURVES -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            CurveChannel.ALL to strings.curveAll,
                            CurveChannel.RED to strings.curveRed,
                            CurveChannel.GREEN to strings.curveGreen,
                            CurveChannel.BLUE to strings.curveBlue
                        ).forEach { (ch, label) ->
                            val selected = curves.activeChannel == ch
                            val chipAccent = when (ch) {
                                CurveChannel.ALL -> scheme.accent
                                CurveChannel.RED -> Color(0xFFFF453A)
                                CurveChannel.GREEN -> Color(0xFF32D74B)
                                CurveChannel.BLUE -> Color(0xFF0A84FF)
                            }
                            Text(
                                text = label,
                                color = if (selected) scheme.onAccent else scheme.textPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) chipAccent else scheme.card)
                                    .clickable { onCurvesChange(curves.copy(activeChannel = ch)) }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }

                    Text(
                        text = strings.curveResetChannel,
                        color = scheme.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                            .clickable {
                                onCurvesChange(
                                    curves.withPoints(curves.activeChannel, ToneCurvesState.defaultPoints)
                                )
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Sub-tabs: Sliders | Blur | Curves
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                AdjustSubTab.SLIDERS to strings.tabSliders,
                AdjustSubTab.BLUR to strings.tabBlur,
                AdjustSubTab.CURVES to strings.tabCurves
            ).forEach { (tab, title) ->
                val selected = activeTab == tab
                Text(
                    text = title,
                    color = if (selected) scheme.accent else scheme.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { onTabChange(tab) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Cancel / Done
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.cancel,
                color = scheme.textSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onCancel() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
            Text(
                text = strings.done,
                color = scheme.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onDone() }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

private fun labelForParam(p: SliderParam, s: MediaEditorStrings): String = when (p) {
    SliderParam.SMOOTH_SKIN -> s.adjSmoothSkin
    SliderParam.ENHANCE -> s.adjEnhance
    SliderParam.EXPOSURE -> s.adjExposure
    SliderParam.CONTRAST -> s.adjContrast
    SliderParam.SATURATION -> s.adjSaturation
    SliderParam.WARMTH -> s.adjWarmth
    SliderParam.FADE -> s.adjFade
    SliderParam.HIGHLIGHTS -> s.adjHighlights
    SliderParam.SHADOWS -> s.adjShadows
    SliderParam.VIGNETTE -> s.adjVignette
    SliderParam.GRAIN -> s.adjGrain
    SliderParam.SHARPEN -> s.adjSharpen
}

private fun getParamValue(a: PhotoAdjustments, p: SliderParam): Float = when (p) {
    SliderParam.SMOOTH_SKIN -> a.smoothSkin
    SliderParam.ENHANCE -> a.enhance
    SliderParam.EXPOSURE -> a.exposure
    SliderParam.CONTRAST -> a.contrast
    SliderParam.SATURATION -> a.saturation
    SliderParam.WARMTH -> a.warmth
    SliderParam.FADE -> a.fade
    SliderParam.HIGHLIGHTS -> a.highlights
    SliderParam.SHADOWS -> a.shadows
    SliderParam.VIGNETTE -> a.vignette
    SliderParam.GRAIN -> a.grain
    SliderParam.SHARPEN -> a.sharpen
}

private fun setParamValue(a: PhotoAdjustments, p: SliderParam, v: Float): PhotoAdjustments = when (p) {
    SliderParam.SMOOTH_SKIN -> a.copy(smoothSkin = v)
    SliderParam.ENHANCE -> a.copy(enhance = v)
    SliderParam.EXPOSURE -> a.copy(exposure = v)
    SliderParam.CONTRAST -> a.copy(contrast = v)
    SliderParam.SATURATION -> a.copy(saturation = v)
    SliderParam.WARMTH -> a.copy(warmth = v)
    SliderParam.FADE -> a.copy(fade = v)
    SliderParam.HIGHLIGHTS -> a.copy(highlights = v)
    SliderParam.SHADOWS -> a.copy(shadows = v)
    SliderParam.VIGNETTE -> a.copy(vignette = v)
    SliderParam.GRAIN -> a.copy(grain = v)
    SliderParam.SHARPEN -> a.copy(sharpen = v)
}
