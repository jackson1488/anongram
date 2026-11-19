import { el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';

export function createAppHeader(rightActions?: HTMLElement): HTMLElement {
  return el(
    'header',
    { class: 'ag-app-header' },
    el(
      'div',
      { class: 'ag-app-header__left' },
      el('div', { class: 'ag-app-header__logo' }, icons.shield(17)),
      el('span', { class: 'ag-app-header__title' }, 'AnonGram'),
      el(
        'span',
        { class: 'ag-onion-badge' },
        el('span', { class: 'ag-onion-dot' }),
        'Onion v3',
      ),
    ),
    rightActions ?? null,
  );
}
