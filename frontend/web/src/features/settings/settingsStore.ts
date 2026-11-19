import { kvStore } from '../../core/storage/kvStore';

export interface SettingsPrefs {
  hideOnlineStatus: boolean;
  blockScreenshots: boolean;
  passcodeEnabled: boolean;
  biometricLock: boolean;
  pushNotifications: boolean;
  inAppSounds: boolean;
  autoDownloadMedia: boolean;
  animationsEnabled: boolean;
  messageAnimations: boolean;
  powerSavingMode: boolean;
  voiceModule: boolean;
  translationModule: boolean;
}

const KEY_PREFS = 'settings.prefs.v1';

const DEFAULT_PREFS: SettingsPrefs = {
  hideOnlineStatus: true,
  blockScreenshots: false,
  passcodeEnabled: false,
  biometricLock: false,
  pushNotifications: true,
  inAppSounds: true,
  autoDownloadMedia: true,
  animationsEnabled: true,
  messageAnimations: true,
  powerSavingMode: false,
  voiceModule: true,
  translationModule: true,
};

class SettingsStore {
  private prefs: SettingsPrefs;
  private listeners = new Set<() => void>();

  constructor() {
    this.prefs = { ...DEFAULT_PREFS, ...kvStore.getJson<Partial<SettingsPrefs>>(KEY_PREFS, {}) };
  }

  get all(): SettingsPrefs {
    return this.prefs;
  }

  toggle(key: keyof SettingsPrefs): void {
    this.prefs = { ...this.prefs, [key]: !this.prefs[key] };
    kvStore.setJson(KEY_PREFS, this.prefs);
    for (const fn of this.listeners) fn();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }
}

export const settingsStore = new SettingsStore();
