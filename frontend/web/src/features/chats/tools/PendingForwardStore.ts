import type { ChatMessage } from '../models';

export interface PendingForwardItem {
  id: string;
  message: ChatMessage;
}

export interface PendingForwardState {
  items: PendingForwardItem[];
  excludedIds: Set<string>;
  comment: string;
}

type PendingForwardListener = (state: PendingForwardState | null) => void;

class PendingForwardStore {
  private current: PendingForwardState | null = null;
  private listeners = new Set<PendingForwardListener>();

  getState(): PendingForwardState | null {
    return this.current
      ? {
          items: [...this.current.items],
          excludedIds: new Set(this.current.excludedIds),
          comment: this.current.comment,
        }
      : null;
  }

  getActiveMessages(): ChatMessage[] {
    if (!this.current) return [];
    const { items, excludedIds } = this.current;
    return items.filter((it) => !excludedIds.has(it.id)).map((it) => it.message);
  }

  start(messages: ChatMessage[], comment = ''): void {
    if (messages.length === 0) return;
    this.current = {
      items: messages.map((m) => ({
        id: m.id,
        // AnonGram Core Protocol: strip sender attribution on pending forward copy
        message: {
          ...m,
          senderName: '',
          replyTo: undefined,
        },
      })),
      excludedIds: new Set(),
      comment,
    };
    this.emit();
  }

  setComment(comment: string): void {
    if (!this.current) return;
    this.current = { ...this.current, comment };
    this.emit();
  }

  /** Toggles inclusion of a message; returns true if all messages are now excluded (cancelled). */
  toggleInclude(id: string): boolean {
    if (!this.current) return false;
    const nextExcluded = new Set(this.current.excludedIds);
    if (nextExcluded.has(id)) nextExcluded.delete(id);
    else nextExcluded.add(id);

    const remaining = this.current.items.filter((it) => !nextExcluded.has(it.id));
    if (remaining.length === 0) {
      this.current = null;
      this.emit();
      return true;
    }
    this.current = { ...this.current, excludedIds: nextExcluded };
    this.emit();
    return false;
  }

  removeItem(id: string): boolean {
    if (!this.current) return false;
    const nextItems = this.current.items.filter((it) => it.id !== id);
    const nextExcluded = new Set(this.current.excludedIds);
    nextExcluded.delete(id);
    const remaining = nextItems.filter((it) => !nextExcluded.has(it.id));
    if (remaining.length === 0) {
      this.current = null;
      this.emit();
      return true;
    }
    this.current = { ...this.current, items: nextItems, excludedIds: nextExcluded };
    this.emit();
    return false;
  }

  clear(): void {
    if (!this.current) return;
    this.current = null;
    this.emit();
  }

  subscribe(fn: PendingForwardListener): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private emit(): void {
    const snap = this.getState();
    for (const fn of this.listeners) fn(snap);
  }
}

export const pendingForwardStore = new PendingForwardStore();
