package com.anongram.web.features.settings.main.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.web.core.i18n.AppLanguage
import com.anongram.web.core.i18n.LanguageSettingsStore
import com.anongram.web.core.theme.LocalAnonGramScheme
import com.anongram.web.core.ui.safeStatusBarPadding
import com.anongram.web.features.auth.domain.model.UserIdentity
import com.anongram.web.features.settings.main.ui.components.AccountsSectionCard
import com.anongram.web.features.settings.main.ui.components.ConnectedAccountItem
import com.anongram.web.features.settings.main.ui.components.SettingsItemRow
import com.anongram.web.features.settings.main.ui.components.SettingsOverflowMenu
import com.anongram.web.features.settings.main.ui.components.SettingsProfileHeader
import com.anongram.web.features.settings.navigation.SettingsRoute

@Composable
fun SettingsMainScreen(
    identity: UserIdentity?,
    onNavigate: (SettingsRoute) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentLanguage by LanguageSettingsStore.language.collectAsState()
    val isRu = currentLanguage == AppLanguage.RU

    val displayName = identity?.nickname?.takeIf { it.isNotBlank() }
        ?: if (isRu) "Пользователь" else "User"
    val handleTag = "@${displayName.replace(" ", "").uppercase().take(8)}"
    val publicShort = identity?.publicId?.take(8)?.let { "0x$it" } ?: "Anon"
    val subtitleInfo = "$publicShort • $handleTag"

    var overflowExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    val headerScrollFraction by remember {
        derivedStateOf { (scrollState.value / 180f).coerceIn(0f, 1f) }
    }
    val animatedHeaderAlpha by animateFloatAsState(
        targetValue = headerScrollFraction,
        label = "settingsStickyHeaderAlpha"
    )

    val scheme = LocalAnonGramScheme.current

    val connectedAccounts = remember(displayName, identity?.publicId, scheme.accent) {
        listOf(
            ConnectedAccountItem(
                id = identity?.publicId ?: "current",
                name = displayName,
                avatarColorStart = scheme.accent,
                avatarColorEnd = scheme.accentDim,
                unreadCount = 0,
                isCurrent = true
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (overflowExpanded) overflowExpanded = false
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .safeStatusBarPadding()
                .padding(top = 46.dp)
                .padding(horizontal = 14.dp)
                .padding(bottom = 115.dp)
        ) {
            SettingsProfileHeader(
                displayName = displayName,
                subtitleText = subtitleInfo,
                onAvatarClick = { onNavigate(SettingsRoute.Account) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            AccountsSectionCard(
                accounts = connectedAccounts,
                onAccountClick = { onNavigate(SettingsRoute.Account) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 6.dp)
            ) {
                SettingsItemRow(
                    icon = Icons.Default.AccountCircle,
                    iconBackgroundColor = Color(0xFF29B6F6),
                    title = if (isRu) "Аккаунт" else "Account",
                    subtitle = if (isRu) "ID, имя пользователя, «О себе»" else "ID, username, Bio",
                    onClick = { onNavigate(SettingsRoute.Account) }
                )

                SettingsItemRow(
                    icon = Icons.Default.ChatBubble,
                    iconBackgroundColor = Color(0xFFFFA726),
                    title = if (isRu) "Настройки чатов" else "Chat Settings",
                    subtitle = if (isRu) "Темы, цветовой круг, размер шрифта" else "Themes, color wheel, font size",
                    onClick = { onNavigate(SettingsRoute.ChatSettings) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Key,
                    iconBackgroundColor = Color(0xFF66BB6A),
                    title = if (isRu) "Конфиденциальность" else "Privacy",
                    subtitle = if (isRu) "Время захода, устройства, ключи доступа" else "Last seen, devices, passkeys",
                    onClick = { onNavigate(SettingsRoute.Privacy) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Security,
                    iconBackgroundColor = Color(0xFF43A047),
                    title = if (isRu) "Безопасность" else "Security",
                    subtitle = if (isRu) "Код-пароль, автоблокировка, квантовая защита" else "Passcode, auto-lock, quantum guard",
                    onClick = { onNavigate(SettingsRoute.Security) }
                )

                SettingsItemRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconBackgroundColor = Color(0xFFEF5350),
                    title = if (isRu) "Уведомления" else "Notifications",
                    subtitle = if (isRu) "Звуки, звонки, счётчик сообщений" else "Sounds, calls, badge counter",
                    onClick = { onNavigate(SettingsRoute.Notifications) }
                )

                SettingsItemRow(
                    icon = Icons.Default.PieChart,
                    iconBackgroundColor = Color(0xFF5C6BC0),
                    title = if (isRu) "Данные и память" else "Data & Storage",
                    subtitle = if (isRu) "Настройки загрузки медиафайлов" else "Media auto-download & cache",
                    onClick = { onNavigate(SettingsRoute.DataStorage) }
                )

                SettingsItemRow(
                    icon = Icons.Default.AutoAwesome,
                    iconBackgroundColor = Color(0xFFEC407A),
                    title = if (isRu) "Анимации" else "Animations",
                    subtitle = if (isRu) "Плавные переходы, размытие Glass, эффекты" else "Smooth transitions, Glass blur, effects",
                    onClick = { onNavigate(SettingsRoute.Animations) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Devices,
                    iconBackgroundColor = Color(0xFF26A69A),
                    title = if (isRu) "Устройства" else "Devices",
                    subtitle = if (isRu) "Управление активными сеансами" else "Manage active sessions",
                    onClick = { onNavigate(SettingsRoute.Devices) }
                )

                SettingsItemRow(
                    icon = Icons.Default.BatteryChargingFull,
                    iconBackgroundColor = Color(0xFFFB8C00),
                    title = if (isRu) "Энергосбережение" else "Power Saving",
                    subtitle = if (isRu) "Экономия энергии при низком заряде" else "Save power on low battery",
                    onClick = { onNavigate(SettingsRoute.Battery) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Language,
                    iconBackgroundColor = Color(0xFFAB47BC),
                    title = if (isRu) "Язык" else "Language",
                    subtitle = currentLanguage.displayName,
                    onClick = { onNavigate(SettingsRoute.Language) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Extension,
                    iconBackgroundColor = Color(0xFF00ACC1),
                    title = if (isRu) "Модули" else "Modules",
                    subtitle = if (isRu) "Голосовые сообщения, перевод чатов" else "Voice notes, chat translation",
                    onClick = { onNavigate(SettingsRoute.Modules) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(scheme.card)
                    .border(1.dp, scheme.cardBorder, RoundedCornerShape(20.dp))
                    .padding(vertical = 6.dp)
            ) {
                SettingsItemRow(
                    icon = Icons.Default.Star,
                    iconBackgroundColor = scheme.accent,
                    iconTint = scheme.onAccent,
                    title = "AnonGram Quantum Core",
                    subtitle = if (isRu) "Пост-квантовое шифрование ML-DSA-87" else "Post-quantum encryption ML-DSA-87",
                    onClick = { onNavigate(SettingsRoute.Privacy) }
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scheme.surface.copy(alpha = 0.84f + 0.14f * animatedHeaderAlpha),
                            scheme.background.copy(alpha = 0.65f + 0.30f * animatedHeaderAlpha),
                            scheme.background.copy(alpha = 0.15f * (1f - animatedHeaderAlpha) + 0.88f * animatedHeaderAlpha)
                        )
                    )
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeStatusBarPadding()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .height(44.dp)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(scheme.islandBg.copy(alpha = 0.90f))
                        .border(0.8.dp, scheme.islandBorder, CircleShape)
                        .clickable { onNavigate(SettingsRoute.MyQr) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = if (isRu) "Мой QR-код" else "My QR Code",
                        tint = scheme.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = if (isRu) "Настройки" else "Settings",
                    color = scheme.textPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center)
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .clip(RoundedCornerShape(22.dp))
                        .background(scheme.islandBg.copy(alpha = 0.90f))
                        .border(0.8.dp, scheme.islandBorder, RoundedCornerShape(22.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { onNavigate(SettingsRoute.Search) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = if (isRu) "Поиск настроек" else "Search settings",
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(2.dp))

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { overflowExpanded = !overflowExpanded },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = if (isRu) "Меню" else "Menu",
                            tint = scheme.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }

        SettingsOverflowMenu(
            expanded = overflowExpanded,
            onLogoutClick = {
                overflowExpanded = false
                onLogout()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeStatusBarPadding()
                .padding(top = 46.dp, end = 16.dp)
        )
    }
}
