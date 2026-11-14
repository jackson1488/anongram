package com.anongram.app.features.settings.animations.ui.transitions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.navigation.ParticleStyle
import com.anongram.app.core.navigation.TransitionType
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.settings.animations.data.AnimationsSettingsState
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.animations.i18n.AnimationsI18n
import com.anongram.app.features.settings.animations.i18n.AnimationsI18nStrings
import com.anongram.app.features.settings.animations.ui.components.AnimationsTopBar
import kotlin.math.roundToInt

private val BaseTransitionTypes = listOf(
    TransitionType.SPRING_SWIPE,
    TransitionType.CUPERTINO_PARALLAX,
    TransitionType.CARD_STACK_PEEK,
    TransitionType.PREDICTIVE_BACK
)

private val EffectTransitionTypes = listOf(
    TransitionType.REAL_BOOK_3D,
    TransitionType.CUBE_ROTATION_3D,
    TransitionType.ROTATION_SPIN,
    TransitionType.CASCADE,
    TransitionType.WINDMILL,
    TransitionType.PARTICLE_DISSOLVE,
    TransitionType.INSTANT
)

/**
 * Экран настройки переходов между экранами:
 * - «Базовые анимации» на тумблерах со скрещиванием только между ними;
 * - «Эффекты переходов» с индивидуальным контроллером под выбранным стилем.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScreenTransitionsSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current
    val strings = AnimationsI18n.current
    val state by AnimationsSettingsStore.state.collectAsState()

    LaunchedEffect(Unit) {
        AnimationsSettingsStore.ensureInitialized(context)
    }

    AnonGramScreenSurface(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize()) {
            AnimationsTopBar(
                title = strings.transitionsSectionTitle,
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                SectionHeader(title = strings.transitionsBaseSectionHeader)

                BaseTransitionTypes.forEach { type ->
                    TransitionStyleItem(
                        type = type,
                        selected = state.screenTransitionType == type,
                        strings = strings,
                        onSelect = {
                            AnimationsSettingsStore.setScreenTransitionType(context, type)
                        }
                    )
                    StyleInlineController(
                        type = type,
                        isSelected = state.screenTransitionType == type,
                        state = state,
                        strings = strings
                    )
                }

                HorizontalDivider(
                    color = scheme.divider,
                    modifier = Modifier.padding(top = 6.dp)
                )

                SectionHeader(title = strings.transitionsComboHeader)

                ComboToggleRow(
                    title = "+ ${strings.transCardStackTitle}",
                    description = strings.transCardStackDesc,
                    checked = state.enableMiniCardCombo,
                    onCheckedChange = {
                        AnimationsSettingsStore.setEnableMiniCardCombo(context, it)
                    }
                )

                ComboToggleRow(
                    title = strings.transitionsComboPredictiveTitle,
                    description = strings.transitionsComboPredictiveDesc,
                    checked = state.enablePredictiveBackCombo,
                    onCheckedChange = {
                        AnimationsSettingsStore.setEnablePredictiveBackCombo(context, it)
                    }
                )

                ComboToggleRow(
                    title = strings.transitionsComboGestureTitle,
                    description = strings.transitionsComboGestureDesc,
                    checked = state.enableGesturePullCombo,
                    onCheckedChange = {
                        AnimationsSettingsStore.setEnableGesturePullCombo(context, it)
                    }
                )

                ComboToggleRow(
                    title = strings.transitionsBaseEnterTitle,
                    description = strings.transitionsBaseEnterDesc,
                    checked = state.enableBaseTelegramEnter,
                    onCheckedChange = {
                        AnimationsSettingsStore.setEnableBaseTelegramEnter(context, it)
                    }
                )

                HorizontalDivider(
                    color = scheme.divider,
                    modifier = Modifier.padding(top = 6.dp)
                )

                SectionHeader(title = strings.transitionsEffectsSectionHeader)

                EffectTransitionTypes.forEach { type ->
                    TransitionStyleItem(
                        type = type,
                        selected = state.screenTransitionType == type,
                        strings = strings,
                        onSelect = {
                            AnimationsSettingsStore.setScreenTransitionType(context, type)
                        }
                    )
                    StyleInlineController(
                        type = type,
                        isSelected = state.screenTransitionType == type,
                        state = state,
                        strings = strings
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    val scheme = LocalAnonGramScheme.current
    Text(
        text = title,
        color = scheme.accent,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 6.dp)
    )
}

@Composable
private fun TransitionStyleItem(
    type: TransitionType,
    selected: Boolean,
    strings: AnimationsI18nStrings,
    onSelect: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val (title, desc) = resolveTransitionLabels(type, strings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = scheme.accent,
                unselectedColor = scheme.textMuted
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                if (type != TransitionType.INSTANT) {
                    val badgeText = if (type.isBidirectional) {
                        strings.transitionBadgeBothWays
                    } else {
                        strings.transitionBadgeExitOnly
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (type.isBidirectional) {
                                    scheme.accent.copy(alpha = 0.16f)
                                } else {
                                    scheme.card
                                }
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = if (type.isBidirectional) scheme.accent else scheme.textMuted,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
            Text(
                text = desc,
                color = scheme.textSecondary,
                fontSize = 12.5.sp
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StyleInlineController(
    type: TransitionType,
    isSelected: Boolean,
    state: AnimationsSettingsState,
    strings: AnimationsI18nStrings
) {
    val context = LocalContext.current
    val scheme = LocalAnonGramScheme.current

    AnimatedVisibility(visible = isSelected && type != TransitionType.INSTANT) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 52.dp, end = 16.dp, top = 2.dp, bottom = 10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.card)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (type) {
                TransitionType.PARTICLE_DISSOLVE -> {
                    Text(
                        text = strings.transitionsParticleHeader,
                        color = scheme.accent,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ParticleStyle.entries.forEach { pStyle ->
                            val label = resolveParticleLabel(pStyle, strings)
                            val chipSelected = state.screenParticleStyle == pStyle
                            FilterChip(
                                selected = chipSelected,
                                onClick = {
                                    AnimationsSettingsStore.setScreenParticleStyle(context, pStyle)
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                leadingIcon = if (chipSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = scheme.surface,
                                    labelColor = scheme.textSecondary,
                                    selectedContainerColor = scheme.accent.copy(alpha = 0.2f),
                                    selectedLabelColor = scheme.accent,
                                    selectedLeadingIconColor = scheme.accent
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CompactSwitchRow(
                        title = strings.transitionsEnableEnterTitle,
                        checked = state.enableEnterAnimation,
                        onCheckedChange = { AnimationsSettingsStore.setEnableEnterAnimation(context, it) }
                    )
                    CompactSwitchRow(
                        title = strings.transitionsEnableExitTitle,
                        checked = state.enableExitAnimation,
                        onCheckedChange = { AnimationsSettingsStore.setEnableExitAnimation(context, it) }
                    )
                    CompactSwitchRow(
                        title = strings.transitionsParticleSlideTitle,
                        checked = state.particleSlideCombo,
                        onCheckedChange = { AnimationsSettingsStore.setParticleSlideCombo(context, it) }
                    )
                    CompactSwitchRow(
                        title = strings.transitionsParticleDensityTitle,
                        checked = state.denseParticles,
                        onCheckedChange = { AnimationsSettingsStore.setDenseParticles(context, it) }
                    )
                    SpeedSliderBlock(
                        title = strings.transitionsSpeedTitle,
                        speedScale = state.transitionSpeedScale,
                        onSpeedChange = { AnimationsSettingsStore.setTransitionSpeedScale(context, it) }
                    )
                }

                TransitionType.SPRING_SWIPE -> {
                    DampingSliderBlock(
                        title = strings.transitionsSpringDampingTitle,
                        damping = state.screenTransitionSpringDamping,
                        onDampingChange = {
                            AnimationsSettingsStore.setScreenTransitionSpringDamping(context, it)
                        }
                    )
                }

                TransitionType.REAL_BOOK_3D,
                TransitionType.CUBE_ROTATION_3D,
                TransitionType.CASCADE,
                TransitionType.WINDMILL,
                TransitionType.ROTATION_SPIN -> {
                    if (type.isBidirectional) {
                        CompactSwitchRow(
                            title = strings.transitionsEnableEnterTitle,
                            checked = state.enableEnterAnimation,
                            onCheckedChange = { AnimationsSettingsStore.setEnableEnterAnimation(context, it) }
                        )
                        CompactSwitchRow(
                            title = strings.transitionsEnableExitTitle,
                            checked = state.enableExitAnimation,
                            onCheckedChange = { AnimationsSettingsStore.setEnableExitAnimation(context, it) }
                        )
                    }
                    SpeedSliderBlock(
                        title = strings.transitionsSpeedTitle,
                        speedScale = state.transitionSpeedScale,
                        onSpeedChange = { AnimationsSettingsStore.setTransitionSpeedScale(context, it) }
                    )
                }

                else -> {
                    SpeedSliderBlock(
                        title = strings.transitionsSpeedTitle,
                        speedScale = state.transitionSpeedScale,
                        onSpeedChange = { AnimationsSettingsStore.setTransitionSpeedScale(context, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = scheme.textPrimary,
            fontSize = 13.5.sp,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = scheme.onAccent,
                checkedTrackColor = scheme.accent,
                uncheckedThumbColor = scheme.textMuted,
                uncheckedTrackColor = scheme.surface
            )
        )
    }
}

@Composable
private fun SpeedSliderBlock(
    title: String,
    speedScale: Float,
    onSpeedChange: (Float) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = scheme.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(speedScale * 10f).roundToInt() / 10f}x",
                color = scheme.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = speedScale,
            onValueChange = onSpeedChange,
            valueRange = 0.5f..2.0f,
            steps = 14,
            colors = SliderDefaults.colors(
                thumbColor = scheme.accent,
                activeTrackColor = scheme.accent,
                inactiveTrackColor = scheme.divider
            )
        )
    }
}

@Composable
private fun DampingSliderBlock(
    title: String,
    damping: Float,
    onDampingChange: (Float) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = scheme.textPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${(damping * 100).roundToInt() / 100f}",
                color = scheme.accent,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Slider(
            value = damping,
            onValueChange = onDampingChange,
            valueRange = 0.50f..1.0f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = scheme.accent,
                activeTrackColor = scheme.accent,
                inactiveTrackColor = scheme.divider
            )
        )
    }
}

@Composable
private fun ComboToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = scheme.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                color = scheme.textSecondary,
                fontSize = 12.5.sp
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = scheme.onAccent,
                checkedTrackColor = scheme.accent,
                uncheckedThumbColor = scheme.textMuted,
                uncheckedTrackColor = scheme.card
            )
        )
    }
}

internal fun resolveTransitionLabels(
    type: TransitionType,
    strings: AnimationsI18nStrings
): Pair<String, String> = when (type) {
    TransitionType.SPRING_SWIPE -> strings.transSpringSwipeTitle to strings.transSpringSwipeDesc
    TransitionType.CUPERTINO_PARALLAX -> strings.transCupertinoTitle to strings.transCupertinoDesc
    TransitionType.REAL_BOOK_3D -> strings.transRealBookTitle to strings.transRealBookDesc
    TransitionType.CUBE_ROTATION_3D -> strings.transCube3DTitle to strings.transCube3DDesc
    TransitionType.ROTATION_SPIN -> strings.transRotationSpinTitle to strings.transRotationSpinDesc
    TransitionType.CASCADE -> strings.transCascadeTitle to strings.transCascadeDesc
    TransitionType.WINDMILL -> strings.transWindmillTitle to strings.transWindmillDesc
    TransitionType.PARTICLE_DISSOLVE -> strings.transParticleTitle to strings.transParticleDesc
    TransitionType.CARD_STACK_PEEK -> strings.transCardStackTitle to strings.transCardStackDesc
    TransitionType.PREDICTIVE_BACK -> strings.transPredictiveBackTitle to strings.transPredictiveBackDesc
    TransitionType.INSTANT -> strings.transInstantTitle to strings.transInstantDesc
}

private fun resolveParticleLabel(
    style: ParticleStyle,
    strings: AnimationsI18nStrings
): String = when (style) {
    ParticleStyle.VAPOR_DUST -> strings.particleVaporDust
    ParticleStyle.PUZZLE_PIECES -> strings.particlePuzzlePieces
    ParticleStyle.DIGITAL_VOXELS -> strings.particleDigitalVoxels
    ParticleStyle.ASH_EMBERS -> strings.particleAshEmbers
}
