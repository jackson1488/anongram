import { i18nStore } from '../../../core/i18n/i18nStore';

export interface PlayerStrings {
  nowPlaying: string;
  queue: string;
  shuffle: string;
  repeat: string;
  close: string;
  next: string;
  prev: string;
  play: string;
  pause: string;
  speed: string;
  emptyQueue: string;
  musicInChat: string;
}

const RU: PlayerStrings = {
  nowPlaying: 'Сейчас играет',
  queue: 'Очередь',
  shuffle: 'Перемешать',
  repeat: 'Повтор',
  close: 'Закрыть',
  next: 'Следующий',
  prev: 'Предыдущий',
  play: 'Воспроизвести',
  pause: 'Пауза',
  speed: 'Скорость',
  emptyQueue: 'Нет треков',
  musicInChat: 'Музыка в чате',
};

const EN: PlayerStrings = {
  nowPlaying: 'Now playing',
  queue: 'Queue',
  shuffle: 'Shuffle',
  repeat: 'Repeat',
  close: 'Close',
  next: 'Next',
  prev: 'Previous',
  play: 'Play',
  pause: 'Pause',
  speed: 'Speed',
  emptyQueue: 'No tracks',
  musicInChat: 'Music in chat',
};

export const PlayerI18n = {
  get current(): PlayerStrings {
    return i18nStore.language === 'ru' ? RU : EN;
  },
};
