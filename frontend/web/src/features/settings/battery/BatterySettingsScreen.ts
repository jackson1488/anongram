import { createSettingsToggleGroup } from '../common/SettingsToggleRow';
import { SettingsI18n } from '../i18n';

export function createBatterySettingsScreen(): HTMLElement {
  const s = SettingsI18n.current;
  return createSettingsToggleGroup([
    [s.powerSavingMode, 'powerSavingMode'],
  ]);
}
