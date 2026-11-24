import { el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { chatActionsT } from '../../i18n/ChatActionsI18n';
import type { ChatMessage } from '../../models';
import { pinnedMessageStore } from './PinnedMessageBar';

export const QUICK_REACTIONS = ['👍', '❤️', '🔥', '😂', '😮', '😢', '🛡️'];

export interface MenuActions {
  onReact: (emoji: string) => void;
  onReply: () => void;
  onEdit?: () => void;
  onTranslate?: () => void;
  onCopy: () => void;
  onForward: () => void;
  onPin?: () => void;
  onSelect?: () => void;
  onDelete: () => void;
}

let current: { close: () => void } | null = null;

export function closeMessageContextMenu(): void {
  current?.close();
}

/**
 * Unified Telegram-style message context popup for both desktop and mobile:
 * crisp blurred surface with compact reaction bar + icon/label action list.
 */
export function showMessageContextMenu(msg: ChatMessage, x: number, y: number, a: MenuActions): void {
  closeMessageContextMenu();
  const ca = chatActionsT();
  const isPinned = pinnedMessageStore.isPinned(msg.chatId, msg.id);
  const hasText = msg.text.trim().length > 0;
  const openedAt = performance.now();
  let closed = false;

  const list = el('div', { class: 'ag-cs-menu__list', role: 'menu' });
  const addItem = (ico: SVGElement, label: string, fn: () => void, danger = false) => {
    ico.style.flexShrink = '0';
    ico.style.opacity = danger ? '1' : '0.82';
    list.appendChild(
      el(
        'button',
        {
          type: 'button',
          class: `ag-cs-menu__item${danger ? ' is-danger' : ''}`,
          role: 'menuitem',
          style: 'height:36px;padding:0 10px;border-radius:11px;gap:10px;font-size:13.5px;',
          onClick: (e: Event) => {
            e.stopPropagation();
            close();
            fn();
          },
        },
        ico,
        el(
          'span',
          {
            style: 'flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;',
          },
          label,
        ),
      ),
    );
  };

  addItem(icons.reply(18), ca.reply, a.onReply);
  if (msg.isOutgoing && hasText && a.onEdit) {
    addItem(icons.edit(18), ca.edit, a.onEdit);
  }
  if (hasText && a.onTranslate) {
    addItem(tgIcons.language(18), ca.translate, a.onTranslate);
  }
  if (hasText) {
    addItem(icons.copy(18), ca.copy, a.onCopy);
  }
  addItem(
    isPinned ? tgIcons.pinOff(18) : icons.pin(18),
    isPinned ? ca.unpin : ca.pin,
    () => {
      pinnedMessageStore.togglePin(msg.chatId, msg.id);
      a.onPin?.();
    },
  );
  addItem(icons.forward(18), ca.forward, a.onForward);
  if (a.onSelect) {
    addItem(tgIcons.select(18), ca.select, a.onSelect);
  }

  list.appendChild(
    el('div', {
      role: 'separator',
      style: 'height:1px;margin:4px 8px;background:var(--ag-divider);',
    }),
  );
  addItem(icons.trash(18), ca.delete, a.onDelete, true);

  const reactions = el(
    'div',
    {
      class: 'ag-cs-menu__reactions',
      style: 'padding:3px 4px 7px;gap:2px;',
    },
    ...QUICK_REACTIONS.map((em) =>
      el(
        'button',
        {
          type: 'button',
          class: 'ag-cs-menu__react',
          style: 'width:32px;height:32px;font-size:18px;',
          onClick: (e: Event) => {
            e.stopPropagation();
            close();
            a.onReact(em);
          },
        },
        em,
      ),
    ),
  );

  const menu = el(
    'div',
    {
      class: 'ag-cs-menu',
      style: [
        'min-width:228px',
        'max-width:min(272px, calc(100vw - 16px))',
        'max-height:calc(100vh - 16px)',
        'overflow-y:auto',
        'padding:6px',
        'border-radius:18px',
        'background:color-mix(in srgb, var(--ag-island-bg) 94%, transparent)',
        'border:1px solid var(--ag-island-border)',
        'box-shadow:0 12px 32px var(--ag-shadow)',
        'backdrop-filter:blur(20px)',
        '-webkit-backdrop-filter:blur(20px)',
        'user-select:none',
        '-webkit-touch-callout:none',
        'opacity:0',
        'transform:scale(0.92)',
        'transition:opacity 0.14s ease,transform 0.14s cubic-bezier(0.2, 0.8, 0.2, 1)',
      ].join(';'),
      onContextMenu: (e: Event) => e.preventDefault(),
    },
    reactions,
    list,
  );

  const backdrop = el('div', {
    class: 'ag-cs-menu-backdrop',
    onPointerDown: (e: Event) => {
      const pe = e as PointerEvent;
      if (e.target === backdrop && pe.pointerType === 'mouse') {
        close();
      }
    },
    onClick: (e: Event) => {
      if (e.target !== backdrop) return;
      if (performance.now() - openedAt < 120) return;
      close();
    },
    onContextMenu: (e: Event) => {
      e.preventDefault();
      close();
    },
  });

  const layer = el(
    'div',
    {
      class: 'ag-cs-menu-layer',
      style: 'z-index:9050;',
      onContextMenu: (e: Event) => e.preventDefault(),
    },
    backdrop,
    menu,
  );

  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      close();
    }
  };
  const onResize = () => close();

  function close(): void {
    if (closed) return;
    closed = true;
    document.removeEventListener('keydown', onKey);
    window.removeEventListener('resize', onResize);
    layer.remove();
    if (current?.close === close) current = null;
  }

  document.addEventListener('keydown', onKey);
  window.addEventListener('resize', onResize, { passive: true });
  current = { close };
  document.body.appendChild(layer);

  const vw = window.innerWidth;
  const vh = window.innerHeight;
  const pad = 8;
  const gap = 6;
  const w = menu.offsetWidth || 236;
  const h = menu.offsetHeight || 300;

  const preferRight = (x + w > vw - pad || (msg.isOutgoing && x > vw * 0.52)) && x - w >= pad;
  const rawLeft = preferRight ? x - w : x;
  const left = Math.max(pad, Math.min(vw - w - pad, rawLeft));

  const canFitBelow = y + gap + h <= vh - pad;
  const canFitAbove = y - gap - h >= pad;
  const flipUp = !canFitBelow && canFitAbove;
  const rawTop = flipUp ? y - gap - h : y + gap;
  const top = Math.max(pad, Math.min(vh - h - pad, rawTop));

  menu.style.left = `${Math.round(left)}px`;
  menu.style.top = `${Math.round(top)}px`;
  menu.style.transformOrigin = `${flipUp ? 'bottom' : 'top'} ${preferRight ? 'right' : 'left'}`;

  window.requestAnimationFrame(() => {
    if (!closed) {
      menu.style.opacity = '1';
      menu.style.transform = 'scale(1)';
    }
  });
}
