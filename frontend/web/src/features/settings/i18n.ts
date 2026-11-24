import { i18nStore } from '../../core/i18n/i18nStore';
import type { TransitionType } from '../../core/navigation/TransitionModels';
import type { AutoLockOption, FontFamilyOption, VoiceEngineOption } from './settingsStore';

export interface SettingsStrings {
  hideOnlineStatus: string;
  blockScreenshots: string;
  passcodeEnabled: string;
  biometricLock: string;
  twoFactorEnabled: string;
  pushNotifications: string;
  inAppSounds: string;
  autoDownloadMedia: string;
  autoDownloadPhotos: string;
  autoDownloadVoice: string;
  animationsEnabled: string;
  messageAnimations: string;
  screenTransitionsEnabled: string;
  springPhysicsEnabled: string;
  reduceMotion: string;
  animationSpeedTitle: string;
  transitionStyleTitle: string;
  transitionStyles: Record<TransitionType, string>;
  powerSavingMode: string;
  voiceModule: string;
  translationModule: string;
  localAiModule: string;
  showTranslateButton: string;
  autoTranslateIncoming: string;
  voiceEngineTitle: string;
  voiceEngines: Record<VoiceEngineOption, string>;
  fontsTitle: string;
  fontFamilyTitle: string;
  fontSizeTitle: string;
  fontFamilies: Record<FontFamilyOption, string>;
  wallpaperTitle: string;
  wallpaperChoose: string;
  wallpaperReset: string;
  wallpaperActive: string;
  wallpaperNone: string;
  storageUsageTitle: string;
  storageTotal: string;
  storageMedia: string;
  storageMessages: string;
  storageModels: string;
  storageFree: string;
  clearCacheBtn: string;
  cacheClearedToast: string;
  currentSession: string;
  activeSessionsTitle: string;
  otherSessionsTitle: string;
  onlineStatus: string;
  terminateSession: string;
  terminateOthersBtn: string;
  terminateOthersConfirm: string;
  sessionTerminatedToast: string;
  noOtherSessions: string;
  fingerprint: string;
  setPasscodeTitle: string;
  changePasscodeBtn: string;
  enterPinPlaceholder: string;
  confirmPinPlaceholder: string;
  savePinBtn: string;
  cancelBtn: string;
  pinTooShort: string;
  pinMismatch: string;
  pinSavedToast: string;
  pinDisabledToast: string;
  autoLockTitle: string;
  autoLockOptions: Record<AutoLockOption, string>;
  cryptoStatusTitle: string;
  cryptoStatusDesc: string;

  // Modules Hub & Sub-screens
  modulesTitle: string;
  modulesVoiceTitle: string;
  modulesTranslationTitle: string;
  modulesLocalAiTitle: string;
  modulesFontsTitle: string;
  moduleMemoryLabel: (mbLabel: string) => string;
  mbUnit: string;
  downloadBtn: string;
  removeBtn: string;
  cancelBtnShort: string;
  activeBadge: string;
  selectBtn: string;
  installedLabel: (mbLabel: string) => string;
  downloadingProgress: (pct: number) => string;

  // Voice Module
  voiceEngineHeader: string;
  voiceModelsHeader: string;
  voiceAutoTranscribe: string;
  voiceAutoTranscribeDesc: string;
  voiceWhisperTitle: string;
  voiceWhisperDesc: string;
  voiceVoskTitle: string;
  voiceVoskDesc: string;
  voiceSystemTitle: string;
  voiceSystemDesc: string;
  voiceRecommendedBadge: string;
  voiceNotRecommendedBadge: string;
  voicePrivacyTitle: string;
  voicePrivacyBody: string;
  voicePrivacyKeepLocal: string;
  voicePrivacyAcceptAnyway: string;
  voiceRecognitionLangLabel: string;
  langAuto: string;
  langRu: string;
  langEn: string;
  voiceWhisperTinyTitle: string;
  voiceWhisperTinyDesc: string;
  voiceWhisperBaseTitle: string;
  voiceWhisperBaseDesc: string;
  voiceVoskRuTitle: string;
  voiceVoskRuDesc: string;
  voiceVoskEnTitle: string;
  voiceVoskEnDesc: string;
  voiceSystemSupported: string;
  voiceSystemUnsupported: string;
  voicePrivacyNote: string;

