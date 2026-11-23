import { i18nStore } from '../../../core/i18n/i18nStore';
import { copyToClipboard, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { pillButton } from '../../../core/ui/components';
import { showToast } from '../../../core/ui/toast';

interface ExternalLinkStrings {
  title: string;
  warning: string;
  copyUrl: string;
  copiedToast: string;
  openInNewTab: string;
  cancel: string;
  invalidUrlToast: string;
}

const RU: ExternalLinkStrings = {
  title: 'Внешняя ссылка',
  warning:
    'Вы покидаете защищённую среду Onion / AnonGram. Переход по внешней ссылке откроет её в новой вкладке браузера.',
  copyUrl: 'Копировать',
  copiedToast: 'Ссылка скопирована',
  openInNewTab: 'Открыть',
  cancel: 'Отмена',
  invalidUrlToast: 'Некорректная или небезопасная ссылка',
};

const EN: ExternalLinkStrings = {
  title: 'External Link',
  warning:
    'You are leaving the encrypted Onion / AnonGram environment. Opening this link will load it in a new browser tab.',
  copyUrl: 'Copy URL',
  copiedToast: 'Link copied',
  openInNewTab: 'Open',
  cancel: 'Cancel',
  invalidUrlToast: 'Invalid or unsafe URL',
};

function linkT(): ExternalLinkStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}

function normalizeSafeUrl(rawUrl: string): string | null {
  const trimmed = rawUrl.trim();
  if (!trimmed) return null;
  const candidate = /^https?:\/\//i.test(trimmed) ? trimmed : `https://${trimmed}`;
  try {
    const parsed = new URL(candidate);
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return null;
    return parsed.toString();
  } catch {
    return null;
  }
}

/**
 * Safe external link confirmation dialog.
 * Warns the user before leaving Onion/AnonGram and lets them copy the URL or open it in a new tab.
 */
export function openExternalLink(rawUrl: string): void {
  const s = linkT();
  const safeUrl = normalizeSafeUrl(rawUrl);
  if (!safeUrl) {
    showToast(s.invalidUrlToast);
    return;
  }

  const overlay = el('div', { class: 'ag-overlay ag-overlay--dialog' });
  let closed = false;

  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') close();
  };

  function close(): void {
    if (closed) return;
    closed = true;
    document.removeEventListener('keydown', onKey);
    overlay.classList.remove('is-open');
    window.setTimeout(() => overlay.remove(), 200);
  }

  const urlBox = el(
    'div',
    {
      style:
        'margin:12px 0 4px;padding:10px 12px;border-radius:14px;background:var(--ag-input-bg);border:1px solid var(--ag-input-border);color:var(--ag-accent);font-family:monospace;font-size:12.5px;line-height:1.4;word-break:break-all;max-height:110px;overflow-y:auto;',
    },
    safeUrl,
  );

  const actions = el(
    'div',
    { class: 'ag-dialog-actions', style: 'flex-wrap:wrap;' },
    pillButton(s.cancel, close, 'ghost'),
    pillButton(
      s.copyUrl,
      async () => {
        await copyToClipboard(safeUrl);
        showToast(s.copiedToast);
      },
      'secondary',
      icons.copy(16),
    ),
    pillButton(
      s.openInNewTab,
      () => {
        close();
        window.open(safeUrl, '_blank', 'noopener,noreferrer');
      },
      'primary',
      icons.globe(16),
    ),
  );

  const dialog = el(
    'div',
    { class: 'ag-dialog', style: 'max-width:400px;' },
    el(
      'div',
      {
        class: 'ag-dialog-title',
        style: 'display:flex;align-items:center;gap:8px;',
      },
      icons.alert(20),
      el('span', {}, s.title),
    ),
    el('div', { class: 'ag-dialog-msg' }, s.warning),
    urlBox,
    actions,
  );

  overlay.appendChild(dialog);
  overlay.addEventListener('mousedown', (e) => {
    if (e.target === overlay) close();
  });
  document.addEventListener('keydown', onKey);
  document.body.appendChild(overlay);
  requestAnimationFrame(() => overlay.classList.add('is-open'));
}
