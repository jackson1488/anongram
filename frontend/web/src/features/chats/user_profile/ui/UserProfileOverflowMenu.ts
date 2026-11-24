import { openMenu } from '../../../../core/ui/Menu';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { showToast } from '../../../../core/ui/toast';
import { chatStore } from '../../chatStore';
import { blockedUsersStore } from '../data/BlockedUsersStore';
import type { UserProfileStrings } from '../i18n/UserProfileI18n';
import { derivePeerMetadata } from '../model/UserProfileModels';
import { openAutoDeleteCustomSheet } from './AutoDeleteCustomSheet';

export interface UserProfileOverflowMenuProps {
  anchor: HTMLElement;
  chatId?: string;
  isBlocked: boolean;
  strings: UserProfileStrings;
  onOpenAutoDelete?: () => void;
  onToggleBlock?: () => void;
  onClearHistory?: () => void;
  onDeleteChat?: () => void;
  onCloseProfile?: () => void;
}

function resolveChatIdFromAnchor(anchor: HTMLElement): string | null {
  const profile = anchor.closest('.ag-user-profile');
  if (!profile) return null;
  const publicId = profile.querySelector('.ag-up-info-row__value.is-mono')?.textContent?.trim();
  if (!publicId) return null;
  const thread = chatStore.threads.find((t) => derivePeerMetadata(t).publicId === publicId);
  return thread?.id ?? null;
}

export function openUserProfileOverflowMenu(props: UserProfileOverflowMenuProps): () => void {
  const {
    anchor,
    chatId,
    isBlocked,
    strings,
    onOpenAutoDelete,
    onToggleBlock,
    onClearHistory,
    onDeleteChat,
    onCloseProfile,
  } = props;

  const resolvedChatId = chatId ?? resolveChatIdFromAnchor(anchor);
  const profileEl = anchor.closest('.ag-user-profile') as HTMLElement | null;
  const chatRoomEl = anchor.closest('.ag-chat-room') as HTMLElement | null;

  const closeProfileView = () => {
    if (onCloseProfile) {
      onCloseProfile();
    } else {
      profileEl?.querySelector<HTMLButtonElement>('.ag-up-topbar > .ag-up-topbar__btn:first-child')?.click();
    }
  };

  return openMenu(
    anchor,
    [
      {
        label: strings.menuAutoDelete,
        icon: () => tgIcons.clock(17),
        onClick: () => {
          if (onOpenAutoDelete) {
            onOpenAutoDelete();
          } else if (resolvedChatId) {
            openAutoDeleteCustomSheet({
              current: blockedUsersStore.getAutoDelete(resolvedChatId),
              strings,
              onApply: (cfg) => {
                blockedUsersStore.setAutoDelete(resolvedChatId, cfg);
                showToast(strings.autoDeleteUpdatedToast);
              },
            });
          }
        },
      },
      { separator: true },
      {
        label: isBlocked ? strings.menuUnblock : strings.menuBlock,
        icon: () => (isBlocked ? tgIcons.unlock(17) : tgIcons.ban(17)),
        danger: !isBlocked,
        onClick: () => {
          if (onToggleBlock) {
            onToggleBlock();
          } else if (resolvedChatId) {
            const nowBlocked = blockedUsersStore.toggle(resolvedChatId);
            showToast(nowBlocked ? strings.blockedToast : strings.unblockedToast);
          }
        },
      },
      {
        label: strings.menuClearHistory,
        icon: () => icons.trash(17),
        onClick: () => {
          const isLegacyLoop = Boolean(onClearHistory && String(onClearHistory).includes('deleteMessage'));
          if (resolvedChatId && (!onClearHistory || isLegacyLoop)) {
            chatStore.clearHistory(resolvedChatId);
            showToast(strings.clearedHistoryToast);
            return;
          }
          onClearHistory?.();
        },
      },
      {
        label: strings.menuDeleteChat,
        icon: () => icons.trash(17),
        danger: true,
        onClick: () => {
          const isLegacyArchive = Boolean(onDeleteChat && String(onDeleteChat).includes('toggleArchive'));
          if (resolvedChatId && (!onDeleteChat || isLegacyArchive)) {
            closeProfileView();
            chatStore.deleteChat(resolvedChatId);
            showToast(strings.deletedChatToast);
            if (window.innerWidth < 840) {
              chatRoomEl
                ?.querySelector<HTMLButtonElement>('.ag-chat-header.is-floating .ag-top-pill--icon')
                ?.click();
            }
            return;
          }
          onDeleteChat?.();
          closeProfileView();
        },
      },
    ],
    { align: 'right', compact: false },
  );
}
