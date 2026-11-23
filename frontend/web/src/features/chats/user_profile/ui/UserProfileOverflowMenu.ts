import { openMenu } from '../../../../core/ui/Menu';
import { icons, tgIcons } from '../../../../core/ui/icons';
import type { UserProfileStrings } from '../i18n/UserProfileI18n';

export interface UserProfileOverflowMenuProps {
  anchor: HTMLElement;
  isBlocked: boolean;
  strings: UserProfileStrings;
  onOpenAutoDelete: () => void;
  onToggleBlock: () => void;
  onClearHistory: () => void;
  onDeleteChat: () => void;
}

export function openUserProfileOverflowMenu(props: UserProfileOverflowMenuProps): () => void {
  const {
    anchor,
    isBlocked,
    strings,
    onOpenAutoDelete,
    onToggleBlock,
    onClearHistory,
    onDeleteChat,
  } = props;

  return openMenu(
    anchor,
    [
      {
        label: strings.menuAutoDelete,
        icon: () => tgIcons.clock(17),
        onClick: onOpenAutoDelete,
      },
      { separator: true },
      {
        label: isBlocked ? strings.menuUnblock : strings.menuBlock,
        icon: () => (isBlocked ? tgIcons.unlock(17) : tgIcons.ban(17)),
        danger: !isBlocked,
        onClick: onToggleBlock,
      },
      {
        label: strings.menuClearHistory,
        icon: () => icons.trash(17),
        onClick: onClearHistory,
      },
      {
        label: strings.menuDeleteChat,
        icon: () => icons.trash(17),
        danger: true,
        onClick: onDeleteChat,
      },
    ],
    { align: 'right', compact: false },
  );
}
