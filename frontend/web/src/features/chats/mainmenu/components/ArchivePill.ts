import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import type { ChatThread } from '../../models';
import { mainMenuT } from '../i18n';

export function createArchivePill(archived: ChatThread[], onClick: () => void): HTMLElement {
  const preview = archived
    .slice(0, 2)
    .map((th) => th.title)
    .join(', ');
  return el(
    'div',
    { class: 'ag-archive-pill', onClick },
    el('div', { class: 'ag-archive-pill__icon' }, icons.archive(20)),
    el(
      'div',
      { class: 'ag-chat-row__body' },
      el('div', { class: 'ag-chat-row__title' }, mainMenuT().archive),
      el('div', { class: 'ag-chat-row__preview' }, preview),
    ),
    el('span', { class: 'ag-archive-pill__count' }, String(archived.length)),
  );
}
