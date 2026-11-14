package com.anongram.app.features.settings.animations.ui.transitions

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.navigation.AnonGramTransitionScreen
import com.anongram.app.core.navigation.NavigationTransitionConfig
import com.anongram.app.core.navigation.ParticleStyle
import com.anongram.app.core.navigation.PhysicsSprings
import com.anongram.app.core.navigation.TransitionType
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.AnonGramScreenSurface
import com.anongram.app.features.settings.animations.data.AnimationsSettingsStore
import com.anongram.app.features.settings.animations.i18n.AnimationsI18n
import com.anongram.app.features.settings.animations.i18n.AnimationsI18nStrings
import com.anongram.app.features.settings.animations.ui.components.AnimationsTopBar
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Экран настройки переходов между экранами с живым интерактивным превью.
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


    val previewConfig = remember(state) {
        NavigationTransitionConfig(
            transitionType = state.screenTransitionType,
            speedMultiplier = state.transitionSpeedMultiplier,
            springDamping = state.screenTransitionSpringDamping,
            particleStyle = state.screenParticleStyle,
            enableGesturePullCombo = state.enableGesturePullCombo,
            enablePredictiveBackCombo = state.enablePredictiveBackCombo
        )
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
                TransitionLivePreviewCard(
                    config = previewConfig,
                    strings = strings
                )

                HorizontalDivider(color = scheme.divider)

                Text(
                    text = strings.transitionsStyleHeader,
                    color = scheme.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 6.dp)
                )

                TransitionType.entries.forEach { type ->
                    val (title, desc) = resolveTransitionLabels(type, strings)
                    val selected = state.screenTransitionType == type
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                AnimationsSettingsStore.setScreenTransitionType(context, type)
                            }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = {
                                AnimationsSettingsStore.setScreenTransitionType(context, type)
                            },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = scheme.accent,
                                unselectedColor = scheme.textMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = scheme.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = desc,
                                color = scheme.textSecondary,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }

                AnimatedVisibility(visible = state.screenTransitionType == TransitionType.PARTICLE_DISSOLVE) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = strings.transitionsParticleHeader,
                            color = scheme.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ParticleStyle.entries.forEach { pStyle ->
                                val label = resolveParticleLabel(pStyle, strings)
                                val selected = state.screenParticleStyle == pStyle
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        AnimationsSettingsStore.setScreenParticleStyle(context, pStyle)
                                    },
                                    label = { Text(label, fontSize = 12.5.sp) },
                                    leadingIcon = if (selected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    } else null,
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = scheme.card,
                                        labelColor = scheme.textSecondary,
                                        selectedContainerColor = scheme.accent.copy(alpha = 0.2f),
                                        selectedLabelColor = scheme.accent,
                                        selectedLeadingIconColor = scheme.accent
                                    )
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    color = scheme.divider,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = strings.transitionsComboHeader,
                    color = scheme.accent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 4.dp)
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
                    title = strings.transitionsComboPredictiveTitle,
                    description = strings.transitionsComboPredictiveDesc,
                    checked = state.enablePredictiveBackCombo,
                    onCheckedChange = {
                        AnimationsSettingsStore.setEnablePredictiveBackCombo(context, it)
                    }
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.transitionsSpringDampingTitle,
                            color = scheme.textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "${(state.screenTransitionSpringDamping * 100).roundToInt() / 100f}",
                            color = scheme.accent,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Slider(
                        value = state.screenTransitionSpringDamping,
                        onValueChange = {
                            AnimationsSettingsStore.setScreenTransitionSpringDamping(context, it)
                        },
                        valueRange = 0.50f..1.0f,
                        steps = 9,
                        colors = SliderDefaults.colors(
                            thumbColor = scheme.accent,
                            activeTrackColor = scheme.accent,
                            inactiveTrackColor = scheme.divider
                        )
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun TransitionLivePreviewCard(
    config: NavigationTransitionConfig,
    strings: AnimationsI18nStrings
) {
    val scheme = LocalAnonGramScheme.current
    val scope = rememberCoroutineScope()
    val progress = remember { Animatable(0f) }
    var isDetailVisible by remember { mutableStateOf(true) }

    fun animateOpen() {
        scope.launch {
            isDetailVisible = true
            progress.snapTo(1f)
            if (config.transitionType == TransitionType.INSTANT) {
                progress.snapTo(0f)
            } else {
                progress.animateTo(
                    targetValue = 0f,
                    animationSpec = if (config.transitionType == TransitionType.SPRING_SWIPE) {
                        PhysicsSprings.screenSpringSpec(
                            damping = config.springDamping,
                            stiffness = config.springStiffness * config.speedMultiplier
                        )
                    } else {
                        tween(durationMillis = config.effectiveDurationMs)
                    }
                )
            }
        }
    }

    fun animateBack() {
        scope.launch {
            if (!isDetailVisible) isDetailVisible = true
            if (config.transitionType == TransitionType.INSTANT) {
                progress.snapTo(1f)
                isDetailVisible = false
            } else {
                progress.animateTo(
                    targetValue = 1f,
                    animationSpec = if (config.transitionType == TransitionType.SPRING_SWIPE) {
                        PhysicsSprings.screenSpringSpec(
                            damping = config.springDamping,
                            stiffness = config.springStiffness * config.speedMultiplier
                        )
                    } else {
                        tween(durationMillis = config.effectiveDurationMs)
                    }
                )
                isDetailVisible = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = strings.previewHeader,
            color = scheme.accent,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = strings.transitionsPreviewHint,
            color = scheme.textMuted,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(scheme.background)
                .border(1.dp, scheme.islandBorder, RoundedCornerShape(20.dp))
                .pointerInput(config) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            if (!isDetailVisible) {
                                isDetailVisible = true
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            scope.launch {
                                val delta = dragAmount / size.width.toFloat().coerceAtLeast(1f)
                                progress.snapTo((progress.value + delta).coerceIn(0f, 1f))
                            }
                        },
                        onDragEnd = {
                            if (progress.value > 0.35f) {
                                animateBack()
                            } else {
                                animateOpen()
                            }
                        },
                        onDragCancel = {
                            animateOpen()
                        }
                    )
                }
        ) {
            // Задний экран (список чатов)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scheme.surface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = strings.transitionsPreviewChatsTitle,
                    color = scheme.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                repeat(2) { idx ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(scheme.card)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(scheme.accent.copy(alpha = if (idx == 0) 0.25f else 0.14f))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (idx == 0) strings.transitionsPreviewChatPeer else "AnonGram",
                                color = scheme.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = strings.transitionsPreviewChatSub,
                                color = scheme.textMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Передний экран (чат)
            if (isDetailVisible) {
                AnonGramTransitionScreen(
                    progress = progress.value,
                    isExit = false,
                    config = config
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(scheme.background)
                            .padding(12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(scheme.islandBg)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = scheme.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = strings.transitionsPreviewChatPeer,
                                color = scheme.textPrimary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.transitionsPreviewChatSub,
                                color = scheme.accent,
                                fontSize = 11.5.sp
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(scheme.card)
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = strings.previewIncomingSample,
                                    color = scheme.incomingText,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(scheme.accent)
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = strings.previewOutgoingSample,
                                    color = scheme.onAccent,
                                    fontSize = 12.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { animateOpen() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.accent,
                    contentColor = scheme.onAccent
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(strings.transitionsPreviewOpenBtn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = { animateBack() },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    strings.transitionsPreviewBackBtn,
                    color = scheme.textPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
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
