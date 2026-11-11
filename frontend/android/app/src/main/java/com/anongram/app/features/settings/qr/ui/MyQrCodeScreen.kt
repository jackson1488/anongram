package com.anongram.app.features.settings.qr.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.settings.qr.ui.components.QrCodeCard
import com.anongram.app.features.settings.qr.ui.components.QrColorPaletteSheet
import com.anongram.app.features.settings.qr.ui.components.QrImageRenderer
import com.anongram.app.features.settings.qr.ui.components.QrThemeItem
import com.anongram.app.features.settings.qr.ui.components.QrThemePalette

/**
 * Экран собственного QR-кода профиля AnonGram (Скриншот 3).
 *
 * Особенности:
 * - По умолчанию палитра скрыта, карточка QR-кода крупная и находится строго по центру экрана;
 * - При нажатии на кнопку-кисточку карточка плавно сдвигается вверх, и снизу выезжает 42-цветная палитра Telegram;
 * - Повторный клик по кисточке или тапу в пустое место возвращает карточку в центр;
 * - Реальная генерация QR-кода через ZXing со Стелс-иконкой приложения (щит + самолётик) в центре;
 * - iPhone Glass стиль оформления для всех кнопок и панелей;
 * - Кнопка «Поделиться» отправляет реальное PNG-изображение со стилем и именем пользователя.
 */
@Composable
fun MyQrCodeScreen(
    handle: String,
    publicKeyHex: String,
    onClose: () -> Unit,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    val cleanHandle = handle.removePrefix("@").uppercase()
    val qrPayload = "anongram://user?handle=$cleanHandle"

    var selectedTheme by remember { mutableStateOf<QrThemeItem>(QrThemePalette.defaultTheme) }
    var isPaletteVisible by remember { mutableStateOf(false) } // Палитра по дефолту скрыта!

    // Плавное вертикальное смещение карточки вверх при открытии палитры
    val cardOffsetY by animateDpAsState(
        targetValue = if (isPaletteVisible) (-92).dp else 0.dp,
        animationSpec = tween(durationMillis = 320),
        label = "QrCardOffsetAnimation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(selectedTheme.gradient))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Тап по фону при открытой палитре скрывает её и возвращает QR в центр
                if (isPaletteVisible) {
                    isPaletteVisible = false
                }
            }
    ) {
        // 1. Верхняя плашка действий (iPhone Glass стиль): Закрыть (X) слева, Сканировать QR справа
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .safeStatusBarPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Кнопка (X) слева (iPhone Glass Circle)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.2.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                    .clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрыть",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Кнопка «Сканировать QR-код» справа (iPhone Glass Capsule)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.2.dp, Color.White.copy(alpha = 0.24f), RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenScanner)
                    .padding(horizontal = 16.dp, vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = "Сканировать QR-код",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Сканировать QR-код",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // 2. Центральная крупная карточка QR-кода (при открытии палитры плавно отодвигается вверх)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            QrCodeCard(
                handleText = "@$cleanHandle",
                qrPayload = qrPayload,
                qrColor = selectedTheme.color,
                modifier = Modifier.offset(y = cardOffsetY)
            )
        }

        // 3. Выезжающая снизу 42-цветная палитра Telegram (над нижней панелью кнопок)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .safeNavigationBarPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 80.dp)
        ) {
            AnimatedVisibility(
                visible = isPaletteVisible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 300)
                ) + fadeIn(animationSpec = tween(durationMillis = 250)),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 280)
                ) + fadeOut(animationSpec = tween(durationMillis = 220))
            ) {
                QrColorPaletteSheet(
                    selectedTheme = selectedTheme,
                    onSelectTheme = { selectedTheme = it }
                )
            }
        }

        // 4. Нижняя панель действий (iPhone Glass): Поделиться, Кисточка (единая кнопка по центру), Скачать
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .safeNavigationBarPadding()
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Кнопка «Поделиться» (HD PNG-изображение со стилем, хэндлом и стелс-иконкой)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.2.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                        .clickable {
                            QrImageRenderer.shareQrImage(
                                context = context,
                                handle = cleanHandle,
                                qrColor = selectedTheme.color,
                                gradientColors = selectedTheme.gradient,
                                qrPayload = qrPayload
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться изображением QR",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 2. В ЦЕНТРЕ — ЕДИНАЯ КНОПКА «КИСТОЧКА» (плавное открытие/скрытие палитры)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPaletteVisible) Color.White.copy(alpha = 0.35f)
                            else Color.White.copy(alpha = 0.18f)
                        )
                        .border(
                            1.2.dp,
                            if (isPaletteVisible) Color.White else Color.White.copy(alpha = 0.26f),
                            CircleShape
                        )
                        .clickable {
                            isPaletteVisible = !isPaletteVisible
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Brush,
                        contentDescription = "Изменить стиль (кисточка)",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // 3. Кнопка «Скачать» (сохранение HD PNG в Галерею)
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .border(1.2.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                        .clickable {
                            val saved = QrImageRenderer.saveQrImageToGallery(
                                context = context,
                                handle = cleanHandle,
                                qrColor = selectedTheme.color,
                                gradientColors = selectedTheme.gradient,
                                qrPayload = qrPayload
                            )
                            val message = if (saved) {
                                "QR-код @$cleanHandle сохранён в Галерею"
                            } else {
                                "Ошибка при сохранении QR-кода"
                            }
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Скачать QR в галерею",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
