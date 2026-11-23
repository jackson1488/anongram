import { el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { chatStore } from '../../chats/chatStore';
import { NavigationI18n } from '../i18n';

export function createAppHeader(rightActions?: HTMLElement): HTMLElement {
  const s = NavigationI18n.current;
  const badge = el(
    'button',
    {
      class: `ag-onion-badge ${chatStore.onionOnline ? '' : 'is-offline'}`,
      title: s.onionStatusTitle,
      onClick: () => chatStore.toggleOnionOnline(),
    },
    el('span', { class: 'ag-onion-dot' }),
    el('span', null, chatStore.onionOnline ? s.onionOnline : s.onionOffline),
  );

  const unsub = chatStore.subscribe(() => {
    const cur = NavigationI18n.current;
    badge.className = `ag-onion-badge ${chatStore.onionOnline ? '' : 'is-offline'}`;
    const label = badge.lastChild;
    if (label) label.textContent = chatStore.onionOnline ? cur.onionOnline : cur.onionOffline;
  });

  const header = el(
    'header',
    { class: 'ag-app-header' },
    el(
      'div',
      { class: 'ag-app-header__left' },
      el('div', { class: 'ag-app-header__logo' }, icons.shield(17)),
      el('span', { class: 'ag-app-header__title' }, 'AnonGram'),
      badge,
    ),
    rightActions ?? null,
  );
  (header as unknown as { __cleanup?: () => void }).__cleanup = () => unsub();
  return header;
}
