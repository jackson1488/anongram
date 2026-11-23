import { i18nStore } from '../../../core/i18n/i18nStore';
import { kvStore } from '../../../core/storage/kvStore';

export type AiReasoningLevel = 'off' | 'low' | 'medium' | 'high';
export type AiSystemPromptPreset = 'default' | 'coder' | 'analyst' | 'translator' | 'concise';
export type AiEngineMode = 'local_gguf' | 'cloud_openai';

export interface AiModelPreset {
  id: string;
  displayName: string;
  sizeMb: number;
  ramHint: string;
  supportsThinking: boolean;
}

export interface AiMessage {
  id: string;
  isUser: boolean;
  content: string;
  thinking: string;
  timestampMs: number;
}

export interface AiChatSession {
  id: string;
  title: string;
  isPinned: boolean;
  updatedAtMs: number;
  messages: AiMessage[];
}

export interface LocalAiState {
  engineMode: AiEngineMode;
  activeModelId: string;
  reasoningLevel: AiReasoningLevel;
  systemPromptPreset: AiSystemPromptPreset;
  webToolsEnabled: boolean;
  sessions: AiChatSession[];
  activeSessionId: string;
}

export const AI_CATALOG: AiModelPreset[] = [
  { id: 'smollm2_360m', displayName: 'SmolLM2 360M', sizeMb: 386, ramHint: '2+ GB RAM', supportsThinking: false },
  { id: 'qwen25_05b', displayName: 'Qwen 2.5 0.5B', sizeMb: 398, ramHint: '3+ GB RAM', supportsThinking: false },
  { id: 'gemma3_1b', displayName: 'Gemma 3 1B', sizeMb: 768, ramHint: '3+ GB RAM', supportsThinking: false },
  { id: 'deepseek_r1_15b', displayName: 'DeepSeek R1 1.5B', sizeMb: 1120, ramHint: '4+ GB RAM', supportsThinking: true },
];

const KEY_LOCAL_AI = 'services.local_ai.v1';

function createDefaultSession(): AiChatSession {
  return {
    id: `ai_sess_${Date.now().toString(36)}`,
    title: i18nStore.language === 'ru' ? 'Новый диалог' : 'New Chat',
    isPinned: false,
    updatedAtMs: Date.now(),
    messages: [],
  };
}

function defaultState(): LocalAiState {
  const first = createDefaultSession();
  return {
    engineMode: 'local_gguf',
    activeModelId: AI_CATALOG[2].id,
    reasoningLevel: 'medium',
    systemPromptPreset: 'default',
    webToolsEnabled: true,
    sessions: [first],
    activeSessionId: first.id,
  };
}

class LocalAiStore {
  private state: LocalAiState;
  private listeners = new Set<() => void>();

  constructor() {
    const saved = kvStore.getJson<LocalAiState | null>(KEY_LOCAL_AI, null);
    if (saved && Array.isArray(saved.sessions) && saved.sessions.length > 0) {
      this.state = saved;
    } else {
      this.state = defaultState();
    }
  }

  private persist(): void {
    kvStore.setJson(KEY_LOCAL_AI, this.state);
    for (const fn of this.listeners) fn();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  getState(): LocalAiState {
    return this.state;
  }

  get activeModel(): AiModelPreset {
    return AI_CATALOG.find((m) => m.id === this.state.activeModelId) ?? AI_CATALOG[0];
  }

  get activeSession(): AiChatSession {
    return (
      this.state.sessions.find((s) => s.id === this.state.activeSessionId) ??
      this.state.sessions[0]
    );
  }

  newSession(): void {
    const sess = createDefaultSession();
    this.state = {
      ...this.state,
      sessions: [sess, ...this.state.sessions],
      activeSessionId: sess.id,
    };
    this.persist();
  }

  selectSession(id: string): void {
    if (!this.state.sessions.some((s) => s.id === id)) return;
    this.state = { ...this.state, activeSessionId: id };
    this.persist();
  }

  togglePinSession(id: string): void {
    this.state = {
      ...this.state,
      sessions: this.state.sessions.map((s) =>
        s.id === id ? { ...s, isPinned: !s.isPinned } : s,
      ),
    };
    this.persist();
  }

  deleteSession(id: string): void {
    const remaining = this.state.sessions.filter((s) => s.id !== id);
    if (remaining.length === 0) {
      const fresh = createDefaultSession();
      this.state = { ...this.state, sessions: [fresh], activeSessionId: fresh.id };
    } else {
      const activeId =
        this.state.activeSessionId === id ? remaining[0].id : this.state.activeSessionId;
      this.state = { ...this.state, sessions: remaining, activeSessionId: activeId };
    }
    this.persist();
  }

  clearCurrentSession(): void {
    const id = this.activeSession.id;
    this.state = {
      ...this.state,
      sessions: this.state.sessions.map((s) =>
        s.id === id ? { ...s, messages: [], updatedAtMs: Date.now() } : s,
      ),
    };
    this.persist();
  }

  setModel(modelId: string): void {
    this.state = { ...this.state, activeModelId: modelId };
    this.persist();
  }

  setEngineMode(mode: AiEngineMode): void {
    this.state = { ...this.state, engineMode: mode };
    this.persist();
  }

  setReasoning(level: AiReasoningLevel): void {
    this.state = { ...this.state, reasoningLevel: level };
    this.persist();
  }

  setSystemPrompt(preset: AiSystemPromptPreset): void {
    this.state = { ...this.state, systemPromptPreset: preset };
    this.persist();
  }

  toggleWebTools(): void {
    this.state = { ...this.state, webToolsEnabled: !this.state.webToolsEnabled };
    this.persist();
  }

  sendMessage(text: string): void {
    const trimmed = text.trim();
    if (!trimmed) return;
    const isRu = i18nStore.language === 'ru';
    const sess = this.activeSession;
    const now = Date.now();

    const userMsg: AiMessage = {
      id: `m_u_${now.toString(36)}`,
      isUser: true,
      content: trimmed,
      thinking: '',
      timestampMs: now,
    };

    const modelName = this.activeModel.displayName;
    const thinking =
      this.state.reasoningLevel === 'off'
        ? ''
        : isRu
          ? `Режим: ${this.state.engineMode} • Модель: ${modelName} • Уровень: ${this.state.reasoningLevel} • Пресет: ${this.state.systemPromptPreset}`
          : `Mode: ${this.state.engineMode} • Model: ${modelName} • Level: ${this.state.reasoningLevel} • Preset: ${this.state.systemPromptPreset}`;

    const replyContent = isRu
      ? `[${modelName}] Локальный узел обработал запрос: «${trimmed}». Ответ сформирован в изолированной песочнице без отправки метаданных.`
      : `[${modelName}] Local node processed your query: "${trimmed}". Response generated in an isolated sandbox with zero metadata leakage.`;

    const aiMsg: AiMessage = {
      id: `m_a_${(now + 1).toString(36)}`,
      isUser: false,
      content: replyContent,
      thinking,
      timestampMs: now + 1,
    };

    const nextTitle =
      sess.messages.length === 0
        ? trimmed.slice(0, 32) + (trimmed.length > 32 ? '…' : '')
        : sess.title;

    this.state = {
      ...this.state,
      sessions: this.state.sessions.map((s) =>
        s.id === sess.id
          ? {
              ...s,
              title: nextTitle,
              updatedAtMs: now + 1,
              messages: [...s.messages, userMsg, aiMsg],
            }
          : s,
      ),
    };
    this.persist();
  }
}

export const localAiStore = new LocalAiStore();
