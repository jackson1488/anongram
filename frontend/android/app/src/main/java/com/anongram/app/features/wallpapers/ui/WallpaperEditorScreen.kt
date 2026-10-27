package com.anongram.app.features.wallpapers.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.wallpapers.logic.EditorViewModel
import com.anongram.app.features.wallpapers.ui.components.WallpaperMockChat

private val AccentViolet = Color(0xFF7A4DF3)

/**
 * Экран редактора обоев:
 * - Полноэкранный фон (с живым превью, затемнением, размытием и параллаксом)
 * - Превью чата с пузырями сообщений
 * - Регулировка затемнения свайпом (0..100%)
 * - Чипы «Размытие» и «Движение»
 * - Кнопка «Применить у меня»
 */
@Composable
fun WallpaperEditorScreen(
    chatId: String,
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onChangePhoto: () -> Unit,
    onApplied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val screenWidthPx = LocalConfiguration.current.screenWidthDp * 2.5f
    var accumulatedDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            // Горизонтальный свайп по экрану меняет степень затемнения 0..100%
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        accumulatedDrag = state.dim.toFloat()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        accumulatedDrag = (accumulatedDrag + (dragAmount.x / 5f)).coerceIn(0f, 100f)
                        viewModel.setDim(accumulatedDrag.toInt())
                    }
                )
            }
    ) {
        // Подложка обоев с живым превью (размытие, затемнение, параллакс)
        WallpaperBackground(
            chatId = chatId,
            modifier = Modifier.fillMaxSize()
        )

        // Контент поверх фона
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Верхняя панель (TopBar)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                Text(
                    text = "Обои",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                // Кисть: сменить фон
                IconButton(
                    onClick = onChangePhoto,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Brush,
                        contentDescription = "Сменить фон",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Солнце/луна
                IconButton(
                    onClick = { /* превью темы */ },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WbSunny,
                        contentDescription = "Вариант темы",
                        tint = Color(0xFFFFD54F),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))
            }

            // Плашка "Затемнение N%"
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Затемнение ${state.dim}% (проведите пальцем)",
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Превью пузырей сообщений чата
            WallpaperMockChat(
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Нижняя панель управления (чипы + кнопка)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                    .border(1.dp, CardBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                // Чипы «Размытие» и «Движение»
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    EditorOptionChip(
                        title = "Размытие",
                        icon = Icons.Outlined.BlurOn,
                        isActive = state.blur,
                        onClick = viewModel::toggleBlur,
                        modifier = Modifier.weight(1f)
                    )

                    EditorOptionChip(
                        title = "Движение",
                        icon = Icons.Outlined.ScreenRotation,
                        isActive = state.motion,
                        onClick = viewModel::toggleMotion,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Кнопка "Применить у меня"
                Button(
                    onClick = {
                        viewModel.applyWallpaper(onApplied)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentViolet),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Применить у меня",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Интерактивный чип переключения эффектов (Размытие, Движение).
 */
@Composable
private fun EditorOptionChip(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (isActive) AccentViolet else Color(0xFF1E293B)
    val contentColor = if (isActive) Color.White else TextSecondary
    val borderColor = if (isActive) AccentViolet else CardBorder

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
