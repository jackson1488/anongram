import { i18nStore } from '../../core/i18n/i18nStore';
import { ANONGRAM_PRESETS } from '../../core/theme/presets';
import {
  decodeThemeSpec,
  encodeThemeSpec,
  type BubbleColorStyle,
  type ThemePolarity,
  type ThemeSpec,
  type ThemeTintMode,
} from '../../core/theme/themeSpec';
import { themeStore } from '../../core/theme/themeStore';
import { clear, copyToClipboard, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { createHsvColorWheel } from './components/HsvColorWheel';

export function createChatAppearanceView(): HTMLElement {
  const root = el('div', { class: 'ag-settings-content' });

  const render = () => {
    clear(root);
    const t = i18nStore.t;
    const ap = t.settings.appearance;
    const scheme = themeStore.scheme;
    const spec: ThemeSpec = { ...scheme.spec };

    // 1. Live Bubble Preview + Text Size Slider
    const previewCard = el(
      'div',
      { class: 'ag-card ag-settings-card' },
      el('div', { class: 'ag-section-label' }, `${ap.textSizeTitle}: ${themeStore.messageTextSize} px`),
      el(
        'div',
        { class: 'ag-preview-box' },
        el(
          'div',
          { class: 'ag-msg-row is-incoming' },
          el('div', { class: 'ag-bubble is-incoming' }, el('div', { class: 'ag-bubble__text' }, ap.textSizeSampleIn)),
        ),
        el(
          'div',
          { class: 'ag-msg-row is-outgoing' },
          el('div', { class: 'ag-bubble is-outgoing' }, el('div', { class: 'ag-bubble__text' }, ap.textSizeSampleOut)),
        ),
      ),
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
    );
    root.appendChild(previewCard);

    // 2. Curated Presets
    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el('div', { class: 'ag-section-label' }, ap.presetsTitle),
        el(
          'div',
          { class: 'ag-presets-grid' },
          ...ANONGRAM_PRESETS.map((preset) =>
            el(
              'button',
              {
                class: `ag-preset-chip ${scheme.id === preset.id ? 'is-active' : ''}`,
                onClick: () => themeStore.selectPreset(preset.id),
              },
              el('span', {
                class: 'ag-preset-dot',
                style: `background:${preset.accent};border-color:${preset.background}`,
              }),
              el('span', null, preset.displayName),
            ),
          ),
        ),
      ),
    );

    // 3. HSV Color Wheel + Polarity + Tint + Bubble Style
    const wheel = createHsvColorWheel({
      hue: spec.hue,
      saturation: spec.saturation,
      onChange: (hue, saturation) => {
        themeStore.applyCustomSpec({ ...spec, hue, saturation });
      },
    });

    const polarities: Array<{ id: ThemePolarity; label: string }> = [
      { id: 'DARK', label: ap.dark },
      { id: 'LIGHT', label: ap.light },
    ];
    const tints: Array<{ id: ThemeTintMode; label: string }> = [
      { id: 'MONOCHROME', label: ap.tintMono },
      { id: 'AMOLED_ACCENT', label: ap.tintAmoled },
      { id: 'SOFT_OS', label: ap.tintSoft },
      { id: 'FULL_OS', label: ap.tintFull },
    ];
    const bubbles: Array<{ id: BubbleColorStyle; label: string }> = [
      { id: 'GRADIENT', label: ap.bubbleGradient },
      { id: 'TINTED', label: ap.bubbleTinted },
      { id: 'OUTLINE', label: ap.bubbleOutline },
    ];

    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el('div', { class: 'ag-section-label' }, ap.colorWheelTitle),
        el('div', { class: 'ag-wheel-center' }, wheel),
        el('div', { class: 'ag-section-label' }, ap.polarityTitle),
        el(
          'div',
          { class: 'ag-chips-wrap' },
          ...polarities.map((p) =>
            el(
              'button',
              {
                class: `ag-chip ${spec.polarity === p.id ? 'is-active' : ''}`,
                onClick: () => themeStore.applyCustomSpec({ ...spec, polarity: p.id }),
              },
              p.label,
            ),
          ),
        ),
        el('div', { class: 'ag-section-label' }, ap.tintTitle),
        el(
          'div',
          { class: 'ag-chips-wrap' },
          ...tints.map((m) =>
            el(
              'button',
              {
                class: `ag-chip ${spec.tintMode === m.id ? 'is-active' : ''}`,
                onClick: () => themeStore.applyCustomSpec({ ...spec, tintMode: m.id }),
              },
              m.label,
            ),
          ),
        ),
        el('div', { class: 'ag-section-label' }, ap.bubbleStyleTitle),
        el(
          'div',
          { class: 'ag-chips-wrap' },
          ...bubbles.map((b) =>
            el(
              'button',
              {
                class: `ag-chip ${spec.bubbleStyle === b.id ? 'is-active' : ''}`,
                onClick: () => themeStore.applyCustomSpec({ ...spec, bubbleStyle: b.id }),
              },
              b.label,
            ),
          ),
        ),
      ),
    );

    // 4. Custom Saved Themes & AG1 Share/Import
    const ag1Code = encodeThemeSpec(spec);
    const nameInput = el('input', {
      type: 'text',
      class: 'ag-input',
      placeholder: ap.themeNamePlaceholder,
    }) as HTMLInputElement;
    const importInput = el('input', {
      type: 'text',
      class: 'ag-input',
      placeholder: ap.importAg1Placeholder,
    }) as HTMLInputElement;

    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el('div', { class: 'ag-section-label' }, ap.customThemesTitle),
        el(
          'div',
          { class: 'ag-inline-row' },
          nameInput,
          el(
            'button',
            {
              class: 'ag-pill-btn ag-pill-btn--primary',
              onClick: () => {
                themeStore.saveCustomTheme(nameInput.value, spec);
                showToast(t.common.save);
              },
            },
            ap.saveThemeBtn,
          ),
        ),
        themeStore.savedCustomThemes.length > 0
          ? el(
              'div',
              { class: 'ag-presets-grid' },
              ...themeStore.savedCustomThemes.map((saved) =>
                el(
                  'div',
                  { class: 'ag-saved-theme-item' },
                  el(
                    'button',
                    {
                      class: 'ag-chip',
                      onClick: () => themeStore.applyCustomSpec(saved.spec, saved.name),
                    },
                    saved.name,
                  ),
                  el(
                    'button',
                    {
                      class: 'ag-icon-btn',
                      style: 'width:28px;height:28px',
                      onClick: () => themeStore.deleteCustomTheme(saved.id),
                    },
                    icons.trash(14),
                  ),
                ),
              ),
            )
          : null,
        el('div', { class: 'ag-section-label' }, ap.ag1CodeTitle),
        el(
          'div',
          { class: 'ag-inline-row' },
          el('code', { class: 'ag-ag1-code' }, ag1Code),
          el(
            'button',
            {
              class: 'ag-pill-btn ag-pill-btn--secondary',
              onClick: async () => {
                await copyToClipboard(ag1Code);
                showToast(t.common.copied);
              },
            },
            ap.copyAg1,
          ),
        ),
        el(
          'div',
          { class: 'ag-inline-row' },
          importInput,
          el(
            'button',
            {
              class: 'ag-pill-btn ag-pill-btn--ghost',
              onClick: () => {
                const decoded = decodeThemeSpec(importInput.value);
                if (!decoded) {
                  showToast(ap.invalidAg1);
                  return;
                }
                themeStore.applyCustomSpec(decoded);
                showToast(t.common.apply);
              },
            },
            ap.importAg1Btn,
          ),
        ),
      ),
    );
  };

  const unsubTheme = themeStore.subscribe(render);
  const unsubI18n = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    unsubTheme();
    unsubI18n();
  };

  return root;
}
