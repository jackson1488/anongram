import { i18nStore } from '../../../core/i18n/i18nStore';
import { themeStore } from '../../../core/theme/themeStore';
import { clear, el } from '../../../core/ui/dom';
import { createSettingsToggleGroup } from '../common/SettingsToggleRow';
import { SettingsI18n } from '../i18n';
import { settingsStore, type FontFamilyOption, type VoiceEngineOption } from '../settingsStore';

const VOICE_ENGINES: VoiceEngineOption[] = ['whisper', 'vosk', 'system'];
const FONT_FAMILIES: FontFamilyOption[] = ['system', 'inter', 'mono', 'serif', 'rounded'];

export function createModulesSettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-settings-content' });

  const render = () => {
    clear(root);
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;

    // 1. Core Modules Toggles
    root.appendChild(
      createSettingsToggleGroup([
        [s.voiceModule, 'voiceModule'],
        [s.translationModule, 'translationModule'],
        [s.localAiModule, 'localAiModule'],
        [s.showTranslateButton, 'showTranslateButton'],
        [s.autoTranslateIncoming, 'autoTranslateIncoming'],
      ]),
    );

    // 2. Voice Engine Selector
    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el('div', { class: 'ag-section-label' }, s.voiceEngineTitle),
        el(
          'div',
          { class: 'ag-chips-wrap' },
          ...VOICE_ENGINES.map((eng) =>
            el(
              'button',
              {
                class: `ag-chip ${prefs.voiceEngine === eng ? 'is-active' : ''}`,
                onClick: () => settingsStore.set('voiceEngine', eng),
              },
              s.voiceEngines[eng],
            ),
          ),
        ),
      ),
    );

    // 3. Fonts Module: Family & Size selector
    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el('div', { class: 'ag-section-label' }, s.fontsTitle),
        el('div', { style: 'font-size:13px;color:var(--ag-text-secondary)' }, s.fontFamilyTitle),
        el(
          'div',
          { class: 'ag-chips-wrap' },
          ...FONT_FAMILIES.map((ff) =>
            el(
              'button',
              {
                class: `ag-chip ${prefs.fontFamily === ff ? 'is-active' : ''}`,
                onClick: () => settingsStore.set('fontFamily', ff),
              },
              s.fontFamilies[ff],
            ),
          ),
        ),
        el('div', { style: 'font-size:13px;color:var(--ag-text-secondary)' }, `${s.fontSizeTitle}: ${themeStore.messageTextSize} px`),
        el('input', {
          type: 'range',
          class: 'ag-slider',
          min: 11,
          max: 30,
          value: themeStore.messageTextSize,
          onInput: (e: Event) => {
            themeStore.setMessageTextSize(Number((e.target as HTMLInputElement).value));
          },
        }),
      ),
    );
  };

  const u1 = settingsStore.subscribe(render);
  const u2 = themeStore.subscribe(render);
  const u3 = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
    u3();
  };

  return root;
}
