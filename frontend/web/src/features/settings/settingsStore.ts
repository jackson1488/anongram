import type { TransitionType } from '../../core/navigation/TransitionModels';
import { kvStore } from '../../core/storage/kvStore';

export type FontFamilyOption = 'system' | 'inter' | 'mono' | 'serif' | 'rounded';
export type AnimationSpeedOption = '0.5x' | '1.0x' | '1.5x' | '2.0x';
export type VoiceEngineOption = 'whisper' | 'vosk' | 'system';
export type AutoLockOption = 'off' | '1m' | '5m' | '1h';

export interface DeviceSession {
  id: string;
  deviceName: string;
  platform: string;
  location: string;
  lastActive: string;
  isCurrent?: boolean;
}

export interface SettingsPrefs {
  hideOnlineStatus: boolean;
  blockScreenshots: boolean;
  passcodeEnabled: boolean;
  biometricLock: boolean;
  twoFactorEnabled: boolean;
  autoLockTimeout: AutoLockOption;
  passcodeHash: string;
  pushNotifications: boolean;
  inAppSounds: boolean;
  autoDownloadMedia: boolean;
  autoDownloadPhotos: boolean;
  autoDownloadVoice: boolean;
  animationsEnabled: boolean;
  messageAnimations: boolean;
  screenTransitionsEnabled: boolean;
  springPhysicsEnabled: boolean;
  reduceMotion: boolean;
  animationSpeed: AnimationSpeedOption;
  transitionType: TransitionType;
  powerSavingMode: boolean;
  voiceModule: boolean;
  translationModule: boolean;
  localAiModule: boolean;
  showTranslateButton: boolean;
  autoTranslateIncoming: boolean;
  voiceEngine: VoiceEngineOption;
  fontFamily: FontFamilyOption;
  mediaCacheMb: number;
}

const KEY_PREFS = 'settings.prefs.v1';
const KEY_SESSIONS = 'settings.sessions.v1';

const FONT_STACKS: Record<FontFamilyOption, string> = {
  system: '-apple-system, BlinkMacSystemFont, "SF Pro Text", "Inter", "Segoe UI", Roboto, sans-serif',
  inter: '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  mono: '"JetBrains Mono", "Fira Code", ui-monospace, SFMono-Regular, Menlo, monospace',
  serif: 'Georgia, "Times New Roman", Cambria, serif',
  rounded: '"SF Pro Rounded", "Nunito", ui-rounded, system-ui, sans-serif',
};

const SPEED_SCALES: Record<AnimationSpeedOption, string> = {
  '0.5x': '1.8',
  '1.0x': '1',
  '1.5x': '0.7',
  '2.0x': '0.5',
};

const DEFAULT_PREFS: SettingsPrefs = {
  hideOnlineStatus: true,
  blockScreenshots: false,
  passcodeEnabled: false,
  biometricLock: false,
  twoFactorEnabled: false,
  autoLockTimeout: '1m',
  passcodeHash: '',
  pushNotifications: true,
  inAppSounds: true,
  autoDownloadMedia: true,
  autoDownloadPhotos: true,
  autoDownloadVoice: true,
  animationsEnabled: true,
  messageAnimations: true,
  screenTransitionsEnabled: true,
  springPhysicsEnabled: true,
  reduceMotion: false,
  animationSpeed: '1.0x',
  transitionType: 'SPRING_SWIPE',
  powerSavingMode: false,
  voiceModule: true,
  translationModule: true,
  localAiModule: false,
  showTranslateButton: true,
  autoTranslateIncoming: false,
  voiceEngine: 'whisper',
  fontFamily: 'system',
  mediaCacheMb: 38.4,
};

function detectCurrentSession(): DeviceSession {
  const ua = typeof navigator !== 'undefined' ? navigator.userAgent : '';
  let browser = 'Web Browser';
  if (ua.includes('Firefox/')) browser = 'Firefox';
  else if (ua.includes('Edg/')) browser = 'Edge';
  else if (ua.includes('Chrome/')) browser = 'Chrome';
  else if (ua.includes('Safari/')) browser = 'Safari';

  let os = 'Desktop';
  if (ua.includes('Android')) os = 'Android';
  else if (ua.includes('iPhone') || ua.includes('iPad')) os = 'iOS';
  else if (ua.includes('Mac OS')) os = 'macOS';
  else if (ua.includes('Windows')) os = 'Windows';
  else if (ua.includes('Linux')) os = 'Linux';

  return {
    id: 'current-web',
    deviceName: `AnonGram Web (${browser})`,
    platform: `${os} • WebCrypto E2EE`,
    location: 'Local Session',
    lastActive: 'online',
    isCurrent: true,
  };
}

