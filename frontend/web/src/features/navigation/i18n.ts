import { i18nStore } from '../../core/i18n/i18nStore';

export interface NavigationStrings {
  chats: string;
  services: string;
  settings: string;
  profile: string;
  qrHub: string;
  selectChatHint: string;
  savedMessages: string;
  archiveChats: string;
  contacts: string;
  calls: string;
  nightMode: string;
  footerVersion: string;
  onionOnline: string;
  onionOffline: string;
  onionStatusTitle: string;
  defaultDisplayName: string;
}

const RU: NavigationStrings = {
  chats: 'Чаты',
  services: 'Сервисы',
  settings: 'Настройки',
  profile: 'Профиль',
  qrHub: 'Stealth QR',
  selectChatHint: 'Выберите чат, чтобы начать общение',
  savedMessages: 'Избранное',
  archiveChats: 'Архив чатов',
  contacts: 'Контакты',
  calls: 'Звонки',
  nightMode: 'Ночной режим',
  footerVersion: 'AnonGram Web • Onion v3',
  onionOnline: 'Onion v3',
  onionOffline: 'Офлайн',
  onionStatusTitle: 'Статус ретранслятора Onion v3',
  defaultDisplayName: 'Ghost',
};

const EN: NavigationStrings = {
  chats: 'Chats',
  services: 'Services',
  settings: 'Settings',
  profile: 'Profile',
  qrHub: 'Stealth QR',
  selectChatHint: 'Select a chat to start messaging',
  savedMessages: 'Saved Messages',
  archiveChats: 'Archived Chats',
  contacts: 'Contacts',
  calls: 'Calls',
  nightMode: 'Night Mode',
  footerVersion: 'AnonGram Web • Onion v3',
  onionOnline: 'Onion v3',
  onionOffline: 'Offline',
  onionStatusTitle: 'Onion v3 Relay Status',
  defaultDisplayName: 'Ghost',
};

export const NavigationI18n = {
  get current(): NavigationStrings {
    return i18nStore.language === 'en' ? EN : RU;
  },
};
