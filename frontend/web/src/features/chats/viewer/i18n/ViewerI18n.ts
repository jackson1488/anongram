import { i18nStore } from '../../../../core/i18n/i18nStore';

const RU = { close: 'Закрыть', prev: 'Назад', next: 'Далее', zoomIn: 'Увеличить', zoomOut: 'Уменьшить', download: 'Скачать', file: 'Файл' };
const EN = { close: 'Close', prev: 'Previous', next: 'Next', zoomIn: 'Zoom in', zoomOut: 'Zoom out', download: 'Download', file: 'File' };

export const ViewerI18n = {
  get current(): typeof RU {
    return i18nStore.language === 'ru' ? RU : EN;
  },
};
