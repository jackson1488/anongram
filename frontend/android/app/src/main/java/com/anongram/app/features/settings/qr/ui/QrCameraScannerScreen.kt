package com.anongram.app.features.settings.qr.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.anongram.app.features.settings.qr.ui.components.QrPermissionNotice
import com.anongram.app.features.settings.qr.ui.components.QrScannerViewfinder

/**
 * Экран сканирования QR-кода камерой (Скриншот 4).
 *
 * ОБЯЗАТЕЛЬНАЯ ЗАЩИТА ОТ ЧЁРНОГО ЭКРАНА:
 * При открытии проверяет разрешение `Manifest.permission.CAMERA`.
 * Если доступ к камере не предоставлен — показывает понятный экран `QrPermissionNotice`
 * с кнопкой запроса разрешения и переходом в системные настройки приложения.
 */
@Composable
fun QrCameraScannerScreen(
    onBack: () -> Unit,
    onOpenGallery: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isFlashlightOn by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Автоматически запрашиваем разрешение при первом входе, если оно ещё не выдано
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E121A))
    ) {
        if (!hasCameraPermission) {
            // Защита от пустого чёрного экрана при отсутствии разрешения на камеру
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                }
                QrPermissionNotice(
                    icon = Icons.Outlined.CameraAlt,
                    title = "Требуется доступ к камере",
                    description = "Чтобы отсканировать QR-код собеседника или войти на другом устройстве, разрешите AnonGram доступ к камере.",
                    buttonText = "Разрешить доступ к камере",
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            }
        } else {
            // Активный видоискатель сканера QR-кода (Скриншот 4)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF141923),
                                Color(0xFF1E1B2E),
                                Color(0xFF11151F)
                            )
                        )
                    )
            ) {
                // Кнопка Назад в левом верхнем углу
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 12.dp, top = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = Color.White
                    )
                }

                // Центральный блок: Заголовок «Сканировать QR-код», рамка видоискателя и кнопки внизу
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Сканировать QR-код",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    QrScannerViewfinder(
                        modifier = Modifier.size(260.dp)
                    )

                    Spacer(modifier = Modifier.height(48.dp))

                    // Нижние кнопки управления: Галерея слева, Вспышка справа
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(56.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Кнопка открытия Галереи
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(onClick = onOpenGallery) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoLibrary,
                                    contentDescription = "Выбрать QR из галереи",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Кнопка включения/выключения фонарика
                        Surface(
                            shape = CircleShape,
                            color = if (isFlashlightOn) Color(0xFF8B5CF6) else Color.White.copy(alpha = 0.14f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    isFlashlightOn = !isFlashlightOn
                                    val msg = if (isFlashlightOn) "Фонарик включён" else "Фонарик выключен"
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(
                                    imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Фонарик",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
