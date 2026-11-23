import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { openContactsOverlay } from '../../contacts/ContactsOverlay';
import { draftsStore } from '../../drafts/draftsStore';
import { chatStore } from '../chatStore';
import type { ChatFolder } from '../models';
import { createArchivePill } from './components/ArchivePill';
import { createChatListItemCard } from './components/ChatListItemCard';
import { createChatsFab } from './components/ChatsFab';
import { createEmptyChatsState } from './components/EmptyChatsState';
import { renderFolderChips } from './components/FolderChips';
import { createOnionOfflineBanner } from './components/OnionOfflineBanner';
import { createSearchRow } from './components/SearchRow';
import { mainMenuT } from './i18n';
import './mainmenu.css';

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
export function createChatsMainMenuScreen(callbacks: ChatListCallbacks): HTMLElement {
  const root = el('div', { class: 'ag-list-pane ag-mm', style: 'position:relative;' });
  let query = '';
  let folder: ChatFolder = 'all';
  let inArchive = false;

  const mainScreen = el('div', { class: 'ag-list-pane', style: 'min-height:0;flex:1;' });
  const search = createSearchRow((q) => {
    query = q;
    renderList();
  }, callbacks.onOpenMenu);
  const bannerSlot = el('div');
  const chipsRow = el('div', { class: 'ag-chips-row' });
  const listWrap = el('div', { class: 'ag-scroll-list' });
  const fab = createChatsFab(() =>
    openContactsOverlay((chatId) => {
      chatStore.markRead(chatId);
      callbacks.onSelectChat(chatId);
    }),
  );
  mainScreen.append(search.root, bannerSlot, chipsRow, listWrap, fab);

  const archiveScreen = el('div', { class: 'ag-list-pane', style: 'display:none;min-height:0;flex:1;' });
  root.append(mainScreen, archiveScreen);

  const select = (id: string) => {
    chatStore.markRead(id);
    callbacks.onSelectChat(id);
  };

  const renderBanner = () => {
    clear(bannerSlot);
    if (!chatStore.onionOnline) bannerSlot.appendChild(createOnionOfflineBanner());
  };

  const renderChips = () => {
    search.input.placeholder = i18nStore.t.chats.searchPlaceholder;
    search.refreshLabels();
    renderFolderChips(chipsRow, folder, (f) => {
      folder = f;
      renderChips();
      renderList();
    });
  };

  const renderList = () => {
    const activeId = callbacks.getActiveChatId();
    clear(listWrap);
    const q = query.trim().toLowerCase();

    const archived = chatStore.threads.filter((th) => th.isArchived);
    if (archived.length > 0 && !q && folder === 'all') {
      listWrap.appendChild(
        createArchivePill(archived, () => {
          inArchive = true;
          showArchive();
        }),
      );
    }

    const threads = chatStore.threads.filter((thread) => {
      if (thread.isArchived) return false;
      if (folder === 'unread' && thread.unreadCount === 0) return false;
      if (folder === 'pinned' && !thread.isPinned) return false;
      if (!q) return true;
      const last = chatStore.getLastMessage(thread.id);
      return thread.title.toLowerCase().includes(q) || (last?.text.toLowerCase().includes(q) ?? false);
    });

    if (threads.length === 0) {
      listWrap.appendChild(
        q || folder !== 'all' ? createEmptyChatsState(i18nStore.t.chats.emptySearch) : createEmptyChatsState(),
      );
      return;
    }
    for (const thread of threads) {
      listWrap.appendChild(createChatListItemCard(thread, activeId === thread.id, () => select(thread.id)));
    }
  };

  const renderArchive = () => {
    const mt = mainMenuT();
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
        el('div', { class: 'ag-chat-header__title' }, mt.archive),
      ),
    );
    const archList = el('div', { class: 'ag-scroll-list' });
    const archived = chatStore.threads.filter((th) => th.isArchived);
    if (archived.length === 0) archList.appendChild(el('div', { class: 'ag-empty-inline' }, mt.archiveEmpty));
    for (const thread of archived) {
      const card = createChatListItemCard(thread, activeId === thread.id, () => callbacks.onSelectChat(thread.id));
      card.appendChild(
        el(
          'button',
          {
            class: 'ag-chip',
            onClick: (e: Event) => {
              e.stopPropagation();
              chatStore.toggleArchive(thread.id);
            },
          },
          mt.restore,
        ),
      );
      archList.appendChild(card);
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
  callbacks.focusSearchSignal?.(() => search.input.focus());

  const u1 = chatStore.subscribe(refresh);
  const u2 = draftsStore.subscribe(renderList);
  const u3 = i18nStore.subscribe(() => {
    renderBanner();
    renderChips();
    renderList();
    if (inArchive) renderArchive();
  });

  const r = root as unknown as {
    __refresh?: () => void;
    __openArchive?: () => void;
    __cleanup?: () => void;
  };
  r.__refresh = refresh;
  r.__openArchive = () => {
    inArchive = true;
    showArchive();
  };
  r.__cleanup = () => {
    u1();
    u2();
    u3();
  };
  return root;
}
