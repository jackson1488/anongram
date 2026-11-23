import { kvStore } from '../../core/storage/kvStore';

export interface CallLogItem {
  id: string;
  peerName: string;
  avatarEmoji: string;
  direction: 'incoming' | 'outgoing' | 'missed';
  isVideo: boolean;
  timestampMs: number;
  durationSec: number;
}

const KEY_CALL_LOGS = 'calls.history.v1';

const now = Date.now();
const INITIAL_CALL_LOGS: CallLogItem[] = [
  {
    id: 'c1',
    peerName: 'Ghost Node #42',
    avatarEmoji: '🕶️',
    direction: 'outgoing',
    isVideo: false,
    timestampMs: now - 2 * 3600_000,
    durationSec: 252,
  },
  {
    id: 'c2',
    peerName: 'Maya Cipher',
    avatarEmoji: '⚡',
    direction: 'missed',
    isVideo: true,
    timestampMs: now - 26 * 3600_000,
    durationSec: 0,
  },
  {
    id: 'c3',
    peerName: 'AnonGram Core',
    avatarEmoji: '🛡️',
    direction: 'incoming',
    isVideo: false,
    timestampMs: now - 72 * 3600_000,
    durationSec: 665,
  },
];

export function formatDuration(sec: number): string {
  if (sec <= 0) return '';
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
}

class CallsStore {
  private logs: CallLogItem[];
  private listeners = new Set<() => void>();

  constructor() {
    const saved = kvStore.getJson<CallLogItem[] | null>(KEY_CALL_LOGS, null);
    this.logs = saved && Array.isArray(saved) ? saved : [...INITIAL_CALL_LOGS];
  }

  private persist(): void {
    kvStore.setJson(KEY_CALL_LOGS, this.logs);
    for (const fn of this.listeners) fn();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  getLogs(): CallLogItem[] {
    return this.logs;
  }

  addCall(item: Omit<CallLogItem, 'id'>): void {
    const entry: CallLogItem = {
      ...item,
      id: `call_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 6)}`,
    };
    this.logs = [entry, ...this.logs];
    this.persist();
  }

  deleteCall(id: string): void {
    this.logs = this.logs.filter((c) => c.id !== id);
    this.persist();
  }

  clearAll(): void {
    this.logs = [];
    this.persist();
  }
}

export const callsStore = new CallsStore();
