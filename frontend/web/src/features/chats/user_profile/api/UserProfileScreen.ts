import '../user_profile.css';
import { i18nStore } from '../../../../core/i18n/i18nStore';
import { clear, el } from '../../../../core/ui/dom';
import { showToast } from '../../../../core/ui/toast';
import { openActiveCallModal } from '../../../calls/CallsView';
import { chatStore } from '../../chatStore';
import { blockedUsersStore } from '../data/BlockedUsersStore';
import { userProfileT } from '../i18n/UserProfileI18n';
import {
  derivePeerMetadata,
  extractProfileBuckets,
  getAvailableTabs,
  type UserProfileTab,
} from '../model/UserProfileModels';
import { openAutoDeleteCustomSheet } from '../ui/AutoDeleteCustomSheet';
import { createUserProfileActionsRow } from '../ui/UserProfileActionsRow';
import { createUserProfileHero, createUserProfileTopBar } from '../ui/UserProfileHeader';
import { createUserProfileInfoCard } from '../ui/UserProfileInfoCard';
import { createUserProfileMediaTabs } from '../ui/UserProfileMediaTabs';
import { openUserProfileOverflowMenu } from '../ui/UserProfileOverflowMenu';

export interface UserProfileScreenOptions {
  desktopColumn?: boolean;
}

export function createUserProfileScreen(
  chatId: string,
  onClose: () => void,
  opts?: UserProfileScreenOptions,
): HTMLElement {
  const desktopColumn = opts?.desktopColumn ?? false;
  const root = el('aside', {
    class: 'ag-user-profile' + (desktopColumn ? ' is-desktop-column' : ''),
    'aria-label': userProfileT().headerTitle,
  });

  let activeTab: UserProfileTab = 'MEDIA';
  let tabInitialized = false;

  const openAutoDelete = () => {
    const strings = userProfileT();
    openAutoDeleteCustomSheet({
      current: blockedUsersStore.getAutoDelete(chatId),
      strings,
      onApply: (cfg) => {
        blockedUsersStore.setAutoDelete(chatId, cfg);
        showToast(strings.autoDeleteUpdatedToast);
      },
    });
  };

  const handleToggleBlock = () => {
    const strings = userProfileT();
    const nowBlocked = blockedUsersStore.toggle(chatId);
    showToast(nowBlocked ? strings.blockedToast : strings.unblockedToast);
  };

  const handleToggleMute = () => {
    const strings = userProfileT();
    const nowMuted = blockedUsersStore.toggleMute(chatId);
    showToast(nowMuted ? strings.mutedToast : strings.unmutedToast);
  };

  const handleClearHistory = () => {
    const strings = userProfileT();
    const msgs = chatStore.getMessages(chatId);
    for (const m of msgs) {
      chatStore.deleteMessage(chatId, m.id);
    }
    showToast(strings.clearedHistoryToast);
  };

  const handleDeleteChat = () => {
    const strings = userProfileT();
    handleClearHistory();
    const thread = chatStore.getThread(chatId);
    if (thread && !thread.isArchived) {
      chatStore.toggleArchive(chatId);
    }
    showToast(strings.deletedChatToast);
    onClose();
  };

  const openOverflow = (anchor: HTMLElement) => {
    openUserProfileOverflowMenu({
      anchor,
      isBlocked: blockedUsersStore.isBlocked(chatId),
      strings: userProfileT(),
      onOpenAutoDelete: openAutoDelete,
      onToggleBlock: handleToggleBlock,
      onClearHistory: handleClearHistory,
      onDeleteChat: handleDeleteChat,
    });
  };

  const render = () => {
    const thread = chatStore.getThread(chatId);
    if (!thread) {
      clear(root);
      return;
    }

    const strings = userProfileT();
    const meta = derivePeerMetadata(thread);
    const messages = chatStore.getMessages(chatId);
    const buckets = extractProfileBuckets(messages);
    const available = getAvailableTabs(buckets);
    if (!tabInitialized) {
      activeTab = available[0] ?? 'MEDIA';
      tabInitialized = true;
    }

    const isBlocked = blockedUsersStore.isBlocked(chatId);
    const isMuted = blockedUsersStore.isMuted(chatId);
    const autoDelete = blockedUsersStore.getAutoDelete(chatId);

    const headerProps = {
      thread,
      meta,
      onionOnline: chatStore.onionOnline,
      isBlocked,
      desktopColumn,
      strings,
      onClose,
      onOpenMenu: openOverflow,
    };

    const topBar = createUserProfileTopBar(headerProps);
    const scrollArea = el(
      'div',
      { class: 'ag-up-scroll' },
      createUserProfileHero(headerProps),
      createUserProfileActionsRow({
        isMuted,
        isBlocked,
        strings,
        onToggleMute: handleToggleMute,
        onCall: () => openActiveCallModal(thread.title, thread.avatarEmoji, false),
        onVideo: () => openActiveCallModal(thread.title, thread.avatarEmoji, true),
        onSearch: () => showToast(strings.searchSoonToast),
        onMoreOrBlock: () => handleToggleBlock(),
      }),
      createUserProfileInfoCard({
        meta,
        autoDelete,
        isMuted,
        strings,
        onOpenAutoDelete: openAutoDelete,
        onToggleNotifications: handleToggleMute,
      }),
      createUserProfileMediaTabs({
        buckets,
        activeTab,
        strings,
        onSelectTab: (tab) => {
          activeTab = tab;
          render();
        },
      }),
    );

    clear(root);
    root.appendChild(topBar);
    root.appendChild(scrollArea);
  };

  render();

  const unsubChat = chatStore.subscribe(render);
  const unsubBlocked = blockedUsersStore.subscribe(render);
  const unsubLang = i18nStore.subscribe(render);

  // Clean up store listeners automatically if the node is detached from DOM
  const observer = new MutationObserver(() => {
    if (!root.isConnected) {
      unsubChat();
      unsubBlocked();
      unsubLang();
      observer.disconnect();
    }
  });
  queueMicrotask(() => {
    if (root.parentNode) {
      observer.observe(root.parentNode, { childList: true });
    } else if (document.body) {
      observer.observe(document.body, { childList: true, subtree: true });
    }
  });

  return root;
}
