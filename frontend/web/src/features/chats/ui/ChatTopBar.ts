import { i18nStore } from '../../../core/i18n/i18nStore';
import { openDialog } from '../../../core/ui/components';
import { el } from '../../../core/ui/dom';
import { icons, tgIcons } from '../../../core/ui/icons';
import { openMenu, type MenuItem } from '../../../core/ui/Menu';
import { showToast } from '../../../core/ui/toast';
import { openActiveCallModal } from '../../calls/CallsView';
import { openWallpaperPicker } from '../../wallpapers';
import { chatStore } from '../chatStore';
import { chatScreenT } from '../i18n';
import type { ChatThread } from '../models';
import { blockedUsersStore } from '../user_profile/data/BlockedUsersStore';
import { userProfileT } from '../user_profile/i18n/UserProfileI18n';

export interface ChatTopBarActions {
  onOpenProfile?: () => void;
  onToggleSearch?: () => void;
  onClearHistory?: () => void;
  onDeleteChat?: () => void;
  onToggleMute?: () => void;
  onOpenWallpaper?: () => void;
}

function createAvatar(thread: ChatThread, onOpenProfile?: () => void): HTMLElement {
  return el(
    'div',
    {
      class: 'ag-avatar ag-avatar--sm',
      style: `background:${thread.avatarColor}22;border-color:${thread.avatarColor}55;${onOpenProfile ? 'cursor:pointer;' : ''}`,
      onClick: onOpenProfile,
    },
    thread.avatarEmoji,
  );
}

function createMeta(thread: ChatThread, showLock: boolean, onOpenProfile?: () => void): HTMLElement {
  const t = i18nStore.t;
  const statusText =
    thread.statusText || (thread.isOnline ? t.chats.online : t.chats.lastSeenRecently);

  const titleEl = showLock
    ? el(
        'div',
        { class: 'ag-chat-header__title', style: 'display:flex;align-items:center;gap:4px;' },
        el(
          'span',
          { style: 'min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;' },
          thread.title,
        ),
        el(
          'span',
          { style: 'display:inline-flex;flex-shrink:0;color:var(--ag-accent);' },
          icons.lock(11),
        ),
      )
    : el('div', { class: 'ag-chat-header__title' }, thread.title);

  const subEl = el(
    'div',
    {
      class: 'ag-chat-header__sub',
      style: [
        'overflow:hidden;text-overflow:ellipsis;white-space:nowrap;',
        thread.isOnline ? 'color:color-mix(in srgb, var(--ag-accent) 85%, transparent);' : '',
      ].join(''),
    },
    statusText,
  );

  return el(
    'div',
    {
      class: 'ag-chat-header__meta',
      style: onOpenProfile ? 'cursor:pointer' : undefined,
      onClick: onOpenProfile,
    },
    titleEl,
    subEl,
  );
}

function handleOpenWallpaper(actions?: ChatTopBarActions): void {
  if (actions?.onOpenWallpaper) {
    actions.onOpenWallpaper();
    return;
  }
  openWallpaperPicker();
}

function handleToggleMute(threadId: string, actions?: ChatTopBarActions): void {
  if (actions?.onToggleMute) {
    actions.onToggleMute();
    return;
  }
  const nowMuted = blockedUsersStore.toggleMute(threadId);
  const ups = userProfileT();
  showToast(nowMuted ? ups.mutedToast : ups.unmutedToast);
}

function handleClearHistory(thread: ChatThread, actions?: ChatTopBarActions): void {
  if (actions?.onClearHistory) {
    actions.onClearHistory();
    return;
  }
  const isRu = i18nStore.language === 'ru';
  const ups = userProfileT();
  openDialog({
    title: ups.menuClearHistory,
    message: isRu
      ? `Вы точно хотите очистить историю сообщений с ${thread.title}?`
      : `Are you sure you want to clear chat history with ${thread.title}?`,
    confirmLabel: isRu ? 'Очистить' : 'Clear',
    cancelLabel: i18nStore.t.common.cancel,
    danger: true,
    onConfirm: () => {
      for (const m of chatStore.getMessages(thread.id)) {
        chatStore.deleteMessage(thread.id, m.id);
      }
      showToast(ups.clearedHistoryToast);
    },
  });
}

function handleDeleteChat(
  thread: ChatThread,
  onBackMobile: () => void,
  actions?: ChatTopBarActions,
): void {
  if (actions?.onDeleteChat) {
    actions.onDeleteChat();
    return;
  }
  const isRu = i18nStore.language === 'ru';
  const ups = userProfileT();
  openDialog({
    title: ups.menuDeleteChat,
    message: isRu
      ? `Вы точно хотите удалить чат с ${thread.title}?`
      : `Are you sure you want to delete the chat with ${thread.title}?`,
    confirmLabel: i18nStore.t.common.delete,
    cancelLabel: i18nStore.t.common.cancel,
    danger: true,
    onConfirm: () => {
      for (const m of chatStore.getMessages(thread.id)) {
        chatStore.deleteMessage(thread.id, m.id);
      }
      const current = chatStore.getThread(thread.id);
      if (current && !current.isArchived) {
        chatStore.toggleArchive(thread.id);
      }
      showToast(ups.deletedChatToast);
      onBackMobile();
    },
  });
}