  // Translation Module
  showTranslateButtonDesc: string;
  autoTranslateIncomingDesc: string;
  translationTargetLangHeader: string;
  translationPacksHeader: string;
  translationSearchPlaceholder: string;
  translationPrivacyNote: string;
  translationEnabledStatus: string;
  translationDisabledStatus: string;
  translationHubEngine: string;
  translationPacksCount: (count: number, mbLabel: string) => string;

  // Local AI Module
  localAiHubEmpty: string;
  localAiInstalledHeader: string;
  localAiCatalogHeader: string;
  localAiSearchPlaceholder: string;
  localAiCategoryAll: string;
  localAiPrivacyNote: string;
  localAiInstalledToast: string;
  localAiSelectedToast: string;

  // Fonts Module
  fontScopeLabel: string;
  fontScopeAll: string;
  fontScopeMessages: string;
  fontScopeUi: string;
  fontResetBtn: string;
  fontPreviewHeader: string;
  fontPreviewIncoming: string;
  fontPreviewOutgoing: string;
  fontCardsHeader: string;
  fontSampleGlyphs: string;
  fontDescriptions: Record<'system' | 'inter' | 'mono' | 'roboto' | 'sfPro' | 'comfortaa', string>;
}

const RU: SettingsStrings = {
  hideOnlineStatus: 'Скрывать статус «в сети»',
  blockScreenshots: 'Защита от скриншотов',
  passcodeEnabled: 'Код-пароль на вход',
  biometricLock: 'Биометрическая разблокировка',
  twoFactorEnabled: 'Облачный пароль (2FA)',
  pushNotifications: 'Push-уведомления',
  inAppSounds: 'Звуки в приложении',
  autoDownloadMedia: 'Автозагрузка медиа',
  autoDownloadPhotos: 'Автозагрузка фото',
  autoDownloadVoice: 'Автозагрузка голосовых',
  animationsEnabled: 'Анимации интерфейса',
  messageAnimations: 'Анимации сообщений',
  screenTransitionsEnabled: 'Плавные переходы экранов',
  springPhysicsEnabled: 'Пружинная физика (Spring)',
  reduceMotion: 'Уменьшить движение',
  animationSpeedTitle: 'Скорость анимаций',
  transitionStyleTitle: 'Стиль переходов',
  transitionStyles: {
    SPRING_SWIPE: 'Пружинный свайп',
    CUPERTINO_PARALLAX: 'Параллакс',
    REAL_BOOK_3D: 'Книга 3D',
    CUBE_ROTATION_3D: 'Куб 3D',
    ROTATION_SPIN: 'Вращение',
    CASCADE: 'Каскад',
    WINDMILL: 'Мельница',
    PARTICLE_DISSOLVE: 'Частицы',
    CARD_STACK_PEEK: 'Карточки',
    PREDICTIVE_BACK: 'Жест назад',
    INSTANT: 'Мгновенно',
  },
  powerSavingMode: 'Режим энергосбережения',
  voiceModule: 'Модуль распознавания голоса',
  translationModule: 'Модуль локального перевода',
  localAiModule: 'Локальный ИИ (Private LLM)',
  showTranslateButton: 'Показывать кнопку «Перевести»',
  autoTranslateIncoming: 'Автоперевод входящих',
  voiceEngineTitle: 'Движок расшифровки речи',
  voiceEngines: {
    whisper: 'Whisper (оффлайн)',
    vosk: 'Vosk (быстрый)',
    system: 'Системный',
  },
  fontsTitle: 'Шрифты',
  fontFamilyTitle: 'Семейство шрифта',
  fontSizeTitle: 'Размер шрифта сообщений',
  fontFamilies: {
    system: 'Системный',
    inter: 'Inter',
    mono: 'JetBrains Mono',
    roboto: 'Roboto',
    sfPro: 'SF Pro',
    comfortaa: 'Comfortaa',
    serif: 'С засечками',
    rounded: 'Округлый',
  },
  wallpaperTitle: 'Обои чата',
  wallpaperChoose: 'Выбрать обои',
  wallpaperReset: 'Сбросить',
  wallpaperActive: 'Установлены обои',
  wallpaperNone: 'Без обоев',
  storageUsageTitle: 'Использование памяти',
  storageTotal: 'Всего занято',
  storageMedia: 'Медиа и обои',
  storageMessages: 'Сообщения и ключи',
  storageModels: 'Модели и шрифты',
  storageFree: 'Кэш приложения',
  clearCacheBtn: 'Очистить кэш',
  cacheClearedToast: 'Кэш медиафайлов очищен',
  currentSession: 'Текущая сессия',
  activeSessionsTitle: 'Активные сессии',
  otherSessionsTitle: 'Другие устройства',
  onlineStatus: 'в сети',
  terminateSession: 'Завершить',
  terminateOthersBtn: 'Завершить все остальные сессии',
  terminateOthersConfirm: 'Завершить все остальные активные сессии?',
  sessionTerminatedToast: 'Сессия завершена',
  noOtherSessions: 'Нет других активных сессий',
  fingerprint: 'Отпечаток',
  setPasscodeTitle: 'Установить код-пароль (4–6 цифр)',
  changePasscodeBtn: 'Изменить код-пароль',
  enterPinPlaceholder: 'Введите код (4–6 цифр)',
  confirmPinPlaceholder: 'Повторите код',
  savePinBtn: 'Сохранить код',
  cancelBtn: 'Отмена',
  pinTooShort: 'Код должен содержать от 4 до 6 цифр',
  pinMismatch: 'Коды не совпадают',
  pinSavedToast: 'Код-пароль установлен',
  pinDisabledToast: 'Код-пароль отключён',
  autoLockTitle: 'Автоблокировка',
  autoLockOptions: {
    off: 'Выкл',
    '1m': '1 мин',
    '5m': '5 мин',
    '1h': '1 час',
  },
  cryptoStatusTitle: 'Постквантовое шифрование',
  cryptoStatusDesc: 'Все сообщения и медиа защищены E2EE (X25519 + ChaCha20-Poly1305). Ключи хранятся только локально.',

  modulesTitle: 'Модули',
  modulesVoiceTitle: 'Расшифровка голосовых',
  modulesTranslationTitle: 'Перевод сообщений',
  modulesLocalAiTitle: 'Локальный ИИ',
  modulesFontsTitle: 'Шрифты',
  moduleMemoryLabel: (mb) => `Память: ${mb}`,
  mbUnit: 'МБ',
  downloadBtn: 'Скачать',
  removeBtn: 'Удалить',
  cancelBtnShort: 'Отмена',
  activeBadge: 'Активна',
  selectBtn: 'Выбрать',
  installedLabel: (mb) => `Установлено • ${mb}`,
  downloadingProgress: (pct) => `Загрузка ${pct}%`,

  voiceEngineHeader: 'Движок распознавания',
  voiceModelsHeader: 'Модели и языковые пакеты',
  voiceAutoTranscribe: 'Авторасшифровка голосовых',
  voiceAutoTranscribeDesc: 'Автоматически переводить входящие ГС в текст локально',
  voiceWhisperTitle: 'Whisper (Локальный ИИ)',
  voiceWhisperDesc: 'Открытый код • Модели Tiny / Base понимают все языки и ставят пунктуацию',
  voiceVoskTitle: 'Vosk (Локальный поток)',
  voiceVoskDesc: 'Открытый код • Быстрые лёгкие модели Vosk Small по языкам',
  voiceSystemTitle: 'Системный (System Speech)',
  voiceSystemDesc: 'Закрытый исходный код • Встроенная служба браузера/ОС без загрузки моделей',
  voiceRecommendedBadge: 'Рекомендуется',
  voiceNotRecommendedBadge: 'Не рекомендуется',
  voicePrivacyTitle: 'Внимание: закрытый исходный код',
  voicePrivacyBody:
    'Системный движок имеет закрытый исходный код: мы не можем провести аудит и гарантировать, что аудиозаписи не отправляются на внешние серверы.\n\nДля максимальной анонимности в AnonGram рекомендуется использовать открытые локальные движки Whisper или Vosk.',
  voicePrivacyKeepLocal: 'Оставить локальный',
  voicePrivacyAcceptAnyway: 'Всё равно выбрать',
  voiceRecognitionLangLabel: 'Язык распознавания',
  langAuto: 'Авто',
  langRu: 'Русский',
  langEn: 'English',
  voiceWhisperTinyTitle: 'Whisper Tiny (q5_1)',
  voiceWhisperTinyDesc: 'Быстрая мультиязычная модель • ~32 МБ',
  voiceWhisperBaseTitle: 'Whisper Base (q5_1)',
  voiceWhisperBaseDesc: 'Повышенная точность и пунктуация • ~60 МБ',
  voiceVoskRuTitle: 'Русский (Vosk Small)',
  voiceVoskRuDesc: 'Быстрая потоковая модель RU • ~45 МБ',
  voiceVoskEnTitle: 'English (Vosk Small)',
  voiceVoskEnDesc: 'Быстрая потоковая модель EN • ~40 МБ',
  voiceSystemSupported: 'Системное распознавание речи доступно в текущем браузере',
  voiceSystemUnsupported: 'Системное распознавание речи недоступно в текущем браузере. Выберите Whisper или Vosk.',
  voicePrivacyNote: 'Модели Whisper и Vosk работают 100% офлайн на устройстве без отправки голосовых сообщений на внешние серверы.',

  showTranslateButtonDesc: 'Отображать кнопку быстрого перевода в меню сообщения',
  autoTranslateIncomingDesc: 'Автоматически переводить входящие сообщения на других языках локально',
  translationTargetLangHeader: 'Язык перевода',
  translationPacksHeader: 'Офлайн-языки (ML Kit)',
  translationSearchPlaceholder: 'Поиск языка…',
  translationPrivacyNote: 'Перевод выполняется 100% локально на устройстве с помощью офлайн-пакетов без отправки текста на серверы.',
  translationEnabledStatus: 'Включено',
  translationDisabledStatus: 'Выключено',
  translationHubEngine: 'ML Kit',
  translationPacksCount: (count, mb) => `${count} пак. • ${mb}`,

  localAiHubEmpty: 'GGUF • 0 моделей',
  localAiInstalledHeader: 'Установленные',
  localAiCatalogHeader: 'Каталог моделей GGUF',
  localAiSearchPlaceholder: 'Поиск GGUF-модели…',
  localAiCategoryAll: 'Все',
  localAiPrivacyNote: 'Локальные GGUF-модели работают в изолированной среде без отправки истории чата и промптов в облако.',
  localAiInstalledToast: 'GGUF-модель установлена',
  localAiSelectedToast: 'Активная модель обновлена',

  fontScopeLabel: 'Где применять',
  fontScopeAll: 'Всё',
  fontScopeMessages: 'Сообщения',
  fontScopeUi: 'Интерфейс',
  fontResetBtn: 'Сбросить',
  fontPreviewHeader: 'Предпросмотр',
  fontPreviewIncoming: 'Привет! Как читается новый шрифт в теме?',
  fontPreviewOutgoing: 'Супер чётко и 100% локально 🛡️',
  fontCardsHeader: 'Шрифты интерфейса и чата',
  fontSampleGlyphs: 'Аа Бб Привет! Aa Bb 123',
  fontDescriptions: {
    system: 'Шрифт операционной системы по умолчанию',
    inter: 'Чистый UI-гротеск с высокой читаемостью',
    mono: 'Моноширинный шрифт для кода и ключей',
    roboto: 'Классический геометричный гротеск Android',
    sfPro: 'Системная типографика Apple с мягкими пропорциями',
    comfortaa: 'Округлый мягкий шрифт с плавными формами',
  },
};

