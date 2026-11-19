import { el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { chatStore } from '../../chats/chatStore';

export function createAppHeader(rightActions?: HTMLElement): HTMLElement {
  const badge = el(
    'button',
    {
      class: `ag-onion-badge ${chatStore.onionOnline ? '' : 'is-offline'}`,
      title: 'Onion v3 Relay Status',
      onClick: () => chatStore.toggleOnionOnline(),
    },
    el('span', { class: 'ag-onion-dot' }),
    el('span', null, chatStore.onionOnline ? 'Onion v3' : 'Offline'),
  );

  const unsub = chatStore.subscribe(() => {
    badge.className = `ag-onion-badge ${chatStore.onionOnline ? '' : 'is-offline'}`;
    const label = badge.lastChild;
    if (label) label.textContent = chatStore.onionOnline ? 'Onion v3' : 'Offline';
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
