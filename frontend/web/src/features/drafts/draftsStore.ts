import { kvStore } from '../../core/storage/kvStore';

export interface ChatDraft {
  chatId: string;
  text: string;
  replyToMessageId?: string;
  replySnippet?: string;
  updatedAtEpochMs: number;
}

const KEY_DRAFTS = 'chats.drafts.v1';

class DraftsStore {
  private map: Record<string, ChatDraft>;
  private listeners = new Set<() => void>();

  constructor() {
    this.map = kvStore.getJson<Record<string, ChatDraft>>(KEY_DRAFTS, {});
  }

  getDraft(chatId: string): ChatDraft | undefined {
    const d = this.map[chatId];
    if (!d || (!d.text.trim() && !d.replyToMessageId)) return undefined;
    return d;
  }

  setDraft(chatId: string, text: string, replyToMessageId?: string, replySnippet?: string): void {
    const clean = text;
    if (!clean.trim() && !replyToMessageId) {
      if (this.map[chatId]) {
        delete this.map[chatId];
        this.persist();
      }
      return;
    }
    this.map[chatId] = {
      chatId,
      text: clean,
      replyToMessageId,
      replySnippet,
      updatedAtEpochMs: Date.now(),
    };
    this.persist();
  }

  clearDraft(chatId: string): void {
    if (!this.map[chatId]) return;
    delete this.map[chatId];
    this.persist();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private persist(): void {
    kvStore.setJson(KEY_DRAFTS, this.map);
    for (const fn of this.listeners) fn();
  }
}

export const draftsStore = new DraftsStore();