const EN: SettingsStrings = {
  hideOnlineStatus: 'Hide online status',
  blockScreenshots: 'Screenshot protection',
  passcodeEnabled: 'Passcode lock',
  biometricLock: 'Biometric unlock',
  twoFactorEnabled: 'Two-step verification (2FA)',
  pushNotifications: 'Push notifications',
  inAppSounds: 'In-app sounds',
  autoDownloadMedia: 'Auto-download media',
  autoDownloadPhotos: 'Auto-download photos',
  autoDownloadVoice: 'Auto-download voice',
  animationsEnabled: 'Interface animations',
  messageAnimations: 'Message animations',
  screenTransitionsEnabled: 'Screen transitions',
  springPhysicsEnabled: 'Spring physics',
  reduceMotion: 'Reduce motion',
  animationSpeedTitle: 'Animation speed',
  transitionStyleTitle: 'Transition style',
  transitionStyles: {
    SPRING_SWIPE: 'Spring swipe',
    CUPERTINO_PARALLAX: 'Parallax',
    REAL_BOOK_3D: '3D Book',
    CUBE_ROTATION_3D: '3D Cube',
    ROTATION_SPIN: 'Spin',
    CASCADE: 'Cascade',
    WINDMILL: 'Windmill',
    PARTICLE_DISSOLVE: 'Particles',
    CARD_STACK_PEEK: 'Card stack',
    PREDICTIVE_BACK: 'Predictive back',
    INSTANT: 'Instant',
  },
  powerSavingMode: 'Power saving mode',
  voiceModule: 'Voice recognition module',
  translationModule: 'Local translation module',
  localAiModule: 'Local AI (Private LLM)',
  showTranslateButton: 'Show «Translate» button',
  autoTranslateIncoming: 'Auto-translate incoming',
  voiceEngineTitle: 'Speech recognition engine',
  voiceEngines: {
    whisper: 'Whisper (offline)',
    vosk: 'Vosk (fast)',
    system: 'System',
  },
  fontsTitle: 'Fonts',
  fontFamilyTitle: 'Font family',
  fontSizeTitle: 'Message font size',
  fontFamilies: {
    system: 'System',
    inter: 'Inter',
    mono: 'JetBrains Mono',
    roboto: 'Roboto',
    sfPro: 'SF Pro',
    comfortaa: 'Comfortaa',
    serif: 'Serif',
    rounded: 'Rounded',
  },
  wallpaperTitle: 'Chat wallpaper',
  wallpaperChoose: 'Choose wallpaper',
  wallpaperReset: 'Reset',
  wallpaperActive: 'Wallpaper active',
  wallpaperNone: 'No wallpaper',
  storageUsageTitle: 'Storage usage',
  storageTotal: 'Total used',
  storageMedia: 'Media & wallpapers',
  storageMessages: 'Messages & keys',
  storageModels: 'Models & fonts',
  storageFree: 'App cache',
  clearCacheBtn: 'Clear cache',
  cacheClearedToast: 'Media cache cleared',
  currentSession: 'Current session',
  activeSessionsTitle: 'Active sessions',
  otherSessionsTitle: 'Other devices',
  onlineStatus: 'online',
  terminateSession: 'Terminate',
  terminateOthersBtn: 'Terminate all other sessions',
  terminateOthersConfirm: 'Terminate all other active sessions?',
  sessionTerminatedToast: 'Session terminated',
  noOtherSessions: 'No other active sessions',
  fingerprint: 'Fingerprint',
  setPasscodeTitle: 'Set passcode (4–6 digits)',
  changePasscodeBtn: 'Change passcode',
  enterPinPlaceholder: 'Enter PIN (4–6 digits)',
  confirmPinPlaceholder: 'Confirm PIN',
  savePinBtn: 'Save PIN',
  cancelBtn: 'Cancel',
  pinTooShort: 'PIN must be 4 to 6 digits',
  pinMismatch: 'PINs do not match',
  pinSavedToast: 'Passcode saved',
  pinDisabledToast: 'Passcode disabled',
  autoLockTitle: 'Auto-lock',
  autoLockOptions: {
    off: 'Off',
    '1m': '1 min',
    '5m': '5 min',
    '1h': '1 hour',
  },
  cryptoStatusTitle: 'Post-quantum encryption',
  cryptoStatusDesc: 'All messages and media are E2EE protected (X25519 + ChaCha20-Poly1305). Keys never leave your device.',

  modulesTitle: 'Modules',
  modulesVoiceTitle: 'Voice Transcription',
  modulesTranslationTitle: 'Message Translation',
  modulesLocalAiTitle: 'Local AI',
  modulesFontsTitle: 'Fonts',
  moduleMemoryLabel: (mb) => `Memory: ${mb}`,
  mbUnit: 'MB',
  downloadBtn: 'Download',
  removeBtn: 'Remove',
  cancelBtnShort: 'Cancel',
  activeBadge: 'Active',
  selectBtn: 'Select',
  installedLabel: (mb) => `Installed • ${mb}`,
  downloadingProgress: (pct) => `Downloading ${pct}%`,

  voiceEngineHeader: 'Recognition engine',
  voiceModelsHeader: 'Models & language packs',
  voiceAutoTranscribe: 'Auto-transcribe voice messages',
  voiceAutoTranscribeDesc: 'Automatically transcribe incoming voice notes locally on device',
  voiceWhisperTitle: 'Whisper (Local AI)',
  voiceWhisperDesc: 'Open-source • Tiny / Base multilingual models with punctuation',
  voiceVoskTitle: 'Vosk (Local Stream)',
  voiceVoskDesc: 'Open-source • Fast lightweight per-language Vosk Small models',
  voiceSystemTitle: 'System Speech',
  voiceSystemDesc: 'Closed-source • Built-in device/browser service without extra downloads',
  voiceRecommendedBadge: 'Recommended',
  voiceNotRecommendedBadge: 'Not recommended',
  voicePrivacyTitle: 'Privacy warning: closed source',
  voicePrivacyBody:
    'System Speech is closed-source: we cannot audit its code or guarantee that your voice audio is not sent to external servers.\n\nFor maximum anonymity in AnonGram, we recommend open-source local engines Whisper or Vosk.',
  voicePrivacyKeepLocal: 'Keep local',
  voicePrivacyAcceptAnyway: 'Use anyway',
  voiceRecognitionLangLabel: 'Recognition language',
  langAuto: 'Auto',
  langRu: 'Russian',
  langEn: 'English',
  voiceWhisperTinyTitle: 'Whisper Tiny (q5_1)',
  voiceWhisperTinyDesc: 'Fast multilingual model • ~32 MB',
  voiceWhisperBaseTitle: 'Whisper Base (q5_1)',
  voiceWhisperBaseDesc: 'Higher accuracy & punctuation • ~60 MB',
  voiceVoskRuTitle: 'Russian (Vosk Small)',
  voiceVoskRuDesc: 'Fast streaming RU model • ~45 MB',
  voiceVoskEnTitle: 'English (Vosk Small)',
  voiceVoskEnDesc: 'Fast streaming EN model • ~40 MB',
  voiceSystemSupported: 'System Speech API is available in this browser',
  voiceSystemUnsupported: 'System Speech API is unavailable in this browser. Choose Whisper or Vosk.',
  voicePrivacyNote: 'Whisper and Vosk models run 100% offline on device without sending voice recordings to servers.',

  showTranslateButtonDesc: 'Show quick translate action in the message menu',
  autoTranslateIncomingDesc: 'Automatically translate incoming foreign-language messages locally',
  translationTargetLangHeader: 'Target language',
  translationPacksHeader: 'Offline language packs (ML Kit)',
  translationSearchPlaceholder: 'Search language…',
  translationPrivacyNote: 'Translation runs 100% locally on your device using offline packs without sending chats to servers.',
  translationEnabledStatus: 'Enabled',
  translationDisabledStatus: 'Disabled',
  translationHubEngine: 'ML Kit',
  translationPacksCount: (count, mb) => `${count} packs • ${mb}`,

  localAiHubEmpty: 'GGUF • 0 models',
  localAiInstalledHeader: 'Installed',
  localAiCatalogHeader: 'GGUF Model Catalog',
  localAiSearchPlaceholder: 'Search GGUF models…',
  localAiCategoryAll: 'All',
  localAiPrivacyNote: 'Local GGUF models run in an isolated sandbox without sending chat history or prompts to the cloud.',
  localAiInstalledToast: 'GGUF model installed',
  localAiSelectedToast: 'Active model updated',

  fontScopeLabel: 'Apply to',
  fontScopeAll: 'All',
  fontScopeMessages: 'Messages',
  fontScopeUi: 'UI only',
  fontResetBtn: 'Reset',
  fontPreviewHeader: 'Live preview',
  fontPreviewIncoming: 'Hey! How does the new font read in this theme?',
  fontPreviewOutgoing: 'Super crisp and 100% local 🛡️',
  fontCardsHeader: 'Interface & chat fonts',
  fontSampleGlyphs: 'Aa Bb Hello World 123',
  fontDescriptions: {
    system: 'Default operating system typeface',
    inter: 'Clean UI sans-serif with high legibility',
    mono: 'Developer monospace for keys and code',
    roboto: 'Crisp geometric Android sans-serif',
    sfPro: 'Apple system typography with balanced proportions',
    comfortaa: 'Smooth rounded display typeface',
  },
};

export const SettingsI18n = {
  get current(): SettingsStrings {
    return i18nStore.language === 'en' ? EN : RU;
  },
};
