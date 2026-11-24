import type { TransitionType } from '../../core/navigation/TransitionModels';
import { kvStore } from '../../core/storage/kvStore';

export type FontFamilyOption =
  | 'system'
  | 'inter'
  | 'mono'
  | 'roboto'
  | 'sfPro'
  | 'comfortaa'
  | 'serif'
  | 'rounded';
export type FontApplyScope = 'all' | 'messages' | 'ui';
export type AnimationSpeedOption = '0.5x' | '1.0x' | '1.5x' | '2.0x';
export type VoiceEngineOption = 'whisper' | 'vosk' | 'system';
export type VoiceWhisperModelId = 'whisper_tiny_q5' | 'whisper_base_q5';
export type VoiceVoskPackId = 'vosk_ru' | 'vosk_en';
export type VoiceLanguageOption = 'auto' | 'ru' | 'en';
export type VoiceSystemLanguageOption = 'ru-RU' | 'en-US';
export type TargetTranslateLanguageOption = 'auto' | 'ru' | 'en';
export type AutoLockOption = 'off' | '1m' | '5m' | '1h';

export interface VoicePackSpec {
  id: string;
  engine: 'whisper' | 'vosk';
  languageCode: string;
  sizeMb: number;
}

export const VOICE_PACK_SPECS: VoicePackSpec[] = [
  { id: 'whisper_tiny_q5', engine: 'whisper', languageCode: 'multi', sizeMb: 32 },
  { id: 'whisper_base_q5', engine: 'whisper', languageCode: 'multi', sizeMb: 60 },
  { id: 'vosk_ru', engine: 'vosk', languageCode: 'ru', sizeMb: 45 },
  { id: 'vosk_en', engine: 'vosk', languageCode: 'en', sizeMb: 40 },
];

export interface LocalAiModelSpec {
  id: string;
  displayName: string;
  repoId: string;
  filename: string;
  sizeMb: number;
  ramHint: string;
  category: 'qwen' | 'llama' | 'gemma' | 'deepseek' | 'other';
  supportsThinking?: boolean;
}

export const LOCAL_AI_MODEL_SPECS: LocalAiModelSpec[] = [
  {
    id: 'qwen25_15b',
    displayName: 'Qwen 2.5 1.5B',
    repoId: 'Qwen/Qwen2.5-1.5B-Instruct-GGUF',
    filename: 'qwen2.5-1.5b-instruct-q4_k_m.gguf',
    sizeMb: 1120,
    ramHint: '6+ GB RAM',
    category: 'qwen',
  },
  {
    id: 'llama32_1b',
    displayName: 'Llama 3.2 1B',
    repoId: 'bartowski/Llama-3.2-1B-Instruct-GGUF',
    filename: 'Llama-3.2-1B-Instruct-Q4_K_M.gguf',
    sizeMb: 808,
    ramHint: '4+ GB RAM',
    category: 'llama',
  },
  {
    id: 'gemma2_2b',
    displayName: 'Gemma 2 2B',
    repoId: 'bartowski/gemma-2-2b-it-GGUF',
    filename: 'gemma-2-2b-it-Q4_K_M.gguf',
    sizeMb: 1710,
    ramHint: '6+ GB RAM',
    category: 'gemma',
  },
  {
    id: 'deepseek_r1_15b',
    displayName: 'DeepSeek R1 1.5B',
    repoId: 'bartowski/DeepSeek-R1-Distill-Qwen-1.5B-GGUF',
    filename: 'DeepSeek-R1-Distill-Qwen-1.5B-Q4_0.gguf',
    sizeMb: 1019,
    ramHint: '4+ GB RAM',
    category: 'deepseek',
    supportsThinking: true,
  },
  {
    id: 'smollm2_360m',
    displayName: 'SmolLM2 360M',
    repoId: 'HuggingFaceTB/SmolLM2-360M-Instruct-GGUF',
    filename: 'smollm2-360m-instruct-q8_0.gguf',
    sizeMb: 386,
    ramHint: '2+ GB RAM',
    category: 'other',
  },
];

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
  targetTranslateLanguage: TargetTranslateLanguageOption;
  voiceEngine: VoiceEngineOption;
  voiceWhisperModel: VoiceWhisperModelId;
  voiceVoskPack: VoiceVoskPackId;
  voiceLanguage: VoiceLanguageOption;
  voiceSystemLang: VoiceSystemLanguageOption;
  autoTranscribeVoice: boolean;
  installedVoicePacks: string[];
  installedAiModels: string[];
  activeAiModelId: string | null;
  fontFamily: FontFamilyOption;
  fontApplyScope: FontApplyScope;
  mediaCacheMb: number;
}

