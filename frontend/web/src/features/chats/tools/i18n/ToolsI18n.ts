import { i18nStore } from '../../../../core/i18n/i18nStore';

export interface ToolsI18nStrings {
  back: string;
  cancel: string;
  clear: string;
  close: string;
  done: string;
  forwardTitle: string;
  selectChatTitle: string;
  anonymousForwardBadge: string;
  anonymousForwardHint: string;
  addCommentPlaceholder: string;
  searchChatsPlaceholder: string;
  savedMessagesTitle: string;
  savedMessagesSubtitle: string;
  nothingFound: string;
  previewButton: (count: number) => string;
  sendButton: (count: number) => string;
  selectedTargetsCount: (count: number) => string;
  pendingForwardPreviewTitle: string;
  pendingForwardCancelAll: string;
  messageFallback: string;
  forwardedToast: (count: number) => string;
}

const RU: ToolsI18nStrings = {
  back: 'Назад',
  cancel: 'Отмена',
  clear: 'Очистить',
  close: 'Закрыть',
  done: 'Готово',
  forwardTitle: 'Переслать...',
  selectChatTitle: 'Выберите чаты',
  anonymousForwardBadge: '100% анонимная пересылка',
  anonymousForwardHint: 'Имя отправителя и источник полностью удаляются',
  addCommentPlaceholder: 'Добавить сообщение...',
  searchChatsPlaceholder: 'Поиск чатов',
  savedMessagesTitle: 'Избранное',
  savedMessagesSubtitle: 'Сохранить на память',
  nothingFound: 'Чаты не найдены',
  previewButton: (n) => `Предпросмотр (${n})`,
  sendButton: (n) => (n > 1 ? `Отправить (${n})` : 'Отправить'),
  selectedTargetsCount: (n) => `Выбрано: ${n}`,
  pendingForwardPreviewTitle: 'Пересылаемые сообщения',
  pendingForwardCancelAll: 'Отменить все',
  messageFallback: 'Сообщение',
  forwardedToast: (n) => (n > 1 ? `Переслано анонимно: ${n}` : 'Переслано анонимно'),
};

const EN: ToolsI18nStrings = {
  back: 'Back',
  cancel: 'Cancel',
  clear: 'Clear',
  close: 'Close',
  done: 'Done',
  forwardTitle: 'Forward...',
  selectChatTitle: 'Select chats',
  anonymousForwardBadge: '100% anonymous forward',
  anonymousForwardHint: 'Original sender and source metadata are stripped',
  addCommentPlaceholder: 'Add a message...',
  searchChatsPlaceholder: 'Search chats',
  savedMessagesTitle: 'Saved Messages',
  savedMessagesSubtitle: 'Save to memory',
  nothingFound: 'No chats found',
  previewButton: (n) => `Preview (${n})`,
  sendButton: (n) => (n > 1 ? `Send (${n})` : 'Send'),
  selectedTargetsCount: (n) => `${n} selected`,
  pendingForwardPreviewTitle: 'Forwarded messages',
  pendingForwardCancelAll: 'Cancel all',
  messageFallback: 'Message',
  forwardedToast: (n) => (n > 1 ? `Forwarded anonymously: ${n}` : 'Forwarded anonymously'),
};

export function toolsT(): ToolsI18nStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}
