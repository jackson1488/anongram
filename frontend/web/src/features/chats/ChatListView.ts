import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { chatStore } from './chatStore';
import type { ChatFolder } from './models';

export interface ChatListCallbacks {
  getActiveChatId: () => string | null;
  onSelectChat: (chatId: string) => void;
  focusSearchSignal?: (focusFn: () => void) => void;
}

export function createChatListView(callbacks: ChatListCallbacks): HTMLElement {
  const root = el('div', { class: 'ag-list-pane' });
  let query = '';
  let folder: ChatFolder = 'all';
  let searchInputRef: HTMLInputElement | null = null;

  const render = () => {
    clear(root);
    const t = i18nStore.t;
    const activeId = callbacks.getActiveChatId();

    const searchInput = el('input', {
      type: 'text',
      placeholder: t.chats.searchPlaceholder,
      value: query,
      onInput: (e: Event) => {
        query = (e.target as HTMLInputElement).value;
        renderListOnly();
      },
    }) as HTMLInputElement;
    searchInputRef = searchInput;

    root.appendChild(
      el(
        'div',
        { class: 'ag-pane-header' },
        el('h1', { class: 'ag-pane-title' }, t.chats.title),
      ),
    );

    root.appendChild(
      el(
        'div',
        { class: 'ag-search-wrap' },
        el(
          'div',
          { class: 'ag-search-pill' },
          icons.search(16),
          searchInput,
          query
            ? el(
                'button',
                {
                  class: 'ag-icon-btn',
                  style: 'width:28px;height:28px',
                  onClick: () => {
                    query = '';
                    render();
                  },
                },
                icons.close(14),
              )
            : null,
        ),
      ),
    );

    const folders: Array<{ id: ChatFolder; label: string }> = [
      { id: 'all', label: t.chats.folderAll },
      { id: 'unread', label: t.chats.folderUnread },
      { id: 'pinned', label: t.chats.folderPinned },
    ];

    root.appendChild(
      el(
        'div',
        { class: 'ag-chips-row' },
        ...folders.map((f) =>
          el(
            'button',
            {
              class: `ag-chip ${folder === f.id ? 'is-active' : ''}`,
              onClick: () => {
                folder = f.id;
                render();
              },
            },
            f.label,
          ),
        ),
      ),
    );

    const listWrap = el('div', { class: 'ag-scroll-list' });
    root.appendChild(listWrap);

    const renderListOnly = () => {
      clear(listWrap);
      const q = query.trim().toLowerCase();
      const threads = chatStore.threads.filter((thread) => {
        if (folder === 'unread' && thread.unreadCount === 0) return false;
        if (folder === 'pinned' && !thread.isPinned) return false;
        if (!q) return true;
        const last = chatStore.getLastMessage(thread.id);
        return (
          thread.title.toLowerCase().includes(q) ||
          (last?.text.toLowerCase().includes(q) ?? false)
        );
      });

      if (threads.length === 0) {
        listWrap.appendChild(el('div', { class: 'ag-empty-inline' }, t.chats.emptySearch));
        return;
      }

      for (const thread of threads) {
        const last = chatStore.getLastMessage(thread.id);
        const previewText = last
          ? last.attachment && !last.text
            ? `📎 ${last.attachment.title}`
            : last.text
          : t.chats.noMessages;

        listWrap.appendChild(
          el(
            'div',
            {
              class: `ag-chat-row ${activeId === thread.id ? 'is-selected' : ''}`,
              onClick: () => {
                chatStore.markRead(thread.id);
                callbacks.onSelectChat(thread.id);
              },
            },
            el(
              'div',
              {
                class: 'ag-avatar',
                style: `background:${thread.avatarColor}22;border-color:${thread.avatarColor}55`,
              },
              thread.avatarEmoji,
              thread.isOnline ? el('span', { class: 'ag-online-dot' }) : null,
            ),
            el(
              'div',
              { class: 'ag-chat-row__body' },
              el(
                'div',
                { class: 'ag-chat-row__top' },
                el('span', { class: 'ag-chat-row__title' }, thread.title),
                el('span', { class: 'ag-chat-row__time' }, last?.timestamp ?? ''),
              ),
              el(
                'div',
                { class: 'ag-chat-row__bottom' },
                el('span', { class: 'ag-chat-row__preview' }, previewText),
                thread.unreadCount > 0
                  ? el('span', { class: 'ag-unread-badge' }, String(thread.unreadCount))
                  : thread.isPinned
                    ? el('span', { class: 'ag-pin-icon' }, icons.pin(13))
                    : null,
              ),
            ),
          ),
        );
      }
    };

    renderListOnly();
  };

  if (callbacks.focusSearchSignal) {
    callbacks.focusSearchSignal(() => {
      searchInputRef?.focus();
    });
  }

  chatStore.subscribe(render);
  i18nStore.subscribe(render);
  render();
  return root;
}
