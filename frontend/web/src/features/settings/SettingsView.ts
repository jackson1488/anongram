import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { createAccountSettingsScreen } from './account/AccountSettingsScreen';
import { createAnimationsSettingsScreen } from './animations/AnimationsSettingsScreen';
import { createBatterySettingsScreen } from './battery/BatterySettingsScreen';
import { createChatAppearanceSettingsScreen } from './chat_settings/ui/ChatAppearanceSettingsScreen';
import { createDataStorageSettingsScreen } from './data_storage/DataStorageSettingsScreen';
import { createDevicesSettingsScreen } from './devices/DevicesSettingsScreen';
import { createLanguageSettingsScreen } from './language/LanguageSettingsScreen';
import { createSettingsListPane } from './main/SettingsMainScreen';
import { createModulesSettingsScreen } from './modules/ModulesSettingsScreen';
import type { SettingsRoute } from './navigation/SettingsRoute';
import { createNotificationsSettingsScreen } from './notifications/NotificationsSettingsScreen';
import { createPrivacySecuritySettingsScreen } from './privacy/PrivacySecuritySettingsScreen';
import { createMyQrCodeScreen } from './qr/MyQrCodeScreen';
import { createSettingsSearchScreen } from './search/SettingsSearchScreen';
import { createSecuritySettingsScreen } from './security/SecuritySettingsScreen';
import { settingsStore } from './settingsStore';

export type { SettingsRoute };
export { createSettingsListPane };

export function createSettingsDetailView(
  route: SettingsRoute,
  onNavigate: (r: SettingsRoute) => void,
  onBackMobile: () => void,
): HTMLElement {
  const root = el('div', { class: 'ag-settings-detail' });

  const build = (): HTMLElement | null => {
    switch (route) {
      case 'chatAppearance': return createChatAppearanceSettingsScreen();
      case 'myQr': return createMyQrCodeScreen();
      case 'search': return createSettingsSearchScreen(onNavigate);
      case 'account': return createAccountSettingsScreen();
      case 'language': return createLanguageSettingsScreen();
      case 'privacy': return createPrivacySecuritySettingsScreen();
      case 'security': return createSecuritySettingsScreen();
      case 'notifications': return createNotificationsSettingsScreen();
      case 'dataStorage': return createDataStorageSettingsScreen();
      case 'animations': return createAnimationsSettingsScreen();
      case 'battery': return createBatterySettingsScreen();
      case 'modules': return createModulesSettingsScreen();
      case 'devices': return createDevicesSettingsScreen();
      default: return null;
    }
  };

  const render = () => {
    clear(root);
    const t = i18nStore.t;
    const titles: Record<SettingsRoute, string> = {
      main: t.settings.title,
      search: t.common.search,
      myQr: t.settings.myQr,
      account: t.settings.account,
      chatAppearance: t.settings.chatAppearance,
      privacy: t.settings.privacy,
      security: t.settings.security,
      notifications: t.settings.notifications,
      dataStorage: t.settings.dataStorage,
      animations: t.settings.animations,
      devices: t.settings.devices,
      battery: t.settings.battery,
      language: t.settings.language,
      modules: t.settings.modules,
    };

    const handleBack = () => {
      if (window.innerWidth >= 840) {
        onNavigate('chatAppearance');
      } else {
        onBackMobile();
      }
    };

    const backBtnClass =
      route === 'chatAppearance'
        ? 'ag-icon-btn ag-mobile-only ag-settings-back'
        : 'ag-icon-btn ag-settings-back';

    root.appendChild(
      el(
        'div',
        { class: 'ag-chat-header ag-settings-header' },
        el('button', { class: backBtnClass, title: t.common.back, onClick: handleBack }, icons.back(22)),
        el('div', { class: 'ag-chat-header__title' }, titles[route]),
      ),
    );
    const body = el('div', { class: 'ag-settings-scroll' });
    const screen = build();
    if (screen) body.appendChild(screen);
    root.appendChild(body);
  };

  // Language change rebuilds; toggles update in place (no re-render, focus kept).
  const u1 = i18nStore.subscribe(render);
  void settingsStore;
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => u1();
  return root;
}
