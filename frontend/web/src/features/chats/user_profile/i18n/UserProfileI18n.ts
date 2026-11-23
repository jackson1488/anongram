import { i18nStore } from '../../../../core/i18n/i18nStore';
import type { AutoDeleteConfig } from '../model/UserProfileModels';

export interface UserProfileStrings {
  headerTitle: string;
  back: string;
  close: string;
  moreOptions: string;
  online: string;
  onionOnline: string;
  lastSeenRecently: string;
  blockedBadge: string;
  actionMute: string;
  actionUnmute: string;
  actionCall: string;
  actionVideo: string;
  actionSearch: string;
  actionMore: string;
  actionBlock: string;
  actionUnblock: string;
  labelPublicId: string;
  labelFingerprint: string;
  labelUsername: string;
  labelBio: string;
  labelAutoDelete: string;
  labelNotifications: string;
  notificationsEnabled: string;
  notificationsMuted: string;
  copiedToast: string;
  tabMedia: string;
  tabFiles: string;
  tabLinks: string;
  tabVoice: string;
  emptyTab: string;
  menuAutoDelete: string;
  menuBlock: string;
  menuUnblock: string;
  menuClearHistory: string;
  menuDeleteChat: string;
  blockedToast: string;
  unblockedToast: string;
  mutedToast: string;
  unmutedToast: string;
  clearedHistoryToast: string;
  deletedChatToast: string;
  searchSoonToast: string;
  autoDeleteTitle: string;
  autoDeleteOff: string;
  autoDelete1h: string;
  autoDelete24h: string;
  autoDelete7d: string;
  autoDelete30d: string;
  autoDeleteCustom: string;
  autoDeleteCustomValue: string;
  autoDeleteUnitMin: string;
  autoDeleteUnitHours: string;
  autoDeleteUnitDays: string;
  autoDeleteFooter: string;
  autoDeleteApply: string;
  autoDeleteDisable: string;
  autoDeleteCancel: string;
  autoDeleteUpdatedToast: string;
  formatAutoDeleteValue: (cfg: AutoDeleteConfig) => string;
}

const RU: UserProfileStrings = {
  headerTitle: 'Информация',
  back: 'Назад',
  close: 'Закрыть',
  moreOptions: 'Ещё',
  online: 'в сети',
  onionOnline: 'Tor Onion • в сети',
  lastSeenRecently: 'был(а) недавно',
  blockedBadge: 'Заблокирован',
  actionMute: 'Звук',
  actionUnmute: 'Без звука',
  actionCall: 'Звонок',
  actionVideo: 'Видео',
  actionSearch: 'Поиск',
  actionMore: 'Ещё',
  actionBlock: 'Блок',
  actionUnblock: 'Разблок.',
  labelPublicId: 'Public ID',
  labelFingerprint: 'Отпечаток Ed25519',
  labelUsername: 'Имя пользователя',
  labelBio: 'О себе',
  labelAutoDelete: 'Автоудаление сообщений',
  labelNotifications: 'Уведомления',
  notificationsEnabled: 'Включены',
  notificationsMuted: 'Отключены',
  copiedToast: 'Скопировано',
  tabMedia: 'Медиа',
  tabFiles: 'Файлы',
  tabLinks: 'Ссылки',
  tabVoice: 'Голосовые',
  emptyTab: 'В этом разделе пока пусто',
  menuAutoDelete: 'Автоудаление',
  menuBlock: 'Заблокировать',
  menuUnblock: 'Разблокировать',
  menuClearHistory: 'Очистить историю',
  menuDeleteChat: 'Удалить чат',
  blockedToast: 'Пользователь заблокирован',
  unblockedToast: 'Пользователь разблокирован',
  mutedToast: 'Уведомления отключены',
  unmutedToast: 'Уведомления включены',
  clearedHistoryToast: 'История чата очищена',
  deletedChatToast: 'Чат удалён',
  searchSoonToast: 'Поиск по чату в разработке',
  autoDeleteTitle: 'Автоудаление сообщений',
  autoDeleteOff: 'Выкл.',
  autoDelete1h: '1 час',
  autoDelete24h: '24 часа',
  autoDelete7d: '7 дней',
  autoDelete30d: '30 дней',
  autoDeleteCustom: 'Другое время',
  autoDeleteCustomValue: 'Значение',
  autoDeleteUnitMin: 'мин.',
  autoDeleteUnitHours: 'ч.',
  autoDeleteUnitDays: 'дн.',
  autoDeleteFooter: 'Новые сообщения в этом чате будут автоматически удаляться через выбранное время.',
  autoDeleteApply: 'Применить',
  autoDeleteDisable: 'Выключить',
  autoDeleteCancel: 'Отмена',
  autoDeleteUpdatedToast: 'Таймер автоудаления обновлён',
  formatAutoDeleteValue: (cfg) => {
    switch (cfg.preset) {
      case 'off':
        return 'Выкл.';
      case '1h':
        return '1 час';
      case '24h':
        return '24 часа';
      case '7d':
        return '7 дней';
      case '30d':
        return '30 дней';
      case 'custom': {
        if (cfg.seconds >= 86400) return `${Math.round(cfg.seconds / 86400)} дн.`;
        if (cfg.seconds >= 3600) return `${Math.round(cfg.seconds / 3600)} ч.`;
        return `${Math.max(1, Math.round(cfg.seconds / 60))} мин.`;
      }
    }
  },
};

