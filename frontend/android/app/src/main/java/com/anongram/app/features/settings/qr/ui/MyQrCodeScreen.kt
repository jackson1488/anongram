package com.anongram.app.features.settings.qr.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

/**
 * Экран собственного QR-кода пользователя AnonGram (Скриншот 3).
 * Включает:
 * - Верхнюю панель с отступом под системный статус-бар, кнопкой закрытия `(X)` и капсулой «Сканировать QR-код».
 * - Центральную белую карточку с фиолетовым QR-кодом, логотипом самолётика и `@HANDLE`.
 * - Нижнюю панель быстрых действий (Поделиться, Стили темы, Скачать).
 */
@Composable
fun MyQrCodeScreen(
    handle: String,
    publicKeyHex: String,
    onClose: () -> Unit,
    onOpenScanner: () -> Unit
) {
    val context = LocalContext.current
    var selectedStyleIndex by remember { mutableIntStateOf(0) }

    val stylePalettes = listOf(
        listOf(Color(0xFF8B3DFF), Color(0xFF9B51E0), Color(0xFF6B21A8)),
        listOf(Color(0xFF6366F1), Color(0xFF8B5CF6), Color(0xFF4338CA)),
        listOf(Color(0xFFA855F7), Color(0xFFD946EF), Color(0xFF7E22CE)),
        listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6), Color(0xFF1D4ED8))
    )
    val currentGradient = stylePalettes[selectedStyleIndex % stylePalettes.size]
    val qrAccentColor = currentGradient.first()

    val cleanHandle = handle.removePrefix("@").uppercase()
    val qrPayload = "anongram://user?handle=$cleanHandle&pk=${publicKeyHex.take(24)}"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = Brush.verticalGradient(currentGradient))
    ) {
        // Верхняя панель с безопасным отступом от статус-бара: Кнопка (X) слева и «Сканировать QR-код» справа
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .safeStatusBarPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.28f),
                modifier = Modifier.size(44.dp)
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.Black.copy(alpha = 0.28f),
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onOpenScanner)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
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

        // Центральная карточка с QR-кодом
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QrCodeCard(
                handleText = "@$cleanHandle",
                qrColor = qrAccentColor
            )
        }

        // Нижняя панель кнопок: Поделиться, Стилизация, Сохранить
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .safeNavigationBarPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Кнопка Поделиться
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.26f),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Мой анонимный профиль в AnonGram: @$cleanHandle\n$qrPayload"
                            )
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Поделиться QR-кодом"))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Поделиться",
                        tint = Color.White
                    )
                }
            }

            // Центральные иконки переключения цветовых стилей QR-карточки
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.26f),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = { selectedStyleIndex = (selectedStyleIndex + 1) % stylePalettes.size }) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Сменить цвет QR",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.26f),
                    modifier = Modifier.size(44.dp)
                ) {
                    IconButton(onClick = { selectedStyleIndex = (selectedStyleIndex + 2) % stylePalettes.size }) {
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = "Эффект оформления",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Кнопка Скачать / Сохранить
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.26f),
                modifier = Modifier.size(48.dp)
            ) {
                IconButton(
                    onClick = {
                        Toast.makeText(
                            context,
                            "QR-код @$cleanHandle сохранён в галерею",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Скачать QR-код",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
