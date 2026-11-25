package com.anongram.app.features.chats.mediaeditor.ui.adjust

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.BlurCircular
import androidx.compose.material.icons.outlined.LinearScale
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
    SHARPEN(0f, 1f),
    SMOOTH_SKIN(0f, 1f)
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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.86f),
                        Color.Black.copy(alpha = 0.96f)
                    )
                )
            )
            .navigationBarsPadding()
            .padding(top = 14.dp, bottom = 6.dp)
    ) {
        when (activeTab) {
            AdjustSubTab.SLIDERS -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SliderParam.entries.forEach { param ->
                        val curVal = getParamValue(adjustments, param)
                        val displayNum = (curVal * 100f).roundToInt()
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = labelForParam(param, strings),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
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
                                onValueChange = { v ->
                                    onAdjustmentsChange(setParamValue(adjustments, param, v))
                                },
                                valueRange = param.min..param.max,
                                colors = SliderDefaults.colors(
                                    thumbColor = scheme.accent,
                                    activeTrackColor = scheme.accent,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(32.dp)
                            )
                        }
                    }
                }
            }

            AdjustSubTab.BLUR -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Triple(BlurMode.OFF, strings.blurOff, Icons.Outlined.Remove),
                        Triple(BlurMode.RADIAL, strings.blurRadial, Icons.Outlined.BlurCircular),
                        Triple(BlurMode.LINEAR, strings.blurLinear, Icons.Outlined.LinearScale)
                    ).forEach { (mode, label, icon) ->
                        val selected = blur.mode == mode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onBlurChange(blur.copy(mode = mode)) }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = if (selected) scheme.accent else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                color = if (selected) scheme.accent else Color.White,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                if (blur.mode != BlurMode.OFF) {
                    Slider(
                        value = blur.intensity,
                        onValueChange = { onBlurChange(blur.copy(intensity = it)) },
                        valueRange = 0.1f..1f,
                        colors = SliderDefaults.colors(
                            thumbColor = scheme.accent,
                            activeTrackColor = scheme.accent,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                }
            }

            AdjustSubTab.CURVES -> {
                // 4 circular radio indicators with labels below (matching Telegram screenshot 2)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        Triple(CurveChannel.ALL, strings.curveAll, Color.White),
                        Triple(CurveChannel.RED, strings.curveRed, Color(0xFFFF453A)),
                        Triple(CurveChannel.GREEN, strings.curveGreen, Color(0xFF32D74B)),
                        Triple(CurveChannel.BLUE, strings.curveBlue, Color(0xFF0A84FF))
                    ).forEach { (ch, label, chColor) ->
                        val selected = curves.activeChannel == ch
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onCurvesChange(curves.copy(activeChannel = ch)) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, chColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(11.dp)
                                            .clip(CircleShape)
                                            .background(chColor)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = label,
                                color = chColor,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Bottom row: ОТМЕНА | [Tune]  [Droplet]  [Curves] | ГОТОВО
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = strings.cancel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onCancel() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(26.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    AdjustSubTab.SLIDERS to Icons.Default.Tune,
                    AdjustSubTab.BLUR to Icons.Outlined.WaterDrop,
                    AdjustSubTab.CURVES to Icons.Outlined.ShowChart
                ).forEach { (tab, icon) ->
                    val selected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (selected) scheme.accent.copy(alpha = 0.16f) else Color.Transparent)
                            .clickable { onTabChange(tab) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (selected) scheme.accent else Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Text(
                text = strings.done,
                color = scheme.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .clickable { onDone() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
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
