import { i18nStore } from '../../../core/i18n/i18nStore';
import { themeStore } from '../../../core/theme/themeStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { SettingsI18n } from '../i18n';
import {
  FONT_STACKS,
  settingsStore,
  type FontApplyScope,
  type FontFamilyOption,
} from '../settingsStore';
import './modules.css';

type FeaturedFontId = 'system' | 'inter' | 'mono' | 'roboto' | 'sfPro' | 'comfortaa';

const FEATURED_FONTS: FeaturedFontId[] = [
  'system',
  'inter',
  'mono',
  'roboto',
  'sfPro',
  'comfortaa',
];

export function createFontsModuleSettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-mod-flat-screen' });

  const scopeHeader = el('div', { class: 'ag-mod-section-head' });
  const scopeRow = el('div', { class: 'ag-mod-scope-row' });

  const previewHeader = el('div', { class: 'ag-mod-section-head' });
  const previewTitleEl = el('span', { class: 'ag-mod-section-title' });
  const previewMetaEl = el('span', { class: 'ag-mod-section-meta' });
  previewHeader.append(previewTitleEl, previewMetaEl);

  const bubbleInText = el('div', { class: 'ag-bubble__text' });
  const bubbleOutText = el('div', { class: 'ag-bubble__text' });
  const previewBox = el(
    'div',
    { class: 'ag-preview-box' },
    el('div', { class: 'ag-msg-row is-incoming' }, el('div', { class: 'ag-bubble is-incoming' }, bubbleInText)),
    el('div', { class: 'ag-msg-row is-outgoing' }, el('div', { class: 'ag-bubble is-outgoing' }, bubbleOutText)),
  );

  const slider = el('input', {
    type: 'range',
    class: 'ag-slider',
    min: 11,
    max: 30,
    value: themeStore.messageTextSize,
    onInput: (e: Event) => {
      themeStore.setMessageTextSize(Number((e.target as HTMLInputElement).value));
    },
  }) as HTMLInputElement;

  const previewWrap = el('div', { class: 'ag-mod-preview-wrap' }, previewBox, slider);

  const cardsHeader = el('div', { class: 'ag-mod-section-head' });
  const cardsGrid = el('div', { class: 'ag-mod-font-grid' });

  root.append(
    scopeHeader,
    scopeRow,
    el('div', { class: 'ag-mod-divider' }),
    previewHeader,
    previewWrap,
    el('div', { class: 'ag-mod-divider' }),
    cardsHeader,
    cardsGrid,
  );

  const updatePreviewOnly = () => {
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;
    const activeStack = FONT_STACKS[prefs.fontFamily] ?? FONT_STACKS.system;

    previewTitleEl.textContent = s.fontPreviewHeader;
    previewMetaEl.textContent = `${s.fontFamilies[prefs.fontFamily]} • ${themeStore.messageTextSize} px`;
    bubbleInText.textContent = s.fontPreviewIncoming;
    bubbleOutText.textContent = s.fontPreviewOutgoing;
    previewBox.style.fontFamily = `var(--ag-font-family, ${activeStack})`;
    slider.value = String(themeStore.messageTextSize);
  };

  const renderAll = () => {
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;

    // 1. Scope header + Reset
    clear(scopeHeader);
    scopeHeader.append(
      el('span', { class: 'ag-mod-section-title' }, s.fontScopeLabel),
      ...(prefs.fontFamily !== 'system'
        ? [
            el(
              'button',
              {
                type: 'button',
                class: 'ag-mod-btn ag-mod-btn--ghost',
                onClick: () => settingsStore.set('fontFamily', 'system'),
              },
              s.fontResetBtn,
            ),
          ]
        : []),
    );

    clear(scopeRow);
    const scopes: Array<{ id: FontApplyScope; label: string }> = [
      { id: 'all', label: s.fontScopeAll },
      { id: 'messages', label: s.fontScopeMessages },
      { id: 'ui', label: s.fontScopeUi },
    ];
    scopes.forEach((sc) => {
      scopeRow.appendChild(
        el(
          'button',
          {
            type: 'button',
            class: `ag-mod-scope-pill${prefs.fontApplyScope === sc.id ? ' is-active' : ''}`,
            onClick: () => settingsStore.set('fontApplyScope', sc.id),
          },
          sc.label,
        ),
      );
    });

    // 2. Live Preview
    updatePreviewOnly();

    // 3. Featured Font Cards
    clear(cardsHeader);
    cardsHeader.append(
      el('span', { class: 'ag-mod-section-title' }, s.fontCardsHeader),
      el('span', { class: 'ag-mod-section-meta' }, `${FEATURED_FONTS.length}`),
    );

    clear(cardsGrid);
    FEATURED_FONTS.forEach((ff: FontFamilyOption & FeaturedFontId) => {
      const isActive = prefs.fontFamily === ff;
      const stack = FONT_STACKS[ff];

      cardsGrid.appendChild(
        el(
          'button',
          {
            type: 'button',
            class: `ag-mod-font-card${isActive ? ' is-active' : ''}`,
            onClick: () => settingsStore.set('fontFamily', ff),
          },
          el(
            'div',
            { class: 'ag-mod-font-card__top' },
            el(
              'span',
              { class: 'ag-mod-font-card__name', style: `font-family:${stack}` },
              s.fontFamilies[ff],
            ),
            isActive
              ? el('span', { class: 'ag-mod-font-card__check' }, icons.check(16))
              : null,
          ),
          el(
            'div',
            { class: 'ag-mod-font-card__sample', style: `font-family:${stack}` },
            s.fontSampleGlyphs,
          ),
          el('div', { class: 'ag-mod-font-card__desc' }, s.fontDescriptions[ff]),
        ),
      );
    });
  };

  const u1 = settingsStore.subscribe(renderAll);
  const u2 = themeStore.subscribe(updatePreviewOnly);
  const u3 = i18nStore.subscribe(renderAll);
  renderAll();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
    u3();
  };

  return root;
}
