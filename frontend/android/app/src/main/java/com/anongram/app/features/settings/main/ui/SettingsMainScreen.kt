package com.anongram.app.features.settings.main.ui

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import com.anongram.app.core.i18n.LanguageSettingsStore
import com.anongram.app.core.ui.safeStatusBarPadding
import com.anongram.app.features.auth.data.crypto.KeystoreVault
import com.anongram.app.features.settings.main.ui.components.AccountsSectionCard
import com.anongram.app.features.settings.main.ui.components.ConnectedAccountItem
import com.anongram.app.features.settings.main.ui.components.SettingsItemRow
import com.anongram.app.features.settings.main.ui.components.SettingsOverflowMenu
import com.anongram.app.features.settings.main.ui.components.SettingsProfileHeader
import com.anongram.app.features.settings.navigation.SettingsRoute

/**
 * Главный экран раздела «Настройки» (Макеты 1 и 2).
 *
 * Включает:
 * - Топбар с круглой кнопкой QR слева и островком Поиск + Три точки справа;
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

    val displayName = identity?.nickname?.takeIf { it.isNotBlank() } ?: "Кайрат"
    val handleTag = "@${displayName.replace(" ", "").uppercase().take(8)}"
    val publicShort = identity?.publicId?.take(10)?.let { "0x$it" } ?: "+996 553797515"
    val subtitleInfo = "$publicShort • $handleTag"

    var overflowExpanded by remember { mutableStateOf(false) }

    val connectedAccounts = remember {
        listOf(
            ConnectedAccountItem(
                id = "acc_1",
                name = "Джексон",
                avatarColorStart = Color(0xFF4A148C),
                avatarColorEnd = Color(0xFF880E4F),
                unreadCount = 0
            ),
            ConnectedAccountItem(
                id = "acc_2",
                name = "siteweb",
                avatarColorStart = Color(0xFF7E57C2),
                avatarColorEnd = Color(0xFF5E35B1),
                unreadCount = 17
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .safeStatusBarPadding()
                .padding(horizontal = 14.dp)
                .padding(bottom = 115.dp)
        ) {
            // 1. Верхняя панель: QR слева, Поиск + 3 точки справа
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Круглая кнопка QR
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF17161C))
                        .clickable { onNavigate(SettingsRoute.MyQr) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = "Мой QR-код",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Островок Поиск + Меню 3 точки
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFF17161C))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Поиск настроек",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

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
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // 2. Шапка профиля (Аватар с камерой + Имя + Хэндл)
            SettingsProfileHeader(
                displayName = displayName,
                subtitleText = subtitleInfo,
                onAvatarClick = { onNavigate(SettingsRoute.Account) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Карточка «Аккаунты»
            AccountsSectionCard(
                accounts = connectedAccounts,
                onAccountClick = { onNavigate(SettingsRoute.Account) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 4. Основная карточка подразделов настроек (9 разделов)
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
                    icon = Icons.Default.Folder,
                    iconBackgroundColor = Color(0xFF26C6DA),
                    title = "Папки с чатами",
                    subtitle = "Сортировка чатов по папкам",
                    onClick = { onNavigate(SettingsRoute.Folders) }
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

            // 5. Дополнительный блок крипто-возможностей AnonGram
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF141318))
                    .padding(vertical = 6.dp)
            ) {
                SettingsItemRow(
                    icon = Icons.Default.Star,
                    iconBackgroundColor = Color(0xFF7E57C2),
                    title = "AnonGram Quantum Core",
                    subtitle = "Пост-квантовое шифрование ML-DSA-87",
                    onClick = { onNavigate(SettingsRoute.Privacy) }
                )
            }
        }

        // Выпадающее меню «Выход» (Макет 2)
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
                .padding(top = 56.dp, end = 16.dp)
        )
    }
}
