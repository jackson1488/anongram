import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { openContactsOverlay } from '../../contacts/ContactsOverlay';
import { draftsStore } from '../../drafts/draftsStore';
import { chatStore } from '../chatStore';
import type { ChatFolder } from '../models';
import { renderGlobalSearchResults, renderRecentSearchesSection } from '../search/ChatSearchScreen';
import { createArchivePill } from './components/ArchivePill';
import { createChatListItemCard } from './components/ChatListItemCard';
import { createChatsFab } from './components/ChatsFab';
import { createEmptyChatsState } from './components/EmptyChatsState';
import { renderFolderChips } from './components/FolderChips';
import { createOnionOfflineBanner } from './components/OnionOfflineBanner';
import { createSearchRow } from './components/SearchRow';
import './mainmenu.css';

export interface ChatListCallbacks {
  getActiveChatId: () => string | null;
  onSelectChat: (chatId: string, messageId?: string) => void;
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
  let searchFocused = false;

  const mainScreen = el('div', { class: 'ag-list-pane', style: 'min-height:0;flex:1;' });
  const search = createSearchRow((q) => {
    query = q;
    renderList();
  }, callbacks.onOpenMenu);
  search.input.addEventListener('focus', () => {
    if (!searchFocused) {
      searchFocused = true;
      if (!query.trim()) renderList();
    }
  });
  search.input.addEventListener('blur', () => {
    window.setTimeout(() => {
      if (document.activeElement !== search.input && searchFocused) {
        searchFocused = false;
        if (!query.trim()) renderList();
      }
    }, 150);
  });

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

  const select = (id: string, messageId?: string) => {
    chatStore.markRead(id);
    callbacks.onSelectChat(id, messageId);
  };

  const pickSearchQuery = (q: string) => {
    search.input.value = q;
    search.input.dispatchEvent(new Event('input'));
    search.input.focus();
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
    const q = query.trim();

    if (q) {
      renderGlobalSearchResults(listWrap, q, {
        onSelectChat: (chatId, messageId) => select(chatId, messageId),
        onPickQuery: pickSearchQuery,
      });
      return;
    }

    clear(listWrap);

    if (searchFocused) {
      renderRecentSearchesSection(listWrap, pickSearchQuery, renderList);
    }

    const archived = chatStore.threads.filter((th) => th.isArchived);
    if (archived.length > 0 && folder === 'all') {
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
      return true;
    });

    if (threads.length === 0) {
      listWrap.appendChild(
        folder !== 'all' ? createEmptyChatsState(i18nStore.t.chats.emptySearch) : createEmptyChatsState(),
      );
      return;
    }
    for (const thread of threads) {
      listWrap.appendChild(createChatListItemCard(thread, activeId === thread.id, () => select(thread.id)));
    }
  };

  let archiveSettingsOpen = false;

  const renderArchive = () => {
    clear(archiveScreen);
    if (archiveSettingsOpen) {
      import('../archive/ArchiveSettingsScreen').then(({ createArchiveSettingsScreen }) => {
        clear(archiveScreen);
        archiveScreen.appendChild(
          createArchiveSettingsScreen(() => {
            archiveSettingsOpen = false;
            renderArchive();
          }),
        );
      });
      return;
    }
    import('../archive/ArchiveScreen').then(({ createArchiveScreen }) => {
      clear(archiveScreen);
      archiveScreen.appendChild(
        createArchiveScreen(
          () => {
            inArchive = false;
            showArchive();
          },
          (chatId) => callbacks.onSelectChat(chatId),
          () => {
            archiveSettingsOpen = true;
            renderArchive();
          },
        ),
      );
    });
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
