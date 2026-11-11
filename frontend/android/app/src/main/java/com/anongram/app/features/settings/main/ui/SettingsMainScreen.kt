package com.anongram.app.features.settings.main.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.theme.LocalAnonGramScheme
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.auth.data.crypto.KeystoreVault
import com.anongram.app.features.settings.main.ui.components.AccountsSectionCard
import com.anongram.app.features.settings.main.ui.components.ConnectedAccountItem
import com.anongram.app.features.settings.main.ui.components.SettingsItemRow
import com.anongram.app.features.settings.main.ui.components.SettingsOverflowMenu
import com.anongram.app.features.settings.main.ui.components.SettingsProfileHeader
import com.anongram.app.features.settings.navigation.SettingsRoute
import com.anongram.app.features.settings.qr.i18n.QrI18n

/**
 * Главный экран раздела «Настройки» (Макеты 1 и 2).
 *
 * Включает:
 * - Закреплённую сверху полупрозрачную шапку с эффектом матового стекла (блюр-вуаль),
 *   круглой кнопкой QR слева и островком «Поиск + 3 точки» справа, которая остаётся на месте при прокрутке;
 * - Шапку профиля с крупным аватаром, бейджем камеры, именем и хэндлом;
 * - Карточку мульти-аккаунтов («Аккаунты»);
 * - Сгруппированный список из 9 подразделов настроек с цветными иконками;
 * - Всплывающее меню «Выход» по нажатию на три точки.
 */
