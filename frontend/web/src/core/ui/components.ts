import { el, type Child } from './dom';
import { tr } from '../i18n/i18nStore';
import './menu.css';
import './components.css';

/** AnonGramScreenSurface equivalent: full-screen themed container. */
export function screenSurface(...children: Child[]): HTMLElement {
  return el('div', { class: 'ag-screen' }, ...children);
}

export function pillButton(
  label: Child,
  onClick: (e: Event) => void,
  variant: 'primary' | 'secondary' | 'ghost' | 'danger' = 'primary',
  icon?: SVGElement,
): HTMLButtonElement {
  return el('button', { class: `ag-pill-btn ag-pill-btn--${variant}`, type: 'button', onclick: onClick }, icon, label);
}

export function iconButton(icon: SVGElement, onClick: (e: Event) => void, title?: string): HTMLButtonElement {
  return el('button', { class: 'ag-icon-btn', type: 'button', title, 'aria-label': title, onclick: onClick }, icon);
}

export function chip(label: string, active: boolean, onClick: () => void): HTMLButtonElement {
  return el('button', { class: 'ag-chip' + (active ? ' is-active' : ''), type: 'button', onclick: onClick }, label);
}

export interface SwitchHandle { node: HTMLElement; get(): boolean; set(v: boolean): void }
export function switchControl(initial: boolean, onChange: (v: boolean) => void): SwitchHandle {
  let value = initial;
  const node = el('button', { class: 'ag-switch' + (value ? ' is-on' : ''), type: 'button', role: 'switch', 'aria-checked': String(value) },
    el('span', { class: 'ag-switch-thumb' }));
  const set = (v: boolean) => {
    value = v;
    node.classList.toggle('is-on', v);
    node.setAttribute('aria-checked', String(v));
  };
  node.addEventListener('click', () => { set(!value); onChange(value); });
  return { node, get: () => value, set };
}

export interface ModalHandle { close(): void; root: HTMLElement; body: HTMLElement }
function mountOverlay(kind: 'dialog' | 'sheet', content: HTMLElement, onClose?: () => void): ModalHandle {
  const overlay = el('div', { class: `ag-overlay ag-overlay--${kind}` });
  overlay.appendChild(el('div', { class: `ag-${kind}` }, content));
  let closed = false;
  const onKey = (e: KeyboardEvent) => { if (e.key === 'Escape') close(); };
  function close() {
    if (closed) return;
    closed = true;
    document.removeEventListener('keydown', onKey);
    overlay.classList.remove('is-open');
    window.setTimeout(() => overlay.remove(), 200);
    onClose?.();
  }
  overlay.addEventListener('mousedown', (e) => { if (e.target === overlay) close(); });
  document.addEventListener('keydown', onKey);
  document.body.appendChild(overlay);
  requestAnimationFrame(() => overlay.classList.add('is-open'));
  return { close, root: overlay, body: content };
}

export function openDialog(opts: {
  title?: string; message?: string; content?: HTMLElement;
  confirmLabel?: string; cancelLabel?: string | null; danger?: boolean;
  onConfirm?: () => void; onCancel?: () => void;
}): ModalHandle {
  const body = el('div', { class: 'ag-dialog-body' },
    opts.title ? el('div', { class: 'ag-dialog-title' }, opts.title) : null,
    opts.message ? el('div', { class: 'ag-dialog-msg' }, opts.message) : null,
    opts.content ?? null);
  const handle = mountOverlay('dialog', body, opts.onCancel);
  const actions = el('div', { class: 'ag-dialog-actions' });
  if (opts.cancelLabel !== null) {
    actions.appendChild(pillButton(opts.cancelLabel ?? tr('common.cancel'), () => handle.close(), 'ghost'));
  }
  actions.appendChild(pillButton(opts.confirmLabel ?? tr('common.ok'), () => { handle.close(); opts.onConfirm?.(); }, opts.danger ? 'danger' : 'primary'));
  body.appendChild(actions);
  return handle;
}

export function openBottomSheet(content: HTMLElement, onClose?: () => void): ModalHandle {
  const wrap = el('div', { class: 'ag-sheet-body' }, el('div', { class: 'ag-sheet-grab' }), content);
  return mountOverlay('sheet', wrap, onClose);
}

/** Snackbar with optional action. Returns dismiss fn. */
export function showSnackbar(message: string, action?: { label: string; onClick: () => void }, durationMs = 4000): () => void {
  const node = el('div', { class: 'ag-snackbar' }, el('span', {}, message));
  const dismiss = () => { node.classList.remove('is-visible'); window.setTimeout(() => node.remove(), 220); };
  if (action) node.appendChild(el('button', { class: 'ag-snackbar-action', onclick: () => { dismiss(); action.onClick(); } }, action.label));
  document.body.appendChild(node);
  requestAnimationFrame(() => node.classList.add('is-visible'));
  window.setTimeout(dismiss, durationMs);
  return dismiss;
}
