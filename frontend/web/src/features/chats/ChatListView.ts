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
  /** Desktop (Telegram Web style): hamburger button left of the search field. */
  onOpenMenu?: () => void;
  focusSearchSignal?: (focusFn: () => void) => void;
}

/**
 * Chat list with a PERSISTENT skeleton: the search input is created once and never
 * re-created, so typing never loses focus. Only the list / chips / banner are redrawn.
 */
export function createChatListView(callbacks: ChatListCallbacks): HTMLElement {
  const root = el('div', { class: 'ag-list-pane', style: 'position:relative;' });
  let query = '';
  let folder: ChatFolder = 'all';
  let inArchive = false;

  // ---- Persistent main screen ----
  const mainScreen = el('div', { class: 'ag-list-pane', style: 'min-height:0;flex:1;' });
  const searchInput = el('input', { type: 'text' }) as HTMLInputElement;
  const clearBtn = el(
    'button',
    {
      class: 'ag-icon-btn',
      style: 'width:28px;height:28px;display:none;',
      onClick: () => {
        query = '';
        searchInput.value = '';
        clearBtn.style.display = 'none';
        renderList();
        searchInput.focus();
      },
    },
    icons.close(14),
  );
  searchInput.addEventListener('input', () => {
    query = searchInput.value;
    clearBtn.style.display = query ? 'inline-flex' : 'none';
    renderList();
  });

  const searchRow = el('div', { class: 'ag-search-wrap ag-search-row' });
  if (callbacks.onOpenMenu) {
    searchRow.appendChild(
      el(
        'button',
        { class: 'ag-icon-btn', title: 'Menu', onClick: () => callbacks.onOpenMenu?.() },
        icons.menu(22),
      ),
    );
  }
  searchRow.appendChild(
    el('div', { class: 'ag-search-pill', style: 'flex:1;min-width:0;' }, icons.search(16), searchInput, clearBtn),
  );

  const bannerSlot = el('div');
  const chipsRow = el('div', { class: 'ag-chips-row' });
  const listWrap = el('div', { class: 'ag-scroll-list' });
  const fab = el(
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
  );

  mainScreen.append(searchRow, bannerSlot, chipsRow, listWrap, fab);

  // ---- Archive screen ----
  const archiveScreen = el('div', { class: 'ag-list-pane', style: 'display:none;min-height:0;flex:1;' });

  root.append(mainScreen, archiveScreen);

  const renderBanner = () => {
    clear(bannerSlot);
    if (!chatStore.onionOnline) {
      bannerSlot.appendChild(
        el(
          'div',
          { class: 'ag-onion-banner' },
          icons.alert(16),
          el('span', null, 'Onion-сеть недоступна — исходящие ждут в очереди шифрования'),
        ),
      );
    }
  };

  const renderChips = () => {
    const t = i18nStore.t;
    searchInput.placeholder = t.chats.searchPlaceholder;
    clear(chipsRow);
    const folders: Array<{ id: ChatFolder; label: string }> = [
      { id: 'all', label: t.chats.folderAll },
      { id: 'unread', label: t.chats.folderUnread },
      { id: 'pinned', label: t.chats.folderPinned },
    ];
    for (const f of folders) {
      chipsRow.appendChild(
        el(
          'button',
          {
            class: `ag-chip ${folder === f.id ? 'is-active' : ''}`,
            onClick: () => {
              folder = f.id;
              renderChips();
              renderList();
            },
          },
          f.label,
        ),
      );
    }
  };

  const renderList = () => {
    const t = i18nStore.t;
    const activeId = callbacks.getActiveChatId();
    clear(listWrap);
    const q = query.trim().toLowerCase();

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
              showArchive();
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
        thread.title.toLowerCase().includes(q) || (last?.text.toLowerCase().includes(q) ?? false)
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

  const renderArchive = () => {
    clear(archiveScreen);
    const activeId = callbacks.getActiveChatId();
    archiveScreen.appendChild(
      el(
        'div',
        { class: 'ag-chat-header' },
        el(
          'button',
          {
            class: 'ag-icon-btn',
            onClick: () => {
              inArchive = false;
              showArchive();
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
    }
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
    archiveScreen.appendChild(archList);
  };

  const showArchive = () => {
    mainScreen.style.display = inArchive ? 'none' : 'flex';
    archiveScreen.style.display = inArchive ? 'flex' : 'none';
    if (inArchive) renderArchive();
    else renderList();
  };

  const refresh = () => {
    renderBanner();
    renderList();
    if (inArchive) renderArchive();
  };

  renderBanner();
  renderChips();
  renderList();

  callbacks.focusSearchSignal?.(() => searchInput.focus());

  const u1 = chatStore.subscribe(refresh);
  const u2 = draftsStore.subscribe(renderList);
  const u3 = i18nStore.subscribe(() => {
    renderChips();
    renderList();
  });

  // Lets the shell highlight the active chat without rebuilding the pane (search keeps focus/text).
  (root as unknown as { __refresh?: () => void }).__refresh = refresh;
  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
    u3();
  };

  return root;
}
