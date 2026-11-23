import { el } from '../../../core/ui/dom';
import { settingsStore, type SettingsPrefs } from '../settingsStore';

export type BooleanPrefKey = {
  [K in keyof SettingsPrefs]: SettingsPrefs[K] extends boolean ? K : never;
}[keyof SettingsPrefs];

/** Toggle row; updates in place so focus/scroll are never lost. */
export function createSettingsToggleRow(label: string, key: BooleanPrefKey): HTMLElement {
  const sw = el('div', { class: `ag-switch ${settingsStore.all[key] ? 'is-on' : ''}` }, el('div', { class: 'ag-switch__thumb' }));
  return el(
    'div',
    {
      class: 'ag-settings-row',
      onClick: () => {
        settingsStore.toggle(key);
        sw.classList.toggle('is-on', settingsStore.all[key]);
      },
    },
    el('span', { class: 'ag-settings-row__label' }, label),
    sw,
  );
}

export function createSettingsToggleGroup(rows: Array<[string, BooleanPrefKey]>): HTMLElement {
  return el('div', { class: 'ag-card ag-settings-group' }, ...rows.map(([l, k]) => createSettingsToggleRow(l, k)));
}
