package com.anongram.app.features.settings.security.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.anongram.app.core.theme.LocalAnonGramScheme

/**
 * Карточка квантово-устойчивой криптографической безопасности AnonGram.
 * Изолированный компонент раздела `features/settings/security/`.
 */
@Composable
fun SecurityCryptoCard(
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF141318))
            .padding(16.dp)
    ) {
        // Шапка карточки с неоновым щитом в акцентных цветах активной темы
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(scheme.accent, scheme.accentDim))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Криптографическая защита",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "AnonGram Quantum Shield",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Пост-квантовый протокол защиты данных",
                    color = scheme.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = Color.White.copy(alpha = 0.06f))
        Spacer(modifier = Modifier.height(12.dp))

        // Пункт 1: ML-DSA-87
        CryptoStatusRow(
            title = "Подписи ML-DSA-87 (FIPS 204)",
            description = "Защита от атак квантовых суперкомпьютеров (NIST Level 5)",
            accentColor = scheme.accent
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Пункт 2: Perfect Forward Secrecy
        CryptoStatusRow(
            title = "Perfect Forward Secrecy (PFS)",
            description = "Автоматическая ротация сессионных ключей каждые 100 сообщений",
            accentColor = scheme.accent
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Пункт 3: 100% анонимность пересылки
        CryptoStatusRow(
            title = "Абсолютная анонимность (Zero Leaks)",
            description = "forwardedFrom = null — автор исходного сообщения никогда не раскрывается",
            accentColor = scheme.accent
        )
    }
}

@Composable
private fun CryptoStatusRow(
    title: String,
    description: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.20f))
                .border(1.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = Color(0xFF8E99A8),
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
        }
    }
}
