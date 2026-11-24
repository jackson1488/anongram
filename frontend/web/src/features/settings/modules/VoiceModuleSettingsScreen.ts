import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { SettingsI18n } from '../i18n';
import {
  VOICE_PACK_SPECS,
  settingsStore,
  type VoiceLanguageOption,
  type VoiceSystemLanguageOption,
} from '../settingsStore';
import {
  createFlatToggleRow,
  createProgressBar,
  createRadioDot,
  formatModuleMb,
} from './ModulesSettingsScreen';
import './modules.css';

interface VoicePackMeta {
  id: string;
  engine: 'whisper' | 'vosk';
  sizeMb: number;
  title: () => string;
  desc: () => string;
}

const VOICE_PACK_META: VoicePackMeta[] = [
  {
    id: 'whisper_tiny_q5',
    engine: 'whisper',
    sizeMb: 32,
    title: () => SettingsI18n.current.voiceWhisperTinyTitle,
    desc: () => SettingsI18n.current.voiceWhisperTinyDesc,
  },
  {
    id: 'whisper_base_q5',
    engine: 'whisper',
    sizeMb: 60,
    title: () => SettingsI18n.current.voiceWhisperBaseTitle,
    desc: () => SettingsI18n.current.voiceWhisperBaseDesc,
  },
  {
    id: 'vosk_ru',
    engine: 'vosk',
    sizeMb: 45,
    title: () => SettingsI18n.current.voiceVoskRuTitle,
    desc: () => SettingsI18n.current.voiceVoskRuDesc,
  },
  {
    id: 'vosk_en',
    engine: 'vosk',
    sizeMb: 40,
    title: () => SettingsI18n.current.voiceVoskEnTitle,
    desc: () => SettingsI18n.current.voiceVoskEnDesc,
  },
];

function isWebSpeechSupported(): boolean {
  if (typeof window === 'undefined') return false;
  const w = window as unknown as {
    SpeechRecognition?: unknown;
    webkitSpeechRecognition?: unknown;
  };
  return Boolean(w.SpeechRecognition || w.webkitSpeechRecognition);
}

