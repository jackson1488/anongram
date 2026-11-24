import { i18nStore } from '../../../../core/i18n/i18nStore';
import { el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { showToast } from '../../../../core/ui/toast';
import { openActiveCallModal } from '../../../calls/CallsView';
import { blockedUsersStore } from '../data/BlockedUsersStore';
import type { UserProfileStrings } from '../i18n/UserProfileI18n';

export interface UserProfileActionsProps {
  chatId?: string;
  peerName?: string;
  avatarEmoji?: string;
  isMuted: boolean;
  isBlocked: boolean;
  strings: UserProfileStrings;
  onToggleMute?: () => void;
  onCall?: () => void;
  onVideo?: () => void;
  onSearch?: () => void;
  onMoreOrBlock?: (anchor: HTMLElement) => void;
}

function buildActionTile(
  iconNode: SVGSVGElement,
  label: string,
  active: boolean,
  danger: boolean,
  onClick: (anchor: HTMLElement) => void,
): HTMLElement {
  return el(
    'button',
    {
      type: 'button',
      class:
        'ag-up-action-tile' +
        (active ? ' is-active' : '') +
        (danger ? ' is-danger' : ''),
      title: label,
      'aria-label': label,
      'aria-pressed': active ? 'true' : undefined,
      onClick: (e: Event) => {
        e.stopPropagation();
        onClick(e.currentTarget as HTMLElement);
      },
    },
    el('span', { class: 'ag-up-action-tile__icon' }, iconNode),
    el('span', { class: 'ag-up-action-tile__label' }, label),
  );
}

function triggerProfileSearch(anchor: HTMLElement, onSearch?: () => void): void {
  const chatRoom = anchor.closest('.ag-chat-room') as HTMLElement | null;
  const profileEl = anchor.closest('.ag-user-profile') as HTMLElement | null;
  const isRealCallback = Boolean(onSearch && !String(onSearch).includes('searchSoonToast'));

  if (isRealCallback) {
    onSearch!();
    if (chatRoom?.querySelector('.ag-inchat-search')) {
      if (window.innerWidth < 840) {
        profileEl?.querySelector<HTMLButtonElement>('.ag-up-topbar > .ag-up-topbar__btn:first-child')?.click();
      }
      chatRoom.querySelector<HTMLInputElement>('.ag-inchat-search__input')?.focus();
      return;
    }
  }

  if (!chatRoom) {
    onSearch?.();
    return;
  }

  const closeProfileOnMobile = () => {
    if (window.innerWidth < 840) {
      profileEl?.querySelector<HTMLButtonElement>('.ag-up-topbar > .ag-up-topbar__btn:first-child')?.click();
    }
  };

  const existingInput = chatRoom.querySelector<HTMLInputElement>('.ag-inchat-search__input');
  if (existingInput) {
    closeProfileOnMobile();
    existingInput.focus();
    return;
  }

  const searchLabel = i18nStore.t.common.search;
  const desktopSearchBtn = chatRoom.querySelector<HTMLButtonElement>(
    `.ag-chat-header:not(.is-floating) button.ag-icon-btn[aria-label="${CSS.escape(searchLabel)}"]`,
  );
  if (desktopSearchBtn) {
    desktopSearchBtn.click();
    closeProfileOnMobile();
    chatRoom.querySelector<HTMLInputElement>('.ag-inchat-search__input')?.focus();
    return;
  }

  const mobileMoreBtn = chatRoom.querySelector<HTMLButtonElement>(
    '.ag-chat-header.is-floating .ag-top-pill--actions .ag-icon-btn:last-child',
  );
  if (mobileMoreBtn) {
    closeProfileOnMobile();
    mobileMoreBtn.click();
    const menuItems = document.querySelectorAll<HTMLButtonElement>('.ag-menu-backdrop .ag-menu-item');
    const searchItem = Array.from(menuItems).find(
      (btn) => btn.querySelector('.ag-menu-label')?.textContent?.trim() === searchLabel,
    );
    if (searchItem) {
      searchItem.click();
      chatRoom.querySelector<HTMLInputElement>('.ag-inchat-search__input')?.focus();
      return;
    }
    const backdrop = document.querySelector<HTMLElement>('.ag-menu-backdrop');
    backdrop?.dispatchEvent(new MouseEvent('mousedown', { bubbles: false }));
  }

  onSearch?.();
}

export function createUserProfileActionsRow(props: UserProfileActionsProps): HTMLElement {
  const {
    chatId,
    peerName,
    avatarEmoji,
    isMuted,
    isBlocked,
    strings,
    onToggleMute,
    onCall,
    onVideo,
    onSearch,
    onMoreOrBlock,
  } = props;

  return el(
    'div',
    { class: 'ag-up-actions' },
    buildActionTile(
      isMuted ? tgIcons.bellOff(20) : tgIcons.bell(20),
      isMuted ? strings.actionUnmute : strings.actionMute,
      isMuted,
      false,
      () => {
        if (onToggleMute) {
          onToggleMute();
        } else if (chatId) {
          const nowMuted = blockedUsersStore.toggleMute(chatId);
          showToast(nowMuted ? strings.mutedToast : strings.unmutedToast);
        }
      },
    ),
    buildActionTile(icons.phone(20), strings.actionCall, false, false, () => {
      if (onCall) {
        onCall();
      } else if (peerName) {
        openActiveCallModal(peerName, avatarEmoji ?? '👤', false);
      }
    }),
    buildActionTile(icons.video(20), strings.actionVideo, false, false, () => {
      if (onVideo) {
        onVideo();
      } else if (peerName) {
        openActiveCallModal(peerName, avatarEmoji ?? '👤', true);
      }
    }),
    buildActionTile(icons.search(20), strings.actionSearch, false, false, (anchor) =>
      triggerProfileSearch(anchor, onSearch),
    ),
    buildActionTile(
      isBlocked ? tgIcons.unlock(20) : tgIcons.ban(20),
      isBlocked ? strings.actionUnblock : strings.actionBlock,
      isBlocked,
      !isBlocked,
      (anchor) => {
        if (onMoreOrBlock) {
          onMoreOrBlock(anchor);
        } else if (chatId) {
          const nowBlocked = blockedUsersStore.toggle(chatId);
          showToast(nowBlocked ? strings.blockedToast : strings.unblockedToast);
        }
      },
    ),
  );
}
