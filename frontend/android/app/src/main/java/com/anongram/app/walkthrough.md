# Walkthrough: Telegram-Style Attachment Modules & Player System

Полная реализация группы из 4 изолированных модулей (`features/attach`, `features/player`, `features/camera`, `features/mediaeditor`) в стилистике Telegram с тёмной темой и фиолетовым акцентом (`#8B5CF6`).

---

## 1. Архитектурная изоляция и контракты (`api/`)

Каждый модуль изолирован в своей папке и наружу выставляет только пакет `api/`. Зависимости строго однонаправленные: `attach -> player, camera, mediaeditor`. Обратные зависимости запрещены. Модули ничего не шлют в сеть или Rust-ядро самостоятельно, отдавая готовые драфты `AttachmentDraft`.

### `features/attach/api/`
- **`MediaTtl.kt`**: `Never`, `OneView`, `Sec3`, `Sec10`, `Sec30`.
- **`AttachmentDraft.kt`**:
  - `Media(uri, isVideo, caption, hd, ttl)`
  - `File(uri, name, size)`
  - `Location(lat, lon, address)`
  - `LiveLocation(durationMin)`
  - `Contact(name, phones)`
  - `Audio(uri, title, artist, durationMs)`
- **`AttachSheet.kt`**: `@Composable AttachSheet(visible, recipientName, onDismiss, onSend, modifier)`

### `features/player/api/`
- **`Track.kt`**, **`RepeatMode.kt`**, **`PlayerState.kt`**, **`PlayerController.kt`**
- **`MiniPlayer.kt`**: `@Composable MiniPlayer(onExpand, modifier)`
- **`FullPlayerSheet.kt`**: `@Composable FullPlayerSheet(onDismiss, modifier)`

### `features/camera/api/`
- **`CapturedMedia.kt`**: `(uri, isVideo, durationMs, width, height)`
- **`CameraScreen.kt`**: `@Composable CameraScreen(onResult, onClose, modifier)`

### `features/mediaeditor/api/`
- **`EditableMediaItem.kt`**, **`EditedMedia.kt`**
- **`MediaEditorScreen.kt`**: `@Composable MediaEditorScreen(items, recipientName, onSend, onAddMore, onBack, modifier)`

---

