package com.anongram.app.features.settings.account.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.features.profile.i18n.ProfileI18n
import java.util.Calendar
import kotlin.math.abs

/**
 * Шторка выбора даты рождения с 3D-барабаном (День | Месяц | Год).
 */
@Composable
fun BirthdayWheelSheet(
    initialDay: Int?,
    initialMonth: Int?,
    initialYear: Int?,
    onSave: (day: Int, month: Int, year: Int) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val strings = ProfileI18n.current
    val appLanguage by LanguageSettingsStore.language.collectAsState()
    val isRu = appLanguage == AppLanguage.RU

    val currentYear = remember { Calendar.getInstance().get(Calendar.YEAR) }
    val years = remember(currentYear) { (1800..currentYear).toList() }
    val months = remember(isRu) {
        if (isRu) {
            listOf(
                "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
            )
        } else {
            listOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
        }
    }

    var selectedYear by remember { mutableIntStateOf(initialYear ?: 2000) }
    var selectedMonth by remember { mutableIntStateOf((initialMonth ?: 9).coerceIn(1, 12)) }
    val maxDay = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonth - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    var selectedDay by remember { mutableIntStateOf((initialDay ?: 8).coerceIn(1, maxDay)) }
    LaunchedEffect(maxDay) {
        if (selectedDay > maxDay) selectedDay = maxDay
    }

    val days = remember(maxDay) { (1..maxDay).toList() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(scheme.overlayScrim)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(scheme.card)
                .border(0.8.dp, scheme.cardBorder, RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {}
                .safeNavigationBarPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.birthdaySheetTitle,
                color = scheme.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            )

            // 3D-барабан из 3 колонок с центральными акцентными линиями
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(color = scheme.accent.copy(alpha = 0.75f), thickness = 1.2.dp)
                    Spacer(modifier = Modifier.height(42.dp))
                    HorizontalDivider(color = scheme.accent.copy(alpha = 0.75f), thickness = 1.2.dp)
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelColumn(
                        items = days.map { it.toString() },
                        initialIndex = (selectedDay - 1).coerceIn(0, days.lastIndex),
                        onSelectedIndex = { idx -> selectedDay = days[idx] },
                        modifier = Modifier.weight(0.8f)
                    )
                    WheelColumn(
                        items = months,
                        initialIndex = (selectedMonth - 1).coerceIn(0, months.lastIndex),
                        onSelectedIndex = { idx -> selectedMonth = idx + 1 },
                        modifier = Modifier.weight(1.4f)
                    )
                    WheelColumn(
                        items = years.map { it.toString() },
                        initialIndex = years.indexOf(selectedYear).coerceAtLeast(0),
                        onSelectedIndex = { idx -> selectedYear = years[idx] },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onSave(selectedDay, selectedMonth, selectedYear) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = scheme.accent,
                    contentColor = scheme.onAccent
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = strings.save,
                    fontSize = 15.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (onDelete != null) {
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = onDelete,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = strings.deleteBirthdayAction,
                        color = scheme.accent,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
    onSelectedIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current
    val itemHeight = 42.dp
    val safeSize = items.size.coerceAtLeast(1)
    val startBase = remember(safeSize) {
        val mid = Int.MAX_VALUE / 2
        mid - (mid % safeSize)
    }
    val startIndex = remember(startBase, initialIndex, safeSize) {
        startBase + initialIndex.coerceIn(0, safeSize - 1)
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    val centerVirtualIndex by remember {
        derivedStateOf {
            val first = listState.firstVisibleItemIndex
            val offset = listState.firstVisibleItemScrollOffset
            if (offset > 45) first + 1 else first
        }
    }

    LaunchedEffect(centerVirtualIndex, safeSize) {
        val mod = ((centerVirtualIndex % safeSize) + safeSize) % safeSize
        if (mod in items.indices) {
            onSelectedIndex(mod)
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = flingBehavior,
        contentPadding = PaddingValues(vertical = itemHeight * 2),
        modifier = modifier.height(itemHeight * 5)
    ) {
        items(Int.MAX_VALUE) { virtualIdx ->
            val realIdx = ((virtualIdx % safeSize) + safeSize) % safeSize
            val distance = abs(virtualIdx - centerVirtualIndex)
            val alpha = when (distance) {
                0 -> 1f
                1 -> 0.58f
                else -> 0.24f
            }
            val scale = when (distance) {
                0 -> 1.06f
                1 -> 0.90f
                else -> 0.76f
            }
            val tilt = ((virtualIdx - centerVirtualIndex) * 18f).coerceIn(-45f, 45f)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .graphicsLayer {
                        this.alpha = alpha
                        this.scaleX = scale
                        this.scaleY = scale
                        this.rotationX = -tilt
                        cameraDistance = 12f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = items[realIdx],
                    color = if (distance == 0) scheme.textPrimary else scheme.textSecondary,
                    fontSize = if (distance == 0) 17.sp else 15.sp,
                    fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

fun formatBirthdayWithAge(day: Int, month: Int, year: Int, isRu: Boolean): String {
    val shortMonthsRu = listOf(
        "янв.", "февр.", "мар.", "апр.", "мая", "июн.",
        "июл.", "авг.", "сент.", "окт.", "нояб.", "дек."
    )
    val shortMonthsEn = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    val mName = (if (isRu) shortMonthsRu else shortMonthsEn).getOrElse(month - 1) { "" }
    val now = Calendar.getInstance()
    var age = now.get(Calendar.YEAR) - year
    val curMonth = now.get(Calendar.MONTH) + 1
    val curDay = now.get(Calendar.DAY_OF_MONTH)
    if (curMonth < month || (curMonth == month && curDay < day)) {
        age--
    }
    val dayStr = day.toString().padStart(2, '0')
    return if (age > 0) {
        if (isRu) "$dayStr $mName $year ($age лет)" else "$dayStr $mName $year ($age y.o.)"
    } else {
        "$dayStr $mName $year"
    }
}
