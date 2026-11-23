import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { chatScreenT } from '../../i18n';
import type { ChatMessage } from '../../models';

export const QUICK_REACTIONS = ['👍', '❤️', '🔥', '😂', '😮', '😢', '🛡️'];

export interface MenuActions {
  onReact: (emoji: string) => void;
  onReply: () => void;
  onCopy: () => void;
  onForward: () => void;
  onSelect: () => void;
  onDelete: () => void;
}

let current: { close: () => void } | null = null;

export function closeMessageContextMenu(): void {
  current?.close();
}

/** Telegram-Web-A style menu: quick-reaction bar + action list. Desktop: popup at cursor; mobile: bottom sheet. */
export function showMessageContextMenu(msg: ChatMessage, x: number, y: number, a: MenuActions): void {
  closeMessageContextMenu();
  const t = i18nStore.t;
  const cs = chatScreenT();
  const mobile = window.innerWidth < 840;

  const list = el('div', { class: 'ag-cs-menu__list' });
  const item = (ico: SVGElement, label: string, fn: () => void, danger = false) => {
    list.appendChild(
      el(
        'button',
        {
          class: `ag-cs-menu__item${danger ? ' is-danger' : ''}`,
          onClick: () => {
            close();
            fn();
          },
        },
        ico,
        el('span', null, label),
      ),
    );
  };
  item(icons.reply(18), t.chats.replyAction, a.onReply);
  if (msg.text) item(icons.copy(18), t.chats.copyAction, a.onCopy);
  item(icons.forward(18), t.chats.forwardAction, a.onForward);
  item(icons.check(18), cs.select, a.onSelect);
  item(icons.trash(18), t.chats.deleteAction, a.onDelete, true);

  const reactions = el(
    'div',
    { class: 'ag-cs-menu__reactions' },
    ...QUICK_REACTIONS.map((em) =>
      el(
        'button',
        {
          class: 'ag-cs-menu__react',
          onClick: () => {
            close();
            a.onReact(em);
          },
        },
        em,
      ),
    ),
  );

  const menu = el('div', { class: `ag-cs-menu${mobile ? ' is-sheet' : ''}` }, reactions, list);
  const backdrop = el('div', {
    class: 'ag-cs-menu-backdrop',
    onClick: () => close(),
    onContextMenu: (e: Event) => {
      e.preventDefault();
      close();
    },
  });
  const layer = el('div', { class: 'ag-cs-menu-layer' }, backdrop, menu);

  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') close();
  };
  function close() {
    document.removeEventListener('keydown', onKey);
    layer.remove();
    if (current?.close === close) current = null;
  }
  document.addEventListener('keydown', onKey);
  current = { close };
  document.body.appendChild(layer);

  if (!mobile) {
    const w = menu.offsetWidth || 220;
    const h = menu.offsetHeight || 260;
    const left = Math.max(8, Math.min(window.innerWidth - w - 8, x));
    const top = Math.max(8, Math.min(window.innerHeight - h - 8, y));
    menu.style.left = `${left}px`;
    menu.style.top = `${top}px`;
  }
}
