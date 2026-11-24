import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import {
  localTranslator,
  type TranslationPackInfo,
} from '../../chats/tools/translate/LocalMessageTranslator';
import { SettingsI18n } from '../i18n';
import { settingsStore, type TargetTranslateLanguageOption } from '../settingsStore';
import { createFlatToggleRow, createProgressBar, formatModuleMb } from './ModulesSettingsScreen';
import './modules.css';

export function createTranslationModuleSettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-mod-flat-screen' });

  const togglesSection = el('div');
  const targetSection = el('div');
  const packsHeader = el('div');
  const searchInput = el('input', {
    type: 'text',
    onInput: () => renderPacksList(),
  }) as HTMLInputElement;
  const searchWrap = el(
    'div',
    { class: 'ag-mod-search-wrap' },
    el('div', { class: 'ag-search-pill' }, icons.search(16), searchInput),
  );
  const packsList = el('div');
  const privacyNote = el('div', { class: 'ag-mod-note' });

  root.append(
    togglesSection,
    el('div', { class: 'ag-mod-divider' }),
    targetSection,
    el('div', { class: 'ag-mod-divider' }),
    packsHeader,
    searchWrap,
    el('div', { class: 'ag-mod-divider--pad' }),
    packsList,
    el('div', { class: 'ag-mod-divider' }),
    privacyNote,
  );

  const renderHeaderAndSettings = () => {
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;

    clear(togglesSection);
    togglesSection.append(
      createFlatToggleRow(
        s.showTranslateButton,
        s.showTranslateButtonDesc,
        prefs.showTranslateButton,
        () => settingsStore.toggle('showTranslateButton'),
      ),
      el('div', { class: 'ag-mod-divider--pad' }),
      createFlatToggleRow(
        s.autoTranslateIncoming,
        s.autoTranslateIncomingDesc,
        prefs.autoTranslateIncoming,
        () => settingsStore.toggle('autoTranslateIncoming'),
      ),
    );

    clear(targetSection);
    targetSection.appendChild(
      el(
        'div',
        { class: 'ag-mod-section-head' },
        el('span', { class: 'ag-mod-section-title' }, s.translationTargetLangHeader),
      ),
    );

    const targetOptions: Array<{ code: TargetTranslateLanguageOption; label: string }> = [
      { code: 'auto', label: s.langAuto },
      { code: 'ru', label: s.langRu },
      { code: 'en', label: s.langEn },
    ];

    targetOptions.forEach((opt, idx) => {
      const selected = prefs.targetTranslateLanguage === opt.code;
      targetSection.appendChild(
        el(
          'button',
          {
            type: 'button',
            class: 'ag-mod-row ag-mod-row--clickable',
            onClick: () => {
              settingsStore.set('targetTranslateLanguage', opt.code);
              localTranslator.setTargetLang(opt.code);
            },
          },
          el(
            'span',
            { class: `ag-mod-row__title${selected ? ' is-selected' : ''}` },
            opt.label,
          ),
          selected
            ? el('span', { style: 'color:var(--ag-accent);display:inline-flex' }, icons.check(18))
            : null,
        ),
      );
      if (idx < targetOptions.length - 1) {
        targetSection.appendChild(el('div', { class: 'ag-mod-divider--pad' }));
      }
    });

    searchInput.placeholder = s.translationSearchPlaceholder;
    privacyNote.textContent = s.translationPrivacyNote;
  };

  const renderPacksList = () => {
    const s = SettingsI18n.current;
    const isRu = i18nStore.language !== 'en';
    const packs: TranslationPackInfo[] = localTranslator.getPacks();
    const downloadedSet = new Set(localTranslator.getDownloadedPacks());
    const downloadingCode = localTranslator.getDownloadingCode();
    const rawProgress = localTranslator.getDownloadProgress();
    const progressPct = rawProgress <= 1 ? Math.round(rawProgress * 100) : Math.round(rawProgress);
    const totalMb = localTranslator.getTotalDownloadedMb();

    clear(packsHeader);
    packsHeader.appendChild(
      el(
        'div',
        { class: 'ag-mod-section-head' },
        el(
          'span',
          { class: 'ag-mod-section-title' },
          `${s.translationPacksHeader} (${packs.length})`,
        ),
        el(
          'span',
          { class: 'ag-mod-section-meta' },
          s.moduleMemoryLabel(formatModuleMb(totalMb, s.mbUnit)),
        ),
      ),
    );

    const q = searchInput.value.trim().toLowerCase();
    const filtered = packs
      .filter((p) => {
        if (!q) return true;
        return (
          p.nameRu.toLowerCase().includes(q) ||
          p.nameEn.toLowerCase().includes(q) ||
          p.code.toLowerCase().includes(q)
        );
      })
      .sort((a, b) => {
        const aDl = downloadedSet.has(a.code) || a.downloaded ? 1 : 0;
        const bDl = downloadedSet.has(b.code) || b.downloaded ? 1 : 0;
        return bDl - aDl;
      });

    clear(packsList);

    if (filtered.length === 0) {
      packsList.appendChild(
        el('div', { class: 'ag-mod-note' }, i18nStore.t.chats.emptySearch),
      );
      return;
    }

    filtered.forEach((pack, idx) => {
      const isDownloaded = downloadedSet.has(pack.code) || pack.downloaded;
      const isDownloading = downloadingCode === pack.code;
      const sizeStr = formatModuleMb(pack.sizeMb, s.mbUnit);

      const subtitle = isDownloading
        ? `${s.downloadingProgress(progressPct)} • ~${sizeStr}`
        : isDownloaded
          ? s.installedLabel(sizeStr)
          : `${pack.code.toUpperCase()} • ~${sizeStr}`;

      const subtitleClass = isDownloaded
        ? 'ag-mod-row__status is-accent'
        : 'ag-mod-row__status';

      const actionBtn = isDownloading
        ? el(
            'button',
            {
              type: 'button',
              class: 'ag-mod-btn ag-mod-btn--secondary',
              onClick: () => localTranslator.removePack(pack.code),
            },
            s.cancelBtnShort,
          )
        : isDownloaded
          ? el(
              'button',
              {
                type: 'button',
                class: 'ag-mod-btn ag-mod-btn--danger',
                onClick: () => localTranslator.removePack(pack.code),
              },
              icons.trash(14),
              s.removeBtn,
            )
          : el(
              'button',
              {
                type: 'button',
                class: 'ag-mod-btn ag-mod-btn--primary',
                disabled: Boolean(downloadingCode && downloadingCode !== pack.code),
                onClick: () => {
                  void localTranslator.downloadPack(pack.code);
                },
              },
              s.downloadBtn,
            );

      packsList.appendChild(
        el(
          'div',
          { class: 'ag-mod-row' },
          el(
            'div',
            { class: 'ag-mod-row__col' },
            el('span', { class: 'ag-mod-row__title' }, isRu ? pack.nameRu : pack.nameEn),
            el('span', { class: subtitleClass }, subtitle),
          ),
          el('div', { class: 'ag-mod-row__actions' }, actionBtn),
        ),
      );

      if (isDownloading) {
        packsList.appendChild(createProgressBar(progressPct));
      }

      if (idx < filtered.length - 1) {
        packsList.appendChild(el('div', { class: 'ag-mod-divider--pad' }));
      }
    });
  };

  const renderAll = () => {
    renderHeaderAndSettings();
    renderPacksList();
  };

  const u1 = settingsStore.subscribe(renderAll);
  const u2 = i18nStore.subscribe(renderAll);
  const u3 = localTranslator.subscribe(renderPacksList);
  renderAll();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
    u3();
  };

  return root;
}
