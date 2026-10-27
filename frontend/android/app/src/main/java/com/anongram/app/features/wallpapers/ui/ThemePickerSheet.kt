package com.anongram.app.features.wallpapers.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CyberCyan
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary

private val AccentViolet = Color(0xFF7A4DF3)
import com.anongram.app.features.wallpapers.logic.ThemePickerViewModel
import com.anongram.app.features.wallpapers.model.WallpaperSource
import com.anongram.app.features.wallpapers.ui.components.ThemeCard

/**
 * Нижнее модальное окно выбора темы оформления чата в стиле Telegram.
 * Фон чата за окном меняется в реальном времени при выборе темы (живое превью).
 */
@Composable
fun ThemePickerSheet(
    viewModel: ThemePickerViewModel,
    onDismiss: () -> Unit,
    onNavigateToPhotos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(Color(0xF0101520))
            .border(1.dp, Color(0xFF222C3E), RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(top = 10.dp, bottom = 24.dp)
    ) {
        // Ручка перетаскивания (Drag Handle)
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Color(0xFF333E54))
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Верхняя панель: Стрелка назад, заголовок «Выбрать тему», иконка солнце/луна
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            IconButton(
                onClick = {
                    viewModel.onDismiss()
                    onDismiss()
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Выбрать тему",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            // Переключатель варианта темы (солнце / луна)
            IconButton(
                onClick = viewModel::toggleVariant,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B2332))
            ) {
                Icon(
                    imageVector = if (state.isDarkVariant) Icons.Outlined.WbSunny else Icons.Outlined.DarkMode,
                    contentDescription = "Светлая/тёмная тема",
                    tint = if (state.isDarkVariant) Color(0xFFFFD54F) else CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Переход на выбор фото
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF161E2D))
                .clickable(onClick = onNavigateToPhotos)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(AccentViolet.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Image,
                    contentDescription = null,
                    tint = AccentViolet,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Выбрать фон из галереи",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Фото, обои или абстракция",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(14.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Горизонтальная лента карточек тем
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Первая карточка: «Без темы» (красный крест)
            item(key = "none") {
                ThemeCard(
                    preset = null,
                    isSelected = state.selectedSource is WallpaperSource.None,
                    onClick = viewModel::selectNone
                )
            }

            // Готовые темы из пресетов
            items(state.presets, key = { it.id }) { preset ->
                val isSelected = (state.selectedSource as? WallpaperSource.Theme)?.themeId == preset.id
                ThemeCard(
                    preset = preset,
                    isSelected = isSelected,
                    onClick = { viewModel.selectTheme(preset) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Кнопка «Применить тему»
        Button(
            onClick = {
                viewModel.applyTheme(onSuccess = onDismiss)
            },
            enabled = !state.isApplying,
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentViolet,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(48.dp)
        ) {
            Text(
                text = if (state.isApplying) "Сохранение..." else "Применить тему",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
