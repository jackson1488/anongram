import { i18nStore } from '../../../../core/i18n/i18nStore';
import { kvStore } from '../../../../core/storage/kvStore';
import { clear, el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { chatStore } from '../../chatStore';
import { chatScreenT } from '../../i18n';
import type { ChatMessage } from '../../models';

const KEY_PINNED = 'chats.pinned_messages.v1';

type PinnedListener = () => void;

class PinnedMessageStore {
  private pinnedMap: Record<string, string>;
  private listeners = new Set<PinnedListener>();

  constructor() {
    this.pinnedMap = kvStore.getJson<Record<string, string>>(KEY_PINNED, {});
  }

  getPinnedId(chatId: string): string | null {
    return this.pinnedMap[chatId] ?? null;
  }

  getPinnedMessage(chatId: string): ChatMessage | null {
    const msgId = this.getPinnedId(chatId);
    if (!msgId) return null;
    return chatStore.getMessages(chatId).find((m) => m.id === msgId) ?? null;
  }

  isPinned(chatId: string, messageId: string): boolean {
    return this.pinnedMap[chatId] === messageId;
  }

  pin(chatId: string, messageId: string): void {
    if (this.pinnedMap[chatId] === messageId) return;
    this.pinnedMap = { ...this.pinnedMap, [chatId]: messageId };
    this.persist();
    this.emit();
  }

  unpin(chatId: string): void {
    if (!(chatId in this.pinnedMap)) return;
    const next = { ...this.pinnedMap };
    delete next[chatId];
    this.pinnedMap = next;
    this.persist();
    this.emit();
  }

  /** Toggles pin state for the given message; returns true if pinned, false if unpinned. */
  togglePin(chatId: string, messageId: string): boolean {
    if (this.isPinned(chatId, messageId)) {
      this.unpin(chatId);
      return false;
    }
    this.pin(chatId, messageId);
    return true;
  }

  subscribe(fn: PinnedListener): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private persist(): void {
    kvStore.setJson(KEY_PINNED, this.pinnedMap);
  }

  private emit(): void {
    for (const fn of this.listeners) fn();
  }
}

export const pinnedMessageStore = new PinnedMessageStore();

function formatPinnedSnippet(msg: ChatMessage): string {
  const text = msg.text.trim();
  if (text) return text;
  if (msg.attachment?.title) return msg.attachment.title;
  return msg.senderName;
}

/**
 * Reactive pinned message strip for the top of ChatScreen.
 * Automatically updates on pin/unpin/delete/i18n changes and hides when empty.
 */
export function createPinnedMessageBar(
  chatId: string,
  onJumpToMessage: (messageId: string) => void,
): HTMLElement {
  const root = el('div', { class: 'ag-pinned-bar', style: 'display:none;' });

  const render = () => {
    clear(root);
    const msg = pinnedMessageStore.getPinnedMessage(chatId);
    if (!msg) {
      root.style.display = 'none';
      return;
    }
    root.style.display = 'flex';
    const cs = chatScreenT();
    const body = el(
      'div',
      {
        class: 'ag-pinned-bar__body',
        onClick: () => onJumpToMessage(msg.id),
      },
      el('strong', { class: 'ag-pinned-bar__title' }, cs.pinnedMessage),
      el('span', { class: 'ag-pinned-bar__snippet' }, formatPinnedSnippet(msg)),
    );

    const unpinBtn = el(
      'button',
      {
        class: 'ag-icon-btn ag-pinned-bar__unpin',
        title: cs.unpinMessage,
        onClick: (e: Event) => {
          e.stopPropagation();
          pinnedMessageStore.unpin(chatId);
        },
      },
      icons.close(16),
    );

    root.append(
      el('span', { class: 'ag-pinned-bar__ico', onClick: () => onJumpToMessage(msg.id) }, icons.pin(16)),
      body,
      unpinBtn,
    );
  };

  render();
  const u1 = pinnedMessageStore.subscribe(render);
  const u2 = chatStore.subscribe(render);
  const u3 = i18nStore.subscribe(render);

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
    u3();
  };

  return root;
}
