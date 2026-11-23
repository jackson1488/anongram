import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import type { ChatThread } from '../../models';

export function createTrailingBadges(thread: ChatThread): HTMLElement | null {
  if (thread.unreadCount > 0) {
    return el('span', { class: 'ag-unread-badge' }, thread.unreadCount > 99 ? '99+' : String(thread.unreadCount));
  }
  if (thread.isPinned) return el('span', { class: 'ag-pin-icon' }, icons.pin(13));
  return null;
}
