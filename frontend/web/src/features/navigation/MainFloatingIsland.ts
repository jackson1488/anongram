import { el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { NavigationI18n } from './i18n';

export type MainTab = 'chats' | 'services' | 'settings' | 'profile';

export interface MainFloatingIslandProps {
  activeTab: MainTab;
  onTabSelected: (tab: MainTab) => void;
  onQrClick: () => void;
}

/**
 * 1:1 port of Android MainFloatingIsland.kt:
 * 4-tab floating pill capsule with an elevated center Stealth QR button.
 */
export function createMainFloatingIsland(props: MainFloatingIslandProps): HTMLElement {
  const s = NavigationI18n.current;
  const tabs: Array<{ id: MainTab; label: string; icon: SVGElement }> = [
    { id: 'chats', label: s.chats, icon: icons.chats(20) },
    { id: 'services', label: s.services, icon: icons.shield(20) },
    { id: 'settings', label: s.settings, icon: icons.settings(20) },
    { id: 'profile', label: s.profile, icon: icons.user(20) },
  ];

  const makeTab = (item: (typeof tabs)[number]) => {
    const active = props.activeTab === item.id;
    return el(
      'button',
      {
        type: 'button',
        class: active ? 'ag-island__tab is-active' : 'ag-island__tab',
        'aria-current': active ? 'page' : undefined,
        onClick: () => props.onTabSelected(item.id),
      },
      el('span', { class: 'ag-island__icon-pill' }, item.icon),
      el('span', { class: 'ag-island__label' }, item.label),
    );
  };

  return el(
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
      {
        type: 'button',
        class: 'ag-island-qr-btn',
        title: s.qrHub,
        'aria-label': s.qrHub,
        onClick: props.onQrClick,
      },
      icons.shield(26),
    ),
  );
}
