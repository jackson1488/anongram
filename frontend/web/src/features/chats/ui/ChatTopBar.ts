import { i18nStore } from '../../../core/i18n/i18nStore';
import { el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { openActiveCallModal } from '../../calls/CallsView';
import { chatStore } from '../chatStore';
import type { ChatThread } from '../models';

export interface ChatTopBarActions {
  onOpenProfile?: () => void;
  onToggleSearch?: () => void;
}

export function createChatTopBar(
  thread: ChatThread,
  onBackMobile: () => void,
  actions?: ChatTopBarActions,
): HTMLElement {
  const t = i18nStore.t;
  return el(
    'div',
    { class: 'ag-chat-header' },
    el('button', { class: 'ag-icon-btn ag-mobile-only', onClick: onBackMobile }, icons.back(22)),
    el(
      'div',
      {
        class: 'ag-avatar ag-avatar--sm',
        style: `background:${thread.avatarColor}22;border-color:${thread.avatarColor}55;cursor:pointer`,
        onClick: () => actions?.onOpenProfile?.(),
      },
      thread.avatarEmoji,
    ),
    el(
      'div',
      {
        class: 'ag-chat-header__meta',
        style: 'cursor:pointer',
        onClick: () => actions?.onOpenProfile?.(),
      },
      el('div', { class: 'ag-chat-header__title' }, thread.title),
      el('div', { class: 'ag-chat-header__sub' }, thread.statusText),
    ),
    ...(actions?.onToggleSearch
      ? [
          el(
            'button',
            {
              class: 'ag-icon-btn',
              title: t.chats.searchPlaceholder,
              onClick: actions.onToggleSearch,
            },
            icons.search(19),
          ),
        ]
      : []),
    el(
      'button',
      {
        class: 'ag-icon-btn',
        title: t.calls.startAudioCall,
        onClick: () => openActiveCallModal(thread.title, thread.avatarEmoji, false),
      },
      icons.phone(19),
    ),
    el('button', { class: 'ag-icon-btn', title: t.chats.pinAction, onClick: () => chatStore.togglePin(thread.id) }, icons.pin(18)),
  );
}
