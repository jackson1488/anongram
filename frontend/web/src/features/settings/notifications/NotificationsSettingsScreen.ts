import { createSettingsToggleGroup } from '../common/SettingsToggleRow';
import { SettingsI18n } from '../i18n';

export function createNotificationsSettingsScreen(): HTMLElement {
  const s = SettingsI18n.current;
  return createSettingsToggleGroup([
    [s.pushNotifications, 'pushNotifications'],
    [s.inAppSounds, 'inAppSounds'],
  ]);
}
