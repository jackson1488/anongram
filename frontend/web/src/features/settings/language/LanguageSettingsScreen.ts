import { i18nStore } from '../../../core/i18n/i18nStore';
import { el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';

export function createLanguageSettingsScreen(): HTMLElement {
  const t = i18nStore.t;
  const group = el('div', { class: 'ag-card ag-settings-group' });
  for (const lang of ['ru', 'en'] as const) {
    group.appendChild(
      el(
        'button',
        { class: 'ag-settings-row', onClick: () => i18nStore.setLanguage(lang) },
        el('span', { class: 'ag-settings-row__label' }, t.settings.languageNames[lang]),
        i18nStore.language === lang ? icons.check(18) : null,
      ),
    );
  }
  return group;
}
