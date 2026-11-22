package com.anongram.app.features.chats.user_profile.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeOff
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.features.chats.user_profile.i18n.UserProfileStrings
import kotlinx.coroutines.launch

@Composable
internal fun UserProfileMuteMenuDialog(
    isMuted: Boolean,
    strings: UserProfileStrings,
    onDismiss: () -> Unit,
    onUnmute: () -> Unit,
    onMutePreset: (label: String) -> Unit,
    onOpenCustomDrum: () -> Unit,
    onOpenFullSettings: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(scheme.card)
                .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                .padding(vertical = 8.dp)
        ) {
            Text(
                text = strings.muteTitle,
                color = scheme.textSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)

            if (isMuted) {
                MuteOptionRow(
                    icon = Icons.AutoMirrored.Outlined.VolumeUp,
                    label = strings.muteEnableSound,
                    tintAccent = true,
                    onClick = {
                        onDismiss()
                        onUnmute()
                    }
                )
            }

            MuteOptionRow(
                icon = Icons.Outlined.Schedule,
                label = strings.mute1Hour,
                onClick = {
                    onDismiss()
                    onMutePreset(strings.mute1Hour)
                }
            )
            MuteOptionRow(
                icon = Icons.Outlined.Schedule,
                label = strings.mute8Hours,
                onClick = {
                    onDismiss()
                    onMutePreset(strings.mute8Hours)
                }
            )
            MuteOptionRow(
                icon = Icons.Outlined.NotificationsActive,
                label = strings.mute2Days,
                onClick = {
                    onDismiss()
                    onMutePreset(strings.mute2Days)
                }
            )
            MuteOptionRow(
                icon = Icons.Outlined.Timer,
                label = strings.muteCustomDrum,
                onClick = {
                    onDismiss()
                    onOpenCustomDrum()
                }
            )
            MuteOptionRow(
                icon = Icons.AutoMirrored.Outlined.VolumeOff,
                label = strings.muteForever,
                onClick = {
                    onDismiss()
                    onMutePreset(strings.mutedForeverToast)
                }
            )

            HorizontalDivider(color = scheme.divider, thickness = 0.5.dp)
            MuteOptionRow(
                icon = Icons.Outlined.Settings,
                label = strings.muteSettings,
                tintAccent = true,
                onClick = {
                    onDismiss()
                    onOpenFullSettings()
                }
            )
        }
    }
}

@Composable
private fun MuteOptionRow(
    icon: ImageVector,
    label: String,
    tintAccent: Boolean = false,
    onClick: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val tint = if (tintAccent) scheme.accent else scheme.textPrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            color = tint,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
internal fun MuteDrumPickerDialog(
    strings: UserProfileStrings,
    onDismiss: () -> Unit,
    onConfirm: (days: Int, hours: Int, minutes: Int) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val daysState = rememberLazyListState(initialFirstVisibleItemIndex = 0)
    val hoursState = rememberLazyListState(initialFirstVisibleItemIndex = 2)
    val minutesState = rememberLazyListState(initialFirstVisibleItemIndex = 0)

    val selectedDays by remember { derivedStateOf { daysState.firstVisibleItemIndex.coerceIn(0, 30) } }
    val selectedHours by remember { derivedStateOf { hoursState.firstVisibleItemIndex.coerceIn(0, 23) } }
    val selectedMinutes by remember { derivedStateOf { minutesState.firstVisibleItemIndex.coerceIn(0, 59) } }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(scheme.card)
                .border(0.7.dp, scheme.islandBorder, RoundedCornerShape(24.dp))
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.muteDrumTitle,
                color = scheme.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DrumWheelColumn(
                    label = strings.muteDrumDays,
                    count = 31,
                    listState = daysState,
                    modifier = Modifier.weight(1f)
                )
                DrumWheelColumn(
                    label = strings.muteDrumHours,
                    count = 24,
                    listState = hoursState,
                    modifier = Modifier.weight(1f)
                )
                DrumWheelColumn(
                    label = strings.muteDrumMinutes,
                    count = 60,
                    listState = minutesState,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.surface)
                        .border(0.6.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.muteCancel,
                        color = scheme.textSecondary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.accent)
                        .clickable {
                            onDismiss()
                            onConfirm(selectedDays, selectedHours, selectedMinutes)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.muteApply,
                        color = scheme.onAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun DrumWheelColumn(
    label: String,
    count: Int,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val scope = rememberCoroutineScope()
    val snapFling = rememberSnapFlingBehavior(lazyListState = listState)
    val selectedIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = scheme.textMuted,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .width(72.dp)
                .height(126.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.surface),
            contentAlignment = Alignment.Center
        ) {
            // Подсветка центральной выбранной строки барабана
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.accent.copy(alpha = 0.14f))
                    .border(0.7.dp, scheme.accent.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
            )
            LazyColumn(
                state = listState,
                flingBehavior = snapFling,
                contentPadding = PaddingValues(vertical = 43.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(count) { idx ->
                    val isSelected = idx == selectedIndex
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clickable {
                                scope.launch { listState.animateScrollToItem(idx) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = idx.toString().padStart(2, '0'),
                            color = if (isSelected) scheme.textPrimary else scheme.textMuted.copy(alpha = 0.55f),
                            fontSize = if (isSelected) 18.sp else 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
