import { el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import type { ChatThread } from '../../models';
import { blockedUsersStore } from '../../user_profile/data/BlockedUsersStore';

export function createTrailingBadges(thread: ChatThread): HTMLElement | null {
  const isMuted = blockedUsersStore.isMuted(thread.id);
  const muteEl = isMuted
    ? el('span', { class: 'ag-pin-icon', style: 'opacity:0.75;' }, tgIcons.mute(13))
    : null;

  const badgeEl =
    thread.unreadCount > 0
      ? el('span', { class: 'ag-unread-badge' }, thread.unreadCount > 99 ? '99+' : String(thread.unreadCount))
      : thread.isPinned
        ? el('span', { class: 'ag-pin-icon' }, icons.pin(13))
        : null;

  if (muteEl && badgeEl) {
    return el(
      'span',
      { style: 'display:inline-flex;align-items:center;gap:4px;flex-shrink:0;' },
      muteEl,
      badgeEl,
    );
  }
  return muteEl ?? badgeEl;
}
