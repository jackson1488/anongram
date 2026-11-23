import { el } from '../../../../core/ui/dom';
import type { ChatThread } from '../../models';

/** Avatar colour comes from the thread (user-chosen data colour), not from the theme. */
export function createChatAvatar(thread: ChatThread, withOnline = true): HTMLElement {
  return el(
    'div',
    {
      class: 'ag-avatar',
      style: `background:${thread.avatarColor}22;border-color:${thread.avatarColor}55`,
    },
    thread.avatarEmoji,
    withOnline && thread.isOnline ? el('span', { class: 'ag-online-dot' }) : null,
  );
}
