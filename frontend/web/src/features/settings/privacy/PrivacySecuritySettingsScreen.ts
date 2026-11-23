import { createSettingsToggleGroup } from '../common/SettingsToggleRow';
import { SettingsI18n } from '../i18n';

export function createPrivacySecuritySettingsScreen(): HTMLElement {
  const s = SettingsI18n.current;
  return createSettingsToggleGroup([
    [s.hideOnlineStatus, 'hideOnlineStatus'],
    [s.blockScreenshots, 'blockScreenshots'],
  ]);
}