@Composable
fun SettingsMainScreen(
    onNavigate: (SettingsRoute) -> Unit,
    onLogout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vault = remember { KeystoreVault(context) }
    val identity = remember { vault.loadIdentity() }
    val currentLanguage by LanguageSettingsStore.language.collectAsState()

    val displayName = identity?.nickname?.takeIf { it.isNotBlank() } ?: "Пользователь"
    val handleTag = "@${displayName.replace(" ", "").uppercase().take(8)}"
    val publicShort = identity?.publicId?.take(8)?.let { "0x$it" } ?: "Anon"
    val subtitleInfo = "$publicShort • $handleTag"

    var overflowExpanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    // Плавное проявление матовой полупрозрачной шапки при прокрутке вниз
    val headerScrollFraction by remember {
        derivedStateOf { (scrollState.value / 180f).coerceIn(0f, 1f) }
    }
    val animatedHeaderAlpha by animateFloatAsState(
        targetValue = headerScrollFraction,
        label = "settingsStickyHeaderAlpha"
    )
    val showCollapsedTitle by remember {
        derivedStateOf { scrollState.value > 60 }
    }

    val scheme = LocalAnonGramScheme.current

    // Только текущий активный аккаунт пользователя (без фейковых заглушек)
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
            .background(Color(0xFF050508))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (overflowExpanded) overflowExpanded = false
            }
    ) {
        // 1. Прокручиваемый контент настроек (проходит под полупрозрачной закреплённой шапкой)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .safeStatusBarPadding()
                .padding(top = 46.dp)
                .padding(horizontal = 14.dp)
                .padding(bottom = 115.dp)
        ) {
            // Шапка профиля (Аватар с камерой + Имя + Хэндл)
            SettingsProfileHeader(
                displayName = displayName,
                subtitleText = subtitleInfo,
                onAvatarClick = { onNavigate(SettingsRoute.Account) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Карточка «Аккаунты»
            AccountsSectionCard(
                accounts = connectedAccounts,
                onAccountClick = { onNavigate(SettingsRoute.Account) }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Основная карточка подразделов настроек (9 разделов)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141318))
                    .padding(vertical = 6.dp)
            ) {
                SettingsItemRow(
                    icon = Icons.Default.AccountCircle,
                    iconBackgroundColor = Color(0xFF29B6F6),
                    title = "Аккаунт",
                    subtitle = "Номер, имя пользователя, «О себе»",
                    onClick = { onNavigate(SettingsRoute.Account) }
                )

                SettingsItemRow(
                    icon = Icons.Default.ChatBubble,
                    iconBackgroundColor = Color(0xFFFFA726),
                    title = "Настройки чатов",
                    subtitle = "Обои, ночной режим, анимации",
                    onClick = { onNavigate(SettingsRoute.ChatSettings) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Key,
                    iconBackgroundColor = Color(0xFF66BB6A),
                    title = "Конфиденциальность",
                    subtitle = "Время захода, устройства, ключи доступа",
                    onClick = { onNavigate(SettingsRoute.Privacy) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Security,
                    iconBackgroundColor = Color(0xFF43A047),
                    title = "Безопасность",
                    subtitle = "Код-пароль, биометрия, квантовая защита",
                    onClick = { onNavigate(SettingsRoute.Security) }
                )

                SettingsItemRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconBackgroundColor = Color(0xFFEF5350),
                    title = "Уведомления",
                    subtitle = "Звуки, звонки, счётчик сообщений",
                    onClick = { onNavigate(SettingsRoute.Notifications) }
                )

                SettingsItemRow(
                    icon = Icons.Default.PieChart,
                    iconBackgroundColor = Color(0xFF5C6BC0),
                    title = "Данные и память",
                    subtitle = "Настройки загрузки медиафайлов",
                    onClick = { onNavigate(SettingsRoute.DataStorage) }
                )

                SettingsItemRow(
                    icon = Icons.Default.AutoAwesome,
                    iconBackgroundColor = Color(0xFFEC407A),
                    title = "Анимации",
                    subtitle = "Плавные переходы, размытие Glass, эффекты",
                    onClick = { onNavigate(SettingsRoute.Animations) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Devices,
                    iconBackgroundColor = Color(0xFF26A69A),
                    title = "Устройства",
                    subtitle = "Управление активными сеансами",
                    onClick = { onNavigate(SettingsRoute.Devices) }
                )

                SettingsItemRow(
                    icon = Icons.Default.BatteryChargingFull,
                    iconBackgroundColor = Color(0xFFFB8C00),
                    title = "Энергосбережение",
                    subtitle = "Экономия энергии при низком заряде",
                    onClick = { onNavigate(SettingsRoute.Battery) }
                )

                SettingsItemRow(
                    icon = Icons.Default.Language,
                    iconBackgroundColor = Color(0xFFAB47BC),
                    title = "Язык",
                    subtitle = currentLanguage.displayName,
                    onClick = { onNavigate(SettingsRoute.Language) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Дополнительный блок крипто-возможностей AnonGram
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141318))
                    .padding(vertical = 6.dp)
            ) {
                SettingsItemRow(
                    icon = Icons.Default.Star,
                    iconBackgroundColor = scheme.accent,
                    title = "AnonGram Quantum Core",
                    subtitle = "Пост-квантовое шифрование ML-DSA-87",
                    onClick = { onNavigate(SettingsRoute.Privacy) }
                )
            }
        }

        // 2. Закреплённая сверху полупрозрачная шапка с эффектом матового стекла (блюр-вуаль):
        // Остаётся на месте при прокрутке вниз, кнопки QR, Поиск и 3 точки всегда доступны.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0E0D14).copy(alpha = 0.72f + 0.22f * animatedHeaderAlpha),
                            Color(0xFF151320).copy(alpha = 0.55f + 0.34f * animatedHeaderAlpha),
                            Color(0xFF050508).copy(alpha = 0.15f * (1f - animatedHeaderAlpha) + 0.82f * animatedHeaderAlpha)
                        )
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeStatusBarPadding()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Круглая кнопка QR (с матовой стеклянной подложкой и тонкой окантовкой)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1C29).copy(alpha = 0.82f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.10f), CircleShape)
                        .clickable { onNavigate(SettingsRoute.MyQr) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = QrI18n.forLanguage(currentLanguage).myQrCode,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Заголовок в центре шапки: по дефолту надпись «Настройки», всегда остаётся сверху
                Text(
                    text = "Настройки",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                // Островок Поиск + Меню 3 точки (с матовой стеклянной подложкой)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF1E1C29).copy(alpha = 0.82f))
                        .border(0.8.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(22.dp))
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
                            contentDescription = "Поиск настроек",
                            tint = Color.White,
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
                            contentDescription = "Меню",
                            tint = Color.White,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (animatedHeaderAlpha > 0.08f) {
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.08f * animatedHeaderAlpha),
                    thickness = 0.5.dp
                )
            }
        }

        // 3. Выпадающее меню «Выход» (Макет 2)
        SettingsOverflowMenu(
            expanded = overflowExpanded,
            onLogoutClick = {
                overflowExpanded = false
                vault.clear()
                onLogout()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .safeStatusBarPadding()
                .padding(top = 46.dp, end = 16.dp)
        )
    }
}
