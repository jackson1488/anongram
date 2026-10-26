# План: Главное меню «Чаты» в стиле Telegram — адаптация под AnonGram

Статус: реализуется. Документ — источник истины по механикам и файловой карте.

## 1. Цель

Главный экран чатов повторяет UX-паттерны Telegram (топбар с названием, поиск,
режим выбора, архив с жестом, FAB), но адаптирован под AnonGram: AMOLED Black,
Cyber-акценты, крипто-семантика (отпечатки, уровни шифрования, OTP-пады).
Каналов и публичных супергрупп нет. Навигация островом не меняется:
Чаты → Сервисы → [QR-щит в центре] → Настройки → Профиль.

## 2. UX-спецификация

### 2.1 Топбар главного меню
- Название «AnonGram» (как «Telegram» в TG) + бейдж «● Onion v3».
- Справа — кнопка поиска (иконка лупы). Инлайн-поле поиска из списка удалено.

### 2.2 Экран поиска (кнопка из топбара)
- Отдельный полноэкранный экран: назад + автофокус-поле + «Отмена».
- Секции результатов: «ЧАТЫ» (по названию / отпечатку / превью) и «ЛЮДИ»
  (контакты по имени / отпечатку).
- Глобальный поиск по сети — НЕ входит в этот слайс (решаем после сервера).

### 2.3 Режим выбора (long-press по чату)
Верхняя панель управления: [закрыть] «N выбрано» | закрепить · без звука ·
корзина | архив · прочитано · очистить историю. Multi-select поддержан.

- **Закрепить/Открепить**: toggle по состоянию выбранных.
- **Без звука** (уникальная логика):
  - чат НЕ замьючен → нажатие открывает bottom-лист: 1 час / 8 часов /
    1 сутки / 2 дня / Навсегда;
  - чат УЖЕ замьючен → нажатие мгновенно включает звук, без окна.
- **Корзина** → диалог подтверждения:
  - групповой чат: «Покинуть чат?»;
  - чат 1-на-1: «Удалить чат?» + галочка «Также удалить у собеседника»;
  - OTP: «Сжечь пад (необратимо)»;
  - мульти-выбор: «Удалить чаты?» без галочки.
- **Архив**: убрать в архив / вернуть (в экране архива иконка меняется на «из архива»).
- **Прочитано**: сброс непрочитанных.
- **Очистить историю** → диалог: «Очистить историю?» + галочка
  «Также очистить у собеседника» (доступна только для одиночного чата 1-на-1;
  в сетевой версии — удаление сообщений у всех, только для 1-на-1).

### 2.4 Архив чатов
- Закреплённая пилюля над списком чатов (иконка коробки, счётчик).
- Архив ПУСТ → пилюли нет вообще (первый заход — раздела не существует).
- Жесты: свайп ВВЕРХ по списку (у верха) — архив уезжает вверх и прячется;
  тянет ВНИЗ от самого верха — архив выезжает обратно.
- Архивация чата из режима выбора автоматически раскрывает пилюлю.
- Экран архива: тот же список карточек + режим выбора с «убрать из архива».

### 2.5 FAB «Новое сообщение»
- Плавающий карандаш справа внизу (как в TG), скрывается в режиме выбора.
- Открывает экран «Контакты» — вместо TG-«нового сообщения»: у нас нет каналов,
  вход в контакты отсюда и из QR-хаба. В контактах: «Добавить контакт» →
  QR-хаб или поиск. Клик по контакту → найти/создать чат 1-на-1 и открыть.

### 2.6 QR-хаб (центральная кнопка-щит)
- Было: только QR ID. Стало: QR ID + кнопка «Мои контакты».
- Сканер QR — следующий слайс (features/qr: камера + AGQR-парсер).

## 3. Система тем (новое требование)

