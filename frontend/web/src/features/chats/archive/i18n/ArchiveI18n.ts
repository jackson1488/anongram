import { i18nStore } from '../../../../core/i18n/i18nStore';

export interface ArchiveI18nStrings {
  archiveTitle: string;
  archiveEmptyTitle: string;
  archiveEmptyHint: string;
  searchChatsPlaceholder: string;
  nothingFound: string;
  selectChats: string;
  archiveSettingsMenu: string;
  howItWorksMenu: string;
  unarchive: string;
  unarchivedToast: (count: number) => string;
  selectedCount: (count: number) => string;
  cancelSelection: string;
  openChat: string;

  // Archive Settings
  archiveSettingsTitle: string;
  chatsWithNotificationsHeader: string;
  alwaysKeepArchivedTitle: string;
  alwaysKeepArchivedDesc: string;
  newChatsFromStrangersHeader: string;
  autoArchiveTitle: string;
  autoArchiveDesc: string;
  mainListVisibilityHeader: string;
  showArchiveInMainListTitle: string;
  showArchiveInMainListDesc: string;

  // Info bottom sheet
  thisIsYourArchiveTitle: string;
  thisIsYourArchiveSubtitlePrefix: string;
  changeLink: string;
  infoArchiveChatsTitle: string;
  infoArchiveChatsDesc: string;
  infoHideArchiveTitle: string;
  infoHideArchiveDesc: string;
  infoNotificationsTitle: string;
  infoNotificationsDesc: string;
  okButton: string;
  back: string;
}

const RU: ArchiveI18nStrings = {
  archiveTitle: 'Архив чатов',
  archiveEmptyTitle: 'В архиве пока пусто',
  archiveEmptyHint: 'Перенесённые в архив чаты будут отображаться здесь',
  searchChatsPlaceholder: 'Поиск чатов',
  nothingFound: 'Ничего не найдено',
  selectChats: 'Выбрать чаты',
  archiveSettingsMenu: 'Настройки архива',
  howItWorksMenu: 'Как это работает?',
  unarchive: 'Вернуть из архива',
  unarchivedToast: (n) => (n > 1 ? `Возвращено из архива: ${n}` : 'Чат возвращён из архива'),
  selectedCount: (n) => `Выбрано: ${n}`,
  cancelSelection: 'Снять выделение',
  openChat: 'Открыть чат',

  archiveSettingsTitle: 'Настройки архива',
  chatsWithNotificationsHeader: 'Чаты с вкл. уведомлениями',
  alwaysKeepArchivedTitle: 'Всегда оставлять в архиве',
  alwaysKeepArchivedDesc:
    'Оставлять чаты в архиве, даже если для них включены уведомления и приходит новое сообщение.',
  newChatsFromStrangersHeader: 'Новые чаты с незнакомцами',
  autoArchiveTitle: 'Автоматически архивировать',
  autoArchiveDesc:
    'Автоматически отключать уведомления о новых чатах и группах от незнакомцев и помещать их в архив.',
  mainListVisibilityHeader: 'Список чатов',
  showArchiveInMainListTitle: 'Показывать архив в списке чатов',
  showArchiveInMainListDesc:
    'Отображать строку архива над основным списком чатов для быстрого доступа.',

  thisIsYourArchiveTitle: 'Это Ваш архив',
  thisIsYourArchiveSubtitlePrefix:
    'Когда приходят новые сообщения, чаты без уведомлений остаются в архиве, а с включёнными уведомлениями — переносятся в «Чаты».',
  changeLink: 'Изменить',
  infoArchiveChatsTitle: 'Архив чатов',
  infoArchiveChatsDesc:
    'Чат можно перенести в архив и обратно через контекстное меню или режим выбора.',
  infoHideArchiveTitle: 'Скрытие архива',
  infoHideArchiveDesc:
    'Вы можете скрыть архив из основного списка в настройках архива.',
  infoNotificationsTitle: 'Тихий режим',
  infoNotificationsDesc:
    'Архивные чаты не беспокоят лишними уведомлениями и хранятся отдельно.',
  okButton: 'ОК',
  back: 'Назад',
};

const EN: ArchiveI18nStrings = {
  archiveTitle: 'Archived Chats',
  archiveEmptyTitle: 'Archive is empty',
  archiveEmptyHint: 'Chats you move to the archive will appear here',
  searchChatsPlaceholder: 'Search chats',
  nothingFound: 'Nothing found',
  selectChats: 'Select chats',
  archiveSettingsMenu: 'Archive Settings',
  howItWorksMenu: 'How does it work?',
  unarchive: 'Unarchive',
  unarchivedToast: (n) => (n > 1 ? `Unarchived ${n} chats` : 'Chat unarchived'),
  selectedCount: (n) => `${n} selected`,
  cancelSelection: 'Clear selection',
  openChat: 'Open chat',

  archiveSettingsTitle: 'Archive Settings',
  chatsWithNotificationsHeader: 'Unmuted chats',
  alwaysKeepArchivedTitle: 'Keep chats archived',
  alwaysKeepArchivedDesc:
    'Keep chats in the archive even when notifications are enabled and a new message arrives.',
  newChatsFromStrangersHeader: 'New chats from non-contacts',
  autoArchiveTitle: 'Archive and mute',
  autoArchiveDesc:
    'Automatically mute and archive new chats and groups from non-contacts.',
  mainListVisibilityHeader: 'Chat list',
  showArchiveInMainListTitle: 'Show archive in chat list',
  showArchiveInMainListDesc:
    'Display the archive row above your main chat list for quick access.',

  thisIsYourArchiveTitle: 'This is your archive',
  thisIsYourArchiveSubtitlePrefix:
    'When new messages arrive, muted chats stay archived while unmuted chats move back to Chats.',
  changeLink: 'Change',
  infoArchiveChatsTitle: 'Archive chats',
  infoArchiveChatsDesc:
    'You can move chats to and from the archive via context menu or selection mode.',
  infoHideArchiveTitle: 'Hide archive',
  infoHideArchiveDesc:
    'You can hide the archive from your main chat list in Archive Settings.',
  infoNotificationsTitle: 'Quiet mode',
  infoNotificationsDesc:
    'Archived chats stay out of your main list without extra distractions.',
  okButton: 'OK',
  back: 'Back',
};

export function archiveT(): ArchiveI18nStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}
