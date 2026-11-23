import { i18nStore } from '../../core/i18n/i18nStore';

export interface QrStrings {
  title: string;
  tabMyQr: string;
  tabScan: string;
  showCodeHint: string;
  copyStealthId: string;
  copiedToast: string;
  myContactsTitle: string;
  myContactsSub: string;
  manualPlaceholder: string;
  addBtn: string;
  addedToast: (name: string) => string;
  cameraStarting: string;
  cameraUnavailable: string;
  cameraPermissionHint: string;
  scanHint: string;
  scanDetectedToast: (payload: string) => string;
  retryCamera: string;
}

const RU: QrStrings = {
  title: 'AnonGram QR ID',
  tabMyQr: 'Мой QR',
  tabScan: 'Сканировать',
  showCodeHint: 'Покажите код для добавления контакта',
  copyStealthId: 'Скопировать мой Stealth ID',
  copiedToast: 'Stealth ID скопирован',
  myContactsTitle: 'Мои контакты',
  myContactsSub: 'Список и SAS-верификация',
  manualPlaceholder: 'Вставьте Public ID или ag://...',
  addBtn: 'Добавить',
  addedToast: (name: string) => `Контакт добавлен: ${name}`,
  cameraStarting: 'Запуск камеры…',
  cameraUnavailable: 'Камера недоступна в текущем браузере или контексте.',
  cameraPermissionHint: 'Разрешите доступ к камере или введите Public ID вручную ниже.',
  scanHint: 'Наведите камеру на QR-код AnonGram',
  scanDetectedToast: (payload: string) => `QR распознан: ${payload}`,
  retryCamera: 'Повторить запуск камеры',
};

const EN: QrStrings = {
  title: 'AnonGram QR ID',
  tabMyQr: 'My QR',
  tabScan: 'Scan QR',
  showCodeHint: 'Show this code to add you as a contact',
  copyStealthId: 'Copy my Stealth ID',
  copiedToast: 'Stealth ID copied',
  myContactsTitle: 'My Contacts',
  myContactsSub: 'Address book & SAS verification',
  manualPlaceholder: 'Paste Public ID or ag://...',
  addBtn: 'Add',
  addedToast: (name: string) => `Contact added: ${name}`,
  cameraStarting: 'Starting camera…',
  cameraUnavailable: 'Camera is unavailable in this browser or context.',
  cameraPermissionHint: 'Allow camera access or enter a Public ID manually below.',
  scanHint: 'Point the camera at an AnonGram QR code',
  scanDetectedToast: (payload: string) => `QR detected: ${payload}`,
  retryCamera: 'Retry camera',
};

export function qrT(): QrStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}
