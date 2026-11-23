import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import { showToast } from '../../../../core/ui/toast';
import { chatScreenT } from '../../i18n';
import { chatStore } from '../../chatStore';
import type { ChatMessage } from '../../models';

/** Anonymous forward (forwardedFrom = null always) of one or many messages. */
export function openForwardModal(messages: ChatMessage[], onDone?: () => void): void {
  const t = i18nStore.t;
  const overlay = el('div', {
    class: 'ag-modal-overlay',
    onClick: (e: Event) => {
      if (e.target === overlay) overlay.remove();
    },
  });
  overlay.appendChild(
    el(
      'div',
      { class: 'ag-card ag-forward-modal' },
      el(
        'div',
        { class: 'ag-forward-modal__head' },
        el('h3', { style: 'margin:0;font-size:17px;' }, t.chats.forwardAnonTitle),
        el('button', { class: 'ag-icon-btn', onClick: () => overlay.remove() }, icons.close(18)),
      ),
      el('p', { style: 'margin:0 0 10px;font-size:13px;color:var(--ag-text-secondary);' }, t.chats.forwardSelectChat),
      el(
        'div',
        { class: 'ag-forward-list' },
        ...chatStore.threads.map((thread) =>
          el(
            'div',
            {
              class: 'ag-chat-row',
              onClick: () => {
                for (const m of messages) chatStore.forwardAnonymously(thread.id, m);
                overlay.remove();
                showToast(chatScreenT().forwardedToast(messages.length));
                onDone?.();
              },
            },
            el('div', { class: 'ag-avatar', style: `background:${thread.avatarColor}22` }, thread.avatarEmoji),
            el('div', { class: 'ag-chat-row__title' }, thread.title),
          ),
        ),
      ),
    ),
  );
  document.body.appendChild(overlay);
}
