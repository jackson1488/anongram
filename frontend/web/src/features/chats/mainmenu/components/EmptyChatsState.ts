import { el } from '../../../../core/ui/dom';
import { mainMenuT } from '../i18n';

export function createEmptyChatsState(text?: string): HTMLElement {
  const t = mainMenuT();
  return el(
    'div',
    { class: 'ag-empty-inline ag-mm-empty' },
    el('div', { class: 'ag-mm-empty__title' }, text ?? t.noChats),
    text ? null : el('div', { class: 'ag-mm-empty__hint' }, t.noChatsHint),
  );
}
