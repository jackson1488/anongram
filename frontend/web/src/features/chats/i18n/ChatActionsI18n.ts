import { i18nStore } from '../../../core/i18n/i18nStore';

export interface ChatActionsStrings {
  reply: string;
  edit: string;
  translate: string;
  showOriginal: string;
  translated: string;
  translating: string;
  edited: string;
  transcribe: string;
  transcribing: string;
  transcribeShort: string;
  copy: string;
  pin: string;
  unpin: string;
  forward: string;
  select: string;
  delete: string;
  cancel: string;
  videoCircle: string;
  deleteSingleTitle: string;
  deleteMultipleTitle: (count: number) => string;
  deleteSinglePrompt: string;
  deleteMultiplePrompt: string;
  deleteForEveryone: string;
  deleteForPeer: (peerName: string) => string;
  deleteConfirm: string;
}

function formatRuDeleteMultipleTitle(count: number): string {
  const mod100 = count % 100;
  const mod10 = count % 10;
  if (mod100 >= 11 && mod100 <= 14) return `Удалить ${count} сообщений`;
  if (mod10 === 1) return `Удалить ${count} сообщение`;
  if (mod10 >= 2 && mod10 <= 4) return `Удалить ${count} сообщения`;
  return `Удалить ${count} сообщений`;
}

const RU: ChatActionsStrings = {
  reply: 'Ответить',
  edit: 'Редактировать',
  translate: 'Перевести',
  showOriginal: 'Показать оригинал',
  translated: 'Переведено',
  translating: 'Перевод…',
  edited: 'изм.',
  transcribe: 'Расшифровать',
  transcribing: 'Расшифровка…',
  transcribeShort: 'A→',
  copy: 'Копировать',
  pin: 'Закрепить',
  unpin: 'Открепить',
  forward: 'Переслать анонимно',
  select: 'Выбрать',
  delete: 'Удалить',
  cancel: 'Отмена',
  videoCircle: 'Видеокружок',
  deleteSingleTitle: 'Удалить сообщение',
  deleteMultipleTitle: formatRuDeleteMultipleTitle,
  deleteSinglePrompt: 'Вы точно хотите удалить это сообщение?',
  deleteMultiplePrompt: 'Вы точно хотите удалить эти сообщения?',
  deleteForEveryone: 'Также удалить для всех',
  deleteForPeer: (peer) => `Также удалить для ${peer}`,
  deleteConfirm: 'Удалить',
};

const EN: ChatActionsStrings = {
  reply: 'Reply',
  edit: 'Edit',
  translate: 'Translate',
  showOriginal: 'Show original',
  translated: 'Translated',
  translating: 'Translating…',
  edited: 'edited',
  transcribe: 'Transcribe',
  transcribing: 'Transcribing…',
  transcribeShort: 'A→',
  copy: 'Copy',
  pin: 'Pin',
  unpin: 'Unpin',
  forward: 'Forward anonymously',
  select: 'Select',
  delete: 'Delete',
  cancel: 'Cancel',
  videoCircle: 'Video note',
  deleteSingleTitle: 'Delete message',
  deleteMultipleTitle: (count) => (count === 1 ? 'Delete message' : `Delete ${count} messages`),
  deleteSinglePrompt: 'Are you sure you want to delete this message?',
  deleteMultiplePrompt: 'Are you sure you want to delete these messages?',
  deleteForEveryone: 'Also delete for everyone',
  deleteForPeer: (peer) => `Also delete for ${peer}`,
  deleteConfirm: 'Delete',
};

export function chatActionsT(): ChatActionsStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}

export const ChatActionsI18n = {
  get current(): ChatActionsStrings {
    return chatActionsT();
  },
};
