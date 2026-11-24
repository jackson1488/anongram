import { i18nStore } from '../../../core/i18n/i18nStore';
import { themeStore } from '../../../core/theme/themeStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { localTranslator } from '../../chats/tools/translate/LocalMessageTranslator';
import { SettingsI18n } from '../i18n';
import type { SettingsRoute } from '../navigation/SettingsRoute';
import { settingsStore } from '../settingsStore';
import './modules.css';

const SVG_NS = 'http://www.w3.org/2000/svg';

function modSvg(paths: string, size = 24): SVGSVGElement {
  const svg = document.createElementNS(SVG_NS, 'svg');
  svg.setAttribute('width', String(size));
  svg.setAttribute('height', String(size));
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('fill', 'none');
  svg.setAttribute('stroke', 'currentColor');
  svg.setAttribute('stroke-width', '2');
  svg.setAttribute('stroke-linecap', 'round');
  svg.setAttribute('stroke-linejoin', 'round');
  svg.innerHTML = paths;
  return svg;
}

export const moduleIcons = {
  graphicEq: (s = 24) =>
    modSvg(
      '<line x1="4" y1="10" x2="4" y2="14"/><line x1="8" y1="7" x2="8" y2="17"/><line x1="12" y1="3" x2="12" y2="21"/><line x1="16" y1="7" x2="16" y2="17"/><line x1="20" y1="10" x2="20" y2="14"/>',
      s,
    ),
  translate: (s = 24) =>
    modSvg(
      '<path d="m5 8 6 6"/><path d="m4 14 6-6 2-3"/><path d="M2 5h12"/><path d="M7 2h1"/><path d="m22 22-5-10-5 10"/><path d="M14 18h6"/>',
      s,
    ),
  sparkles: (s = 24) =>
    modSvg(
      '<path d="m12 3-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z"/>',
      s,
    ),
  formatSize: (s = 24) =>
    modSvg(
      '<path d="M4 7V4h16v3"/><path d="M12 4v16"/><path d="M8 20h8"/>',
      s,
    ),
};

export function formatModuleMb(mb: number, unit: string): string {
  if (mb <= 0) return `0 ${unit}`;
  const formatted = Number.isInteger(mb) ? String(mb) : mb.toFixed(1);
  return `${formatted} ${unit}`;
}

export function createRadioDot(selected: boolean, danger = false): HTMLElement {
  return el(
    'span',
    { class: `ag-mod-radio${selected ? ' is-selected' : ''}${danger ? ' is-danger' : ''}` },
    selected ? el('span', { class: 'ag-mod-radio__dot' }) : null,
  );
}

export function createProgressBar(pct: number): HTMLElement {
  const clamped = Math.max(0, Math.min(100, Math.round(pct)));
  return el(
    'div',
    { class: 'ag-mod-progress-wrap' },
    el(
      'div',
      { class: 'ag-mod-progress' },
      el('div', { class: 'ag-mod-progress__fill', style: `width:${clamped}%` }),
    ),
  );
}

export function createFlatToggleRow(
  title: string,
  desc: string,
  checked: boolean,
  onToggle: () => void,
): HTMLElement {
  const sw = el(
    'div',
    { class: `ag-switch ${checked ? 'is-on' : ''}` },
    el('div', { class: 'ag-switch__thumb' }),
  );
  return el(
    'button',
    {
      type: 'button',
      class: 'ag-mod-row ag-mod-row--clickable',
      onClick: onToggle,
    },
    el(
      'div',
      { class: 'ag-mod-row__col' },
      el('span', { class: 'ag-mod-row__title' }, title),
      desc ? el('span', { class: 'ag-mod-row__desc' }, desc) : null,
    ),
    sw,
  );
}