export function createVoiceModuleSettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-mod-flat-screen' });
  let activeDialogCleanup: (() => void) | null = null;

  const openPrivacyWarningDialog = () => {
    activeDialogCleanup?.();
    const s = SettingsI18n.current;

    const overlay = el('div', { class: 'ag-overlay ag-overlay--dialog is-open' });
    const close = () => {
      document.removeEventListener('keydown', onKey);
      overlay.remove();
      activeDialogCleanup = null;
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        close();
      }
    };
    document.addEventListener('keydown', onKey);
    overlay.addEventListener('click', (e) => {
      if (e.target === overlay) close();
    });

    const dialog = el(
      'div',
      { class: 'ag-dialog ag-mod-dialog-center' },
      el('div', { class: 'ag-mod-warn-icon' }, icons.alert(24)),
      el(
        'div',
        { class: 'ag-mod-warn-badge-wrap' },
        el('span', { class: 'ag-mod-badge ag-mod-badge--danger' }, s.voiceNotRecommendedBadge.toUpperCase()),
      ),
      el('div', { class: 'ag-dialog-title' }, s.voicePrivacyTitle),
      el('div', { class: 'ag-dialog-msg', style: 'white-space:pre-line' }, s.voicePrivacyBody),
      el(
        'div',
        { class: 'ag-mod-dialog-actions' },
        el(
          'button',
          {
            type: 'button',
            class: 'ag-mod-btn ag-mod-btn--primary',
            onClick: close,
          },
          s.voicePrivacyKeepLocal,
        ),
        el(
          'button',
          {
            type: 'button',
            class: 'ag-mod-btn ag-mod-btn--danger',
            onClick: () => {
              close();
              settingsStore.set('voiceEngine', 'system');
            },
          },
          s.voicePrivacyAcceptAnyway,
        ),
      ),
    );

    overlay.appendChild(dialog);
    document.body.appendChild(overlay);
    activeDialogCleanup = close;
  };

  const render = () => {
    clear(root);
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;
    const dl = settingsStore.voiceDownloadState;

    // 1. Auto-transcribe toggle
    root.appendChild(
      createFlatToggleRow(
        s.voiceAutoTranscribe,
        s.voiceAutoTranscribeDesc,
        prefs.autoTranscribeVoice,
        () => settingsStore.toggle('autoTranscribeVoice'),
      ),
    );
    root.appendChild(el('div', { class: 'ag-mod-divider' }));

    // 2. Engine Radio Selector (Whisper Tiny, Whisper Base, Vosk Small, System Speech)
    root.appendChild(
      el(
        'div',
        { class: 'ag-mod-section-head' },
        el('span', { class: 'ag-mod-section-title' }, s.voiceEngineHeader),
      ),
    );

    const engineChoices: Array<{
      id: string;
      title: string;
      desc: string;
      badge: string;
      recommended: boolean;
      selected: boolean;
      onSelect: () => void;
    }> = [
      {
        id: 'whisper_tiny',
        title: s.voiceWhisperTinyTitle,
        desc: s.voiceWhisperDesc,
        badge: s.voiceRecommendedBadge,
        recommended: true,
        selected: prefs.voiceEngine === 'whisper' && prefs.voiceWhisperModel === 'whisper_tiny_q5',
        onSelect: () => {
          settingsStore.set('voiceWhisperModel', 'whisper_tiny_q5');
          settingsStore.set('voiceEngine', 'whisper');
        },
      },
      {
        id: 'whisper_base',
        title: s.voiceWhisperBaseTitle,
        desc: s.voiceWhisperBaseDesc,
        badge: s.voiceRecommendedBadge,
        recommended: true,
        selected: prefs.voiceEngine === 'whisper' && prefs.voiceWhisperModel === 'whisper_base_q5',
        onSelect: () => {
          settingsStore.set('voiceWhisperModel', 'whisper_base_q5');
          settingsStore.set('voiceEngine', 'whisper');
        },
      },
      {
        id: 'vosk',
        title: s.voiceVoskTitle,
        desc: s.voiceVoskDesc,
        badge: s.voiceRecommendedBadge,
        recommended: true,
        selected: prefs.voiceEngine === 'vosk',
        onSelect: () => settingsStore.set('voiceEngine', 'vosk'),
      },
      {
        id: 'system',
        title: s.voiceSystemTitle,
        desc: s.voiceSystemDesc,
        badge: s.voiceNotRecommendedBadge,
        recommended: false,
        selected: prefs.voiceEngine === 'system',
        onSelect: () => {
          if (prefs.voiceEngine !== 'system') {
            openPrivacyWarningDialog();
          }
        },
      },
    ];

    engineChoices.forEach((choice, idx) => {
      root.appendChild(
        el(
          'button',
          {
            type: 'button',
            class: 'ag-mod-row ag-mod-row--clickable',
            onClick: choice.onSelect,
          },
          el(
            'div',
            { class: 'ag-mod-row__col' },
            el(
              'div',
              { class: 'ag-mod-row__title-line' },
              el(
                'span',
                { class: `ag-mod-row__title${choice.selected ? ' is-selected' : ''}` },
                choice.title,
              ),
              el(
                'span',
                {
                  class: `ag-mod-badge ${
                    choice.recommended ? 'ag-mod-badge--accent' : 'ag-mod-badge--danger'
                  }`,
                },
                choice.badge,
              ),
            ),
            el('span', { class: 'ag-mod-row__desc' }, choice.desc),
          ),
          createRadioDot(choice.selected, !choice.recommended),
        ),
      );
      if (idx < engineChoices.length - 1) {
        root.appendChild(el('div', { class: 'ag-mod-divider--pad' }));
      }
    });

    root.appendChild(el('div', { class: 'ag-mod-divider' }));

    // 3. System API notice (when system engine is active) + Recognition language selector
    if (prefs.voiceEngine === 'system') {
      const supported = isWebSpeechSupported();
      root.appendChild(
        el(
          'div',
          { class: `ag-mod-banner${supported ? '' : ' is-danger'}` },
          supported ? s.voiceSystemSupported : s.voiceSystemUnsupported,
        ),
      );
    }

    root.appendChild(
      el(
        'div',
        { class: 'ag-mod-section-head' },
        el('span', { class: 'ag-mod-section-title' }, s.voiceRecognitionLangLabel),
      ),
    );

    if (prefs.voiceEngine === 'system') {
      const sysLangs: Array<{ code: VoiceSystemLanguageOption; label: string }> = [
        { code: 'ru-RU', label: s.langRu },
        { code: 'en-US', label: s.langEn },
      ];
      root.appendChild(
        el(
          'div',
          { class: 'ag-mod-chips-row' },
          ...sysLangs.map((opt) =>
            el(
              'button',
              {
                type: 'button',
                class: `ag-chip ${prefs.voiceSystemLang === opt.code ? 'is-active' : ''}`,
                onClick: () => settingsStore.set('voiceSystemLang', opt.code),
              },
              opt.label,
            ),
          ),
        ),
      );
    } else {
      const localLangs: Array<{ code: VoiceLanguageOption; label: string }> = [
        { code: 'auto', label: s.langAuto },
        { code: 'ru', label: s.langRu },
        { code: 'en', label: s.langEn },
      ];
      root.appendChild(
        el(
          'div',
          { class: 'ag-mod-chips-row' },
          ...localLangs.map((opt) =>
            el(
              'button',
              {
                type: 'button',
                class: `ag-chip ${prefs.voiceLanguage === opt.code ? 'is-active' : ''}`,
                onClick: () => settingsStore.set('voiceLanguage', opt.code),
              },
              opt.label,
            ),
          ),
        ),
      );
    }

    root.appendChild(el('div', { class: 'ag-mod-divider' }));

    // 4. Voice Language Packs & Models (Download / Cancel / Remove)
    root.appendChild(
      el(
        'div',
        { class: 'ag-mod-section-head' },
        el('span', { class: 'ag-mod-section-title' }, s.voiceModelsHeader),
        el(
          'span',
          { class: 'ag-mod-section-meta' },
          s.moduleMemoryLabel(formatModuleMb(settingsStore.installedVoiceMb, s.mbUnit)),
        ),
      ),
    );

    const orderedPacks = [...VOICE_PACK_META].sort((a, b) => {
      const aMatch = a.engine === prefs.voiceEngine ? 0 : 1;
      const bMatch = b.engine === prefs.voiceEngine ? 0 : 1;
      return aMatch - bMatch;
    });

    orderedPacks.forEach((pack, idx) => {
      const spec = VOICE_PACK_SPECS.find((sp) => sp.id === pack.id);
      const sizeMb = spec?.sizeMb ?? pack.sizeMb;
      const isInstalled = prefs.installedVoicePacks.includes(pack.id);
      const isDownloading = dl.downloadingId === pack.id;
      const isSelected =
        (prefs.voiceEngine === 'whisper' && prefs.voiceWhisperModel === pack.id) ||
        (prefs.voiceEngine === 'vosk' && prefs.voiceVoskPack === pack.id);

      const statusText = isDownloading
        ? s.downloadingProgress(dl.progress)
        : isInstalled
          ? s.installedLabel(formatModuleMb(sizeMb, s.mbUnit))
          : pack.desc();

      const statusClass = isInstalled ? 'ag-mod-row__status is-accent' : 'ag-mod-row__status';

      const actionBtn = isDownloading
        ? el(
            'button',
            {
              type: 'button',
              class: 'ag-mod-btn ag-mod-btn--secondary',
              onClick: (e: Event) => {
                e.stopPropagation();
                settingsStore.cancelVoiceDownload();
              },
            },
            s.cancelBtnShort,
          )
        : isInstalled
          ? el(
              'button',
              {
                type: 'button',
                class: 'ag-mod-btn ag-mod-btn--danger',
                onClick: (e: Event) => {
                  e.stopPropagation();
                  settingsStore.removeVoicePack(pack.id);
                },
              },
              icons.trash(14),
              s.removeBtn,
            )
          : el(
              'button',
              {
                type: 'button',
                class: 'ag-mod-btn ag-mod-btn--primary',
                disabled: Boolean(dl.downloadingId),
                onClick: (e: Event) => {
                  e.stopPropagation();
                  settingsStore.downloadVoicePack(pack.id);
                },
              },
              s.downloadBtn,
            );

      root.appendChild(
        el(
          'div',
          {
            class: 'ag-mod-row ag-mod-row--clickable',
            onClick: () => {
              if (pack.engine === 'whisper' && (pack.id === 'whisper_tiny_q5' || pack.id === 'whisper_base_q5')) {
                settingsStore.set('voiceWhisperModel', pack.id);
                settingsStore.set('voiceEngine', 'whisper');
              } else if (pack.engine === 'vosk' && (pack.id === 'vosk_ru' || pack.id === 'vosk_en')) {
                settingsStore.set('voiceVoskPack', pack.id);
                settingsStore.set('voiceEngine', 'vosk');
              }
            },
          },
          el(
            'div',
            { class: 'ag-mod-row__col' },
            el(
              'div',
              { class: 'ag-mod-row__title-line' },
              el(
                'span',
                { class: `ag-mod-row__title${isSelected ? ' is-selected' : ''}` },
                pack.title(),
              ),
              isSelected
                ? el('span', { class: 'ag-mod-badge ag-mod-badge--accent' }, s.activeBadge)
                : null,
            ),
            el('span', { class: statusClass }, statusText),
          ),
          el('div', { class: 'ag-mod-row__actions' }, actionBtn),
        ),
      );

      if (isDownloading) {
        root.appendChild(createProgressBar(dl.progress));
      }

      if (idx < orderedPacks.length - 1) {
        root.appendChild(el('div', { class: 'ag-mod-divider--pad' }));
      }
    });

    root.appendChild(el('div', { class: 'ag-mod-divider' }));
    root.appendChild(el('div', { class: 'ag-mod-note' }, s.voicePrivacyNote));
  };

  const u1 = settingsStore.subscribe(render);
  const u2 = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    activeDialogCleanup?.();
    u1();
    u2();
  };

  return root;
}
