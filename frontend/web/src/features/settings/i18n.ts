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
  showTranslateButton: 'Кнопка «Перевести» в сообщениях',
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
    mono: 'Моноширинный',
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
    mono: 'Monospace',
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
};

export const SettingsI18n = {
  get current(): SettingsStrings {
    return i18nStore.language === 'en' ? EN : RU;
  },
};
