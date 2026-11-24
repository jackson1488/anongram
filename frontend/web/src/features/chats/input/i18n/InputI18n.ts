import { i18nStore, registerI18nModule } from '../../../../core/i18n/i18nStore';

export interface InputI18nStrings {
  voiceRecording: string;
  voiceMessage: string;
  slideToCancel: string;
  cancel: string;
  cancelUpper: string;
  holdToRecord: string;
  tapToToggleCamera: string;
  videoCircleRecording: string;
  videoCircle: string;
  micPermissionFallback: string;
  cameraPermissionFallback: string;
  cameraPreview: string;
  recordingLock: string;
  recordingLocked: string;
  unlockRecording: string;
  switchCamera: string;
  flash: string;
  send: string;
  stop: string;
  micDesc: string;
  videoRoundDesc: string;
  recordingDurationLimit: string;
}

const RU: InputI18nStrings = {
  voiceRecording: 'Запись голосового',
  voiceMessage: 'Голосовое сообщение',
  slideToCancel: '‹ Влево — отмена',
  cancel: 'Отмена',
  cancelUpper: 'ОТМЕНА',
  holdToRecord: 'Удерживайте для записи',
  tapToToggleCamera: 'Тап — камера, удержание — запись',
  videoCircleRecording: 'Запись видео-кружка',
  videoCircle: 'Видеосообщение',
  micPermissionFallback: 'Микрофон недоступен — режим таймера',
  cameraPermissionFallback: 'Камера недоступна — симуляция превью',
  cameraPreview: 'Превью камеры',
  recordingLock: 'Заблокировать запись',
  recordingLocked: 'Запись закреплена',
  unlockRecording: 'Разблокировать',
  switchCamera: 'Сменить камеру',
  flash: 'Вспышка',
  send: 'Отправить',
  stop: 'Остановить',
  micDesc: 'Микрофон (тап = камера)',
  videoRoundDesc: 'Видео-кружок (тап = микрофон)',
  recordingDurationLimit: 'Макс. 1:00',
};

const EN: InputI18nStrings = {
  voiceRecording: 'Voice recording',
  voiceMessage: 'Voice message',
  slideToCancel: '« Slide left to cancel',
  cancel: 'Cancel',
  cancelUpper: 'CANCEL',
  holdToRecord: 'Hold to record',
  tapToToggleCamera: 'Tap for camera, hold to record',
  videoCircleRecording: 'Video note recording',
  videoCircle: 'Video note',
  micPermissionFallback: 'Microphone unavailable — fallback timer mode',
  cameraPermissionFallback: 'Camera unavailable — simulated preview',
  cameraPreview: 'Camera preview',
  recordingLock: 'Lock recording',
  recordingLocked: 'Recording locked',
  unlockRecording: 'Unlock',
  switchCamera: 'Switch camera',
  flash: 'Flash',
  send: 'Send',
  stop: 'Stop',
  micDesc: 'Microphone (tap = camera)',
  videoRoundDesc: 'Video note (tap = mic)',
  recordingDurationLimit: 'Max 1:00',
};

registerI18nModule('input', {
  ru: { ...RU },
  en: { ...EN },
});

export function inputT(): InputI18nStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}

export const InputI18n = {
  get current(): InputI18nStrings {
    return inputT();
  },
};
