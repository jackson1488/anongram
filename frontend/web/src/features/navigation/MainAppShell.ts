import { i18nStore } from '../../core/i18n/i18nStore';
import {
  closeOverlay,
  createTransitionScreen,
  openOverlay,
  snapBackOverlay,
  swipeBackGesture,
  type TransitionScreen,
} from '../../core/navigation/AnimationEngine';
import {
  defaultNavigationConfig,
  type NavigationTransitionConfig,
} from '../../core/navigation/TransitionModels';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { createCallsView } from '../calls/CallsView';
import { createChatListView } from '../chats/ChatListView';
import { createChatRoomView } from '../chats/ChatRoomView';
import '../chats/chats.css';
import { chatStore } from '../chats/chatStore';
import { openContactsOverlay } from '../contacts/ContactsOverlay';
import { createProfileView } from '../profile/ProfileView';
import { openQrHubOverlay } from '../qr/QrHubOverlay';
import { createServicesView } from '../services/ServicesView';
import { settingsStore } from '../settings/settingsStore';
import {
  createSettingsDetailView,
  createSettingsListPane,
  type SettingsRoute,
} from '../settings/SettingsView';
import '../settings/settings.css';
import { createAppHeader } from './components/AppHeader';
import { openDesktopSidebarMenu } from './DesktopSidebarMenu';
import { NavigationI18n } from './i18n';
import { createMainFloatingIsland, type MainTab } from './MainFloatingIsland';
import './navigation.css';

type Cleanable = HTMLElement & {
  __cleanup?: () => void;
  __refresh?: () => void;
  __openArchive?: () => void;
};

/**
 * Responsive shell mirroring Android MainAppShell.kt:
 *  - Mobile (<840px): AppHeader + 4-tab MainFloatingIsland with elevated QR button,
 *    and AnimationEngine-driven detail overlays with swipe-back gesture.
 *  - Desktop (>=840px): Telegram Web two-pane layout with DesktopSidebarMenu.
 */