## 2. Системные разрешения (`AndroidManifest.xml` & `core/permissions/`)
- Добавлены разрешения: `CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_VISUAL_USER_SELECTED` (Android 14+), `READ_EXTERNAL_STORAGE` (`maxSdkVersion="32"`), `READ_CONTACTS`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `FOREGROUND_SERVICE_LOCATION`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`.
- Строго **БЕЗ** `MANAGE_EXTERNAL_STORAGE`.
- Сервисы: `AudioPlayerService` и `LocationSharingService`.
- Утилита `PermissionUtils.kt` для гранулированной проверки и запроса прав с поддержкой баннера частичного доступа `PartialAccessBanner`.

---

## 3. Модуль плеера (`features/player/`)
- **`AudioPlayerController.kt`**: Синглтон-контроллер медиаплеера с очередью, позицией и демонстрационным треком.
- **`AudioPlayerService.kt`**: Фоновый сервис с уведомлением `mediaPlayback`.
- **`MiniPlayerContent.kt`**: Плавающая таблетка под шапкой чата и в окне вложений: кнопка toggle (play/pause),marquee строка "Исполнитель — Название", кнопка закрытия и фиолетовая полоса прогресса по нижнему краю.
- **`FullPlayerSheetContent.kt`**: Нижнее модальное окно с ручкой свайпа: список очереди воспроизведения, текущий трек, интерактивный ползунок перемотки (`0:02 / 0:19`), переключатель режимов повтора (`OFF`, `ALL`, `ONE`), кнопки prev/play-pause/next.

---

## 4. Окно вложений (`features/attach/ui/AttachSheetContent.kt`)
- Жест вертикального перетягивания от 50% до 100% высоты экрана. Свайп вниз ниже 50% плавно закрывает окно.
- Плавающие верхние таблетки `AttachTopBar` (кнопка назад, заголовок/поиск, меню).
- Нижняя плавающая панель вкладок `AttachTabBar` (Галерея, Файл, Геопозиция, Контакт, Музыка). При открытии на весь экран (100%) панель плавно скрывается вниз.
- Интеграция с системным `BackHandler`: сворачивание полноэкранного режима до 50%, закрытие оверлеев.

---

## 5. Вкладки окна вложений:
1. **Вкладка «Галерея» (`GalleryTab.kt`)**:
   - Выпадающий список альбомов ("Все", "Видео", папки устройства).
   - Сетка из 3 колонок.
   - Первая плитка — живой запуск камеры.
   - Мультивыбор до 10 медиа с фиолетовыми бейджами `1..10`.
   - Плавающая панель снизу "Выбрано: N (из 10)" + кнопка "Далее" для перехода в редактор.
2. **Вкладка «Файл» (`FilesTab.kt`)**:
   - 3 карточки сверху: "Внутреннее хранилище" (системный OpenDocument), "Папка приложения" (файлы кэша AnonGram), "Галерея" (без сжатия, `hd=true`).
   - Список недавних файлов (`MediaStore.Files` / Downloads).
   - Динамический цвет иконки по хешу расширения файла (pdf, zip, apk, doc, etc.).
   - Меню сортировки: по дате, по имени, по размеру.
3. **Вкладка «Геопозиция» (`LocationTab.kt`)**:
   - Тёмная стилизованная карта со статической сеткой и фиолетовой булавкой по центру.
   - Координаты через реальный `LocationManager` (без Google Play Services).
   - Карточка "Отправить выбранную геопозицию" с геокодированием адреса.
   - Карточка "Транслировать геопозицию" (15 мин., 1 час, 8 часов) с запуском `LocationSharingService`.
   - Список мест рядом (Overpass OSM POI) с поиском.
4. **Вкладка «Контакт» (`ContactsTab.kt`)**:
   - Чтение `ContactsContract`.
   - Поле-таблетка "Поиск контактов".
   - Цветной круглый аватар с инициалами по хешу имени.
   - Диалог выбора номера при наличии нескольких телефонов у контакта.
5. **Вкладка «Музыка» (`MusicTab.kt`)**:
   - Сканирование `MediaStore.Audio`.
   - Поле "Поиск музыки".
   - Круглая кнопка play запускает трек в `PlayerController`.
   - При воспроизведении под строкой поиска появляется `MiniPlayer`.
   - Тап по строке выбирает трек для отправки.

---

## 6. Модуль камеры (`features/camera/ui/CameraScreenContent.kt`)
- Полноэкранное превью камеры Camera2/CameraX.
- Тап по превью — анимация индикатора фокусировки.
- Вспышка (Авто, Вкл, Выкл).
- Переворот камеры (фронтальная / основная).
- Кнопка затвора:
  - Короткий тап — съёмка фото.
  - Удержание — запись видео со счетчиком и круговым кольцом прогресса (лимит 60 сек).
- Сохранение в приватный кэш приложения (`context.cacheDir`) с удалением EXIF GPS.

---

## 7. Модуль предпросмотра и редактора (`features/mediaeditor/ui/MediaEditorContent.kt`)
- Полноэкранный просмотр с `HorizontalPager` для свайпа между выбранными медиа.
- Верхняя панель: назад, имя получателя, круглый счетчик (тап раскрывает ленту миниатюр), фиолетовая галочка.
- Поле "Добавить подпись..." с кнопкой "ещё медиа" (`onAddMore`) и кнопкой таймера исчезновения.
- Всплывающее меню таймера `MediaTtl`:
  - "Выберите, через какое время после открытия медиафайл исчезнет."
  - "Один просмотр", "3 сек.", "10 сек.", "30 сек.", "Никогда".
  - Сноска: "Получатель всё равно может сфотографировать экран".
  - Индикатор на кнопке таймера, если выбран не "Никогда".
- Панель инструментов: поворот/кадрирование, кисть (рисует фиолетовые линии на холсте), SD/HD переключатель, диалог настроек (яркость, контраст, насыщенность).
- Круглая фиолетовая кнопка отправки справа.
- Экспорт в кэш с масштабированием (SD = ~1280 px, HD = оригинал/2048 px) и очисткой метаданных.

---

## 8. Интеграция с чатом (`ChatScreen.kt`)
- Скрепка в поле ввода открывает `AttachSheet`.
- Под шапкой чата размещён `MiniPlayer` (показывается только при активном воспроизведении).
- Тап по телу мини-плеера открывает `FullPlayerSheet`.
- Чат, Rust-ядро и сетевой стек не модифицировались.
