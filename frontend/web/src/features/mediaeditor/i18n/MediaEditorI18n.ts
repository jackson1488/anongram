import { i18nStore } from '../../../core/i18n/i18nStore';

const RU = { title: 'Редактор', crop: 'Обрезка', draw: 'Рисование', undo: 'Отменить', done: 'Готово', cancel: 'Отмена' };
const EN = { title: 'Editor', crop: 'Crop', draw: 'Draw', undo: 'Undo', done: 'Done', cancel: 'Cancel' };

export const MediaEditorI18n = {
  get current(): typeof RU {
    return i18nStore.language === 'ru' ? RU : EN;
  },
};
