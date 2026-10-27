package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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

private val AccentViolet = Color(0xFF7A4DF3)

/**
 * Bottom-sheet выбора длительности отключения уведомлений (Фото 3):
 * - Заголовок: «Выключить уведомления на...»
 * - Без полоски в центре окон (dragHandle = null)
 * - Колёсико / прокручиваемый список между двумя фиолетовыми линиями:
 *   30 мин, 1 час, 2, 3, 8 часов, 1–6 дней, 1–3 недели, 1–6 месяцев, 1 год
 * - Кнопка «Подтвердить» внизу
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MuteDurationCustomBottomSheet(
    onConfirm: (durationLabel: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null, // Без полоски в центре окна
        containerColor = CardDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp, bottom = 24.dp)
        ) {
            // Заголовок (Фото 3)
            Text(
                text = "Выключить уведомления на…",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Колёсико выбора между двумя фиолетовыми полосками (Фото 3)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                contentAlignment = Alignment.Center
            ) {
                // Две горизонтальные фиолетовые линии (акцент)
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
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 53.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    itemsIndexed(durations) { index, label ->
                        val isSelected = selectedIndex == index
                        Text(
                            text = label,
                            color = if (isSelected) TextPrimary else TextMuted.copy(alpha = 0.5f),
                            fontSize = if (isSelected) 16.sp else 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedIndex = index }
                                .padding(vertical = 10.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Фиолетовая кнопка «Подтвердить» (Фото 3)
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
                shape = RoundedCornerShape(12.dp)
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
