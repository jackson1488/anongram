import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { openContactsOverlay } from '../contacts/ContactsOverlay';
import { draftsStore } from '../drafts/draftsStore';
import { chatStore } from './chatStore';
import type { ChatFolder } from './models';

export interface ChatListCallbacks {
  getActiveChatId: () => string | null;
  onSelectChat: (chatId: string) => void;
  focusSearchSignal?: (focusFn: () => void) => void;
}

export function createChatListView(callbacks: ChatListCallbacks): HTMLElement {
  const root = el('div', { class: 'ag-list-pane', style: 'position:relative;' });
  let query = '';
  let folder: ChatFolder = 'all';
  let inArchive = false;
  let searchInputRef: HTMLInputElement | null = null;

  const render = () => {
    clear(root);
    const t = i18nStore.t;
    const activeId = callbacks.getActiveChatId();

    if (inArchive) {
      root.appendChild(
        el(
          'div',
          { class: 'ag-chat-header' },
          el(
            'button',
            {
              class: 'ag-icon-btn',
              onClick: () => {
                inArchive = false;
                render();
              },
            },
            icons.back(20),
          ),
          el('div', { class: 'ag-chat-header__title' }, 'Архив'),
        ),
      );
      const archList = el('div', { class: 'ag-scroll-list' });
      const archived = chatStore.threads.filter((th) => th.isArchived);
      if (archived.length === 0) {
        archList.appendChild(el('div', { class: 'ag-empty-inline' }, 'Архив пуст'));
      } else {
        for (const thread of archived) {
          archList.appendChild(
            el(
              'div',
              {
                class: `ag-chat-row ${activeId === thread.id ? 'is-selected' : ''}`,
                onClick: () => callbacks.onSelectChat(thread.id),
              },
              el('div', { class: 'ag-avatar', style: `background:${thread.avatarColor}22` }, thread.avatarEmoji),
              el(
                'div',
                { class: 'ag-chat-row__body' },
                el('div', { class: 'ag-chat-row__title' }, thread.title),
                el('div', { class: 'ag-chat-row__preview' }, thread.statusText),
              ),
              el(
                'button',
                {
                  class: 'ag-chip',
                  onClick: (e: Event) => {
                    e.stopPropagation();
                    chatStore.toggleArchive(thread.id);
                  },
                },
                'Вернуть',
              ),
            ),
          );
        }
      }
      root.appendChild(archList);
      return;
    }

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

    if (!chatStore.onionOnline) {
      root.appendChild(
        el(
          'div',
          { class: 'ag-onion-banner' },
          icons.alert(16),
          el('span', null, 'Onion-сеть недоступна — исходящие ждут в очереди шифрования'),
        ),
      );
    }

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

      // ArchivePill (1-to-1 Android ArchivePill.kt)
      const archivedThreads = chatStore.threads.filter((th) => th.isArchived);
      if (archivedThreads.length > 0 && !q && folder === 'all') {
        const preview = archivedThreads
          .slice(0, 2)
          .map((th) => th.title)
          .join(', ');
        listWrap.appendChild(
          el(
            'div',
            {
              class: 'ag-archive-pill',
              onClick: () => {
                inArchive = true;
                render();
              },
            },
            el('div', { class: 'ag-archive-pill__icon' }, icons.archive(20)),
            el(
              'div',
              { class: 'ag-chat-row__body' },
              el('div', { class: 'ag-chat-row__title' }, 'Архив'),
              el('div', { class: 'ag-chat-row__preview' }, preview),
            ),
            el('span', { class: 'ag-archive-pill__count' }, String(archivedThreads.length)),
          ),
        );
      }

      const threads = chatStore.threads.filter((thread) => {
        if (thread.isArchived) return false;
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
        const draft = draftsStore.getDraft(thread.id);
        const previewNode = draft
          ? el(
              'span',
              { class: 'ag-chat-row__preview' },
              el('span', { class: 'ag-draft-prefix' }, 'Черновик: '),
              draft.text || draft.replySnippet || 'В ответ на сообщение',
            )
          : el(
              'span',
              { class: 'ag-chat-row__preview' },
              last
                ? last.attachment && !last.text
                  ? `📎 ${last.attachment.title}`
                  : last.text
                : t.chats.noMessages,
            );

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
                el(
                  'span',
                  { class: 'ag-chat-row__title' },
                  thread.title,
                  thread.isOtpBurn ? el('span', { class: 'ag-otp-badge' }, 'OTP') : null,
                ),
                el('span', { class: 'ag-chat-row__time' }, last?.timestamp ?? ''),
              ),
              el(
                'div',
                { class: 'ag-chat-row__bottom' },
                previewNode,
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

    // ChatsFab (1-to-1 Android ChatsFab.kt)
    root.appendChild(
      el(
        'button',
        {
          class: 'ag-chats-fab',
          title: 'Новое сообщение',
          onClick: () =>
            openContactsOverlay((chatId) => {
              chatStore.markRead(chatId);
              callbacks.onSelectChat(chatId);
            }),
        },
        icons.edit(22),
      ),
    );
  };

  if (callbacks.focusSearchSignal) {
    callbacks.focusSearchSignal(() => {
      searchInputRef?.focus();
    });
  }

  const u1 = chatStore.subscribe(render);
  const u2 = draftsStore.subscribe(render);
  const u3 = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
    u3();
  };

  return root;
}
