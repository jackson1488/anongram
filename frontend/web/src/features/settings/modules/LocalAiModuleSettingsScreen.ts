import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { showToast } from '../../../core/ui/toast';
import { SettingsI18n } from '../i18n';
import { LOCAL_AI_MODEL_SPECS, settingsStore, type LocalAiModelSpec } from '../settingsStore';
import { createProgressBar, formatModuleMb } from './ModulesSettingsScreen';
import './modules.css';

type AiCategoryFilter = 'all' | 'qwen' | 'llama' | 'gemma' | 'deepseek';

export function createLocalAiModuleSettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-mod-flat-screen' });

  let selectedCategory: AiCategoryFilter = 'all';

  const searchInput = el('input', {
    type: 'text',
    onInput: () => renderBody(),
  }) as HTMLInputElement;
  const searchWrap = el(
    'div',
    { class: 'ag-mod-search-wrap' },
    el('div', { class: 'ag-search-pill' }, icons.search(16), searchInput),
  );
  const categoriesWrap = el('div', { class: 'ag-mod-chips-row' });
  const bodyWrap = el('div');

  root.append(searchWrap, categoriesWrap, el('div', { class: 'ag-mod-divider' }), bodyWrap);

  const renderCategories = () => {
    const s = SettingsI18n.current;
    clear(categoriesWrap);
    const cats: Array<{ id: AiCategoryFilter; label: string }> = [
      { id: 'all', label: s.localAiCategoryAll },
      { id: 'qwen', label: 'Qwen' },
      { id: 'llama', label: 'Llama' },
      { id: 'gemma', label: 'Gemma' },
      { id: 'deepseek', label: 'DeepSeek' },
    ];
    cats.forEach((cat) => {
      categoriesWrap.appendChild(
        el(
          'button',
          {
            type: 'button',
            class: `ag-chip ${selectedCategory === cat.id ? 'is-active' : ''}`,
            onClick: () => {
              selectedCategory = cat.id;
              renderCategories();
              renderBody();
            },
          },
          cat.label,
        ),
      );
    });
  };

  const renderBody = () => {
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;
    const dl = settingsStore.aiDownloadState;
    searchInput.placeholder = s.localAiSearchPlaceholder;

    clear(bodyWrap);

    const installedSpecs = LOCAL_AI_MODEL_SPECS.filter((m) =>
      prefs.installedAiModels.includes(m.id),
    );

    // 1. Installed GGUF models
    if (installedSpecs.length > 0) {
      bodyWrap.appendChild(
        el(
          'div',
          { class: 'ag-mod-section-head' },
          el('span', { class: 'ag-mod-section-title' }, s.localAiInstalledHeader),
          el(
            'span',
            { class: 'ag-mod-section-meta' },
            s.moduleMemoryLabel(formatModuleMb(settingsStore.installedAiMb, s.mbUnit)),
          ),
        ),
      );

      installedSpecs.forEach((model, idx) => {
        const isActive = prefs.activeAiModelId === model.id;
        bodyWrap.appendChild(
          el(
            'div',
            {
              class: 'ag-mod-row ag-mod-row--clickable',
              onClick: () => {
                settingsStore.selectAiModel(model.id);
                showToast(s.localAiSelectedToast);
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
                  { class: `ag-mod-row__title${isActive ? ' is-selected' : ''}` },
                  model.displayName,
                ),
                isActive
                  ? el('span', { class: 'ag-mod-badge ag-mod-badge--accent' }, s.activeBadge)
                  : null,
              ),
              el(
                'span',
                { class: 'ag-mod-row__desc' },
                `${formatModuleMb(model.sizeMb, s.mbUnit)} · ${model.ramHint}`,
              ),
            ),
            el(
              'div',
              { class: 'ag-mod-row__actions' },
              isActive
                ? el('span', { style: 'color:var(--ag-accent);display:inline-flex' }, icons.check(18))
                : el(
                    'button',
                    {
                      type: 'button',
                      class: 'ag-mod-btn ag-mod-btn--ghost',
                      onClick: (e: Event) => {
                        e.stopPropagation();
                        settingsStore.selectAiModel(model.id);
                        showToast(s.localAiSelectedToast);
                      },
                    },
                    s.selectBtn,
                  ),
              el(
                'button',
                {
                  type: 'button',
                  class: 'ag-mod-btn ag-mod-btn--danger',
                  onClick: (e: Event) => {
                    e.stopPropagation();
                    settingsStore.removeAiModel(model.id);
                  },
                },
                icons.trash(14),
                s.removeBtn,
              ),
            ),
          ),
        );
        if (idx < installedSpecs.length - 1) {
          bodyWrap.appendChild(el('div', { class: 'ag-mod-divider--pad' }));
        }
      });

      bodyWrap.appendChild(el('div', { class: 'ag-mod-divider' }));
    }

    // 2. Local GGUF Catalog (Qwen 2.5 1.5B, Llama 3.2 1B, Gemma 2 2B, ...)
    bodyWrap.appendChild(
      el(
        'div',
        { class: 'ag-mod-section-head' },
        el('span', { class: 'ag-mod-section-title' }, s.localAiCatalogHeader),
      ),
    );

    const q = searchInput.value.trim().toLowerCase();
    const filtered: LocalAiModelSpec[] = LOCAL_AI_MODEL_SPECS.filter((m) => {
      const catOk = selectedCategory === 'all' || m.category === selectedCategory;
      const qOk =
        !q ||
        m.displayName.toLowerCase().includes(q) ||
        m.repoId.toLowerCase().includes(q) ||
        m.filename.toLowerCase().includes(q);
      return catOk && qOk;
    });

    if (filtered.length === 0) {
      bodyWrap.appendChild(el('div', { class: 'ag-mod-note' }, i18nStore.t.chats.emptySearch));
    } else {
      filtered.forEach((model, idx) => {
        const isInstalled = prefs.installedAiModels.includes(model.id);
        const isActive = prefs.activeAiModelId === model.id;
        const isDownloading = dl.downloadingId === model.id;

        const subtitle = isDownloading
          ? `${s.downloadingProgress(dl.progress)} • ${formatModuleMb(model.sizeMb, s.mbUnit)}`
          : `${formatModuleMb(model.sizeMb, s.mbUnit)} · ${model.ramHint} · ${model.filename}`;

        const actionEl = isDownloading
          ? el(
              'button',
              {
                type: 'button',
                class: 'ag-mod-btn ag-mod-btn--secondary',
                onClick: () => settingsStore.cancelAiDownload(),
              },
              s.cancelBtnShort,
            )
          : isInstalled
            ? isActive
              ? el('span', { class: 'ag-mod-badge ag-mod-badge--accent' }, s.activeBadge)
              : el(
                  'button',
                  {
                    type: 'button',
                    class: 'ag-mod-btn ag-mod-btn--ghost',
                    onClick: () => {
                      settingsStore.selectAiModel(model.id);
                      showToast(s.localAiSelectedToast);
                    },
                  },
                  s.selectBtn,
                )
            : el(
                'button',
                {
                  type: 'button',
                  class: 'ag-mod-btn ag-mod-btn--primary',
                  disabled: Boolean(dl.downloadingId),
                  onClick: () => {
                    settingsStore.downloadAiModel(model.id);
                  },
                },
                s.downloadBtn,
              );

        bodyWrap.appendChild(
          el(
            'div',
            { class: 'ag-mod-row' },
            el(
              'div',
              { class: 'ag-mod-row__col' },
              el('span', { class: 'ag-mod-row__title' }, model.displayName),
              el(
                'span',
                { class: `ag-mod-row__status${isInstalled ? ' is-accent' : ''}` },
                subtitle,
              ),
            ),
            el('div', { class: 'ag-mod-row__actions' }, actionEl),
          ),
        );

        if (isDownloading) {
          bodyWrap.appendChild(createProgressBar(dl.progress));
        }

        if (idx < filtered.length - 1) {
          bodyWrap.appendChild(el('div', { class: 'ag-mod-divider--pad' }));
        }
      });
    }

    bodyWrap.appendChild(el('div', { class: 'ag-mod-divider' }));
    bodyWrap.appendChild(el('div', { class: 'ag-mod-note' }, s.localAiPrivacyNote));
  };

  const renderAll = () => {
    renderCategories();
    renderBody();
  };

  const u1 = settingsStore.subscribe(renderBody);
  const u2 = i18nStore.subscribe(renderAll);
  renderAll();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
  };

  return root;
}
