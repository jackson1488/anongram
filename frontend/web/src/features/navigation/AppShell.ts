import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { createChatListView } from '../chats/ChatListView';
import { createChatRoomView } from '../chats/ChatRoomView';
import '../chats/chats.css';
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

export function createAppShell(): HTMLElement {
  const root = el('div', { class: 'ag-shell' });
  let activeTab: MainTab = 'chats';
  let activeChatId: string | null = 'ghost_node';
  let mobileChatOpen = false;
  let settingsRoute: SettingsRoute = 'chatAppearance';
  let mobileSettingsOpen = false;
  let focusChatSearchFn: (() => void) | null = null;

  let cleanupRight: (() => void) | null = null;
  let cleanupLeft: (() => void) | null = null;

  const isDesktop = () => window.innerWidth >= 840;

  const render = () => {
    if (cleanupLeft) {
      cleanupLeft();
      cleanupLeft = null;
    }
    if (cleanupRight) {
      cleanupRight();
      cleanupRight = null;
    }
    clear(root);

    const t = i18nStore.t;
    const body = el('div', { class: 'ag-shell-body' });

    const leftTabs: Array<{ id: MainTab; label: string; icon: SVGElement }> = [
      { id: 'chats', label: t.nav.chats, icon: icons.chats(20) },
      { id: 'services', label: t.nav.services, icon: icons.shield(20) },
    ];
    const rightTabs: Array<{ id: MainTab; label: string; icon: SVGElement }> = [
      { id: 'settings', label: t.nav.settings, icon: icons.settings(20) },
      { id: 'profile', label: t.nav.profile, icon: icons.user(20) },
    ];
    const allTabs = [...leftTabs, ...rightTabs];

    // Desktop Left Rail
    const rail = el(
      'aside',
      { class: 'ag-left-rail' },
      el('div', { class: 'ag-rail-brand' }, icons.shield(22)),
      ...allTabs.map((item) =>
        el(
          'button',
          {
            class: `ag-rail-btn ${activeTab === item.id ? 'is-active' : ''}`,
            onClick: () => {
              activeTab = item.id;
              render();
            },
          },
          item.icon,
          el('span', null, item.label),
        ),
      ),
      el(
        'button',
        {
          class: 'ag-rail-btn',
          style: 'margin-top:auto;',
          title: t.nav.qrHub,
          onClick: () => openQrHubOverlay(),
        },
        icons.qr(20),
        el('span', null, 'QR'),
      ),
    );
    body.appendChild(rail);

    // Left Pane (with AppHeader matching Android)
    const leftPane = el('section', { class: 'ag-left-pane' });
    leftPane.appendChild(createAppHeader());

    let leftContent: HTMLElement;
    if (activeTab === 'chats') {
      leftContent = createChatListView({
        getActiveChatId: () => activeChatId,
        onSelectChat: (chatId) => {
          activeChatId = chatId;
          mobileChatOpen = true;
          render();
        },
        focusSearchSignal: (fn) => {
          focusChatSearchFn = fn;
        },
      });
    } else if (activeTab === 'services') {
      leftContent = createServicesView();
    } else if (activeTab === 'profile') {
      leftContent = createProfileView();
    } else {
      leftContent = createSettingsListPane(
        () => settingsRoute,
        (route) => {
          settingsRoute = route;
          mobileSettingsOpen = true;
          render();
        },
      );
    }
    cleanupLeft = (leftContent as unknown as { __cleanup?: () => void }).__cleanup ?? null;
    leftPane.appendChild(leftContent);
    body.appendChild(leftPane);

    // Right Pane (Desktop)
    const rightPane = el('main', { class: 'ag-right-pane' });
    if (activeTab === 'settings') {
      const detail = createSettingsDetailView(
        settingsRoute,
        (nextRoute) => {
          settingsRoute = nextRoute;
          render();
        },
        () => {
          mobileSettingsOpen = false;
          render();
        },
      );
      cleanupRight = (detail as unknown as { __cleanup?: () => void }).__cleanup ?? null;
      rightPane.appendChild(detail);
    } else if (activeChatId) {
      const room = createChatRoomView(activeChatId, () => {
        mobileChatOpen = false;
        render();
      });
      cleanupRight = (room as unknown as { __cleanup?: () => void }).__cleanup ?? null;
      rightPane.appendChild(room);
    } else {
      rightPane.appendChild(el('div', { class: 'ag-empty-stage' }, t.nav.selectChatHint));
    }
    body.appendChild(rightPane);

    root.appendChild(body);

    // Mobile Floating Island (1-to-1 with Android MainFloatingIsland: 4 tabs + center elevated QR button)
    const showMobileDetail =
      !isDesktop() &&
      ((activeTab === 'chats' && mobileChatOpen && activeChatId !== null) ||
        (activeTab === 'settings' && mobileSettingsOpen));

    if (!showMobileDetail) {
      const makeIslandTab = (item: { id: MainTab; label: string; icon: SVGElement }) =>
        el(
          'button',
          {
            class: `ag-island__tab ${activeTab === item.id ? 'is-active' : ''}`,
            onClick: () => {
              activeTab = item.id;
              mobileChatOpen = false;
              mobileSettingsOpen = false;
              render();
            },
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
            makeIslandTab(leftTabs[0]),
            makeIslandTab(leftTabs[1]),
            el('div', { class: 'ag-island-spacer' }),
            makeIslandTab(rightTabs[0]),
            makeIslandTab(rightTabs[1]),
          ),
          el(
            'button',
            {
              class: 'ag-island-qr-btn',
              title: t.nav.qrHub,
              onClick: () => openQrHubOverlay(),
            },
            icons.qr(22),
          ),
        ),
      );
    } else {
      const overlay = el('div', { class: 'ag-mobile-detail' });
      if (activeTab === 'chats' && activeChatId) {
        const room = createChatRoomView(activeChatId, () => {
          mobileChatOpen = false;
          render();
        });
        cleanupRight = (room as unknown as { __cleanup?: () => void }).__cleanup ?? null;
        overlay.appendChild(room);
      } else if (activeTab === 'settings') {
        const detail = createSettingsDetailView(
          settingsRoute,
          (nextRoute) => {
            settingsRoute = nextRoute;
            render();
          },
          () => {
            mobileSettingsOpen = false;
            render();
          },
        );
        cleanupRight = (detail as unknown as { __cleanup?: () => void }).__cleanup ?? null;
        overlay.appendChild(detail);
      }
      root.appendChild(overlay);
    }
  };

  const onKeyDown = (e: KeyboardEvent) => {
    if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      activeTab = 'chats';
      mobileChatOpen = false;
      render();
      requestAnimationFrame(() => focusChatSearchFn?.());
    } else if (e.key === 'Escape') {
      if (mobileChatOpen || mobileSettingsOpen) {
        mobileChatOpen = false;
        mobileSettingsOpen = false;
        render();
      }
    }
  };

  let wasDesktop = isDesktop();
  const onResize = () => {
    const nowDesktop = isDesktop();
    if (nowDesktop !== wasDesktop) {
      wasDesktop = nowDesktop;
      render();
    }
  };

  window.addEventListener('keydown', onKeyDown);
  window.addEventListener('resize', onResize);
  const unsubI18n = i18nStore.subscribe(render);

  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    window.removeEventListener('keydown', onKeyDown);
    window.removeEventListener('resize', onResize);
    unsubI18n();
    cleanupLeft?.();
    cleanupRight?.();
  };

  return root;
}
