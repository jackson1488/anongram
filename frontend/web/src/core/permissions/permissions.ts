export type PermState = 'granted' | 'denied' | 'prompt' | 'unsupported';

async function query(name: string): Promise<PermState> {
  try {
    if (!navigator.permissions) return 'prompt';
    const r = await navigator.permissions.query({ name: name as PermissionName });
    return r.state as PermState;
  } catch {
    return 'prompt';
  }
}

export function notificationState(): PermState {
  if (typeof Notification === 'undefined') return 'unsupported';
  const p = Notification.permission;
  return p === 'default' ? 'prompt' : p;
}
export const hasNotificationPermission = (): boolean => notificationState() === 'granted';
export async function requestNotificationPermission(): Promise<boolean> {
  if (typeof Notification === 'undefined') return false;
  if (Notification.permission === 'granted') return true;
  if (Notification.permission === 'denied') return false;
  return (await Notification.requestPermission()) === 'granted';
}

export const cameraState = (): Promise<PermState> => query('camera');
export const microphoneState = (): Promise<PermState> => query('microphone');
export const hasCameraPermission = async (): Promise<boolean> => (await cameraState()) === 'granted';
export const hasMicrophonePermission = async (): Promise<boolean> => (await microphoneState()) === 'granted';

async function requestMedia(constraints: MediaStreamConstraints): Promise<boolean> {
  if (!navigator.mediaDevices?.getUserMedia) return false;
  try {
    const stream = await navigator.mediaDevices.getUserMedia(constraints);
    stream.getTracks().forEach((t) => t.stop());
    return true;
  } catch {
    return false;
  }
}
export const requestCameraPermission = (): Promise<boolean> => requestMedia({ video: true });
export const requestMicrophonePermission = (): Promise<boolean> => requestMedia({ audio: true });

export async function geolocationState(): Promise<PermState> {
  return 'geolocation' in navigator ? query('geolocation') : 'unsupported';
}
export function requestGeolocationPermission(): Promise<boolean> {
  return new Promise((resolve) => {
    if (!('geolocation' in navigator)) return resolve(false);
    navigator.geolocation.getCurrentPosition(() => resolve(true), () => resolve(false));
  });
}
