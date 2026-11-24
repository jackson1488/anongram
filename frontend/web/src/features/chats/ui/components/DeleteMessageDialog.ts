import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { chatActionsT } from '../../i18n/ChatActionsI18n';

/**
 * Telegram-style delete message confirmation modal mirroring Android `DeleteMessageDialog.kt`.
 * Shows count-aware title/prompt, optional "Also delete for everyone" checkbox, and capsule actions.
 */
export function openDeleteMessageDialog(
  count: number,
  canDeleteForAll: boolean,
  onConfirm: (forEveryone: boolean) => void,
  peerName = '',
): void {
  const s = chatActionsT();
  const safeCount = Math.max(1, count);
  let forEveryone = false;
  let closed = false;

  const overlay = el('div', {
    class: 'ag-overlay ag-overlay--dialog ag-del-overlay',
    role: 'dialog',
    'aria-modal': 'true',
    style: [
      'z-index:9200',
      'backdrop-filter:blur(10px)',
      '-webkit-backdrop-filter:blur(10px)',
    ].join(';'),
  });

  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      close();
    }
  };

  function close(): void {
    if (closed) return;
    closed = true;
    document.removeEventListener('keydown', onKeyDown);
    overlay.classList.remove('is-open');
    window.setTimeout(() => overlay.remove(), 200);
  }

  const titleEl = el(
    'div',
    {
      class: 'ag-dialog-title',
      style: 'font-size:17px;font-weight:700;color:var(--ag-text-primary);margin-bottom:8px;',
    },
    safeCount > 1 ? s.deleteMultipleTitle(safeCount) : s.deleteSingleTitle,
  );

  const promptEl = el(
    'div',
    {
      class: 'ag-dialog-msg',
      style: 'font-size:14px;line-height:1.42;color:var(--ag-text-secondary);margin-bottom:0;',
    },
    safeCount > 1 ? s.deleteMultiplePrompt : s.deleteSinglePrompt,
  );

  let checkboxRow: HTMLElement | null = null;
  if (canDeleteForAll) {
    const checkIcon = icons.check(13);
    checkIcon.style.opacity = '0';
    checkIcon.style.transition = 'opacity 0.14s ease';

    const visualBox = el(
      'span',
      {
        'aria-hidden': 'true',
        style: [
          'width:20px',
          'height:20px',
          'border-radius:6px',
          'border:2px solid var(--ag-text-secondary)',
          'background:transparent',
          'color:var(--ag-on-accent)',
          'display:inline-flex',
          'align-items:center',
          'justify-content:center',
          'flex-shrink:0',
          'transition:background-color 0.14s ease,border-color 0.14s ease',
        ].join(';'),
      },
      checkIcon,
    );

    const input = el('input', {
      type: 'checkbox',
      style: [
        'position:absolute',
        'opacity:0',
        'width:1px',
        'height:1px',
        'margin:0',
        'pointer-events:none',
      ].join(';'),
    }) as HTMLInputElement;

    const syncCheckUi = () => {
      forEveryone = input.checked;
      visualBox.style.background = forEveryone ? 'var(--ag-accent)' : 'transparent';
      visualBox.style.borderColor = forEveryone ? 'var(--ag-accent)' : 'var(--ag-text-secondary)';
      checkIcon.style.opacity = forEveryone ? '1' : '0';
    };

    input.addEventListener('change', syncCheckUi);

    const checkboxLabel = peerName.trim() ? s.deleteForPeer(peerName.trim()) : s.deleteForEveryone;
    checkboxRow = el(
      'label',
      {
        style: [
          'position:relative',
          'display:flex',
          'align-items:center',
          'gap:10px',
          'margin-top:14px',
          'padding:8px 10px',
          'border-radius:14px',
          'cursor:pointer',
          'color:var(--ag-text-primary)',
          'font-size:14px',
          'font-weight:500',
          'user-select:none',
          'transition:background-color 0.14s ease',
        ].join(';'),
      },
      input,
      visualBox,
      el('span', { style: 'flex:1;min-width:0;' }, checkboxLabel),
    );

    checkboxRow.addEventListener('mouseenter', () => {
      checkboxRow!.style.background = 'var(--ag-hover)';
    });
    checkboxRow.addEventListener('mouseleave', () => {
      checkboxRow!.style.background = 'transparent';
    });
  }

  const cancelBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-pill-btn ag-pill-btn--ghost',
      style: [
        'min-height:38px',
        'padding:0 16px',
        'border-radius:9999px',
        'background:transparent',
        'color:var(--ag-text-secondary)',
        'font-size:14px',
        'font-weight:600',
      ].join(';'),
      onClick: close,
    },
    s.cancel,
  ) as HTMLButtonElement;

  cancelBtn.addEventListener('mouseenter', () => {
    cancelBtn.style.background = 'var(--ag-hover)';
  });
  cancelBtn.addEventListener('mouseleave', () => {
    cancelBtn.style.background = 'transparent';
  });

  const confirmBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-pill-btn',
      style: [
        'min-height:38px',
        'padding:0 18px',
        'border-radius:9999px',
        'background:var(--ag-danger-soft)',
        'border:1px solid var(--ag-danger-border)',
        'color:var(--ag-danger)',
        'font-size:14px',
        'font-weight:700',
      ].join(';'),
      onClick: () => {
        if (closed) return;
        const deleteAll = canDeleteForAll && forEveryone;
        close();
        onConfirm(deleteAll);
      },
    },
    s.deleteConfirm,
  ) as HTMLButtonElement;

  confirmBtn.addEventListener('mouseenter', () => {
    confirmBtn.style.background = 'var(--ag-danger)';
    confirmBtn.style.color = 'var(--ag-on-accent)';
  });
  confirmBtn.addEventListener('mouseleave', () => {
    confirmBtn.style.background = 'var(--ag-danger-soft)';
    confirmBtn.style.color = 'var(--ag-danger)';
  });

  const actionsEl = el(
    'div',
    {
      class: 'ag-dialog-actions',
      style: 'display:flex;align-items:center;justify-content:flex-end;gap:8px;margin-top:18px;',
    },
    cancelBtn,
    confirmBtn,
  );

  const dialog = el(
    'div',
    {
      class: 'ag-dialog',
      style: [
        'width:100%',
        'max-width:360px',
        'background:var(--ag-card)',
        'border:1px solid var(--ag-card-border)',
        'border-radius:22px',
        'padding:20px',
        'box-shadow:0 16px 48px var(--ag-shadow)',
      ].join(';'),
    },
    titleEl,
    promptEl,
    checkboxRow,
    actionsEl,
  );

  dialog.addEventListener('click', (e) => e.stopPropagation());
  overlay.appendChild(dialog);
  overlay.addEventListener('mousedown', (e) => {
    if (e.target === overlay) close();
  });
  overlay.addEventListener('click', (e) => {
    if (e.target === overlay) close();
  });

  document.addEventListener('keydown', onKeyDown);
  document.body.appendChild(overlay);
  window.requestAnimationFrame(() => overlay.classList.add('is-open'));
}
