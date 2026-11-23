import { i18nStore } from '../../core/i18n/i18nStore';

export interface ServicesStrings {
  title: string;
  aiTitle: string;
  aiDetail: string;
  onionTitle: string;
  onionDetail: string;
  onionActive: string;
  onionPaused: string;
  killSwitchTitle: string;
  killSwitchArmed: string;
  killSwitchDisarmed: string;
  killSwitchDetail: string;
  otpTitle: string;
  otpDetail: string;
  otpRemaining: (n: number) => string;
  callsTitle: string;
  callsSub: string;
  callsDetail: string;

  // Local AI
  aiNewChat: string;
  aiSessions: string;
  aiInputPlaceholder: string;
  aiEmptyTitle: string;
  aiEmptySub: string;
  aiThinking: string;
  aiReasoningLabel: string;
  aiPromptLabel: string;
  aiWebTools: string;
  aiEngineLocal: string;
  aiEngineCloud: string;
  aiClearChat: string;
  aiCopied: string;
  aiSuggestions: string[];

  // Onion
  onionHeading: string;
  onionToggleVpn: string;
  onionToggleKillSwitch: string;
  onionCircuitTitle: string;
  onionRotateBtn: string;
  onionRotatedToast: string;
  onionCamouflage: string;

  // OTP
  otpHeading: string;
  otpCreateBtn: string;
  otpResetBtn: string;
  otpTitlePlaceholder: string;
  otpContentPlaceholder: string;
  otpBurnBtn: string;
  otpRevealBtn: string;
  otpBurnedToast: string;
  otpCreatedToast: string;
  otpResetToast: string;
  otpEmpty: string;
}

const RU: ServicesStrings = {
  title: 'Сервисы Приватности',
  aiTitle: 'Local AI Assistant',
  aiDetail: 'Локальный ИИ, режим рассуждений, системные пресеты и история сессий',
  onionTitle: 'AnonGram Onion VPN',
  onionDetail: 'Трафик замаскирован под TLS 1.3',
  onionActive: 'Статус: Активен · 3 луковых прыжка',
  onionPaused: 'Статус: Отключён',
  killSwitchTitle: 'Kill-Switch',
  killSwitchArmed: 'Взведён',
  killSwitchDisarmed: 'Отключён',
  killSwitchDetail: 'При обрыве туннеля весь трафик блокируется мгновенно',
  otpTitle: 'OTP Блокнот',
  otpDetail: 'Каждая страница сгорает сразу после прочтения',
  otpRemaining: (n: number) => `Осталось страниц: ${n}`,
  callsTitle: 'Анонимные звонки',
  callsSub: 'Зашифрованные P2P-вызовы',
  callsDetail: 'Журнал звонков и E2EE-вызовы с SAS-кодом',

  aiNewChat: 'Новый чат',
  aiSessions: 'Сессии',
  aiInputPlaceholder: 'Спросите локальный AI...',
  aiEmptyTitle: 'AnonGram Local AI',
  aiEmptySub: 'Приватный ассистент без облачных логов и телеметрии',
  aiThinking: 'Ход рассуждений',
  aiReasoningLabel: 'Рассуждения',
  aiPromptLabel: 'Роль',
  aiWebTools: 'Веб-инструменты',
  aiEngineLocal: 'Локальный GGUF',
  aiEngineCloud: 'Облачный API',
  aiClearChat: 'Очистить чат',
  aiCopied: 'Ответ скопирован',
  aiSuggestions: [
    'Объясни, как работает Blind Relay в AnonGram',
    'Проверь безопасность моей seed-фразы (чеклист)',
    'Напиши краткий скрипт генерации Ed25519 ключей',
  ],

  onionHeading: 'Onion VPN & Kill-Switch',
  onionToggleVpn: 'Включить Onion v3 VPN',
  onionToggleKillSwitch: 'Активировать Kill-Switch',
  onionCircuitTitle: 'Активная цепочка (3 хопа)',
  onionRotateBtn: 'Сменить цепочку',
  onionRotatedToast: 'Новая 3-хоп цепочка построена',
  onionCamouflage: 'Маскировка: TLS 1.3 · uTLS Chrome',

  otpHeading: 'OTP Блокнот (Сгораемый)',
  otpCreateBtn: 'Создать страницу',
  otpResetBtn: 'Сбросить 7 страниц',
  otpTitlePlaceholder: 'Заголовок (например: Ключ узла)',
  otpContentPlaceholder: 'Секретный текст одноразовой заметки...',
  otpBurnBtn: 'Сжечь страницу',
  otpRevealBtn: 'Прочитать (одноразово)',
  otpBurnedToast: 'Страница уничтожена без возможности восстановления',
  otpCreatedToast: 'OTP-страница сохранена',
  otpResetToast: 'Блокнот обновлён (7 страниц)',
  otpEmpty: 'Все страницы сожжены. Сбросьте блокнот или создайте новую.',
};