export function createAppShell(): HTMLElement {
  const root = el('div', { class: 'ag-shell' });
  let activeTab: MainTab = 'chats';
  let activeChatId: string | null = null;
  let mobileChatOpen = false;
  let settingsRoute: SettingsRoute = 'chatAppearance';
  let mobileSettingsOpen = false;
  let focusChatSearchFn: (() => void) | null = null;

  let cleanupLeft: (() => void) | null = null;
  let cleanupRight: (() => void) | null = null;
  let cleanupHeader: (() => void) | null = null;
  let chatListRef: Cleanable | null = null;
  let rightPaneRef: HTMLElement | null = null;

  let detailScreen: TransitionScreen | null = null;
  let detailSwipeCleanup: (() => void) | null = null;
  let isClosingDetail = false;

  const isDesktop = () => window.innerWidth >= 840;

  const getNavConfig = (): NavigationTransitionConfig =>
    settingsStore.all.animationsEnabled
      ? defaultNavigationConfig
      : { ...defaultNavigationConfig, transitionType: 'INSTANT' };

  const teardownDetailOverlay = () => {
    detailSwipeCleanup?.();
    detailSwipeCleanup = null;
    if (detailScreen) {
      detailScreen.animation?.cancel();
      detailScreen.el.remove();
      detailScreen = null;
    }
    isClosingDetail = false;
  };

  const closeMobileDetail = () => {
    if (!detailScreen || isClosingDetail) {
      if (mobileChatOpen || mobileSettingsOpen) {
        mobileChatOpen = false;
        mobileSettingsOpen = false;
        teardownDetailOverlay();
      }
      return;
    }
    const screen = detailScreen;
    isClosingDetail = true;
    void closeOverlay(screen, () => {
      if (detailScreen === screen) {
        teardownDetailOverlay();
      } else {
        screen.el.remove();
      }
      cleanupRight?.();
      cleanupRight = null;
      mobileChatOpen = false;
      mobileSettingsOpen = false;
    });
  };

  const openMobileDetail = () => {
    teardownDetailOverlay();
    cleanupRight?.();
    cleanupRight = null;

    const content = buildRightContent();
    const screen = createTransitionScreen(content, getNavConfig, 'ag-mobile-detail');
    detailScreen = screen;

    detailSwipeCleanup = swipeBackGesture(screen.el, {
      enabled: () => !isDesktop() && !isClosingDetail && detailScreen === screen,
      onDragStart: () => {
        screen.setExiting(true);
      },
      onDragProgress: (p) => {
        screen.setProgress(p);
      },
      onDragEnd: (shouldClose) => {
        if (shouldClose) {
          closeMobileDetail();
        } else {
          void snapBackOverlay(screen, () => {
            screen.setExiting(false);
          });
        }
      },
    });

    void openOverlay(screen, () => {
      root.appendChild(screen.el);
    });
  };

  const goTab = (tab: MainTab) => {
    activeTab = tab;
    mobileChatOpen = false;
    mobileSettingsOpen = false;
    render();
  };

  const buildRightContent = (): HTMLElement => {
    const s = NavigationI18n.current;
    if (activeTab === 'settings') {
      const detail = createSettingsDetailView(
        settingsRoute,
        (next) => {
          settingsRoute = next;
          if (isDesktop()) {
            mountRight();
            renderLeftOnly();
          } else {
            openMobileDetail();
          }
        },
        () => {
          if (isDesktop()) {
            mobileSettingsOpen = false;
            render();
          } else {
            closeMobileDetail();
          }
        },
      ) as Cleanable;
      cleanupRight = detail.__cleanup ?? null;
      return detail;
    }
    if (activeChatId) {
      const room = createChatRoomView(activeChatId, () => {
        if (isDesktop()) {
          mobileChatOpen = false;
          render();
        } else {
          closeMobileDetail();
        }
      }) as Cleanable;
      cleanupRight = room.__cleanup ?? null;
      return room;
    }
    cleanupRight = null;
    return el('div', { class: 'ag-empty-stage' }, s.selectChatHint);
  };

  /** Rebuilds only the right pane (desktop) — the left pane keeps its search text and focus. */
  const mountRight = () => {
    if (!rightPaneRef) return;
    cleanupRight?.();
    cleanupRight = null;
    clear(rightPaneRef);
    rightPaneRef.appendChild(buildRightContent());
  };

  const renderLeftOnly = () => {
    if (activeTab === 'settings') render();
    else chatListRef?.__refresh?.();
  };

  const selectChat = (chatId: string) => {
    activeChatId = chatId;
    if (isDesktop()) {
      mountRight();
      chatListRef?.__refresh?.();
    } else {
      mobileChatOpen = true;
      openMobileDetail();
    }
  };

  const openCallsModal = () => {
    const s = NavigationI18n.current;
    const overlay = el('div', {
      class: 'ag-modal-overlay',
      onClick: (e: Event) => {
        if (e.target === overlay) overlay.remove();
      },
    });
    const box = el(
      'div',
      {
        class: 'ag-card ag-forward-modal',
        style:
          'max-width:420px;height:70vh;display:flex;flex-direction:column;padding:0;overflow:hidden;',
      },
      el(
        'div',
        { class: 'ag-chat-header' },
        el('div', { class: 'ag-chat-header__title' }, s.calls),
        el('button', { class: 'ag-icon-btn', onClick: () => overlay.remove() }, icons.close(18)),
      ),
      createCallsView(),
    );
    overlay.appendChild(box);
    document.body.appendChild(overlay);
  };

  const openDrawer = () => {
    const archivedCount = chatStore.threads.filter((th) => th.isArchived).length;
    openDesktopSidebarMenu({
      archivedCount,
      onSavedMessages: () => {
        goTab('chats');
        selectChat('saved');
      },
      onOpenArchive: () => {
        goTab('chats');
        chatListRef?.__openArchive?.();
      },
      onOpenContacts: () =>
        openContactsOverlay((chatId) => {
          goTab('chats');
          selectChat(chatId);
        }),
      onOpenCalls: openCallsModal,
      onOpenServices: () => goTab('services'),
      onOpenQrHub: () => openQrHubOverlay(),
      onOpenSettings: () => goTab('settings'),
    });
  };

  const render = () => {
    teardownDetailOverlay();
    cleanupLeft?.();
    cleanupRight?.();
    cleanupHeader?.();
    cleanupLeft = cleanupRight = cleanupHeader = null;
    chatListRef = null;
    rightPaneRef = null;
    clear(root);

    const s = NavigationI18n.current;
    const desktop = isDesktop();
    const body = el('div', { class: 'ag-shell-body' });

    // ---------------- Left pane ----------------
    const leftPane = el('section', { class: 'ag-left-pane' });

    if (desktop && activeTab !== 'chats') {
      const titles: Record<MainTab, string> = {
        chats: s.chats,
        services: s.services,
        settings: s.settings,
        profile: s.profile,
      };
      leftPane.appendChild(
        el(
          'div',
          { class: 'ag-chat-header' },
          el('button', { class: 'ag-icon-btn', onClick: () => goTab('chats') }, icons.back(22)),
          el('div', { class: 'ag-chat-header__title' }, titles[activeTab]),
        ),
      );
    } else if (!desktop) {
      const header = createAppHeader() as Cleanable;
      cleanupHeader = header.__cleanup ?? null;
      leftPane.appendChild(header);
    }

    let leftContent: Cleanable;
    if (activeTab === 'chats') {
      leftContent = createChatListView({
        getActiveChatId: () => activeChatId,
        onSelectChat: selectChat,
        onOpenMenu: desktop ? openDrawer : undefined,
        focusSearchSignal: (fn) => {
          focusChatSearchFn = fn;
        },
      }) as Cleanable;
      chatListRef = leftContent;
    } else if (activeTab === 'services') {
      leftContent = createServicesView() as Cleanable;
    } else if (activeTab === 'profile') {
      leftContent = createProfileView() as Cleanable;
    } else {
      leftContent = createSettingsListPane(
        () => settingsRoute,
        (route) => {
          settingsRoute = route;
          if (desktop) {
            render();
          } else {
            mobileSettingsOpen = true;
            openMobileDetail();
          }
        },
      ) as Cleanable;
    }
    cleanupLeft = leftContent.__cleanup ?? null;
    leftPane.appendChild(leftContent);
    body.appendChild(leftPane);

    // ---------------- Right pane (desktop only) ----------------
    if (desktop) {
      const rightPane = el('main', { class: 'ag-right-pane' });
      rightPaneRef = rightPane;
      rightPane.appendChild(buildRightContent());
      body.appendChild(rightPane);
    }

    root.appendChild(body);

    // ---------------- Mobile: floating island & optional restored detail ----------------
    if (!desktop) {
      root.appendChild(
        createMainFloatingIsland({
          activeTab,
          onTabSelected: goTab,
          onQrClick: () => openQrHubOverlay(),
        }),
      );

      const showDetail =
        (activeTab === 'chats' && mobileChatOpen && activeChatId !== null) ||
        (activeTab === 'settings' && mobileSettingsOpen);
      if (showDetail) {
        openMobileDetail();
      }
    }
  };

  const onKeyDown = (e: KeyboardEvent) => {
    const target = e.target as HTMLElement | null;
    const typing =
      !!target &&
      (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable);

    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      if (activeTab !== 'chats') goTab('chats');
      if (mobileChatOpen || mobileSettingsOpen) {
        closeMobileDetail();
      }
      requestAnimationFrame(() => focusChatSearchFn?.());
      return;
    }

    if (e.key === 'Escape') {
      const scrim = document.querySelector('.ag-pop-scrim');
      if (scrim) {
        scrim.remove();
        return;
      }
      if (mobileChatOpen || mobileSettingsOpen) {
        closeMobileDetail();
      } else if (isDesktop() && activeChatId && !typing) {
        activeChatId = null;
        mountRight();
        chatListRef?.__refresh?.();
      }
      return;
    }

    // Telegram Web behaviour: typing anywhere focuses the composer of the open chat.
    if (
      isDesktop() &&
      !typing &&
      activeTab === 'chats' &&
      activeChatId &&
      !e.ctrlKey &&
      !e.metaKey &&
      !e.altKey &&
      e.key.length === 1
    ) {
      const input = root.querySelector('.ag-msg-input') as HTMLInputElement | null;
      input?.focus();
    }
  };

  let wasDesktop = isDesktop();
  const onResize = () => {
    const now = isDesktop();
    if (now !== wasDesktop) {
      wasDesktop = now;
      render();
    }
  };

  window.addEventListener('keydown', onKeyDown);
  window.addEventListener('resize', onResize);
  const unsubI18n = i18nStore.subscribe(render);
  const unsubChats = chatStore.subscribe(() => {});

  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    window.removeEventListener('keydown', onKeyDown);
    window.removeEventListener('resize', onResize);
    unsubI18n();
    unsubChats();
    teardownDetailOverlay();
    cleanupLeft?.();
    cleanupRight?.();
    cleanupHeader?.();
  };

  return root;
}