const KEY_PREFS = 'settings.prefs.v1';
const KEY_SESSIONS = 'settings.sessions.v1';

export const FONT_STACKS: Record<FontFamilyOption, string> = {
  system: '-apple-system, BlinkMacSystemFont, "SF Pro Text", "Inter", "Segoe UI", Roboto, sans-serif',
  inter: '"Inter", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif',
  mono: '"JetBrains Mono", "Fira Code", ui-monospace, SFMono-Regular, Menlo, monospace',
  roboto: '"Roboto", -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif',
  sfPro: '"SF Pro Text", "SF Pro Display", -apple-system, BlinkMacSystemFont, "Inter", sans-serif',
  comfortaa: '"Comfortaa", "Nunito", ui-rounded, system-ui, sans-serif',
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
  targetTranslateLanguage: 'auto',
  voiceEngine: 'whisper',
  voiceWhisperModel: 'whisper_tiny_q5',
  voiceVoskPack: 'vosk_ru',
  voiceLanguage: 'auto',
  voiceSystemLang: 'ru-RU',
  autoTranscribeVoice: false,
  installedVoicePacks: [],
  installedAiModels: [],
  activeAiModelId: null,
  fontFamily: 'system',
  fontApplyScope: 'all',
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
  root.setProperty('--ag-msg-font-family', prefs.fontApplyScope === 'ui' ? FONT_STACKS.system : stack);
  document.body.style.fontFamily = prefs.fontApplyScope === 'messages' ? FONT_STACKS.system : stack;

  const animActive = prefs.animationsEnabled && !prefs.reduceMotion;
  root.setProperty('--ag-anim-scale', animActive ? (SPEED_SCALES[prefs.animationSpeed] ?? '1') : '0');
  document.documentElement.classList.toggle('ag-reduce-motion', !animActive);
}

class SettingsStore {
  private prefs: SettingsPrefs;
  private sessionList: DeviceSession[];
  private listeners = new Set<() => void>();

  private downloadingVoicePackId: string | null = null;
  private voiceDownloadPct = 0;
  private voiceTimer: number | null = null;

  private downloadingAiModelId: string | null = null;
  private aiDownloadPct = 0;
  private aiTimer: number | null = null;

