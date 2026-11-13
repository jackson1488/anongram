package com.anongram.app.features.settings.search.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.Color
import com.anongram.app.core.i18n.AppLanguage
import com.anongram.app.features.settings.navigation.SettingsRoute
import com.anongram.app.features.settings.search.model.SettingsSearchItem

/**
 * Умный поисковый движок по настройкам AnonGram.
 * Индексирует все разделы, подпункты, возможности и ключевые слова (с поддержкой RU/EN).
 * Позволяет легко расширять список при добавлении новых меню и подразделов.
 */
object SettingsSearchEngine {

    fun getAllItems(language: AppLanguage): List<SettingsSearchItem> {
        val isRu = language == AppLanguage.RU
        val categoryName = if (isRu) "Настройки" else "Settings"

        return listOf(
            SettingsSearchItem(
                id = "account",
                title = if (isRu) "Аккаунт" else "Account",
                subtitle = if (isRu) "Номер, имя пользователя, «О себе», аватар" else "Phone number, username, bio, avatar",
                targetRoute = SettingsRoute.Account,
                category = categoryName,
                icon = Icons.Default.AccountCircle,
                iconBackgroundColor = Color(0xFF29B6F6),
                keywords = listOf(
                    "аккаунт", "профиль", "имя", "ник", "никнейм", "хэндл", "о себе", "информация", "номер", "аватар", "фото",
                    "account", "profile", "name", "username", "nickname", "handle", "bio", "info", "phone", "avatar", "photo"
                )
            ),
            SettingsSearchItem(
                id = "chat_settings",
                title = if (isRu) "Настройки чатов" else "Chat Settings",
                subtitle = if (isRu) "Цветовые темы, размер текста, ночной режим" else "Color themes, text size, dark mode",
                targetRoute = SettingsRoute.ChatSettings,
                category = categoryName,
                icon = Icons.Default.ChatBubble,
                iconBackgroundColor = Color(0xFFFFA726),
                keywords = listOf(
                    "чат", "чаты", "тема", "темы", "оформление", "шрифт", "размер текста", "ночной режим", "обои", "фон",
                    "chat", "chats", "theme", "themes", "appearance", "font", "text size", "dark mode", "wallpaper", "colors"
                )
            ),
            SettingsSearchItem(
                id = "privacy",
                title = if (isRu) "Конфиденциальность" else "Privacy",
                subtitle = if (isRu) "Время захода, открытый ключ, доступ к профилю" else "Last seen, public key, profile privacy",
                targetRoute = SettingsRoute.Privacy,
                category = categoryName,
                icon = Icons.Default.Key,
                iconBackgroundColor = Color(0xFF66BB6A),
                keywords = listOf(
                    "конфиденциальность", "приватность", "ключи", "публичный ключ", "мастер-ключ", "время захода", "онлайн", "секретность",
                    "privacy", "keys", "public key", "master key", "last seen", "online", "secrecy"
                )
            ),
            SettingsSearchItem(
                id = "security",
                title = if (isRu) "Безопасность" else "Security",
                subtitle = if (isRu) "Код-пароль, биометрия, защита экрана, 2FA, квантовый щит" else "Passcode, biometrics, screen protect, 2FA, quantum shield",
                targetRoute = SettingsRoute.Security,
                category = categoryName,
                icon = Icons.Default.Security,
                iconBackgroundColor = Color(0xFF43A047),
                keywords = listOf(
                    "безопасность", "код-пароль", "пароль", "пин", "pin", "отпечаток", "биометрия", "face id", "сканер",
                    "автоблокировка", "снимок экрана", "скриншот", "двухэтапная", "2fa", "облачный пароль",
                    "квантовая защита", "квантовый щит", "ml-dsa", "fips 204", "pfs", "forward secrecy", "анонимность",
                    "security", "passcode", "password", "pin", "fingerprint", "biometrics", "auto-lock", "screenshot",
                    "two-step", "2fa", "cloud password", "quantum", "shield", "anonymity"
                )
            ),
            SettingsSearchItem(
                id = "notifications",
                title = if (isRu) "Уведомления" else "Notifications",
                subtitle = if (isRu) "Звуки сообщений, звонки, счётчик непрочитанных" else "Message sounds, calls, unread badge",
                targetRoute = SettingsRoute.Notifications,
                category = categoryName,
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                iconBackgroundColor = Color(0xFFEF5350),
                keywords = listOf(
                    "уведомления", "звук", "звуки", "звонки", "вибрация", "счётчик", "оповещения", "тихий режим", "пуши",
                    "notifications", "sound", "sounds", "calls", "vibration", "badge", "mute", "alerts", "push"
                )
            ),
            SettingsSearchItem(
                id = "data_storage",
                title = if (isRu) "Данные и память" else "Data and Storage",
                subtitle = if (isRu) "Автозагрузка медиафайлов, использование кэша" else "Media auto-download, cache storage",
                targetRoute = SettingsRoute.DataStorage,
                category = categoryName,
                icon = Icons.Default.PieChart,
                iconBackgroundColor = Color(0xFF5C6BC0),
                keywords = listOf(
                    "данные", "память", "кэш", "хранилище", "автозагрузка", "медиа", "фото", "видео", "файлы", "очистить кэш",
                    "data", "storage", "cache", "auto-download", "media", "photos", "videos", "files", "clear cache"
                )
            ),
            SettingsSearchItem(
                id = "animations",
                title = if (isRu) "Анимации" else "Animations",
                subtitle = if (isRu) "Плавные переходы, размытие Glass, эффекты, скорость" else "Transitions, glass blur, effects, speed",
                targetRoute = SettingsRoute.Animations,
                category = categoryName,
                icon = Icons.Default.AutoAwesome,
                iconBackgroundColor = Color(0xFFEC407A),
                keywords = listOf(
                    "анимации", "анимация", "плавность", "переходы", "размытие", "блюр", "glass", "стекло",
                    "стикеры", "эмодзи", "реакции", "живые фоны", "скорость анимаций", "уменьшение движения", "reduce motion",
                    "animations", "transitions", "blur", "glass", "stickers", "emoji", "reactions", "speed", "motion"
                )
            ),
            SettingsSearchItem(
                id = "devices",
                title = if (isRu) "Устройства" else "Devices",
                subtitle = if (isRu) "Активные сеансы, привязка через QR, завершение сессий" else "Active sessions, QR linking, terminate sessions",
                targetRoute = SettingsRoute.Devices,
                category = categoryName,
                icon = Icons.Default.Devices,
                iconBackgroundColor = Color(0xFF26A69A),
                keywords = listOf(
                    "устройства", "сеансы", "сессии", "подключения", "компьютер", "десктоп", "web", "завершить сес", "qr-вход",
                    "devices", "sessions", "connections", "desktop", "web", "linked devices", "terminate"
                )
            ),
            SettingsSearchItem(
                id = "modules",
                title = if (isRu) "Модули" else "Modules",
                subtitle = if (isRu) "Расшифровка ГС (Whisper, Vosk), локальный перевод, языки" else "Voice transcription (Whisper, Vosk), local translation, languages",
                targetRoute = SettingsRoute.Modules,
                category = categoryName,
                icon = Icons.Default.Extension,
                iconBackgroundColor = Color(0xFF00ACC1),
                keywords = listOf(
                    "модули", "гс", "голосовые", "расшифровка", "whisper", "vosk", "google", "перевод", "языки", "ассеты", "модели",
                    "modules", "voice", "transcription", "stt", "whisper", "vosk", "google", "translate", "languages", "models", "assets"
                )
            ),
            SettingsSearchItem(
                id = "modules_translation",
                title = if (isRu) "Локальный перевод сообщений" else "Local Message Translation",
                subtitle = if (isRu) "Офлайн-перевод, автоперевод входящих, языковые пакеты" else "Offline translation, auto-translate incoming, language packs",
                targetRoute = SettingsRoute.ModulesTranslation,
                category = categoryName,
                icon = Icons.Default.Extension,
                iconBackgroundColor = Color(0xFF00ACC1),
                keywords = listOf(
                    "перевод", "перевести", "автоперевод", "языки", "mlkit", "офлайн", "локальный",
                    "translate", "translation", "auto-translate", "languages", "mlkit", "offline", "local"
                )
            ),
            SettingsSearchItem(
                id = "battery",
                title = if (isRu) "Энергосбережение" else "Power Saving",
                subtitle = if (isRu) "Экономия энергии при низком заряде аккумулятора" else "Save battery power at low charge level",
                targetRoute = SettingsRoute.Battery,
                category = categoryName,
                icon = Icons.Default.BatteryChargingFull,
                iconBackgroundColor = Color(0xFFFB8C00),
                keywords = listOf(
                    "энергосбережение", "батарея", "аккумулятор", "питание", "экономия", "заряд", "режим экономии",
                    "battery", "power saving", "power", "energy", "battery life"
                )
            ),
            SettingsSearchItem(
                id = "language",
                title = if (isRu) "Язык" else "Language",
                subtitle = if (isRu) "Русский, English, локализация интерфейса" else "Russian, English, interface localization",
                targetRoute = SettingsRoute.Language,
                category = categoryName,
                icon = Icons.Default.Language,
                iconBackgroundColor = Color(0xFFAB47BC),
                keywords = listOf(
                    "язык", "интерфейс", "русский", "английский", "локализация", "перевод", "смена языка",
                    "language", "interface", "russian", "english", "localization", "translation"
                )
            ),
            SettingsSearchItem(
                id = "my_qr",
                title = if (isRu) "Мой QR-код" else "My QR Code",
                subtitle = if (isRu) "Поделиться профилем, выбор палитры оформления" else "Share profile, palette style selection",
                targetRoute = SettingsRoute.MyQr,
                category = categoryName,
                icon = Icons.Default.QrCode2,
                iconBackgroundColor = Color(0xFF7E57C2),
                keywords = listOf(
                    "qr", "кюар", "qr-код", "поделиться qr", "палитра", "стиль", "скачать qr", "визитка",
                    "qr", "qr code", "share qr", "palette", "download qr", "business card"
                )
            ),
            SettingsSearchItem(
                id = "qr_scanner",
                title = if (isRu) "Сканер QR-кода" else "QR Scanner",
                subtitle = if (isRu) "Сканировать код устройства, контакта или из Галереи" else "Scan device QR, contact, or from Gallery",
                targetRoute = SettingsRoute.QrScanner,
                category = categoryName,
                icon = Icons.Default.CameraAlt,
                iconBackgroundColor = Color(0xFF00B0FF),
                keywords = listOf(
                    "сканер", "сканировать", "камера", "сканирование", "галерея", "фонарик",
                    "scanner", "scan", "camera", "scanning", "gallery", "flashlight"
                )
            ),
            SettingsSearchItem(
                id = "quantum_shield",
                title = if (isRu) "Квантовая криптография" else "Quantum Cryptography",
                subtitle = if (isRu) "AnonGram Quantum Shield ML-DSA-87 (NIST Level 5)" else "AnonGram Quantum Shield ML-DSA-87 (NIST Level 5)",
                targetRoute = SettingsRoute.Security,
                category = categoryName,
                icon = Icons.Default.Shield,
                iconBackgroundColor = Color(0xFF2E7D32),
                keywords = listOf(
                    "квантовая", "криптография", "шифрование", "ml-dsa-87", "fips 204", "pfs", "защита", "кванты",
                    "quantum", "cryptography", "encryption", "ml-dsa-87", "fips 204", "pfs", "shield"
                )
            )
        )
    }

