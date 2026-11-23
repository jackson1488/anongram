import { el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import type { ChatThread } from '../../models';
import type { UserProfileStrings } from '../i18n/UserProfileI18n';
import type { PeerCryptoMetadata } from '../model/UserProfileModels';

export interface UserProfileHeaderProps {
  thread: ChatThread;
  meta: PeerCryptoMetadata;
  onionOnline: boolean;
  isBlocked: boolean;
  desktopColumn: boolean;
  strings: UserProfileStrings;
  onClose: () => void;
  onOpenMenu: (anchor: HTMLElement) => void;
}

export function createUserProfileTopBar(props: UserProfileHeaderProps): HTMLElement {
  const { strings, desktopColumn, onClose, onOpenMenu } = props;

  const closeBtn = el(
    'button',
    {
      class: 'ag-icon-btn ag-up-topbar__btn',
      title: desktopColumn ? strings.close : strings.back,
      'aria-label': desktopColumn ? strings.close : strings.back,
      onClick: onClose,
    },
    desktopColumn ? icons.close(20) : icons.back(22),
  );

  const titleEl = el('div', { class: 'ag-up-topbar__title' }, strings.headerTitle);

  const moreBtn = el(
    'button',
    {
      class: 'ag-icon-btn ag-up-topbar__btn',
      title: strings.moreOptions,
      'aria-label': strings.moreOptions,
      onClick: (e: Event) => {
        e.stopPropagation();
        onOpenMenu(e.currentTarget as HTMLElement);
      },
    },
    icons.more(20),
  );

  return el('div', { class: 'ag-up-topbar' }, closeBtn, titleEl, moreBtn);
}

export function createUserProfileHero(props: UserProfileHeaderProps): HTMLElement {
  const { thread, meta, onionOnline, isBlocked, strings } = props;

  const statusText = isBlocked
    ? strings.blockedBadge
    : thread.isOnline && onionOnline
      ? strings.onionOnline
      : thread.isOnline
        ? strings.online
        : thread.statusText || strings.lastSeenRecently;

  const avatarEl = el(
    'div',
    {
      class: 'ag-up-hero__avatar' + (thread.isOnline && !isBlocked ? ' is-online' : ''),
      style: `background: ${thread.avatarColor}26; border-color: ${thread.avatarColor}66`,
    },
    el('span', { class: 'ag-up-hero__emoji' }, thread.avatarEmoji),
    thread.isOnline && !isBlocked ? el('span', { class: 'ag-up-hero__dot' }) : null,
  );

  const nameEl = el('h2', { class: 'ag-up-hero__name' }, thread.title);

  const statusEl = el(
    'div',
    {
      class:
        'ag-up-hero__status' +
        (isBlocked ? ' is-blocked' : thread.isOnline ? ' is-online' : ''),
    },
    el('span', { class: 'ag-up-hero__status-icon' }, icons.shield(13)),
    el('span', null, statusText),
  );

  const subMetaEl = el(
    'div',
    { class: 'ag-up-hero__submeta' },
    `${meta.publicId} • ${meta.bio}`,
  );

  return el(
    'div',
    { class: 'ag-up-hero' },
    avatarEl,
    nameEl,
    statusEl,
    subMetaEl,
  );
}
