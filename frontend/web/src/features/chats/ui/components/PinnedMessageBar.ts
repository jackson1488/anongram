import { i18nStore } from '../../../../core/i18n/i18nStore';
import { kvStore } from '../../../../core/storage/kvStore';
import { clear, el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { showToast } from '../../../../core/ui/toast';
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
  const att = msg.attachment;
  if (att) {
    const cs = chatScreenT();
    if (att.type === 'voice') return att.title.trim() || cs.voiceMessage;
    if (att.type === 'video_circle') return att.title.trim() || cs.videoCircle;
    if (att.title.trim()) return att.title.trim();
  }
  return msg.senderName;
}

function scrollPinnedFallback(root: HTMLElement, messageId: string): void {
  const room = root.closest('.ag-chat-room') ?? document;
  const target = room.querySelector<HTMLElement>(`[data-msg-id="${CSS.escape(messageId)}"]`);
  if (!target) return;
  target.scrollIntoView({ behavior: 'smooth', block: 'center' });
  target.classList.remove('is-flash');
  void target.offsetWidth;
  target.classList.add('is-flash');
  window.setTimeout(() => target.classList.remove('is-flash'), 1200);
}

/**
 * Reactive pinned message strip for the top of ChatScreen.
 * Automatically updates on pin/unpin/delete/i18n changes and hides when empty.
 */
export function createPinnedMessageBar(
  chatId: string,
  onJumpToMessage?: (messageId: string) => void,
  onUnpin?: () => void,
): HTMLElement {
  const root = el('div', { class: 'ag-pinned-bar', style: 'display:none;' });
  let currentMsgId: string | null = null;

  const jumpToCurrent = () => {
    if (!currentMsgId) return;
    if (onJumpToMessage) {
      onJumpToMessage(currentMsgId);
    } else {
      scrollPinnedFallback(root, currentMsgId);
    }
  };

  root.addEventListener('click', () => {
    jumpToCurrent();
  });

  const render = () => {
    clear(root);
    const msg = pinnedMessageStore.getPinnedMessage(chatId);
    if (!msg) {
      currentMsgId = null;
      root.style.display = 'none';
      return;
    }
    currentMsgId = msg.id;
    root.style.display = 'flex';
    const cs = chatScreenT();

    const body = el(
      'div',
      {
        class: 'ag-pinned-bar__body',
        role: 'button',
        tabindex: 0,
        title: cs.jumpToPinned,
        'aria-label': cs.jumpToPinned,
        onClick: (e: Event) => {
          e.stopPropagation();
          jumpToCurrent();
        },
        onKeyDown: (e: Event) => {
          const ke = e as KeyboardEvent;
          if (ke.key === 'Enter' || ke.key === ' ') {
            ke.preventDefault();
            jumpToCurrent();
          }
        },
      },
      el('strong', { class: 'ag-pinned-bar__title' }, cs.pinnedMessage),
      el('span', { class: 'ag-pinned-bar__snippet' }, formatPinnedSnippet(msg)),
    );

    const unpinBtn = el(
      'button',
      {
        type: 'button',
        class: 'ag-icon-btn ag-pinned-bar__unpin',
        title: cs.unpinMessage,
        'aria-label': cs.unpinMessage,
        onClick: (e: Event) => {
          e.stopPropagation();
          pinnedMessageStore.unpin(chatId);
          showToast(cs.unpinnedToast);
          onUnpin?.();
        },
      },
      tgIcons.pinOff(16),
    );

    root.append(
      el(
        'span',
        {
          class: 'ag-pinned-bar__ico',
          title: cs.jumpToPinned,
          onClick: (e: Event) => {
            e.stopPropagation();
            jumpToCurrent();
          },
        },
        icons.pin(16),
      ),
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
