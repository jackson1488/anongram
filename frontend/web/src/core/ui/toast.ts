import { el } from './dom';

let toastTimer: number | undefined;
let toastNode: HTMLElement | null = null;

export function showToast(message: string, durationMs = 2200): void {
  if (toastNode) {
    toastNode.remove();
    toastNode = null;
  }
  if (toastTimer) {
    window.clearTimeout(toastTimer);
  }
  const node = el('div', { class: 'ag-toast' }, message);
  document.body.appendChild(node);
  toastNode = node;
  requestAnimationFrame(() => node.classList.add('is-visible'));
  toastTimer = window.setTimeout(() => {
    node.classList.remove('is-visible');
    window.setTimeout(() => {
      if (toastNode === node) {
        node.remove();
        toastNode = null;
      }
    }, 220);
  }, durationMs);
}