const EN: UserProfileStrings = {
  headerTitle: 'User Info',
  back: 'Back',
  close: 'Close',
  moreOptions: 'More',
  online: 'online',
  onionOnline: 'Tor Onion • online',
  lastSeenRecently: 'last seen recently',
  blockedBadge: 'Blocked',
  actionMute: 'Mute',
  actionUnmute: 'Muted',
  actionCall: 'Call',
  actionVideo: 'Video',
  actionSearch: 'Search',
  actionMore: 'More',
  actionBlock: 'Block',
  actionUnblock: 'Unblock',
  labelPublicId: 'Public ID',
  labelFingerprint: 'Ed25519 Fingerprint',
  labelUsername: 'Username',
  labelBio: 'Bio',
  labelAutoDelete: 'Auto-delete timer',
  labelNotifications: 'Notifications',
  notificationsEnabled: 'Enabled',
  notificationsMuted: 'Muted',
  copiedToast: 'Copied',
  tabMedia: 'Media',
  tabFiles: 'Files',
  tabLinks: 'Links',
  tabVoice: 'Voice',
  emptyTab: 'Nothing shared here yet',
  menuAutoDelete: 'Auto-Delete',
  menuBlock: 'Block User',
  menuUnblock: 'Unblock User',
  menuClearHistory: 'Clear History',
  menuDeleteChat: 'Delete Chat',
  blockedToast: 'User blocked',
  unblockedToast: 'User unblocked',
  mutedToast: 'Notifications muted',
  unmutedToast: 'Notifications enabled',
  clearedHistoryToast: 'Chat history cleared',
  deletedChatToast: 'Chat deleted',
  searchSoonToast: 'In-chat search coming soon',
  autoDeleteTitle: 'Auto-delete messages',
  autoDeleteOff: 'Off',
  autoDelete1h: '1 hour',
  autoDelete24h: '24 hours',
  autoDelete7d: '7 days',
  autoDelete30d: '30 days',
  autoDeleteCustom: 'Custom',
  autoDeleteCustomValue: 'Value',
  autoDeleteUnitMin: 'min',
  autoDeleteUnitHours: 'hr',
  autoDeleteUnitDays: 'days',
  autoDeleteFooter: 'New messages sent in this chat will be automatically deleted after the selected duration.',
  autoDeleteApply: 'Apply',
  autoDeleteDisable: 'Disable',
  autoDeleteCancel: 'Cancel',
  autoDeleteUpdatedToast: 'Auto-delete timer updated',
  formatAutoDeleteValue: (cfg) => {
    switch (cfg.preset) {
      case 'off':
        return 'Off';
      case '1h':
        return '1 hour';
      case '24h':
        return '24 hours';
      case '7d':
        return '7 days';
      case '30d':
        return '30 days';
      case 'custom': {
        if (cfg.seconds >= 86400) return `${Math.round(cfg.seconds / 86400)}d`;
        if (cfg.seconds >= 3600) return `${Math.round(cfg.seconds / 3600)}h`;
        return `${Math.max(1, Math.round(cfg.seconds / 60))}m`;
      }
    }
  },
};

export function userProfileT(): UserProfileStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}
