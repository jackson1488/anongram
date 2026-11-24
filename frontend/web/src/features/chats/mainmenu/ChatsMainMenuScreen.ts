import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { openContactsOverlay } from '../../contacts/ContactsOverlay';
import { draftsStore } from '../../drafts/draftsStore';
import { chatStore } from '../chatStore';
import { archiveSettingsStore } from '../archive/ArchiveSettingsStore';
import type { ChatFolder } from '../models';
import { renderGlobalSearchResults, renderRecentSearchesSection } from '../search/ChatSearchScreen';
import { blockedUsersStore } from '../user_profile/data/BlockedUsersStore';
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
  let mobileSearchOpen = false;

  const isMobile = () => window.innerWidth < 840;

  const mainScreen = el('div', { class: 'ag-list-pane', style: 'min-height:0;flex:1;' });

  const topBarTitle = el(
    'span',
    {
      style:
        'flex:1;min-width:0;font-size:20px;font-weight:900;letter-spacing:-0.02em;color:var(--ag-text-primary);overflow:hidden;text-overflow:ellipsis;white-space:nowrap;',
    },
    i18nStore.t.chats.title,
  );
  const topBarSearchBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-icon-btn ag-chats-topbar__search',
      style:
        'width:40px;height:40px;border-radius:9999px;background:var(--ag-island-bg);border:1px solid var(--ag-island-border);backdrop-filter:blur(18px);-webkit-backdrop-filter:blur(18px);color:var(--ag-text-primary);display:inline-flex;align-items:center;justify-content:center;flex-shrink:0;',
      onClick: () => setMobileSearchOpen(!mobileSearchOpen),
    },
    icons.search(20),
  ) as HTMLButtonElement;
  const topBar = el(
    'div',
    {
      class: 'ag-chats-topbar ag-mobile-only',
      style:
        'display:flex;align-items:center;justify-content:space-between;gap:10px;width:100%;padding:6px 14px;flex-shrink:0;',
    },
    topBarTitle,
    topBarSearchBtn,
  );

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
  search.input.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Escape' && isMobile() && mobileSearchOpen) {
      e.preventDefault();
      e.stopPropagation();
      setMobileSearchOpen(false);
    }
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
  mainScreen.append(topBar, search.root, bannerSlot, chipsRow, listWrap, fab);

  const archiveScreen = el('div', { class: 'ag-list-pane', style: 'display:none;min-height:0;flex:1;' });
  root.append(mainScreen, archiveScreen);

  const syncTopBarAndSearch = () => {
    const mobile = isMobile();
    topBarTitle.textContent = i18nStore.t.chats.title;
    topBar.style.display = mobile ? 'flex' : 'none';

    const showSearchAndChips = !mobile || mobileSearchOpen;
    search.root.style.display = showSearchAndChips ? 'flex' : 'none';
    search.root.style.paddingTop = mobile ? '2px' : '10px';
    chipsRow.style.display = showSearchAndChips ? 'flex' : 'none';

    clear(topBarSearchBtn);
    topBarSearchBtn.appendChild(mobileSearchOpen ? icons.close(20) : icons.search(20));
    const btnLabel = mobileSearchOpen ? i18nStore.t.common.close : mainMenuT().searchChats;
    topBarSearchBtn.title = btnLabel;
    topBarSearchBtn.setAttribute('aria-label', btnLabel);
    topBarSearchBtn.style.borderColor = mobileSearchOpen ? 'var(--ag-accent)' : 'var(--ag-island-border)';
    topBarSearchBtn.style.color = mobileSearchOpen ? 'var(--ag-accent)' : 'var(--ag-text-primary)';
  };

  function setMobileSearchOpen(open: boolean): void {
    if (mobileSearchOpen === open) {
      if (open) search.input.focus();
      return;
    }
    mobileSearchOpen = open;
    if (open) {
      syncTopBarAndSearch();
      search.input.focus();
      return;
    }
    query = '';
    search.input.value = '';
    search.refreshLabels();
    searchFocused = false;
    folder = 'all';
    search.input.blur();
    syncTopBarAndSearch();
    renderChips();
    renderList();
  }

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
    const fabLabel = mainMenuT().newMessage;
    fab.title = fabLabel;
    fab.setAttribute('aria-label', fabLabel);
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
    if (archived.length > 0 && folder === 'all' && archiveSettingsStore.getState().showArchiveInMainList) {
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
      listWrap.appendChild(
        createChatListItemCard(thread, activeId === thread.id, () => select(thread.id), {
          onSwipeArchive: () => chatStore.toggleArchive(thread.id),
        }),
      );
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

  syncTopBarAndSearch();
  renderBanner();
  renderChips();
  renderList();
  callbacks.focusSearchSignal?.(() => {
    if (isMobile() && !mobileSearchOpen) {
      setMobileSearchOpen(true);
    } else {
      search.input.focus();
    }
  });

  let wasMobile = isMobile();
  const onResize = () => {
    const nowMobile = isMobile();
    if (nowMobile !== wasMobile) {
      wasMobile = nowMobile;
      if (!nowMobile) {
        mobileSearchOpen = false;
      }
      syncTopBarAndSearch();
      renderList();
    }
  };
  window.addEventListener('resize', onResize);

  const u1 = chatStore.subscribe(refresh);
  const u2 = draftsStore.subscribe(renderList);
  const u3 = i18nStore.subscribe(() => {
    syncTopBarAndSearch();
    renderBanner();
    renderChips();
    renderList();
    if (inArchive) renderArchive();
  });
  const u4 = archiveSettingsStore.subscribe(renderList);
  const u5 = blockedUsersStore.subscribe(renderList);

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
    window.removeEventListener('resize', onResize);
    u1();
    u2();
    u3();
    u4();
    u5();
  };
  return root;
}

