import { i18nStore } from '../../core/i18n/i18nStore';

export interface ChatScreenStrings {
  today: string;
  yesterday: string;
  select: string;
  selectedCount: (n: number) => string;
  forwardedToast: (n: number) => string;
  deletedToast: (n: number) => string;
  scrollDown: string;
  cancel: string;
  more: string;
  edited: string;
  pinMessage: string;
  unpinMessage: string;
  pinnedMessage: string;
  emojiSearchPlaceholder: string;
  emojiEmpty: string;
  emojiRecent: string;
  emojiSmileys: string;
  emojiGestures: string;
  emojiHearts: string;
  emojiStickers: string;
  blockedBannerText: string;
  unblockUser: string;
  voiceMessage: string;
  voicePlaying: string;
  voicePaused: string;
  voiceRecordedToast: string;
  videoCircle: string;
  videoCircleRecordedToast: string;
  pinnedToast: string;
  unpinnedToast: string;
  jumpToPinned: string;
  linksTitle: string;
  openLink: string;
  linkCopiedToast: string;
  emptyLinks: string;
  externalLinkWarning: string;
}

const RU: ChatScreenStrings = {
  today: 'Сегодня',
  yesterday: 'Вчера',
  select: 'Выбрать',
  selectedCount: (n) => `Выбрано: ${n}`,
  forwardedToast: (n) => (n > 1 ? `Переслано анонимно: ${n}` : 'Переслано анонимно'),
  deletedToast: (n) => `Удалено: ${n}`,
  scrollDown: 'Вниз',
  cancel: 'Отмена',
  more: 'Ещё',
  edited: 'изм.',
  pinMessage: 'Закрепить',
  unpinMessage: 'Открепить',
  pinnedMessage: 'Закреплённое сообщение',
  emojiSearchPlaceholder: 'Поиск эмодзи...',
  emojiEmpty: 'Эмодзи не найдены',
  emojiRecent: 'Недавние',
  emojiSmileys: 'Смайлы',
  emojiGestures: 'Жесты',
  emojiHearts: 'Сердца',
  emojiStickers: 'Стикеры и каомодзи',
  blockedBannerText: 'Вы заблокировали этого пользователя',
  unblockUser: 'Разблокировать',
  voiceMessage: 'Голосовое сообщение',
  voicePlaying: 'Воспроизведение',
  voicePaused: 'Пауза',
  voiceRecordedToast: 'Голосовое сообщение отправлено',
  videoCircle: 'Видеокружок',
  videoCircleRecordedToast: 'Видеокружок отправлен',
  pinnedToast: 'Сообщение закреплено',
  unpinnedToast: 'Сообщение откреплено',
  jumpToPinned: 'Перейти к закреплённому сообщению',
  linksTitle: 'Ссылки',
  openLink: 'Открыть ссылку',
  linkCopiedToast: 'Ссылка скопирована',
  emptyLinks: 'Ссылок пока нет',
  externalLinkWarning: 'Переход по внешней ссылке',
};

const EN: ChatScreenStrings = {
  today: 'Today',
  yesterday: 'Yesterday',
  select: 'Select',
  selectedCount: (n) => `${n} selected`,
  forwardedToast: (n) => (n > 1 ? `Forwarded anonymously: ${n}` : 'Forwarded anonymously'),
  deletedToast: (n) => `Deleted: ${n}`,
  scrollDown: 'Scroll down',
  cancel: 'Cancel',
  more: 'More',
  edited: 'edited',
  pinMessage: 'Pin',
  unpinMessage: 'Unpin',
  pinnedMessage: 'Pinned message',
  emojiSearchPlaceholder: 'Search emoji...',
  emojiEmpty: 'No emojis found',
  emojiRecent: 'Recent',
  emojiSmileys: 'Smileys',
  emojiGestures: 'Gestures',
  emojiHearts: 'Hearts',
  emojiStickers: 'Stickers & Kaomoji',
  blockedBannerText: 'You blocked this user',
  unblockUser: 'Unblock',
  voiceMessage: 'Voice message',
  voicePlaying: 'Playing',
  voicePaused: 'Paused',
  voiceRecordedToast: 'Voice message sent',
  videoCircle: 'Video note',
  videoCircleRecordedToast: 'Video note sent',
  pinnedToast: 'Message pinned',
  unpinnedToast: 'Message unpinned',
  jumpToPinned: 'Jump to pinned message',
  linksTitle: 'Links',
  openLink: 'Open link',
  linkCopiedToast: 'Link copied',
  emptyLinks: 'No links shared yet',
  externalLinkWarning: 'External link warning',
};

export function chatScreenT(): ChatScreenStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}

export function epochDateLabel(epochMs: number): string {
  const s = chatScreenT();
  const d = new Date(epochMs);
  const now = new Date();
  const dayStart = (x: Date) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime();
  const diff = Math.round((dayStart(now) - dayStart(d)) / 86400000);
  if (diff === 0) return s.today;
  if (diff === 1) return s.yesterday;
  const opts: Intl.DateTimeFormatOptions =
    d.getFullYear() === now.getFullYear()
      ? { day: 'numeric', month: 'long' }
      : { day: 'numeric', month: 'long', year: 'numeric' };
  return new Intl.DateTimeFormat(i18nStore.language, opts).format(d);
}