const DEFAULT_OTHER_SESSIONS: DeviceSession[] = [
  {
    id: 'android-primary',
    deviceName: 'AnonGram Android',
    platform: 'Android 14 • PQC X25519',
    location: 'Trusted Device',
    lastActive: '2h',
  },
];

function simpleHash(pin: string): string {
  let h = 0x811c9dc5;
  for (let i = 0; i < pin.length; i++) {
    h ^= pin.charCodeAt(i);
    h = Math.imul(h, 0x01000193);
  }
  return `pin_${(h >>> 0).toString(16)}`;
}

function applyRuntimePrefs(prefs: SettingsPrefs): void {
  if (typeof document === 'undefined') return;
  const root = document.documentElement.style;
  const stack = FONT_STACKS[prefs.fontFamily] ?? FONT_STACKS.system;
  root.setProperty('--ag-font-family', stack);
  document.body.style.fontFamily = stack;

  const animActive = prefs.animationsEnabled && !prefs.reduceMotion;
  root.setProperty('--ag-anim-scale', animActive ? (SPEED_SCALES[prefs.animationSpeed] ?? '1') : '0');
  document.documentElement.classList.toggle('ag-reduce-motion', !animActive);
}

class SettingsStore {
  private prefs: SettingsPrefs;
  private sessionList: DeviceSession[];
  private listeners = new Set<() => void>();

  constructor() {
    this.prefs = { ...DEFAULT_PREFS, ...kvStore.getJson<Partial<SettingsPrefs>>(KEY_PREFS, {}) };
    const savedSessions = kvStore.getJson<DeviceSession[] | null>(KEY_SESSIONS, null);
    this.sessionList = savedSessions ?? [detectCurrentSession(), ...DEFAULT_OTHER_SESSIONS];
    applyRuntimePrefs(this.prefs);
  }

  get all(): SettingsPrefs {
    return this.prefs;
  }

  get sessions(): DeviceSession[] {
    const cur = detectCurrentSession();
    return [cur, ...this.sessionList.filter((s) => !s.isCurrent)];
  }

  private saveAndEmit(): void {
    kvStore.setJson(KEY_PREFS, this.prefs);
    applyRuntimePrefs(this.prefs);
    for (const fn of this.listeners) fn();
  }

  toggle(key: { [K in keyof SettingsPrefs]: SettingsPrefs[K] extends boolean ? K : never }[keyof SettingsPrefs]): void {
    this.prefs = { ...this.prefs, [key]: !this.prefs[key] };
    this.saveAndEmit();
  }

  set<K extends keyof SettingsPrefs>(key: K, value: SettingsPrefs[K]): void {
    this.prefs = { ...this.prefs, [key]: value };
    this.saveAndEmit();
  }

  setPasscode(pin: string): void {
    this.prefs = {
      ...this.prefs,
      passcodeEnabled: true,
      passcodeHash: simpleHash(pin),
    };
    this.saveAndEmit();
  }

  verifyPasscode(pin: string): boolean {
    if (!this.prefs.passcodeHash) return false;
    return this.prefs.passcodeHash === simpleHash(pin);
  }

  disablePasscode(): void {
    this.prefs = {
      ...this.prefs,
      passcodeEnabled: false,
      passcodeHash: '',
    };
    this.saveAndEmit();
  }

  clearMediaCache(): void {
    try {
      if (typeof caches !== 'undefined') {
        void caches.keys().then((keys) => Promise.all(keys.map((k) => caches.delete(k))));
      }
    } catch {
      // ignore sandbox cache restrictions
    }
    this.prefs = { ...this.prefs, mediaCacheMb: 0 };
    this.saveAndEmit();
  }

  terminateSession(id: string): void {
    this.sessionList = this.sessionList.filter((s) => s.id !== id || s.isCurrent);
    kvStore.setJson(KEY_SESSIONS, this.sessionList);
    for (const fn of this.listeners) fn();
  }

  terminateOtherSessions(): void {
    this.sessionList = [detectCurrentSession()];
    kvStore.setJson(KEY_SESSIONS, this.sessionList);
    for (const fn of this.listeners) fn();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }
}

export const settingsStore = new SettingsStore();
