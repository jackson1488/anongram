package com.anongram.app.features.chats.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Radio
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.CardDark
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.ObsidianBlack
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.chats.messages.domain.MessageAttachment

private data class NearbyPlace(val name: String, val address: String, val distance: String)

/**
 * Экран геопозиции (шаг 5):
 * - тёмная карта со стилизованной сеткой, кнопка назад, заголовок, поиск, слои, моя позиция
 * - карточка «Отправить выбранную геопозицию»
 * - карточка «Транслировать геопозицию»
 * - «Или выберите место»: список мест рядом.
 */
@Composable
internal fun GeoPickerScreen(
    onBack: () -> Unit,
    onSendGeo: (MessageAttachment.Geo) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val scheme = LocalAnonGramScheme.current

    val nearbyPlaces = remember {
        listOf(
            NearbyPlace("Центральный парк", "ул. Мира, 12", "120 м"),
            NearbyPlace("Коворкинг Cypherpunk", "пл. Свободы, 4", "350 м"),
            NearbyPlace("Кафе Код", "пр. Ленина, 88", "500 м"),
            NearbyPlace("Метро Станция 1", "ул. Подземная, 1", "750 м")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
        // Верхний тулбар карты
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Назад",
                    tint = TextPrimary
                )
            }
            Text(
                text = "Геопозиция",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.Layers, contentDescription = "Слои", tint = TextSecondary)
            }
            IconButton(onClick = {}) {
                Icon(imageVector = Icons.Outlined.MyLocation, contentDescription = "Моя позиция", tint = scheme.accent)
            }
        }

        // Строка поиска мест
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CardDark)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(17.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(text = "Поиск мест...", color = TextMuted, fontSize = 13.sp)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = TextPrimary, fontSize = 13.sp),
                        singleLine = true,
                        cursorBrush = SolidColor(scheme.accent)
                    )
                }
            }
        }

        // Тёмная карта (Canvas со стилизованными дорогами и маркером)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color(0xFF0F141C)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val gridColor = Color(0xFF1B2330)
                val step = 32.dp.toPx()
                for (x in 0..(size.width / step).toInt()) {
                    drawLine(gridColor, Offset(x * step, 0f), Offset(x * step, size.height), 1f)
                }
                for (y in 0..(size.height / step).toInt()) {
                    drawLine(gridColor, Offset(0f, y * step), Offset(size.width, y * step), 1f)
                }
                // Стиль дороги
                drawLine(Color(0xFF243042), Offset(0f, size.height * 0.45f), Offset(size.width, size.height * 0.55f), 6f)
                drawLine(Color(0xFF243042), Offset(size.width * 0.5f, 0f), Offset(size.width * 0.45f, size.height), 6f)
            }

            // Центральный маркер
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(scheme.accent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(scheme.accent.copy(alpha = 0.5f))
                )
            }
        }

        // Карточки действий и список мест рядом
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                // Карточка «Отправить выбранную геопозицию»
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardDark)
                        .clickable { onSendGeo(MessageAttachment.Geo("ул. Мира, 12, Центральный район")) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(scheme.accent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.Navigation, contentDescription = null, tint = scheme.accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Отправить выбранную геопозицию", color = scheme.accent, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "ул. Мира, 12, Центральный район", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Карточка «Транслировать геопозицию»
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CardDark)
                        .clickable { onSendGeo(MessageAttachment.Geo("Трансляция геопозиции (в реальном времени)")) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Outlined.Radio, contentDescription = null, tint = Color(0xFF66BB6A), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Транслировать геопозицию", color = Color(0xFF66BB6A), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Обновляется в реальном времени", color = TextMuted, fontSize = 12.sp)
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Или выберите место",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            items(nearbyPlaces) { place ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSendGeo(MessageAttachment.Geo("${place.name}, ${place.address}")) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = null, tint = TextMuted, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = place.name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(text = place.address, color = TextMuted, fontSize = 12.sp)
                    }
                    Text(text = place.distance, color = TextMuted, fontSize = 11.sp)
                }
            }
        }
    }
}
