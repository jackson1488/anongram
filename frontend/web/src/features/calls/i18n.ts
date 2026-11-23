import { i18nStore } from '../../core/i18n/i18nStore';

export interface CallsStrings {
  title: string;
  all: string;
  missed: string;
  incoming: string;
  outgoing: string;
  missedLabel: string;
  startAudioCall: string;
  startVideoCall: string;
  calling: string;
  connected: string;
  encryptedCall: string;
  sasCodeLabel: string;
  endCall: string;
  mute: string;
  unmute: string;
  speaker: string;
  cameraOn: string;
  cameraOff: string;
  clearHistory: string;
  clearedToast: string;
  emptyCalls: string;
  searchPlaceholder: string;
  today: string;
  yesterday: string;
}

const RU: CallsStrings = {
  title: 'Звонки',
  all: 'Все',
  missed: 'Пропущенные',
  incoming: 'Входящий',
  outgoing: 'Исходящий',
  missedLabel: 'Пропущенный',
  startAudioCall: 'Аудиозвонок',
  startVideoCall: 'Видеозвонок',
  calling: 'Соединение…',
  connected: 'Зашифровано (E2EE)',
  encryptedCall: 'E2EE P2P-канал',
  sasCodeLabel: 'SAS-код',
  endCall: 'Завершить',
  mute: 'Микрофон',
  unmute: 'Вкл. микр.',
  speaker: 'Динамик',
  cameraOn: 'Камера вкл.',
  cameraOff: 'Камера выкл.',
  clearHistory: 'Очистить',
  clearedToast: 'Журнал звонков очищен',
  emptyCalls: 'Журнал звонков пуст',
  searchPlaceholder: 'Поиск по звонкам...',
  today: 'Сегодня',
  yesterday: 'Вчера',
};

const EN: CallsStrings = {
  title: 'Calls',
  all: 'All',
  missed: 'Missed',
  incoming: 'Incoming',
  outgoing: 'Outgoing',
  missedLabel: 'Missed',
  startAudioCall: 'Audio call',
  startVideoCall: 'Video call',
  calling: 'Connecting…',
  connected: 'Encrypted (E2EE)',
  encryptedCall: 'E2EE P2P Channel',
  sasCodeLabel: 'SAS code',
  endCall: 'End call',
  mute: 'Mute',
  unmute: 'Unmute',
  speaker: 'Speaker',
  cameraOn: 'Camera on',
  cameraOff: 'Camera off',
  clearHistory: 'Clear',
  clearedToast: 'Call history cleared',
  emptyCalls: 'No call history',
  searchPlaceholder: 'Search calls...',
  today: 'Today',
  yesterday: 'Yesterday',
};

export function callsT(): CallsStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}