  constructor() {
    const saved = kvStore.getJson<Partial<SettingsPrefs>>(KEY_PREFS, {});
    this.prefs = {
      ...DEFAULT_PREFS,
      ...saved,
      installedVoicePacks: Array.isArray(saved.installedVoicePacks) ? saved.installedVoicePacks : [],
      installedAiModels: Array.isArray(saved.installedAiModels) ? saved.installedAiModels : [],
    };
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

  get voiceDownloadState(): { downloadingId: string | null; progress: number } {
    return { downloadingId: this.downloadingVoicePackId, progress: this.voiceDownloadPct };
  }

  get aiDownloadState(): { downloadingId: string | null; progress: number } {
    return { downloadingId: this.downloadingAiModelId, progress: this.aiDownloadPct };
  }

  get installedVoiceMb(): number {
    return VOICE_PACK_SPECS.filter((p) => this.prefs.installedVoicePacks.includes(p.id)).reduce(
      (sum, p) => sum + p.sizeMb,
      0,
    );
  }

  get installedAiMb(): number {
    return LOCAL_AI_MODEL_SPECS.filter((m) => this.prefs.installedAiModels.includes(m.id)).reduce(
      (sum, m) => sum + m.sizeMb,
      0,
    );
  }

  get activeAiModel(): LocalAiModelSpec | null {
    if (!this.prefs.activeAiModelId) return null;
    return LOCAL_AI_MODEL_SPECS.find((m) => m.id === this.prefs.activeAiModelId) ?? null;
  }

  private emit(): void {
    for (const fn of this.listeners) fn();
  }

  private saveAndEmit(): void {
    kvStore.setJson(KEY_PREFS, this.prefs);
    applyRuntimePrefs(this.prefs);
    this.emit();
  }

  toggle(key: { [K in keyof SettingsPrefs]: SettingsPrefs[K] extends boolean ? K : never }[keyof SettingsPrefs]): void {
    this.prefs = { ...this.prefs, [key]: !this.prefs[key] };
    this.saveAndEmit();
  }

  set<K extends keyof SettingsPrefs>(key: K, value: SettingsPrefs[K]): void {
    this.prefs = { ...this.prefs, [key]: value };
    this.saveAndEmit();
  }

  downloadVoicePack(packId: string): void {
    if (this.downloadingVoicePackId) return;
    const spec = VOICE_PACK_SPECS.find((p) => p.id === packId);
    if (!spec || this.prefs.installedVoicePacks.includes(packId)) return;

    this.downloadingVoicePackId = packId;
    this.voiceDownloadPct = 4;
    this.emit();

    this.voiceTimer = window.setInterval(() => {
      this.voiceDownloadPct = Math.min(100, this.voiceDownloadPct + 12);
      if (this.voiceDownloadPct >= 100) {
        if (this.voiceTimer !== null) window.clearInterval(this.voiceTimer);
        this.voiceTimer = null;
        this.downloadingVoicePackId = null;
        this.voiceDownloadPct = 0;

        const nextInstalled = Array.from(new Set([...this.prefs.installedVoicePacks, packId]));
        const nextPrefs: SettingsPrefs = { ...this.prefs, installedVoicePacks: nextInstalled };
        if (spec.engine === 'whisper' && (packId === 'whisper_tiny_q5' || packId === 'whisper_base_q5')) {
          nextPrefs.voiceWhisperModel = packId;
        } else if (spec.engine === 'vosk' && (packId === 'vosk_ru' || packId === 'vosk_en')) {
          nextPrefs.voiceVoskPack = packId;
        }
        this.prefs = nextPrefs;
        this.saveAndEmit();
      } else {
        this.emit();
      }
    }, 90);
  }

  cancelVoiceDownload(): void {
    if (this.voiceTimer !== null) window.clearInterval(this.voiceTimer);
    this.voiceTimer = null;
    this.downloadingVoicePackId = null;
    this.voiceDownloadPct = 0;
    this.emit();
  }

  removeVoicePack(packId: string): void {
    this.prefs = {
      ...this.prefs,
      installedVoicePacks: this.prefs.installedVoicePacks.filter((id) => id !== packId),
    };
    this.saveAndEmit();
  }

  downloadAiModel(modelId: string): void {
    if (this.downloadingAiModelId) return;
    const spec = LOCAL_AI_MODEL_SPECS.find((m) => m.id === modelId);
    if (!spec || this.prefs.installedAiModels.includes(modelId)) return;

    this.downloadingAiModelId = modelId;
    this.aiDownloadPct = 5;
    this.emit();

    this.aiTimer = window.setInterval(() => {
      this.aiDownloadPct = Math.min(100, this.aiDownloadPct + 10);
      if (this.aiDownloadPct >= 100) {
        if (this.aiTimer !== null) window.clearInterval(this.aiTimer);
        this.aiTimer = null;
        this.downloadingAiModelId = null;
        this.aiDownloadPct = 0;

        const nextInstalled = Array.from(new Set([...this.prefs.installedAiModels, modelId]));
        this.prefs = {
          ...this.prefs,
          installedAiModels: nextInstalled,
          activeAiModelId: modelId,
          localAiModule: true,
        };
        this.saveAndEmit();
      } else {
        this.emit();
      }
    }, 100);
  }

  cancelAiDownload(): void {
    if (this.aiTimer !== null) window.clearInterval(this.aiTimer);
    this.aiTimer = null;
    this.downloadingAiModelId = null;
    this.aiDownloadPct = 0;
    this.emit();
  }

  selectAiModel(modelId: string): void {
    if (!this.prefs.installedAiModels.includes(modelId)) return;
    this.prefs = { ...this.prefs, activeAiModelId: modelId, localAiModule: true };
    this.saveAndEmit();
  }

  removeAiModel(modelId: string): void {
    const remaining = this.prefs.installedAiModels.filter((id) => id !== modelId);
    const nextActive =
      this.prefs.activeAiModelId === modelId ? (remaining[0] ?? null) : this.prefs.activeAiModelId;
    this.prefs = {
      ...this.prefs,
      installedAiModels: remaining,
      activeAiModelId: nextActive,
      localAiModule: remaining.length > 0,
    };
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
    this.emit();
  }

  terminateOtherSessions(): void {
    this.sessionList = [detectCurrentSession()];
    kvStore.setJson(KEY_SESSIONS, this.sessionList);
    this.emit();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }
}

export const settingsStore = new SettingsStore();
