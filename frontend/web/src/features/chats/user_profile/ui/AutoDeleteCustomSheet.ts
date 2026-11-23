import { clear, el } from '../../../../core/ui/dom';
import { icons } from '../../../../core/ui/icons';
import type { UserProfileStrings } from '../i18n/UserProfileI18n';
import type { AutoDeleteConfig, AutoDeletePreset } from '../model/UserProfileModels';

export interface AutoDeleteSheetOptions {
  current: AutoDeleteConfig;
  strings: UserProfileStrings;
  onApply: (config: AutoDeleteConfig) => void;
}

interface PresetOption {
  preset: AutoDeletePreset;
  seconds: number;
  label: (s: UserProfileStrings) => string;
}

const PRESETS: PresetOption[] = [
  { preset: 'off', seconds: 0, label: (s) => s.autoDeleteOff },
  { preset: '1h', seconds: 3600, label: (s) => s.autoDelete1h },
  { preset: '24h', seconds: 86400, label: (s) => s.autoDelete24h },
  { preset: '7d', seconds: 604800, label: (s) => s.autoDelete7d },
  { preset: '30d', seconds: 2592000, label: (s) => s.autoDelete30d },
  { preset: 'custom', seconds: 1800, label: (s) => s.autoDeleteCustom },
];

export function openAutoDeleteCustomSheet(opts: AutoDeleteSheetOptions): void {
  const { current, strings, onApply } = opts;
  let selectedPreset: AutoDeletePreset = current.preset;
  let customValue =
    current.preset === 'custom' && current.seconds >= 86400
      ? Math.max(1, Math.round(current.seconds / 86400))
      : current.preset === 'custom' && current.seconds >= 3600
        ? Math.max(1, Math.round(current.seconds / 3600))
        : current.preset === 'custom'
          ? Math.max(1, Math.round(current.seconds / 60))
          : 30;
  let customUnit: 'min' | 'hr' | 'day' =
    current.preset === 'custom' && current.seconds >= 86400
      ? 'day'
      : current.preset === 'custom' && current.seconds >= 3600
        ? 'hr'
        : 'min';

  const backdrop = el('div', { class: 'ag-up-sheet-backdrop' });
  const sheet = el('div', { class: 'ag-up-sheet', role: 'dialog', 'aria-modal': 'true' });

  const close = () => {
    document.removeEventListener('keydown', onKeyDown);
    backdrop.remove();
  };

  const onKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') close();
  };
  document.addEventListener('keydown', onKeyDown);

  backdrop.addEventListener('mousedown', (e) => {
    if (e.target === backdrop) close();
  });

  const render = () => {
    clear(sheet);

    const header = el(
      'div',
      { class: 'ag-up-sheet__header' },
      el('div', { class: 'ag-up-sheet__title' }, strings.autoDeleteTitle),
      el(
        'button',
        { class: 'ag-icon-btn', onClick: close, 'aria-label': strings.autoDeleteCancel },
        icons.close(19),
      ),
    );

    const list = el('div', { class: 'ag-up-sheet__options' });
    for (const item of PRESETS) {
      const isSelected = selectedPreset === item.preset;
      list.appendChild(
        el(
          'button',
          {
            class: 'ag-up-sheet__option' + (isSelected ? ' is-selected' : ''),
            onClick: () => {
              selectedPreset = item.preset;
              render();
            },
          },
          el('span', null, item.label(strings)),
          isSelected ? el('span', { class: 'ag-up-sheet__check' }, icons.check(16)) : null,
        ),
      );
    }

    let customBox: HTMLElement | null = null;
    if (selectedPreset === 'custom') {
      const numInput = el('input', {
        type: 'number',
        min: '1',
        max: '365',
        value: String(customValue),
        class: 'ag-up-sheet__input',
        onInput: (e: Event) => {
          const val = Number((e.target as HTMLInputElement).value);
          if (Number.isFinite(val) && val > 0) customValue = Math.min(365, Math.floor(val));
        },
      });

      const unitBtn = (unit: 'min' | 'hr' | 'day', label: string) =>
        el(
          'button',
          {
            class: 'ag-up-sheet__unit' + (customUnit === unit ? ' is-active' : ''),
            onClick: () => {
              customUnit = unit;
              render();
            },
          },
          label,
        );

      customBox = el(
        'div',
        { class: 'ag-up-sheet__custom' },
        numInput,
        el(
          'div',
          { class: 'ag-up-sheet__units' },
          unitBtn('min', strings.autoDeleteUnitMin),
          unitBtn('hr', strings.autoDeleteUnitHours),
          unitBtn('day', strings.autoDeleteUnitDays),
        ),
      );
    }

    const footer = el('div', { class: 'ag-up-sheet__footer' }, strings.autoDeleteFooter);

    const applyBtn = el(
      'button',
      {
        class: 'ag-pill-btn ag-pill-btn--primary ag-up-sheet__apply',
        onClick: () => {
          let seconds = 0;
          if (selectedPreset === 'custom') {
            const mult = customUnit === 'day' ? 86400 : customUnit === 'hr' ? 3600 : 60;
            seconds = Math.max(60, customValue * mult);
          } else {
            seconds = PRESETS.find((p) => p.preset === selectedPreset)?.seconds ?? 0;
          }
          onApply({ preset: selectedPreset, seconds });
          close();
        },
      },
      selectedPreset === 'off' ? strings.autoDeleteDisable : strings.autoDeleteApply,
    );

    sheet.appendChild(header);
    sheet.appendChild(list);
    if (customBox) sheet.appendChild(customBox);
    sheet.appendChild(footer);
    sheet.appendChild(applyBtn);
  };

  render();
  backdrop.appendChild(sheet);
  document.body.appendChild(backdrop);
}