const EN: ServicesStrings = {
  title: 'Privacy Services',
  aiTitle: 'Local AI Assistant',
  aiDetail: 'Local AI, reasoning levels, system presets, and persistent sessions',
  onionTitle: 'AnonGram Onion VPN',
  onionDetail: 'Traffic camouflaged as TLS 1.3',
  onionActive: 'Status: Active · 3 onion hops',
  onionPaused: 'Status: Disconnected',
  killSwitchTitle: 'Kill-Switch',
  killSwitchArmed: 'Armed',
  killSwitchDisarmed: 'Disarmed',
  killSwitchDetail: 'All traffic is blocked instantly if the tunnel drops',
  otpTitle: 'OTP Notepad',
  otpDetail: 'Each page burns immediately after reading',
  otpRemaining: (n: number) => `${n} pages remaining`,
  callsTitle: 'Anonymous Calls',
  callsSub: 'Encrypted P2P calls',
  callsDetail: 'Call history and E2EE calls with SAS verification',

  aiNewChat: 'New Chat',
  aiSessions: 'Sessions',
  aiInputPlaceholder: 'Ask Local AI...',
  aiEmptyTitle: 'AnonGram Local AI',
  aiEmptySub: 'Private assistant with zero cloud logs or telemetry',
  aiThinking: 'Reasoning trace',
  aiReasoningLabel: 'Reasoning',
  aiPromptLabel: 'Persona',
  aiWebTools: 'Web tools',
  aiEngineLocal: 'Local GGUF',
  aiEngineCloud: 'Cloud API',
  aiClearChat: 'Clear chat',
  aiCopied: 'Response copied',
  aiSuggestions: [
    'Explain how Blind Relay works in AnonGram',
    'Give me a seed-phrase security checklist',
    'Write a short Ed25519 keygen snippet',
  ],

  onionHeading: 'Onion VPN & Kill-Switch',
  onionToggleVpn: 'Enable Onion v3 VPN',
  onionToggleKillSwitch: 'Arm Kill-Switch',
  onionCircuitTitle: 'Active Circuit (3 hops)',
  onionRotateBtn: 'Rotate Circuit',
  onionRotatedToast: 'New 3-hop circuit established',
  onionCamouflage: 'Camouflage: TLS 1.3 · uTLS Chrome',

  otpHeading: 'OTP Notepad (Burnable)',
  otpCreateBtn: 'Create Page',
  otpResetBtn: 'Reset 7 Pages',
  otpTitlePlaceholder: 'Title (e.g., Node Key)',
  otpContentPlaceholder: 'Secret one-time note content...',
  otpBurnBtn: 'Burn Page',
  otpRevealBtn: 'Reveal (One-Time)',
  otpBurnedToast: 'Page permanently destroyed',
  otpCreatedToast: 'OTP page saved',
  otpResetToast: 'Notepad reset (7 pages)',
  otpEmpty: 'All pages burned. Reset the pad or create a new page.',
};

export function servicesT(): ServicesStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}
