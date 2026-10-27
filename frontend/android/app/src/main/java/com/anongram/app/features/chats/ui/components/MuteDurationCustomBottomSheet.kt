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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import kotlinx.coroutines.launch
import kotlin.math.abs

private val AccentViolet = Color(0xFF7A4DF3)
private val SheetBackground = Color(0xFF161E2D) // 100% непрозрачный темный фон

/**
 * Bottom-sheet выбора длительности отключения уведомлений (барабан):
 * - Заголовок: «Выключить уведомления на…»
 * - Без полоски в центре окон (dragHandle = null)
 * - Непрозрачный контейнер
 * - Плавный барабан с физикой скролла и snap-доводкой между двумя фиолетовыми линиями
 * - Кнопка «Подтвердить» внизу
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MuteDurationCustomBottomSheet(
    onConfirm: (durationLabel: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val durations = remember {
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
    }

    var selectedIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = 0)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // При остановке свободного вращения барабана фиксируем центральный элемент
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val layout = listState.layoutInfo
            val visible = layout.visibleItemsInfo
            if (visible.isNotEmpty()) {
                val viewportCenter = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                val closest = visible.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    abs(itemCenter - viewportCenter)
                }
                if (closest != null && closest.index in durations.indices) {
                    selectedIndex = closest.index
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null, // Без полоски в центре окна
        containerColor = SheetBackground,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SheetBackground)
                .padding(top = 22.dp, bottom = 28.dp)
        ) {
            // Заголовок
            Text(
                text = "Выключить уведомления на…",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Колёсико-барабан с физикой скролла между двумя фиолетовыми линиями
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                contentAlignment = Alignment.Center
            ) {
                // Две горизонтальные фиолетовые линии (центральный слот 44.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    HorizontalDivider(color = AccentViolet, thickness = 1.dp)
                    HorizontalDivider(color = AccentViolet, thickness = 1.dp)
                }

                LazyColumn(
                    state = listState,
                    flingBehavior = flingBehavior,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(durations) { index, label ->
                        val isSelected = selectedIndex == index
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clickable {
                                    selectedIndex = index
                                    scope.launch { listState.animateScrollToItem(index) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) TextPrimary else TextMuted.copy(alpha = 0.45f),
                                fontSize = if (isSelected) 17.sp else 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Фиолетовая кнопка «Подтвердить»
            Button(
                onClick = {
                    onConfirm(durations[selectedIndex])
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = "Подтвердить",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
