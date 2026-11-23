import { i18nStore } from '../../../../core/i18n/i18nStore';

const RU = {
  close: 'Закрыть',
  prev: 'Назад',
  next: 'Далее',
  zoomIn: 'Увеличить',
  zoomOut: 'Уменьшить',
  zoomReset: 'Масштаб 1x / 2x',
  download: 'Скачать',
  file: 'Файл',
  forwardAnon: 'Переслать анонимно',
  copyText: 'Копировать',
  copied: 'Скопировано',
  previewUnavailable: 'Предпросмотр недоступен для этого типа файла',
  unknownSize: 'Размер не указан',
  videoCircle: 'Видеосообщение',
  tapToPlay: 'Нажмите для воспроизведения',
};

const EN: typeof RU = {
  close: 'Close',
  prev: 'Previous',
  next: 'Next',
  zoomIn: 'Zoom in',
  zoomOut: 'Zoom out',
  zoomReset: 'Toggle 1x / 2x zoom',
  download: 'Download',
  file: 'File',
  forwardAnon: 'Forward anonymously',
  copyText: 'Copy',
  copied: 'Copied',
  previewUnavailable: 'Preview unavailable for this file type',
  unknownSize: 'Unknown size',
  videoCircle: 'Video message',
  tapToPlay: 'Tap to play',
};

export const ViewerI18n = {
  get current(): typeof RU {
    return i18nStore.language === 'ru' ? RU : EN;
  },
};
