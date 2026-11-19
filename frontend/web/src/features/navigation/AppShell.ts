import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { createCallsView } from '../calls/CallsView';
import { createChatListView } from '../chats/ChatListView';
import { createChatRoomView } from '../chats/ChatRoomView';
import '../chats/chats.css';
import {
  createSettingsDetailView,
  createSettingsListPane,
  type SettingsRoute,
} from '../settings/SettingsView';
import '../settings/settings.css';

type MainTab = 'chats' | 'calls' | 'settings';

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

    const navItems: Array<{ id: MainTab; label: string; icon: SVGElement }> = [
      { id: 'chats', label: t.nav.chats, icon: icons.chats(20) },
      { id: 'calls', label: t.nav.calls, icon: icons.phone(20) },
      { id: 'settings', label: t.nav.settings, icon: icons.settings(20) },
    ];

    // Desktop Left Rail
    const rail = el(
      'aside',
      { class: 'ag-left-rail' },
      el('div', { class: 'ag-rail-brand' }, icons.shield(22)),
      ...navItems.map((item) =>
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
    );
    body.appendChild(rail);

    // Left Pane
    const leftPane = el('section', { class: 'ag-left-pane' });
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
    } else if (activeTab === 'calls') {
      leftContent = createCallsView();
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

    // Mobile Floating Island (hidden when detail view is open on mobile)
    const showMobileDetail =
      !isDesktop() &&
      ((activeTab === 'chats' && mobileChatOpen && activeChatId !== null) ||
        (activeTab === 'settings' && mobileSettingsOpen));

    if (!showMobileDetail) {
      root.appendChild(
        el(
          'nav',
          { class: 'ag-island' },
          ...navItems.map((item) =>
            el(
              'button',
              {
                class: `ag-island__btn ${activeTab === item.id ? 'is-active' : ''}`,
                onClick: () => {
                  activeTab = item.id;
                  mobileChatOpen = false;
                  mobileSettingsOpen = false;
                  render();
                },
              },
              item.icon,
              el('span', null, item.label),
            ),
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
