import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import type { SettingsRoute } from '../navigation/SettingsRoute';

export function createSettingsSearchScreen(onNavigate: (r: SettingsRoute) => void): HTMLElement {
  const t = i18nStore.t;
  const qInput = el('input', {
    type: 'text',
    placeholder: t.settings.searchPlaceholder,
    onInput: () => renderResults(),
  }) as HTMLInputElement;
  const resBox = el('div', { class: 'ag-card ag-settings-group' });
  const entries: Array<{ route: SettingsRoute; label: string }> = [
    { route: 'chatAppearance', label: t.settings.chatAppearance },
    { route: 'account', label: t.settings.account },
    { route: 'privacy', label: t.settings.privacy },
    { route: 'security', label: t.settings.security },
    { route: 'notifications', label: t.settings.notifications },
    { route: 'dataStorage', label: t.settings.dataStorage },
    { route: 'animations', label: t.settings.animations },
    { route: 'devices', label: t.settings.devices },
    { route: 'battery', label: t.settings.battery },
    { route: 'language', label: t.settings.language },
    { route: 'modules', label: t.settings.modules },
    { route: 'myQr', label: t.settings.myQr },
  ];
  const renderResults = () => {
    clear(resBox);
    const q = qInput.value.trim().toLowerCase();
    for (const item of entries.filter((e) => !q || e.label.toLowerCase().includes(q))) {
      resBox.appendChild(
        el(
          'button',
          { class: 'ag-settings-row', onClick: () => onNavigate(item.route) },
          el('span', { class: 'ag-settings-row__label' }, item.label),
          icons.chevronRight(16),
        ),
      );
    }
  };
  renderResults();
  return el(
    'div',
    null,
    el('div', { class: 'ag-search-pill', style: 'margin-bottom:12px' }, icons.search(16), qInput),
    resBox,
  );
}
