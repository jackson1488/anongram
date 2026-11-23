import { clear, el } from '../../../../core/ui/dom';
import { allIcons as icons } from '../../../../core/ui/icons';
import { iconButton, pillButton } from '../../../../core/ui/components';
import { pendingForwardStore } from '../PendingForwardStore';
import { toolsT } from '../i18n/ToolsI18n';
import '../tools.css';

export interface ForwardPreviewHandle {
  close(): void;
}

export function openForwardPreviewOverlay(opts?: {
  onCancelAll?: () => void;
  onClose?: () => void;
}): ForwardPreviewHandle {
  const backdrop = el('div', { class: 'ag-fwd-modal-backdrop' });
  const card = el('div', { class: 'ag-fwd-preview' });
  backdrop.appendChild(card);

  let closed = false;
  const onKey = (e: KeyboardEvent) => {
    if (e.key === 'Escape') close();
  };

  function close(): void {
    if (closed) return;
    closed = true;
    unsub();
    document.removeEventListener('keydown', onKey);
    backdrop.remove();
    opts?.onClose?.();
  }

  backdrop.addEventListener('mousedown', (e) => {
    if (e.target === backdrop) close();
  });
  document.addEventListener('keydown', onKey);

  function render(): void {
    clear(card);
    const state = pendingForwardStore.getState();
    if (!state || state.items.length === 0) {
      close();
      return;
    }

    const s = toolsT();
    const activeCount = state.items.filter((it) => !state.excludedIds.has(it.id)).length;

    const head = el(
      'div',
      { class: 'ag-fwd-preview__head' },
      el('div', { class: 'ag-fwd-preview__title' }, `${s.pendingForwardPreviewTitle} (${activeCount})`),
      iconButton(icons.close(18), close, s.close),
    );

    const banner = el(
      'div',
      { class: 'ag-fwd-anon-banner' },
      icons.shield(16),
      el('span', {}, `${s.anonymousForwardBadge} · ${s.anonymousForwardHint}`),
    );

    const list = el('div', { class: 'ag-fwd-preview__list' });
    for (const item of state.items) {
      const isExcluded = state.excludedIds.has(item.id);
      const msg = item.message;
      // AnonGram Core Protocol: strictly anonymous preview (forwardedFrom = null, no senderName)
      const bubble = el('div', { class: 'ag-fwd-preview__bubble' });
      if (msg.attachment) {
        bubble.appendChild(
          el('div', { class: 'ag-fwd-preview__att' }, icons.file(14), msg.attachment.title),
        );
      }
      if (msg.text) {
        bubble.appendChild(el('div', {}, msg.text));
      } else if (!msg.attachment) {
        bubble.appendChild(el('div', {}, s.messageFallback));
      }

      const row = el(
        'div',
        {
          class: 'ag-fwd-preview__item' + (isExcluded ? ' is-excluded' : ''),
          onclick: () => {
            const allRemoved = pendingForwardStore.toggleInclude(item.id);
            if (allRemoved) {
              close();
              opts?.onCancelAll?.();
            }
          },
        },
        el(
          'div',
          {
            class: 'ag-fwd-row__check' + (!isExcluded ? '' : ''),
            style: !isExcluded
              ? 'background:var(--ag-accent);border-color:var(--ag-accent);'
              : 'background:transparent;border-color:var(--ag-divider);',
          },
          !isExcluded ? icons.check(14) : null,
        ),
        bubble,
      );
      list.appendChild(row);
    }

    const actions = el(
      'div',
      { class: 'ag-fwd-preview__actions' },
      pillButton(
        s.pendingForwardCancelAll,
        () => {
          pendingForwardStore.clear();
          close();
          opts?.onCancelAll?.();
        },
        'danger',
        icons.trash(16),
      ),
      pillButton(s.done, close, 'primary'),
    );

    card.append(head, banner, list, actions);
  }

  const unsub = pendingForwardStore.subscribe(render);
  render();
  document.body.appendChild(backdrop);

  return { close };
}
