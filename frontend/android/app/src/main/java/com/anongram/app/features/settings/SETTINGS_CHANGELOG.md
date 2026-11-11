# Модульная архитектура раздела «Настройки» (`features/settings/`)

## 1. Обзор архитектурных изменений
Модуль `features/settings/` полностью переработан по принципу **строгой изоляции дизайн-компонентов по подразделам**.
Вместо общей папки-свалки компонентов каждый подраздел настроек владеет собственным каталогом с персональными `ui/` (экраны) и `ui/components/` (дизайн-компоненты только этого подраздела).

## 2. Дерево модулей `features/settings/`
- `main/` — **Главное меню настроек** (Макеты 1 и 2):
  - `ui/SettingsMainScreen.kt` — топбар с круглой кнопкой QR слева и островком «Поиск + 3 точки» справа, шапка профиля, мульти-аккаунты («Аккаунты») и 9 цветных строк подразделов.
  - `ui/components/SettingsProfileHeader.kt` — крупный аватар с фиолетовым бейджем камеры, имя и `@HANDLE`.
  - `ui/components/AccountsSectionCard.kt` — список подключённых аккаунтов с индикатором непрочитанных чатов.
  - `ui/components/SettingsItemRow.kt` — строка подраздела настроек с цветной иконкой.
  - `ui/components/SettingsOverflowMenu.kt` — выпадающее меню «Выход» по нажатию на 3 точки.
- `qr/` — **QR-хаб: Мой QR, Камера-сканер и Галерея** (Макеты 3, 4 и 5):
  - `ui/MyQrCodeScreen.kt` — фиолетовый экран с белой карточкой QR-кода, центральным самолётиком AnonGram, `@HANDLE`, верхней капсулой «Сканировать QR-код» и нижней панелью (Поделиться, Стили, Скачать).
  - `ui/QrCameraScannerScreen.kt` — сканер QR-кода с обязательной проверкой `Manifest.permission.CAMERA` (защита от чёрного экрана при отсутствии разрешения), рамкой видоискателя и кнопками Галереи и Фонарика.
  - `ui/QrGalleryPickerScreen.kt` — экран «Галерея» (сетка альбомов в 2 колонки: `Camera`, `Все фотографии`, `Screenshots`, `WhatsApp Images`, `Telegram`, `Pictures`) с обязательной проверкой `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE`.
  - `ui/components/{QrCodeCard.kt, QrScannerViewfinder.kt, GalleryAlbumCard.kt, QrPermissionNotice.kt}`.
- `language/` — **Язык и перевод** (консолидированный модуль):
  - `ui/LanguageSettingsScreen.kt`
  - `ui/components/{LanguageListCard.kt, LanguageTopBar.kt, LanguageTranslationCard.kt, TelegramRadioButton.kt, LanguagePickerDialog.kt}`
- **Изолированные подразделы**:
  - `account/` (`AccountSettingsScreen.kt` + `components/AccountInfoCard.kt`)
  - `chat_settings/` (`ChatAppearanceSettingsScreen.kt` + `components/{ChatTextSizeCard.kt, ThemePickerDialog.kt}`)
  - `privacy/` (`PrivacySecuritySettingsScreen.kt` + `components/PrivacyKeyCard.kt`)
  - `notifications/` (`NotificationsSettingsScreen.kt` + `components/NotificationToggleCard.kt`)
  - `data_storage/` (`DataStorageSettingsScreen.kt` + `components/StorageUsageCard.kt`)
  - `folders/` (`ChatFoldersSettingsScreen.kt` + `components/FolderItemCard.kt`)
  - `devices/` (`DevicesSettingsScreen.kt` + `components/DeviceSessionCard.kt`)
  - `battery/` (`BatterySettingsScreen.kt` + `components/BatterySaverCard.kt`)
- `navigation/` — **Навигация внутри настроек**:
  - `SettingsRoute.kt` & `SettingsNavHost.kt`
