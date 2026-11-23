import { i18nStore } from '../../../core/i18n/i18nStore';

export interface MainMenuStrings {
  newMessage: string;
  menu: string;
  archive: string;
  archiveEmpty: string;
  restore: string;
  draft: string;
  draftReply: string;
  onionOffline: string;
  otp: string;
  searchChats: string;
  searchEmpty: string;
  searchTitle: string;
  searchHint: string;
  searchChatsSection: string;
  searchMessagesSection: string;
  noChats: string;
  noChatsHint: string;
  pin: string;
  unpin: string;
  toArchive: string;
  savedMessages: string;
}

const RU: MainMenuStrings = {
  newMessage: 'Новое сообщение',
  menu: 'Меню',
  archive: 'Архив',
  archiveEmpty: 'Архив пуст',
  restore: 'Вернуть',
  draft: 'Черновик: ',
  draftReply: 'В ответ на сообщение',
  onionOffline: 'Onion-сеть недоступна — исходящие ждут в очереди шифрования',
  otp: 'OTP',
  searchChats: 'Поиск',
  searchEmpty: 'Ничего не найдено',
  searchTitle: 'Поиск',
  searchHint: 'Введите имя или текст сообщения',
  searchChatsSection: 'Чаты',
  searchMessagesSection: 'Сообщения',
  noChats: 'Нет чатов',
  noChatsHint: 'Нажмите на кнопку, чтобы начать новый чат',
  pin: 'Закрепить',
  unpin: 'Открепить',
  toArchive: 'В архив',
  savedMessages: 'Избранное',
};

const EN: MainMenuStrings = {
  newMessage: 'New message',
  menu: 'Menu',
  archive: 'Archive',
  archiveEmpty: 'Archive is empty',
  restore: 'Unarchive',
  draft: 'Draft: ',
  draftReply: 'Replying to a message',
  onionOffline: 'Onion network unavailable — outgoing messages are queued for encryption',
  otp: 'OTP',
  searchChats: 'Search',
  searchEmpty: 'Nothing found',
  searchTitle: 'Search',
  searchHint: 'Enter a name or message text',
  searchChatsSection: 'Chats',
  searchMessagesSection: 'Messages',
  noChats: 'No chats',
  noChatsHint: 'Tap the button to start a new chat',
  pin: 'Pin',
  unpin: 'Unpin',
  toArchive: 'Archive',
  savedMessages: 'Saved messages',
};

export const mainMenuT = (): MainMenuStrings => (i18nStore.language === 'ru' ? RU : EN);
