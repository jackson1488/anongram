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
        type: 'button',
        class: 'ag-menu-item' + (it.danger ? ' is-danger' : '') + (it.checked ? ' is-checked' : ''),
        role: 'menuitem',
        'aria-checked': it.checked !== undefined ? String(it.checked) : undefined,
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
  const pad = 8;
  const gap = 6;
  const mw = menu.offsetWidth;
  const mh = menu.offsetHeight;
  let x: number;
  let y: number;
  let originX: 'left' | 'right' = opts.align === 'left' ? 'left' : 'right';
  let originY: 'top' | 'bottom' = 'top';

  if (anchor) {
    const r = anchor.getBoundingClientRect();
    x = opts.align === 'left' ? r.left : r.right - mw;
    const spaceBelow = vh - r.bottom - pad;
    const spaceAbove = r.top - pad;
    const flipUp = spaceBelow < mh + gap && spaceAbove > spaceBelow;
    y = flipUp ? r.top - mh - gap : r.bottom + gap;
    originY = flipUp ? 'bottom' : 'top';
  } else {
    const rawX = opts.x ?? pad;
    const rawY = opts.y ?? pad;
    const flipLeft = rawX + mw > vw - pad && rawX - mw >= pad;
    const flipUp = rawY + mh > vh - pad && rawY - mh >= pad;
    x = flipLeft ? rawX - mw : rawX;
    y = flipUp ? rawY - mh : rawY;
    originX = flipLeft ? 'right' : 'left';
    originY = flipUp ? 'bottom' : 'top';
  }

  x = Math.min(Math.max(pad, x), Math.max(pad, vw - mw - pad));
  y = Math.min(Math.max(pad, y), Math.max(pad, vh - mh - pad));
  menu.style.left = `${Math.round(x)}px`;
  menu.style.top = `${Math.round(y)}px`;
  menu.style.transformOrigin = `${originY} ${originX}`;
  requestAnimationFrame(() => menu.classList.add('is-open'));

  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') close();
  };
  const onResize = () => close();
  document.addEventListener('keydown', onKey);
  window.addEventListener('resize', onResize, { passive: true });
  backdrop.addEventListener('pointerdown', (e) => {
    if (e.target === backdrop && e.pointerType === 'mouse') close();
  });
  backdrop.addEventListener('click', (e) => {
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
    window.removeEventListener('resize', onResize);
    backdrop.remove();
    opts.onClose?.();
  }
  closeCurrent = close;
  return close;
}
