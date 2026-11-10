package com.anongram.app.features.auth.ui.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.ui.safeNavigationBarPadding
import com.anongram.app.core.ui.safeStatusBarPadding

/**
 * Главный экран приветствия AnonGram при первом запуске приложения.
 *
 * Предлагает пользователю два основных пути:
 * 1. Создать новую децентрализованную идентичность (генерация 12 слов);
 * 2. Восстановить существующий аккаунт по сохранённой мнемонике BIP-39.
 *
 * @param onCreateIdentity Колбэк перехода к экрану создания личности.
 * @param onRestoreAccount Колбэк перехода к экрану восстановления по seed-фразе.
 * @param modifier Модификатор макета.
 */
@Composable
fun WelcomeScreen(
    onCreateIdentity: () -> Unit,
    onRestoreAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val obsidianBackground = Color(0xFF0D0B14)
    val purpleAccent = Color(0xFF8B5CF6)
    val lilacAccent = Color(0xFFC084FC)
    val cardBackground = Color(0xFF161224)
    val cardBorder = Color(0x338B5CF6)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(obsidianBackground)
            .safeStatusBarPadding()
            .safeNavigationBarPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Верхний блок: Щит, логотип и философия
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 28.dp)
            ) {
                WelcomeShieldAnimation(size = 130.dp)

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "AnonGram",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Zero-Knowledge Blind Relay Messenger",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = lilacAccent,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Карточки преимуществ протокола
                PrivacyFeatureItem(
                    icon = "🛡️",
                    title = "100% анонимность",
                    subtitle = "Без привязки к SIM-картам, email или паспортам",
                    cardBackground = cardBackground,
                    cardBorder = cardBorder
                )

                Spacer(modifier = Modifier.height(10.dp))

                PrivacyFeatureItem(
                    icon = "⚡",
                    title = "Слепой релей",
                    subtitle = "Сервер видит только зашифрованные конверты",
                    cardBackground = cardBackground,
                    cardBorder = cardBorder
                )

                Spacer(modifier = Modifier.height(10.dp))

                PrivacyFeatureItem(
                    icon = "🔐",
                    title = "Квантовая защита",
                    subtitle = "12 слов BIP-39 + криптография Ed25519 & ML-DSA",
                    cardBackground = cardBackground,
                    cardBorder = cardBorder
                )
            }

            // Нижний блок: Кнопки действий
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Главная кнопка: Создать анонимную личность
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = purpleAccent, spotColor = purpleAccent)
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF7C3AED), Color(0xFF9333EA), Color(0xFFC084FC))
                            )
                        )
                        .clickable { onCreateIdentity() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Создать анонимную личность",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Второстепенная кнопка: Войти по мнемонике
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF191428))
                        .border(1.dp, Color(0x4D8B5CF6), RoundedCornerShape(28.dp))
                        .clickable { onRestoreAccount() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Войти по Seed-фразе (12 слов)",
                        color = lilacAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun PrivacyFeatureItem(
    icon: String,
    title: String,
    subtitle: String,
    cardBackground: Color,
    cardBorder: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(cardBackground)
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF261D3B)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}
