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
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.ui.res.painterResource
import com.anongram.app.R
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.LocalAnonGramScheme
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
    val scheme = LocalAnonGramScheme.current
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .background(scheme.card.copy(alpha = 0.96f))
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume background click */ }
            .padding(top = 10.dp, bottom = 24.dp)
    ) {
        // Ручка перетаскивания (Drag Handle)
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(scheme.textMuted.copy(alpha = 0.45f))
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
                    tint = scheme.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "Выбрать тему",
                color = scheme.textPrimary,
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
                    .background(scheme.surface)
            ) {
                Icon(
                    imageVector = if (state.isDarkVariant) Icons.Outlined.WbSunny else Icons.Outlined.DarkMode,
                    contentDescription = "Светлая/тёмная тема",
                    tint = if (state.isDarkVariant) {
                        if (scheme.isDark) Color(0xFFFFD54F) else Color(0xFFD97706)
                    } else {
                        scheme.accent
                    },
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Сброс обоев к стандартным
            IconButton(
                onClick = {
                    viewModel.resetWallpaper(onDismiss)
                },
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(scheme.surface)
            ) {
                Icon(
                    imageVector = Icons.Outlined.RestartAlt,
                    contentDescription = "Сбросить обои",
                    tint = scheme.textSecondary,
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
                .background(scheme.surface)
                .clickable(onClick = onNavigateToPhotos)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(scheme.accentSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_gallery_horizontal_end),
                    contentDescription = null,
                    tint = scheme.accent,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Выбрать фон из галереи",
                    color = scheme.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Фото, обои или абстракция",
                    color = scheme.textSecondary,
                    fontSize = 12.sp
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = scheme.textSecondary,
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
                containerColor = scheme.accent,
                contentColor = scheme.onAccent
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
