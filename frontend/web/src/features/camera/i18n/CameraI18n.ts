import { i18nStore } from '../../../core/i18n/i18nStore';

const RU = { title: 'Камера', capture: 'Снять', close: 'Закрыть', flip: 'Сменить камеру', denied: 'Нет доступа к камере' };
const EN = { title: 'Camera', capture: 'Capture', close: 'Close', flip: 'Flip camera', denied: 'Camera access denied' };

export const CameraI18n = {
  get current(): typeof RU {
    return i18nStore.language === 'ru' ? RU : EN;
  },
};
