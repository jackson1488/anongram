package com.anongram.web.features.settings.search.ui

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.AnonGramScreenSurface
import com.anongram.web.core.ui.safeNavigationBarPadding
import com.anongram.web.core.ui.safeStatusBarPadding
import com.anongram.web.features.settings.navigation.SettingsRoute

private data class SettingsSearchEntry(
    val titleRu: String,
    val titleEn: String,
    val subtitleRu: String,
    val subtitleEn: String,
    val keywords: String,
    val route: SettingsRoute
)

private val allSearchEntries = listOf(
    SettingsSearchEntry("Аккаунт", "Account", "Имя, @handle, О себе, Public ID", "Name, @handle, Bio, Public ID", "profile avatar bio name handle id", SettingsRoute.Account),
    SettingsSearchEntry("Настройки чатов", "Chat Settings", "Темы, цветовой круг, размер текста", "Themes, color wheel, text size", "theme color wheel palette font size bubble", SettingsRoute.ChatSettings),
    SettingsSearchEntry("Конфиденциальность", "Privacy", "Статус, Sealed Sender, Relay IP", "Last seen, Sealed Sender, IP Relay", "privacy stealth ghost ip relay anonymous", SettingsRoute.Privacy),
    SettingsSearchEntry("Безопасность", "Security", "Код-пароль PIN, автоблокировка, Seed", "Passcode PIN, auto-lock, Seed", "pin password passcode seed ml-kem", SettingsRoute.Security),
    SettingsSearchEntry("Уведомления", "Notifications", "Звуки, предпросмотр, счётчик", "Sounds, previews, badge counter", "sound badge notification mute", SettingsRoute.Notifications),
    SettingsSearchEntry("Данные и память", "Data & Storage", "Кэш, автозагрузка, Onion", "Cache, auto-download, Onion", "cache clear storage proxy tor onion", SettingsRoute.DataStorage),
    SettingsSearchEntry("Анимации", "Animations", "Переходы, размытие Glass, скорость", "Transitions, Glass blur, speed", "animation blur motion speed", SettingsRoute.Animations),
    SettingsSearchEntry("Анимация сообщений", "Message Animations", "Потоковая печать, удаление", "Token stream, deletion", "token stream spring pop particles", SettingsRoute.AnimationsMessages),
    SettingsSearchEntry("Устройства", "Devices", "Активные сеансы и ключи", "Active sessions & devices", "devices sessions terminate web", SettingsRoute.Devices),
    SettingsSearchEntry("Энергосбережение", "Power Saving", "Порог заряда и экономия GPU", "Battery threshold & GPU saving", "battery power saving autoplay", SettingsRoute.Battery),
    SettingsSearchEntry("Язык", "Language", "Русский / English, переводчик", "Russian / English, translator", "language ru en locale translate", SettingsRoute.Language),
    SettingsSearchEntry("Модули", "Modules", "Голосовые сообщения и перевод", "Voice notes & chat translation", "modules voice transcription translate", SettingsRoute.Modules),
    SettingsSearchEntry("Мой QR-код", "My QR Code", "Поделиться контактом через QR", "Share contact via QR", "qr code share link", SettingsRoute.MyQr)
)

@Composable
fun SettingsSearchScreen(
    onNavigate: (SettingsRoute) -> Unit,
    onBack: () -> Unit
) {
    val scheme = LocalAnonGramScheme.current
    val lang by LanguageSettingsStore.language.collectAsState()
    val isRu = lang == AppLanguage.RU

    var query by remember { mutableStateOf("") }

    val results = remember(query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) allSearchEntries
        else allSearchEntries.filter {
            it.titleRu.lowercase().contains(q) ||
                it.titleEn.lowercase().contains(q) ||
                it.subtitleRu.lowercase().contains(q) ||
                it.subtitleEn.lowercase().contains(q) ||
                it.keywords.contains(q)
        }
    }

    AnonGramScreenSurface {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeStatusBarPadding()
                .safeNavigationBarPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 760.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg)
                        .border(0.8.dp, scheme.islandBorder, CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.card)
                        .border(1.dp, scheme.cardBorder, RoundedCornerShape(22.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = scheme.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = if (isRu) "Поиск настроек..." else "Search settings...",
                                color = scheme.textMuted,
                                fontSize = 14.5.sp
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(color = scheme.textPrimary, fontSize = 14.5.sp),
                            cursorBrush = SolidColor(scheme.accent),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (query.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = scheme.textMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { query = "" }
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (results.isEmpty()) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Text(
                        text = if (isRu) "Ничего не найдено" else "Nothing found",
                        color = scheme.textSecondary,
                        fontSize = 15.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    results.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(scheme.card)
                                .border(1.dp, scheme.cardBorder, RoundedCornerShape(16.dp))
                                .clickable { onNavigate(item.route) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isRu) item.titleRu else item.titleEn,
                                    color = scheme.textPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = if (isRu) item.subtitleRu else item.subtitleEn,
                                    color = scheme.textSecondary,
                                    fontSize = 12.5.sp
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = scheme.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
