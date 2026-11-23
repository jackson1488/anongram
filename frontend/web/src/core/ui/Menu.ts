import { el } from './dom';

export interface MenuItem {
  label?: string;
  icon?: SVGElement | (() => SVGElement);
  danger?: boolean;
  disabled?: boolean;
  separator?: boolean;
  checked?: boolean;
  onClick?: () => void;
}
export interface MenuOpts {
  /** Used when anchor is null */
  x?: number;
  y?: number;
  align?: 'left' | 'right';
  compact?: boolean;
  onClose?: () => void;
}

let closeCurrent: (() => void) | null = null;

export function closeMenu(): void {
  closeCurrent?.();
}

/** TG-Web-A style context menu with transparent backdrop. Returns close fn. */
export function openMenu(anchor: HTMLElement | null, items: MenuItem[], opts: MenuOpts = {}): () => void {
  closeMenu();
  const backdrop = el('div', { class: 'ag-menu-backdrop' });
  const menu = el('div', { class: 'ag-menu' + (opts.compact === false ? '' : ' ag-menu--compact'), role: 'menu' });
  for (const it of items) {
    if (it.separator) {
      menu.appendChild(el('div', { class: 'ag-menu-sep' }));
      continue;
    }
    const iconNode = it.icon ? (typeof it.icon === 'function' ? it.icon() : it.icon) : null;
    const row = el(
      'button',
      {
        class: 'ag-menu-item' + (it.danger ? ' is-danger' : '') + (it.checked ? ' is-checked' : ''),
        role: 'menuitem',
        disabled: it.disabled,
        onclick: (e: Event) => {
          e.stopPropagation();
          close();
          it.onClick?.();
        },
      },
      iconNode ? el('span', { class: 'ag-menu-icon' }, iconNode) : null,
      el('span', { class: 'ag-menu-label' }, it.label ?? ''),
    );
    menu.appendChild(row);
  }
  backdrop.appendChild(menu);
  document.body.appendChild(backdrop);

  const vw = window.innerWidth;
  const vh = window.innerHeight;
  const mw = menu.offsetWidth;
  const mh = menu.offsetHeight;
  let x: number;
  let y: number;
  if (anchor) {
    const r = anchor.getBoundingClientRect();
    x = opts.align === 'left' ? r.left : r.right - mw;
    y = r.bottom + 4;
    if (y + mh > vh - 8) y = Math.max(8, r.top - mh - 4);
  } else {
    x = opts.x ?? 0;
    y = opts.y ?? 0;
  }
  x = Math.min(Math.max(8, x), Math.max(8, vw - mw - 8));
  y = Math.min(Math.max(8, y), Math.max(8, vh - mh - 8));
  menu.style.left = `${x}px`;
  menu.style.top = `${y}px`;
  requestAnimationFrame(() => menu.classList.add('is-open'));

  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') close();
  };
  document.addEventListener('keydown', onKey);
  backdrop.addEventListener('mousedown', (e) => {
    if (e.target === backdrop) close();
  });
  backdrop.addEventListener('contextmenu', (e) => {
    e.preventDefault();
    close();
  });

  function close(): void {
    if (closeCurrent !== close) return;
    closeCurrent = null;
    document.removeEventListener('keydown', onKey);
    backdrop.remove();
    opts.onClose?.();
  }
  closeCurrent = close;
  return close;
}
