import { el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import type { UserProfileStrings } from '../i18n/UserProfileI18n';

export interface UserProfileActionsProps {
  isMuted: boolean;
  isBlocked: boolean;
  strings: UserProfileStrings;
  onToggleMute: () => void;
  onCall: () => void;
  onVideo: () => void;
  onSearch: () => void;
  onMoreOrBlock: (anchor: HTMLElement) => void;
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
      class:
        'ag-up-action-tile' +
        (active ? ' is-active' : '') +
        (danger ? ' is-danger' : ''),
      title: label,
      onClick: (e: Event) => {
        e.stopPropagation();
        onClick(e.currentTarget as HTMLElement);
      },
    },
    el('span', { class: 'ag-up-action-tile__icon' }, iconNode),
    el('span', { class: 'ag-up-action-tile__label' }, label),
  );
}

export function createUserProfileActionsRow(props: UserProfileActionsProps): HTMLElement {
  const {
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
      () => onToggleMute(),
    ),
    buildActionTile(icons.phone(20), strings.actionCall, false, false, () => onCall()),
    buildActionTile(icons.video(20), strings.actionVideo, false, false, () => onVideo()),
    buildActionTile(icons.search(20), strings.actionSearch, false, false, () => onSearch()),
    buildActionTile(
      isBlocked ? tgIcons.unlock(20) : tgIcons.ban(20),
      isBlocked ? strings.actionUnblock : strings.actionBlock,
      isBlocked,
      !isBlocked,
      (anchor) => onMoreOrBlock(anchor),
    ),
  );
}
