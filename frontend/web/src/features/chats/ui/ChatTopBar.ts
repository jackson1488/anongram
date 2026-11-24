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

function openNotificationsDialog(thread: ChatThread): void {
  const isRu = i18nStore.language === 'ru';
  const ups = userProfileT();
  const isMuted = blockedUsersStore.isMuted(thread.id);

  const listWrap = el('div', {
    style: 'display:flex;flex-direction:column;gap:6px;margin-top:8px;',
  });

  const handle = openDialog({
    title: isRu ? 'Уведомления' : 'Notifications',
    message: isRu
      ? `Настройка звука уведомлений для чата «${thread.title}»`
      : `Notification sound settings for "${thread.title}"`,
    content: listWrap,
    confirmLabel: i18nStore.t.common.close,
    cancelLabel: null,
  });

  const makeOptionRow = (
    label: string,
    iconNode: SVGElement,
    onSelect: () => void,
    highlight = false,
  ): HTMLButtonElement => {
    const btn = el(
      'button',
      {
        type: 'button',
        style: [
          'display:flex',
          'align-items:center',
          'gap:12px',
          'width:100%',
          'padding:11px 14px',
          'border-radius:16px',
          'border:1px solid var(--ag-island-border)',
          highlight
            ? 'background:var(--ag-accent-soft);color:var(--ag-accent);'
            : 'background:var(--ag-surface);color:var(--ag-text-primary);',
          'font-size:14px',
          'font-weight:600',
          'text-align:left',
          'cursor:pointer',
          'transition:background-color 0.14s ease,transform 0.1s ease',
        ].join(';'),
        onClick: (e: Event) => {
          e.stopPropagation();
          handle.close();
          onSelect();
        },
      },
      el(
        'span',
        {
          style: [
            'display:inline-flex',
            'align-items:center',
            'justify-content:center',
            'width:20px',
            'height:20px',
            'flex-shrink:0',
            highlight ? 'color:var(--ag-accent);' : 'color:var(--ag-text-secondary);',
          ].join(';'),
        },
        iconNode,
      ),
      el('span', { style: 'flex:1;min-width:0;' }, label),
    ) as HTMLButtonElement;
    return btn;
  };

  if (isMuted) {
    listWrap.appendChild(
      makeOptionRow(
        isRu ? 'Включить звук' : 'Unmute',
        tgIcons.bell(18),
        () => {
          blockedUsersStore.setMuted(thread.id, false);
          showToast(ups.unmutedToast);
        },
        true,
      ),
    );
  }

  const muteOptions: Array<{ labelRu: string; labelEn: string; toastRu: string; toastEn: string }> = [
    {
      labelRu: 'Выключить на 1 час',
      labelEn: 'Mute for 1 hour',
      toastRu: 'Уведомления выключены на 1 час',
      toastEn: 'Muted for 1 hour',
    },
    {
      labelRu: 'Выключить на 8 часов',
      labelEn: 'Mute for 8 hours',
      toastRu: 'Уведомления выключены на 8 часов',
      toastEn: 'Muted for 8 hours',
    },
    {
      labelRu: 'Выключить на 2 дня',
      labelEn: 'Mute for 2 days',
      toastRu: 'Уведомления выключены на 2 дня',
      toastEn: 'Muted for 2 days',
    },
    {
      labelRu: 'Выключить навсегда',
      labelEn: 'Mute forever',
      toastRu: ups.mutedToast,
      toastEn: ups.mutedToast,
    },
  ];

  for (const opt of muteOptions) {
    listWrap.appendChild(
      makeOptionRow(
        isRu ? opt.labelRu : opt.labelEn,
        tgIcons.bellOff(18),
        () => {
          blockedUsersStore.setMuted(thread.id, true);
          showToast(isRu ? opt.toastRu : opt.toastEn);
        },
      ),
    );
  }
}

function handleVideoCall(thread: ChatThread): void {
  const isRu = i18nStore.language === 'ru';
  openDialog({
    title: isRu ? 'Видеозвонок' : 'Video Call',
    message: isRu
      ? `Начать зашифрованный E2EE видеозвонок с ${thread.title}?`
      : `Start an encrypted E2EE video call with ${thread.title}?`,
    confirmLabel: isRu ? 'Позвонить' : 'Start Call',
    cancelLabel: i18nStore.t.common.cancel,
    onConfirm: () => {
      openActiveCallModal(thread.title, thread.avatarEmoji, true);
    },
  });
}

function handleClearHistory(thread: ChatThread, actions?: ChatTopBarActions): void {
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
      if (actions?.onClearHistory) {
        actions.onClearHistory();
        return;
      }
      for (const m of [...chatStore.getMessages(thread.id)]) {
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
      if (actions?.onDeleteChat) {
        actions.onDeleteChat();
        return;
      }
      for (const m of [...chatStore.getMessages(thread.id)]) {
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
): void {
  const t = i18nStore.t;
  const ups = userProfileT();
  const isRu = i18nStore.language === 'ru';
  const currentThread = chatStore.getThread(thread.id) ?? thread;
  const isMuted = blockedUsersStore.isMuted(thread.id);

  const items: MenuItem[] = [
    {
      label: isRu ? 'Уведомления   ›' : 'Notifications   ›',
      icon: () => (isMuted ? tgIcons.bellOff(18) : tgIcons.bell(18)),
      onClick: () => openNotificationsDialog(currentThread),
    },
    {
      label: isRu ? 'Видеозвонок' : 'Video Call',
      icon: () => icons.video(18),
      onClick: () => handleVideoCall(currentThread),
    },
    {
      label: t.common.search,
      icon: () => icons.search(18),
      onClick: () => actions?.onToggleSearch?.(),
    },
    {
      label: isRu ? 'Изменить обои' : 'Change wallpaper',
      icon: () => icons.image(18),
      onClick: () => handleOpenWallpaper(actions),
    },
    {
      label: ups.menuClearHistory,
      icon: () => icons.edit(18),
      onClick: () => handleClearHistory(currentThread, actions),
    },
    {
      label: ups.menuDeleteChat,
      icon: () => icons.trash(18),
      danger: true,
      onClick: () => handleDeleteChat(currentThread, onBackMobile, actions),
    },
  ];

  openMenu(anchor, items, { align: 'right', compact: false });
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
        openOverflowMenu(e.currentTarget as HTMLElement, thread, onBackMobile, actions);
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

