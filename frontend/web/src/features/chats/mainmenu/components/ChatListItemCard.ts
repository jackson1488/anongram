import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { draftsStore } from '../../../drafts/draftsStore';
import { chatStore } from '../../chatStore';
import type { ChatThread } from '../../models';
import { mainMenuT } from '../i18n';
import { createChatAvatar } from './ChatAvatar';
import { createTrailingBadges } from './ChatTrailingBadges';

export function createChatListItemCard(
  thread: ChatThread,
  selected: boolean,
  onClick: () => void,
): HTMLElement {
  const mt = mainMenuT();
  const last = chatStore.getLastMessage(thread.id);
  const draft = draftsStore.getDraft(thread.id);
  const preview = draft
    ? el(
        'span',
        { class: 'ag-chat-row__preview' },
        el('span', { class: 'ag-draft-prefix' }, mt.draft),
        draft.text || draft.replySnippet || mt.draftReply,
      )
    : el(
        'span',
        { class: 'ag-chat-row__preview' },
        last
          ? last.attachment && !last.text
            ? `📎 ${last.attachment.title}`
            : last.text
          : i18nStore.t.chats.noMessages,
      );
  return el(
    'div',
    { class: `ag-chat-row ${selected ? 'is-selected' : ''}`, onClick },
    createChatAvatar(thread),
    el(
      'div',
      { class: 'ag-chat-row__body' },
      el(
        'div',
        { class: 'ag-chat-row__top' },
        el(
          'span',
          { class: 'ag-chat-row__title' },
          thread.title,
          thread.isOtpBurn ? el('span', { class: 'ag-otp-badge' }, mt.otp) : null,
        ),
        el('span', { class: 'ag-chat-row__time' }, last?.timestamp ?? ''),
      ),
      el('div', { class: 'ag-chat-row__bottom' }, preview, createTrailingBadges(thread)),
    ),
  );
}