База AMOLED Black (#05070A / карточки / текст) неизменна по бренд-буку;
темы меняют акцентную пару. Архитектура:

- `core/theme/AnonGramScheme.kt` — иммутабельная схема (id, name, accent,
  accentDim) + пресеты: Cyber Stealth (00F2FE/06B6D4, дефолт), Emerald Night
  (10B981/34D399), Violet Ghost (8B5CF6/A78BFA), Solar Amber (F59E0B/FBBF24).
- `core/theme/ThemeRepository.kt` + `SharedPrefsThemeRepository.kt` — персист.
- `core/theme/ThemeController.kt` — StateFlow<AnonGramScheme>, selectTheme(id).
- `Theme.kt` — AnonGramTheme(scheme) провайдит CompositionLocal
  `LocalAnonGramScheme`; MaterialTheme.colorScheme собирается из схемы.
- Настройки → строка «Тема» → пикер-диалог со свотчами.
- Компоненты главного меню читают акцент из LocalAnonGramScheme; остальные
  экраны мигрируют на схему поэтапно (см. роадмап).

## 4. Адаптивность (новое требование)

- Основа: `material3-window-size-class`; MainActivity считает WindowSizeClass
  и передаёт widthSizeClass в MainAppShell.
- Compact (телефон): всё как описано — полноэкранные оверлеи, остров снизу.
- Medium (слайдер/узкий планшет): без изменений в этом слайсе (см. роадмап).
- Expanded (планшет/ландшафт): чат открывается В ПРАВОЙ ПАНЕЛИ рядом со
  списком (list-detail two-pane) вместо полноэкранного оверлея.
- Дальше (роадмап): сетка настроек/сервисов на Expanded, адаптация острова,
  two-pane для поиска, канвасы под foldables.

## 5. Файловая карта (SRP: 1 файл = 1 задача)

    features/chats/
      common/        IChatSession, ChatSecurityTier, SessionGlyph, ChatTime,
                     MuteDuration (enum 1ч/8ч/1с/2д/навсегда)
      direct|group|otp/  стратегии (+isArchived, mutedUntil, очистка истории)
      data/          ChatListRepository (пин/мьют/архив/прочитано/история/
                     удаление/findOrCreate), InMemoryChatListRepository
      state/         ChatListUiState (selection, dialogs, archiveCollapsed),
                     ChatListViewModel (все интенты)
      mainmenu/      ChatsMainMenuScreen
        components/  ChatsTopBar, ArchivePill, SelectionTopBar, MuteDurationSheet,
                     DeleteSessionsDialog, ClearHistoryDialog, ChatListDialogs,
                     ChatsFab, ChatListItemCard, ChatAvatar, ChatTrailingBadges,
                     EmptyChatsState, OnionOfflineBanner
      archive/       ArchiveScreen
      search/        ChatSearchScreen, ChatSearchViewModel
      ui/            ChatDetailScreen, ChatSessionVisuals (без изменений)
    features/contacts/  common/Contact, data/ContactRepository +
                        InMemoryContactRepository, state/ContactsViewModel,
                        ui/ContactsScreen + components (ContactListItem,
                        AddContactCard)
    features/qr/ui/ QrHubOverlay (QR ID + «Мои контакты»)
    features/navigation/ MainAppShell (оверлеи + two-pane Expanded),
                        components/AppHeader
    features/settings/ui/ SettingsScreen (+выбор темы),
                        components/ThemePickerDialog
    core/theme/     AnonGramScheme, ThemeRepository, SharedPrefsThemeRepository,
                    ThemeController, Theme.kt (+scheme)
    MainActivity.kt  WindowSizeClass + ThemeController

Удаляются: chats/ui/ChatListScreen.kt, chats/ui/components/* (перенесены в
mainmenu или заменены selection-режимом), qr/ui/QrIdOverlay.kt.

## 6. Роадмап

1. DirectChatScreen: MessageBubble, ChatInputBar, отправка через ICryptoBridge.
2. Сканер QR (CameraX) + AGQR-парсер + IQrPayloadHandler-хендлеры.
3. Сервер-зависимое: глобальный поиск, «удалить у всех» по сети, presence.
4. Миграция всех экранов на LocalAnonGramScheme; Medium-адаптация.
5. SAS-верификация контактов (60 цифр), экран настройки мьюта по интервалу.

## 7. Фирменные векторные иконки (стикеры/эмодзи запрещены)

Эмодзи и текстовые символы (●, ⏱, ∞, 🔥) в UI не используются — только
векторные иконки. Ассеты res/drawable:
- `ic_stat_anongram.xml` — щит со стелс-корабликом (логотип, существовал ранее);
- `ic_flame.xml` — фирменное пламя OTP: аватар OTP-чата, бейдж страниц пада,
  карточка «OTP Блокнот»;
- `ic_onion.xml` — луковые слои: бейдж «Onion v3» в топбарах, Onion VPN.
Мьют-лист вместо символов использует Material-иконки Schedule / AllInclusive.

## 8. Экран переписки — воспроизведение интерфейса чата Telegram

Эталон: Telegram for Android (DrKLO/Telegram, чатовые ячейки и
ChatActivityEnterView), переосмыслен на Compose с фирменной темой AMOLED.

Реализованные механики TG:
- обратный список (reverseLayout), новейшие внизу, автопрокрутка кнопкой «вниз»;
- группировка подряд идущих сообщений одного направления (окно 5 минут):
  хвостик-уголок у первого в группе, отступы 2dp внутри группы / 8dp между;
- дат-чипы «Сегодня / Вчера / d MMMM» при смене суток;
- пузыри до 78% ширины: incoming CardDark без рамки, outgoing 0C243B с
  акцентной рамкой темы; цитата-реплай полоской внутри пузыря;
- статусы доставки: часы → одинарная галочка → двойная (цветные фирменные
  векторы ic_tick_single / ic_tick_double), двойная окрашивается акцентом;
- long-press: Ответить / Копировать / Удалить (удаление — только своих);
- тулбар: назад, аватар, имя, статус «в сети / был(а) недавно / печатает…»,
  меню «Очистить историю»;
- инпут-бар: пилюля (скрепка + текст + эмодзи), круглая кнопка — самолётик
  при тексте, микрофон при пустом;
- живой диалог: отправка → 0.6с SENT → 1.1с READ → «печатает…» → ответ
  собеседника из демо-пула;
- при открытии чата сессия помечается прочитанной; история и реплаи живут в
  ChatMessagesRepository (заменяется на FFI за тем же интерфейсом).

Файловая карта: features/chats/messages/{domain, data, state, ui, ui/components},
旧 static-заглушка features/chats/ui/ChatDetailScreen.kt удалена; шелл
переведён на ChatScreen (ключеванный ChatMessagesViewModel на каждую сессию).

## 9. Роадмап следующих шагов по чатам

1. Подбор/прокрутка к сообщению по ответу, unread-разделитель.
2. Вложения и голосовые (запись/плеер), эмодзи-панель.
3. Group-чаты: аватары в пузырях, sender-имена, покинуть/добавить участника.
4. OTP-чат: счётчик страниц, автосжигание прочитанного.
5. Перевод тредов и статусов на ICryptoBridge + сетевой транспорт.