export function createModulesSettingsScreen(
  onNavigate?: (route: SettingsRoute) => void,
): HTMLElement {
  const root = el('div', { class: 'ag-mod-flat-screen' });

  const render = () => {
    clear(root);
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;

    // 1. Voice Transcription status
    const isSystemVoice = prefs.voiceEngine === 'system';
    const activeVoiceLabel =
      prefs.voiceEngine === 'whisper'
        ? `Whisper (${prefs.voiceWhisperModel === 'whisper_base_q5' ? 'Base' : 'Tiny'})`
        : prefs.voiceEngine === 'vosk'
          ? 'Vosk (Small)'
          : s.voiceSystemTitle;
    const voiceSub = `${activeVoiceLabel} • ${s.moduleMemoryLabel(
      formatModuleMb(settingsStore.installedVoiceMb, s.mbUnit),
    )}`;

    // 2. Translation status (wired to localTranslator)
    const translateEnabled = prefs.showTranslateButton || prefs.autoTranslateIncoming;
    const packsCount = localTranslator.getDownloadedPacks().length;
    const packsMb = localTranslator.getTotalDownloadedMb();
    const translateStatus = translateEnabled ? s.translationEnabledStatus : s.translationDisabledStatus;
    const translateSub = `${s.translationHubEngine} • ${translateStatus} • ${s.translationPacksCount(
      packsCount,
      formatModuleMb(packsMb, s.mbUnit),
    )}`;

    // 3. Local AI GGUF status
    const installedAiCount = prefs.installedAiModels.length;
    const activeAi = settingsStore.activeAiModel;
    const aiSub =
      installedAiCount > 0 && activeAi
        ? `${activeAi.displayName} • ${s.moduleMemoryLabel(
            formatModuleMb(settingsStore.installedAiMb, s.mbUnit),
          )}`
        : s.localAiHubEmpty;

    // 4. Fonts status
    const fontSub = `${s.fontFamilies[prefs.fontFamily]} • ${themeStore.messageTextSize} px`;

    const rows: Array<{
      route: SettingsRoute;
      icon: SVGElement;
      title: string;
      subtitle: string;
      danger?: boolean;
    }> = [
      {
        route: 'moduleVoice',
        icon: moduleIcons.graphicEq(24),
        title: s.modulesVoiceTitle,
        subtitle: voiceSub,
        danger: isSystemVoice,
      },
      {
        route: 'moduleTranslation',
        icon: moduleIcons.translate(24),
        title: s.modulesTranslationTitle,
        subtitle: translateSub,
      },
      {
        route: 'moduleLocalAi',
        icon: moduleIcons.sparkles(24),
        title: s.modulesLocalAiTitle,
        subtitle: aiSub,
      },
      {
        route: 'moduleFonts',
        icon: moduleIcons.formatSize(24),
        title: s.modulesFontsTitle,
        subtitle: fontSub,
      },
    ];

    const list = el('div', { class: 'ag-mod-hub' });
    rows.forEach((row, idx) => {
      list.appendChild(
        el(
          'button',
          {
            type: 'button',
            class: 'ag-mod-hub-row',
            onClick: () => onNavigate?.(row.route),
          },
          el(
            'div',
            { class: 'ag-mod-hub-row__left' },
            el('span', { class: 'ag-mod-hub-row__icon' }, row.icon),
            el(
              'div',
              { class: 'ag-mod-hub-row__body' },
              el('span', { class: 'ag-mod-hub-row__title' }, row.title),
              el(
                'span',
                { class: `ag-mod-hub-row__sub${row.danger ? ' is-danger' : ''}` },
                row.subtitle,
              ),
            ),
          ),
          el('span', { class: 'ag-mod-hub-row__chevron' }, icons.chevronRight(18)),
        ),
      );
      list.appendChild(
        el('div', {
          class: idx < rows.length - 1 ? 'ag-mod-divider--inset' : 'ag-mod-divider',
        }),
      );
    });

    root.appendChild(list);
  };

  const u1 = settingsStore.subscribe(render);
  const u2 = themeStore.subscribe(render);
  const u3 = i18nStore.subscribe(render);
  const u4 = localTranslator.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
    u3();
    u4();
  };

  return root;
}
