import { i18nStore } from '../../../core/i18n/i18nStore';

const RU = { title: 'Обои чата', none: 'Без обоев', upload: 'Загрузить фото', reset: 'Сбросить', close: 'Закрыть', preset: 'Тема' };
const EN = { title: 'Chat wallpaper', none: 'No wallpaper', upload: 'Upload photo', reset: 'Reset', close: 'Close', preset: 'Theme' };

export const WallpaperI18n = {
  get current(): typeof RU {
    return i18nStore.language === 'ru' ? RU : EN;
  },
};
