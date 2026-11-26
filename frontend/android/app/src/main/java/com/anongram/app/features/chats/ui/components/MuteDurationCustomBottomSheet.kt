package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * Bottom-sheet выбора длительности отключения уведомлений (зацикленный барабан):
 * - Бесконечная циклическая прокрутка с мягкой snap-доводкой
 * - Плавное 3D-масштабирование и затухание элементов по мере удаления от центра
 * - Тактильный отклик при переключении центрального значения
 * - Поддержка RU / EN и текущей темы
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MuteDurationCustomBottomSheet(
    onConfirm: (durationLabel: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val scheme = LocalAnonGramScheme.current
    val haptic = LocalHapticFeedback.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    val durations = remember(isRu) {
        if (isRu) {
            listOf(
                "30 мин.",
                "1 час",
                "2 часа",
                "3 часа",
                "8 часов",
                "1 день",
                "2 дня",
                "3 дня",
                "4 дня",
                "5 дней",
                "6 дней",
                "1 неделя",
                "2 недели",
                "3 недели",
                "1 месяц",
                "2 месяца",
                "3 месяца",
                "4 месяца",
                "5 месяцев",
                "6 месяцев",
                "1 год"
            )
        } else {
            listOf(
                "30 min",
                "1 hour",
                "2 hours",
                "3 hours",
                "8 hours",
                "1 day",
                "2 days",
                "3 days",
                "4 days",
                "5 days",
                "6 days",
                "1 week",
                "2 weeks",
                "3 weeks",
                "1 month",
                "2 months",
                "3 months",
                "4 months",
                "5 months",
                "6 months",
                "1 year"
            )
        }
    }

    val totalVirtualItems = Int.MAX_VALUE
    val startVirtualIndex = remember(durations.size) {
        val mid = totalVirtualItems / 2
        mid - (mid % durations.size)
    }

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startVirtualIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val itemHeightDp = 40.dp
    val itemHeightPx = with(LocalDensity.current) { itemHeightDp.toPx() }

    val centerVirtualIndex by remember {
        derivedStateOf {
            val layout = listState.layoutInfo
            val visible = layout.visibleItemsInfo
            if (visible.isEmpty()) {
                startVirtualIndex
            } else {
                val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                visible.minByOrNull { item ->
                    abs((item.offset + item.size / 2) - viewportCenter)
                }?.index ?: startVirtualIndex
            }
        }
    }

    var lastTickIndex by remember { mutableIntStateOf(startVirtualIndex) }
    LaunchedEffect(centerVirtualIndex) {
        if (centerVirtualIndex != lastTickIndex) {
            lastTickIndex = centerVirtualIndex
            if (listState.isScrollInProgress) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    val selectedRealIndex = ((centerVirtualIndex % durations.size) + durations.size) % durations.size

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = scheme.islandBg,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(scheme.islandBg)
                .padding(top = 22.dp, bottom = 28.dp)
        ) {
            Text(
                text = if (isRu) "Выключить уведомления на…" else "Mute notifications for…",
                color = scheme.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 5-слотовый зацикленный барабан (высота 200.dp, центральный слот 40.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeightDp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    HorizontalDivider(color = scheme.accent, thickness = 1.dp)
                    HorizontalDivider(color = scheme.accent, thickness = 1.dp)
                }

                LazyColumn(
                    state = listState,
                    flingBehavior = flingBehavior,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    items(
                        count = totalVirtualItems,
                        key = { it }
                    ) { virtualIndex ->
                        val realIndex = ((virtualIndex % durations.size) + durations.size) % durations.size
                        val label = durations[realIndex]
                        val isCenter = virtualIndex == centerVirtualIndex

                        val distanceFactor by remember(virtualIndex) {
                            derivedStateOf {
                                val layout = listState.layoutInfo
                                val info = layout.visibleItemsInfo.firstOrNull { it.index == virtualIndex }
                                if (info == null || itemHeightPx <= 0f) {
                                    if (isCenter) 0f else 1.5f
                                } else {
                                    val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2f
                                    val itemCenter = info.offset + info.size / 2f
                                    abs(itemCenter - viewportCenter) / itemHeightPx
                                }
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(itemHeightDp)
                                .graphicsLayer {
                                    val clamped = distanceFactor.coerceIn(0f, 2.5f)
                                    val scale = (1f - 0.13f * clamped).coerceAtLeast(0.72f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = (1f - 0.34f * clamped).coerceIn(0.16f, 1f)
                                }
                                .clickable {
                                    scope.launch {
                                        listState.animateScrollToItem(virtualIndex)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isCenter) scheme.textPrimary else scheme.textMuted,
                                fontSize = if (isCenter) 18.sp else 15.sp,
                                fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = {
                    onConfirm(durations[selectedRealIndex])
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.accent,
                    contentColor = scheme.onAccent
                ),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    text = if (isRu) "Подтвердить" else "Confirm",
                    color = scheme.onAccent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
