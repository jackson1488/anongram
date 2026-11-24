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
import { createFontsModuleSettingsScreen } from './modules/FontsModuleSettingsScreen';
import { createModulesSettingsScreen } from './modules/ModulesSettingsScreen';
import { createTranslationModuleSettingsScreen } from './modules/TranslationModuleSettingsScreen';
import { createVoiceModuleSettingsScreen } from './modules/VoiceModuleSettingsScreen';
import type { SettingsRoute } from './navigation/SettingsRoute';
import { createNotificationsSettingsScreen } from './notifications/NotificationsSettingsScreen';
import { createPrivacySecuritySettingsScreen } from './privacy/PrivacySecuritySettingsScreen';
import { createMyQrCodeScreen } from './qr/MyQrCodeScreen';
import { createSettingsSearchScreen } from './search/SettingsSearchScreen';
import { createSecuritySettingsScreen } from './security/SecuritySettingsScreen';
import { SettingsI18n } from './i18n';
import { settingsStore } from './settingsStore';

export type { SettingsRoute };
export { createSettingsListPane };

export function createSettingsDetailView(
  route: SettingsRoute,
  onNavigate: (r: SettingsRoute) => void,
  onBackMobile: () => void,
): HTMLElement {
  const root = el('div', { class: 'ag-settings-detail' });
  let screenCleanup: (() => void) | null = null;

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
      case 'modules': return createModulesSettingsScreen(onNavigate);
      case 'moduleVoice': return createVoiceModuleSettingsScreen();
      case 'moduleTranslation': return createTranslationModuleSettingsScreen();
      case 'moduleFonts': return createFontsModuleSettingsScreen();
      case 'devices': return createDevicesSettingsScreen();
      default: return null;
    }
  };

  const render = () => {
    screenCleanup?.();
    screenCleanup = null;
    clear(root);
    const t = i18nStore.t;
    const s = SettingsI18n.current;
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
      moduleVoice: s.modulesVoiceTitle,
      moduleTranslation: s.modulesTranslationTitle,
      moduleLocalAi: s.modulesTitle,
      moduleFonts: s.modulesFontsTitle,
    };

    const isModuleSubRoute =
      route === 'moduleVoice' ||
      route === 'moduleTranslation' ||
      route === 'moduleFonts';

    const handleBack = () => {
      if (isModuleSubRoute) {
        onNavigate('modules');
      } else if (window.innerWidth >= 840) {
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
    if (screen) {
      screenCleanup = (screen as unknown as { __cleanup?: () => void }).__cleanup ?? null;
      body.appendChild(screen);
    }
    root.appendChild(body);
  };

  // Language change rebuilds; toggles update in place (no re-render, focus kept).
  const u1 = i18nStore.subscribe(render);
  void settingsStore;
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    screenCleanup?.();
    screenCleanup = null;
  };
  return root;
}