function openOverflowMenu(
  anchor: HTMLElement,
  thread: ChatThread,
  onBackMobile: () => void,
  actions: ChatTopBarActions | undefined,
  isMobile: boolean,
): void {
  const t = i18nStore.t;
  const cs = chatScreenT();
  const ups = userProfileT();
  const isRu = i18nStore.language === 'ru';
  const currentThread = chatStore.getThread(thread.id) ?? thread;
  const isPinned = currentThread.isPinned;
  const isMuted = blockedUsersStore.isMuted(thread.id);

  const items: MenuItem[] = [];
  if (isMobile) {
    items.push({
      label: t.common.search,
      icon: () => icons.search(17),
      onClick: () => actions?.onToggleSearch?.(),
    });
  }
  items.push(
    {
      label: isRu ? 'Изменить обои' : 'Change wallpaper',
      icon: () => icons.image(17),
      onClick: () => handleOpenWallpaper(actions),
    },
    {
      label: isMuted
        ? isRu
          ? 'Включить звук'
          : 'Unmute'
        : isRu
          ? 'Выключить звук'
          : 'Mute',
      icon: () => (isMuted ? tgIcons.unmute(17) : tgIcons.mute(17)),
      onClick: () => handleToggleMute(thread.id, actions),
    },
    {
      label: isPinned ? cs.unpinMessage : t.chats.pinAction,
      icon: () => (isPinned ? tgIcons.pinOff(17) : icons.pin(17)),
      onClick: () => chatStore.togglePin(thread.id),
    },
    { separator: true },
    {
      label: ups.menuClearHistory,
      icon: () => icons.trash(17),
      onClick: () => handleClearHistory(currentThread, actions),
    },
    {
      label: ups.menuDeleteChat,
      icon: () => icons.trash(17),
      danger: true,
      onClick: () => handleDeleteChat(currentThread, onBackMobile, actions),
    },
  );

  openMenu(anchor, items, { align: 'right', compact: !isMobile });
}

export function createChatTopBar(
  thread: ChatThread,
  onBackMobile: () => void,
  actions?: ChatTopBarActions,
): HTMLElement {
  const t = i18nStore.t;
  const cs = chatScreenT();
  const isMobile = window.innerWidth < 840;

  const callBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-icon-btn',
      title: t.calls.startAudioCall,
      'aria-label': t.calls.startAudioCall,
      onClick: () => openActiveCallModal(thread.title, thread.avatarEmoji, false),
    },
    icons.phone(19),
  );

  const moreBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-icon-btn',
      title: cs.more,
      'aria-label': cs.more,
      onClick: (e: Event) => {
        e.stopPropagation();
        openOverflowMenu(e.currentTarget as HTMLElement, thread, onBackMobile, actions, isMobile);
      },
    },
    icons.more(19),
  );

  if (isMobile) {
    const backPill = el(
      'button',
      {
        type: 'button',
        class: 'ag-top-pill ag-top-pill--icon',
        title: t.common.back,
        'aria-label': t.common.back,
        onClick: onBackMobile,
      },
      tgIcons.arrowLeft(20),
    );

    const centerPill = el(
      'div',
      {
        class: 'ag-top-pill ag-top-pill--center',
        role: 'button',
        tabindex: 0,
        style: 'cursor:pointer',
        onClick: () => actions?.onOpenProfile?.(),
        onKeyDown: (e: Event) => {
          const ke = e as KeyboardEvent;
          if (ke.key === 'Enter' || ke.key === ' ') {
            ke.preventDefault();
            actions?.onOpenProfile?.();
          }
        },
      },
      createAvatar(thread),
      createMeta(thread, true),
    );

    const actionsPill = el('div', { class: 'ag-top-pill ag-top-pill--actions' }, callBtn, moreBtn);

    return el('div', { class: 'ag-chat-header is-floating' }, backPill, centerPill, actionsPill);
  }

  const openProfile = actions?.onOpenProfile ? () => actions.onOpenProfile?.() : undefined;
  const searchBtn = el(
    'button',
    {
      type: 'button',
      class: 'ag-icon-btn',
      title: t.common.search,
      'aria-label': t.common.search,
      onClick: () => actions?.onToggleSearch?.(),
    },
    icons.search(19),
  );

  return el(
    'div',
    { class: 'ag-chat-header' },
    createAvatar(thread, openProfile),
    createMeta(thread, false, openProfile),
    searchBtn,
    callBtn,
    moreBtn,
  );
}

