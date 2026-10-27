package com.anongram.app.features.chats.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.IslandBackground
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.TextMuted
import androidx.compose.foundation.border
import com.anongram.app.core.theme.CardBorder

private val SEARCH_ISLAND_BG = IslandBackground // 100% непрозрачный фон островков

/**
 * Верхняя парящая полоска поиска Telegram-style:
 * ДВА РАЗДЕЛЬНЫХ ОСТРОВКА одинаковой высоты (44.dp):
 * 1) Круглый островок «Назад» (выход из поиска)
 * 2) Островок поиска с полем ввода и кнопкой очистки «✕»
 * Оба островка непрозрачные с тонкой обводкой CardBorder.
 */
@Composable
internal fun ChatSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Островок 1: Кнопка Назад (выход из поиска)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SEARCH_ISLAND_BG)
                .border(1.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Закрыть поиск",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Островок 2: Поле ввода поиска
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(SEARCH_ISLAND_BG)
                .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = "Поиск",
                        color = TextMuted,
                        fontSize = 15.sp
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    singleLine = true,
                    cursorBrush = SolidColor(scheme.accent)
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Очистить запрос",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Плавающие круглые кнопки перехода по найденным сообщениям (вверх / вниз):
 * Располагаются справа над нижним островком поиска.
 * Непрозрачные круглые островки с обводкой.
 */
@Composable
internal fun ChatSearchNavigationButtons(
    canNavigate: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Кнопка «Вверх»
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SEARCH_ISLAND_BG)
                .border(1.dp, CardBorder, CircleShape)
                .clickable(enabled = canNavigate, onClick = onPrevious),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowUp,
                contentDescription = "Предыдущее совпадение",
                tint = if (canNavigate) TextPrimary else TextMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Кнопка «Вниз»
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SEARCH_ISLAND_BG)
                .border(1.dp, CardBorder, CircleShape)
                .clickable(enabled = canNavigate, onClick = onNext),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = "Следующее совпадение",
                tint = if (canNavigate) TextPrimary else TextMuted.copy(alpha = 0.4f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Нижний парящий островок поиска в чате Telegram-style (Фото 6):
 * Иконка календаря (поиск по дате), счётчик «1 из 3», кнопка «Списком».
 * Непрозрачный островок с обводкой CardBorder.
 */
@Composable
internal fun ChatSearchBottomBar(
    counter: String,
    onCalendarClick: () -> Unit = {},
    onListClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(SEARCH_ISLAND_BG)
            .border(1.dp, CardBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Иконка календаря
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = "Поиск по дате",
            tint = TextSecondary,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = onCalendarClick)
        )

        // Счётчик результатов («1 из 3» или «0 из 0»)
        Text(
            text = if (counter.isBlank()) "0 из 0" else counter,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        // Кнопка «Списком»
        Text(
            text = "Списком",
            color = scheme.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.clickable(onClick = onListClick)
        )
    }
}
