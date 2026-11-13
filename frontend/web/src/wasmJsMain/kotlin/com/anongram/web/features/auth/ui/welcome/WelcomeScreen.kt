package com.anongram.web.features.auth.ui.welcome

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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.AnonGramScreenSurface

@Composable
fun WelcomeScreen(
    onCreateIdentity: () -> Unit,
    onRestoreAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    AnonGramScreenSurface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
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
                        color = scheme.textPrimary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Zero-Knowledge Blind Relay Messenger",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = scheme.accent,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(28.dp))

                    PrivacyFeatureItem(
                        icon = "🛡️",
                        title = "100% анонимность",
                        subtitle = "Без привязки к SIM-картам, email или паспортам"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrivacyFeatureItem(
                        icon = "⚡",
                        title = "Слепой релей",
                        subtitle = "Сервер видит только зашифрованные конверты"
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    PrivacyFeatureItem(
                        icon = "🔐",
                        title = "Квантовая защита",
                        subtitle = "12 слов BIP-39 + криптография Ed25519 & ML-DSA"
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = scheme.accent, spotColor = scheme.accent)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.horizontalGradient(listOf(scheme.accent, scheme.accentDim)))
                            .clickable { onCreateIdentity() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Создать анонимную личность",
                            color = scheme.onAccent,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(scheme.card)
                            .border(1.dp, scheme.cardBorder, RoundedCornerShape(28.dp))
                            .clickable { onRestoreAccount() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Войти по Seed-фразе (12 слов)",
                            color = scheme.accent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
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
    modifier: Modifier = Modifier
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(scheme.card)
            .border(1.dp, scheme.cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(scheme.accentSoft),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 18.sp)
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, color = scheme.textPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, color = scheme.textSecondary, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}
