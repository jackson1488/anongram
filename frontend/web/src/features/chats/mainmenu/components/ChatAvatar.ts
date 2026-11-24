import { el } from '../../../../core/ui/dom';
import type { ChatThread } from '../../models';

const NON_PEER_AVATAR_IDS = new Set(['saved', 'anongram_core', 'zero_lab']);
const NON_PEER_TITLES = new Set(['AnonGram Core', 'Zero-Knowledge Lab']);

function shouldShowOnlineDot(thread: ChatThread, withOnline: boolean): boolean {
  if (!withOnline || !thread.isOnline) return false;
  if (window.innerWidth < 840) return false;
  if (thread.isSavedMessages || NON_PEER_AVATAR_IDS.has(thread.id) || NON_PEER_TITLES.has(thread.title)) {
    return false;
  }
  return true;
}

/** Avatar colour comes from the thread (user-chosen data colour), not from the theme. */
export function createChatAvatar(thread: ChatThread, withOnline = true): HTMLElement {
  return el(
    'div',
    {
      class: 'ag-avatar',
      style: `background:${thread.avatarColor}22;border-color:${thread.avatarColor}55`,
    },
    thread.avatarEmoji,
    shouldShowOnlineDot(thread, withOnline) ? el('span', { class: 'ag-online-dot' }) : null,
  );
}

