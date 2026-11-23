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
