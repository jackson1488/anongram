import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { chatScreenT } from '../../i18n';

export interface SelectionActions {
  count: number;
  canReply: boolean;
  canCopy: boolean;
  onClose: () => void;
  onReply: () => void;
  onCopy: () => void;
  onForward: () => void;
  onDelete: () => void;
}

/** Android SelectionTopBar: replaces the chat header while messages are selected. */
export function createSelectionTopBar(a: SelectionActions): HTMLElement {
  const t = i18nStore.t;
  const btn = (title: string, ico: SVGElement, fn: () => void, cls = '') =>
    el('button', { class: `ag-icon-btn ${cls}`, title, onClick: fn }, ico);
  return el(
    'div',
    { class: 'ag-chat-header ag-cs-selbar' },
    btn(chatScreenT().cancel, icons.close(22), a.onClose),
    el('div', { class: 'ag-cs-selbar__count' }, chatScreenT().selectedCount(a.count)),
    a.canReply ? btn(t.chats.replyAction, icons.reply(20), a.onReply) : null,
    a.canCopy ? btn(t.chats.copyAction, icons.copy(20), a.onCopy) : null,
    btn(t.chats.forwardAction, icons.forward(20), a.onForward),
    btn(t.chats.deleteAction, icons.trash(20), a.onDelete, 'is-danger'),
  );
}
