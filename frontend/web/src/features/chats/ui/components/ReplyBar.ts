import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import type { ReplyPreview } from '../../models';

/** Reply preview strip above the composer. Returns null when there is nothing to show. */
export function createReplyBar(reply: ReplyPreview | null, onClose: () => void): HTMLElement | null {
  if (!reply) return null;
  const t = i18nStore.t;
  return el(
    'div',
    { class: 'ag-reply-bar' },
    el('span', { class: 'ag-cs-replybar-ico' }, icons.reply(18)),
    el(
      'div',
      { class: 'ag-reply-bar__body' },
      el('strong', null, `${t.chats.replyTo} ${reply.senderName}`),
      el('span', null, reply.snippet),
    ),
    el('button', { class: 'ag-icon-btn', onClick: onClose }, icons.close(16)),
  );
}
