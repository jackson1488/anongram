package com.anongram.app.features.settings.qr.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.scale
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
 * - По умолчанию палитры нет, крупный QR-код расположен строго в центре экрана;
 * - При открытии палитры карточка плавно сдвигается вверх к шапке, а палитра выезжает точно под карточкой (как на фото);
 * - Сохранение выбранного цвета в SharedPreferences — цвет сохраняется при перезаходе и не сбрасывается на дефолт;
 * - При системном жесте «Назад» — закрывается открытая палитра вместо закрытия экрана;
 * - Кнопка «Поделиться» отправляет ИСКЛЮЧИТЕЛЬНО текущий PNG-файл без текста и водяных знаков;
 * - Реальная генерация QR-кода ZXing со стелс-иконкой приложения (щит + самолётик) в центре;
 * - Премиальный iPhone Glass стиль оформления кнопок.
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

    // Загрузка сохранённого пользователем цвета из хранилища (не сбрасывается при перезаходе)
    var selectedTheme by remember { mutableStateOf<QrThemeItem>(QrThemePalette.getSavedTheme(context)) }

    // По умолчанию палитра скрыта!
    var isPaletteVisible by remember { mutableStateOf(false) }

    // При системном жесте "Назад" — закрываем палитру, не выходя из экрана
    BackHandler(enabled = isPaletteVisible) {
        isPaletteVisible = false
    }

    // Плавное масштабирование карточки при открытии палитры
    val cardScale by animateFloatAsState(
        targetValue = if (isPaletteVisible) 0.94f else 1.0f,
        animationSpec = tween(durationMillis = 250),
        label = "QrCardScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(selectedTheme.gradient))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Тап по фону закрывает открытую палитру и возвращает QR в центр
                if (isPaletteVisible) {
                    isPaletteVisible = false
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeStatusBarPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Верхняя плашка действий (iPhone Glass): Закрыть (X) слева, Сканировать QR справа
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Кнопка (X) слева (iPhone Glass Circle)
                // Если открыта палитра — нажатие закрывает палитру, иначе закрывает экран
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .border(1.2.dp, Color.White.copy(alpha = 0.24f), CircleShape)
                        .clickable {
                            if (isPaletteVisible) {
                                isPaletteVisible = false
                            } else {
                                onClose()
                            }
                        },
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
                        .padding(horizontal = 16.dp, vertical = 10.dp),
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

            if (!isPaletteVisible) {
                // 2А. РЕЖИМ ПО УМОЛЧАНИЮ (палитра закрыта):
                // Очень крупный QR-код центрирован строго посередине экрана
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeCard(
                        handleText = "@$cleanHandle",
                        qrPayload = qrPayload,
                        qrColor = selectedTheme.color,
                        modifier = Modifier.scale(cardScale)
                    )
                }

                // 3. Нижняя панель действий (iPhone Glass): Поделиться, Кисточка (по центру), Скачать
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeNavigationBarPadding()
                        .padding(vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Кнопка «Поделиться» (ТОЛЬКО ЧИСТЫЙ PNG-ФАЙЛ БЕЗ ТЕКСТА)
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

                        // 2. В ЦЕНТРЕ — ЕДИНАЯ КНОПКА «КИСТОЧКА» (открытие палитры)
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.2.dp, Color.White.copy(alpha = 0.26f), CircleShape)
                                .clickable {
                                    isPaletteVisible = true
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
            } else {
                // 2Б. РЕЖИМ ОТКРЫТОЙ ПАЛИТРЫ (в точности как на скриншоте 3):
                // - Крупный QR-код сразу под верхней шапкой;
                // - Нижней панели навигации НЕТ («нету ангив»);
                // - Палитра занимает ВСЁ оставшееся пространство экрана внизу («фулл окна берет внизу»);
                // - 84 цвета листаются вертикальным скроллом.
                Spacer(modifier = Modifier.height(4.dp))

                QrCodeCard(
                    handleText = "@$cleanHandle",
                    qrPayload = qrPayload,
                    qrColor = selectedTheme.color,
                    modifier = Modifier
                        .scale(cardScale)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Клик по QR-коду закрывает палитру
                            isPaletteVisible = false
                        }
                )

                Spacer(modifier = Modifier.height(10.dp))

                QrColorPaletteSheet(
                    selectedTheme = selectedTheme,
                    onSelectTheme = { theme ->
                        selectedTheme = theme
                        QrThemePalette.saveTheme(context, theme)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .safeNavigationBarPadding()
                        .padding(bottom = 6.dp)
                )
            }
        }
    }
}