    /**
     * Интеллектуальный поиск по запросу.
     */
    fun search(query: String, language: AppLanguage): List<SettingsSearchItem> {
        val trimmed = query.trim().lowercase()
        if (trimmed.isEmpty()) return emptyList()

        val items = getAllItems(language)
        val queryTokens = trimmed.split(" ").filter { it.isNotBlank() }

        val scoredItems = items.mapNotNull { item ->
            val titleLower = item.title.lowercase()
            val subtitleLower = item.subtitle.lowercase()

            var score = 0

            // 1. Точное совпадение
            if (titleLower == trimmed) {
                score += 150
            } else if (titleLower.startsWith(trimmed)) {
                score += 80
            } else if (titleLower.contains(trimmed)) {
                score += 40
            }

            // 2. Совпадение в описании
            if (subtitleLower.contains(trimmed)) {
                score += 20
            }

            // 3. Совпадение по ключевым словам и тегам
            val matchingKeywords = item.keywords.count { kw ->
                kw.lowercase().contains(trimmed) || trimmed.contains(kw.lowercase())
            }
            score += matchingKeywords * 15

            // 4. Потокеновое совпадение (все слова запроса должны присутствовать)
            val allTokensMatch = queryTokens.all { token ->
                titleLower.contains(token) ||
                    subtitleLower.contains(token) ||
                    item.keywords.any { it.lowercase().contains(token) }
            }

            if (allTokensMatch && score == 0) {
                score += 25
            }

            if (score > 0) item to score else null
        }

        return scoredItems
            .sortedByDescending { it.second }
            .map { it.first }
    }
}
