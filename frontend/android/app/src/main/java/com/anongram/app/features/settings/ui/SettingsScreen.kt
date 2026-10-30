package com.anongram.app.features.settings.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.R
import com.anongram.app.core.theme.AnonGramThemes
import com.anongram.app.core.theme.CardBorder
import com.anongram.app.core.theme.ChatTextSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.theme.SurfaceDark
import com.anongram.app.core.theme.TextMuted
import com.anongram.app.core.theme.TextPrimary
import com.anongram.app.core.theme.TextSecondary
import com.anongram.app.features.settings.ui.components.ThemePickerDialog
import kotlin.math.roundToInt

/**
 * Раздел «Настройки» (секция 4 из 5):
 * - Настройки чатов: ползунок «Размер текста сообщений» (1–30) с живым превью и именем пользователя при регистрации
 * - Тема оформления (через ThemeController)
 * - Переходы экранов, крипто-ядро, жесты
 */
@Composable
fun SettingsScreen(
    currentThemeId: String,
    onThemeSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(context) {
        ChatTextSettingsStore.ensureInitialized(context)
    }

    val textSizeSp by ChatTextSettingsStore.messageTextSizeSp.collectAsState()
    val registeredUserName by ChatTextSettingsStore.registeredUserName.collectAsState()

    var themePickerOpen by remember { mutableStateOf(false) }
    val activeThemeName = AnonGramThemes.byId(currentThemeId).displayName

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Настройки чатов",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 16.sp
        )

        // Карточка «Размер текста сообщений» (1..30) + живое превью чата (как в Telegram)
        ChatTextSizeSettingsCard(
            textSizeSp = textSizeSp,
            userName = registeredUserName,
            onTextSizeChange = { newSize ->
                ChatTextSettingsStore.setMessageTextSizeSp(context, newSize)
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Основные настройки",
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            fontSize = 16.sp
        )

        SettingsRow(
            title = "Тема оформления",
            value = activeThemeName,
            highlight = true,
            onClick = { themePickerOpen = true }
        )
        SettingsRow(title = "Переходы экранов", value = "11 стилей · активен SPRING_SWIPE")
        SettingsRow(title = "Крипто-ядро", value = "Rust · ML-KEM-1024 · ChaCha20-Poly1305")
        SettingsRow(title = "Жесты", value = "Полноэкранный свайп · Predictive Back")
    }

    if (themePickerOpen) {
        ThemePickerDialog(
            currentThemeId = currentThemeId,
            onSelect = { id ->
                onThemeSelect(id)
                themePickerOpen = false
            },
            onDismiss = { themePickerOpen = false }
        )
    }
}

@Composable
private fun ChatTextSizeSettingsCard(
    textSizeSp: Int,
    userName: String,
    onTextSizeChange: (Int) -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val accentPurple = Color(0xFFB388FF)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background( Color(0xFF141318) )
            .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Размер текста сообщений",
                color = accentPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Slider(
                    value = textSizeSp.toFloat(),
                    onValueChange = { raw ->
                        onTextSizeChange(raw.roundToInt().coerceIn(1, 30))
                    },
                    valueRange = 1f..30f,
                    steps = 28,
                    colors = SliderDefaults.colors(
                        thumbColor = accentPurple,
                        activeTrackColor = accentPurple,
                        inactiveTrackColor = Color(0xFF3A3942),
                        activeTickColor = Color.Transparent,
                        inactiveTickColor = Color.Transparent
                    ),
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = textSizeSp.toString(),
                    color = accentPurple,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Живое превью сообщений чата под ползунком
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0B0B0F))
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Входящее сообщение слева с цитатой (имя юзера из регистрации)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 285.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 5.dp,
                                bottomEnd = 16.dp
                            )
                        )
                        .background(Color(0xFF1F222A))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    // Блок цитаты с именем зарегистрированного пользователя
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF182A36))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(32.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF39C5BB))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = userName,
                                color = Color(0xFF39C5BB),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Доброе утро! 👋",
                                color = Color.White,
                                fontSize = (textSizeSp.coerceIn(10, 22) - 2).coerceAtLeast(10).sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Знаешь, который час?",
                            color = Color.White,
                            fontSize = textSizeSp.sp,
                            lineHeight = (textSizeSp + 4).sp,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "20:07",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 2. Исходящее сообщение справа
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 285.dp)
                        .clip(
                            RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 5.dp
                            )
                        )
                        .background(Color(0xFF5E54A4))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "В Токио утро 😎",
                        color = Color.White,
                        fontSize = textSizeSp.sp,
                        lineHeight = (textSizeSp + 4).sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "20:22",
                            color = Color.White.copy(alpha = 0.78f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tick_double),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    value: String,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val scheme = LocalAnonGramScheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(
                1.dp,
                if (highlight) scheme.accent.copy(alpha = 0.45f) else CardBorder,
                RoundedCornerShape(18.dp)
            )
            .let { if (onClick != null) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = if (highlight) scheme.accent else TextPrimary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
