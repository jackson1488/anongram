import { i18nStore } from '../../core/i18n/i18nStore';
import { themeStore } from '../../core/theme/themeStore';
import { AVATAR_GRADIENTS } from '../../core/crypto/identity';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { authStore } from '../auth/authStore';
import { createCallsView } from '../calls/CallsView';
import { createChatListView } from '../chats/ChatListView';
import { createChatRoomView } from '../chats/ChatRoomView';
import '../chats/chats.css';
import { chatStore } from '../chats/chatStore';
import { openContactsOverlay } from '../contacts/ContactsOverlay';
import { createProfileView } from '../profile/ProfileView';
import { openQrHubOverlay } from '../qr/QrHubOverlay';
import { createServicesView } from '../services/ServicesView';
import {
  createSettingsDetailView,
  createSettingsListPane,
  type SettingsRoute,
} from '../settings/SettingsView';
import '../settings/settings.css';
import { createAppHeader } from './components/AppHeader';

type MainTab = 'chats' | 'services' | 'settings' | 'profile';
type Cleanable = HTMLElement & { __cleanup?: () => void; __refresh?: () => void; __openArchive?: () => void };

/**
 * Responsive shell.
 *  - Mobile (<840px): 1:1 Android layout — AppHeader + 4-tab floating island with elevated QR button.
 *  - Desktop (>=840px): Telegram Web layout — left column (hamburger menu + search + chats),
 *    right column (conversation). All navigation goes through the hamburger drawer.
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

  const isDesktop = () => window.innerWidth >= 840;

  const goTab = (tab: MainTab) => {
    activeTab = tab;
    mobileChatOpen = false;
    mobileSettingsOpen = false;
    render();
  };

  const buildRightContent = (): HTMLElement => {
    const t = i18nStore.t;
    if (activeTab === 'settings') {
      const detail = createSettingsDetailView(
        settingsRoute,
        (next) => {
          settingsRoute = next;
          mountRight();
          renderLeftOnly();
        },
        () => {
          mobileSettingsOpen = false;
          render();
        },
      ) as Cleanable;
      cleanupRight = detail.__cleanup ?? null;
      return detail;
    }
    if (activeChatId) {
      const room = createChatRoomView(activeChatId, () => {
        mobileChatOpen = false;
        render();
      }) as Cleanable;
      cleanupRight = room.__cleanup ?? null;
      return room;
    }
    cleanupRight = null;
    return el('div', { class: 'ag-empty-stage' }, t.nav.selectChatHint);
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
      render();
    }
  };

  /** Telegram Web style compact dropdown menu anchored under the hamburger button. */
  const openDrawer = () => {
    const t = i18nStore.t;
    const id = authStore.identity;
    const grad = AVATAR_GRADIENTS[id?.avatarIndex ?? 0];
    const archivedCount = chatStore.threads.filter((th) => th.isArchived).length;
    const close = () => scrim.remove();

    const item = (icon: SVGElement, label: string, action: () => void, badge?: string, trailing?: HTMLElement) =>
      el(
        'button',
        {
          class: 'ag-pop__item',
          onClick: () => {
            if (!trailing) close();
            action();
          },
        },
        icon,
        el('span', { class: 'ag-pop__label' }, label),
        badge ? el('span', { class: 'ag-pop__badge' }, badge) : null,
        trailing ?? null,
      );

    const nightSwitch = el('span', { class: `ag-switch ag-switch--sm ${themeStore.scheme.isDark ? 'is-on' : ''}` }, el('span', { class: 'ag-switch__thumb' }));

    const panel = el(
      'div',
      { class: 'ag-pop' },
      el(
        'div',
        { class: 'ag-pop__account' },
        el(
          'div',
          {
            class: 'ag-pop__avatar',
            style: `background:linear-gradient(135deg, ${grad[0]}, ${grad[1]});`,
          },
          (id?.displayName ?? 'G').slice(0, 1).toUpperCase(),
        ),
        el('span', { class: 'ag-pop__label' }, id?.displayName ?? 'Ghost'),
      ),
      el('div', { class: 'ag-pop__sep' }),
      item(icons.bookmark(18), t.chats.savedMessages, () => {
        goTab('chats');
        selectChat('saved');
      }),
      item(
        icons.archive(18),
        'Архив чатов',
        () => {
          goTab('chats');
          chatListRef?.__openArchive?.();
        },
        archivedCount > 0 ? String(archivedCount) : undefined,
      ),
      item(icons.users(18), 'Контакты', () =>
        openContactsOverlay((chatId) => {
          goTab('chats');
          selectChat(chatId);
        }),
      ),
      item(icons.phone(18), 'Звонки', () => openCallsModal()),
      item(icons.shield(18), t.nav.services, () => goTab('services')),
      item(icons.qr(18), t.nav.qrHub, () => openQrHubOverlay()),
      el('div', { class: 'ag-pop__sep' }),
      item(icons.settings(18), t.nav.settings, () => goTab('settings')),
      item(
        icons.moon(18),
        'Ночной режим',
        () => {
          themeStore.selectPreset(themeStore.scheme.isDark ? 'telegram_day' : 'telegram_night');
          nightSwitch.classList.toggle('is-on', themeStore.scheme.isDark);
        },
        undefined,
        nightSwitch,
      ),
      el('div', { class: 'ag-pop__foot' }, 'AnonGram Web • Onion v3'),
    );

    const scrim = el('div', {
      class: 'ag-pop-scrim',
      onClick: (e: Event) => {
        if (e.target === scrim) close();
      },
      onContextMenu: (e: Event) => {
        e.preventDefault();
        close();
      },
    });
    scrim.appendChild(panel);
    document.body.appendChild(scrim);
    requestAnimationFrame(() => scrim.classList.add('is-open'));
  };

  const openCallsModal = () => {
    const overlay = el('div', {
      class: 'ag-modal-overlay',
      onClick: (e: Event) => {
        if (e.target === overlay) overlay.remove();
      },
    });
    const box = el(
      'div',
      { class: 'ag-card ag-forward-modal', style: 'max-width:420px;height:70vh;display:flex;flex-direction:column;padding:0;overflow:hidden;' },
      el(
        'div',
        { class: 'ag-chat-header' },
        el('div', { class: 'ag-chat-header__title' }, 'Звонки'),
        el('button', { class: 'ag-icon-btn', onClick: () => overlay.remove() }, icons.close(18)),
      ),
      createCallsView(),
    );
    overlay.appendChild(box);
    document.body.appendChild(overlay);
  };

  const render = () => {
    cleanupLeft?.();
    cleanupRight?.();
    cleanupHeader?.();
    cleanupLeft = cleanupRight = cleanupHeader = null;
    chatListRef = null;
    rightPaneRef = null;
    clear(root);

    const t = i18nStore.t;
    const desktop = isDesktop();
    const body = el('div', { class: 'ag-shell-body' });

    // ---------------- Left pane ----------------
    const leftPane = el('section', { class: 'ag-left-pane' });

    if (desktop && activeTab !== 'chats') {
      const titles: Record<MainTab, string> = {
        chats: t.nav.chats,
        services: t.nav.services,
        settings: t.nav.settings,
        profile: t.nav.profile,
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
          mobileSettingsOpen = true;
          if (desktop) {
            render();
          } else {
            render();
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

    // ---------------- Mobile: island or full-screen detail ----------------
    if (!desktop) {
      const showDetail =
        (activeTab === 'chats' && mobileChatOpen && activeChatId !== null) ||
        (activeTab === 'settings' && mobileSettingsOpen);

      if (showDetail) {
        const overlay = el('div', { class: 'ag-mobile-detail' });
        overlay.appendChild(buildRightContent());
        root.appendChild(overlay);
      } else {
        const tabs: Array<{ id: MainTab; label: string; icon: SVGElement }> = [
          { id: 'chats', label: t.nav.chats, icon: icons.chats(20) },
          { id: 'services', label: t.nav.services, icon: icons.shield(20) },
          { id: 'settings', label: t.nav.settings, icon: icons.settings(20) },
          { id: 'profile', label: t.nav.profile, icon: icons.user(20) },
        ];
        const makeTab = (item: (typeof tabs)[number]) =>
          el(
            'button',
            {
              class: `ag-island__tab ${activeTab === item.id ? 'is-active' : ''}`,
              onClick: () => goTab(item.id),
            },
            item.icon,
            el('span', null, item.label),
          );

        root.appendChild(
          el(
            'div',
            { class: 'ag-island-wrap' },
            el(
              'nav',
              { class: 'ag-island-bar' },
              makeTab(tabs[0]),
              makeTab(tabs[1]),
              el('div', { class: 'ag-island-spacer' }),
              makeTab(tabs[2]),
              makeTab(tabs[3]),
            ),
            el(
              'button',
              { class: 'ag-island-qr-btn', title: t.nav.qrHub, onClick: () => openQrHubOverlay() },
              icons.qr(22),
            ),
          ),
        );
      }
    }
  };

  const onKeyDown = (e: KeyboardEvent) => {
    const target = e.target as HTMLElement | null;
    const typing =
      !!target && (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable);

    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      if (activeTab !== 'chats') goTab('chats');
      mobileChatOpen = false;
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
        mobileChatOpen = false;
        mobileSettingsOpen = false;
        render();
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
    cleanupLeft?.();
    cleanupRight?.();
    cleanupHeader?.();
  };

  return root;
}
