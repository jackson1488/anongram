import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { showToast } from '../../../core/ui/toast';
import { createSettingsToggleGroup } from '../common/SettingsToggleRow';
import { SettingsI18n } from '../i18n';
import { settingsStore } from '../settingsStore';

function measureLocalStorageMb(): number {
  try {
    let bytes = 0;
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i) ?? '';
      const v = localStorage.getItem(k) ?? '';
      bytes += (k.length + v.length) * 2;
    }
    return Math.max(0.2, Number((bytes / (1024 * 1024)).toFixed(1)));
  } catch {
    return 1.2;
  }
}

export function createDataStorageSettingsScreen(): HTMLElement {
  const root = el('div', { class: 'ag-settings-content' });

  const render = () => {
    clear(root);
    const s = SettingsI18n.current;
    const prefs = settingsStore.all;

    const mediaMb = prefs.mediaCacheMb;
    const messagesMb = measureLocalStorageMb();
    const modelsMb = prefs.localAiModule ? 48.0 : prefs.voiceModule ? 12.5 : 1.5;
    const appCacheMb = mediaMb > 0 ? Number((mediaMb * 0.22).toFixed(1)) : 0;
    const totalMb = Number((mediaMb + messagesMb + modelsMb + appCacheMb).toFixed(1));
    const denom = Math.max(1, totalMb);

    const pct = (v: number) => `${Math.max(v > 0 ? 4 : 0, (v / denom) * 100).toFixed(1)}%`;

    const legendRow = (dotClass: string, label: string, valMb: number) =>
      el(
        'div',
        { class: 'ag-storage-item' },
        el('span', { class: `ag-storage-dot ${dotClass}` }),
        el('span', { class: 'ag-storage-item__label' }, label),
        el('span', { class: 'ag-storage-item__val' }, `${valMb.toFixed(1)} MB`),
      );

    // 1. Storage Usage Breakdown + Clear Cache
    root.appendChild(
      el(
        'div',
        { class: 'ag-card ag-settings-card' },
        el(
          'div',
          { style: 'display:flex;justify-content:space-between;align-items:center' },
          el('div', { class: 'ag-section-label' }, s.storageUsageTitle),
          el('strong', { style: 'font-size:14px' }, `${s.storageTotal}: ${totalMb.toFixed(1)} MB`),
        ),
        el(
          'div',
          { class: 'ag-storage-bar' },
          el('div', { class: 'ag-storage-seg ag-storage-seg--media', style: `width:${pct(mediaMb)}` }),
          el('div', { class: 'ag-storage-seg ag-storage-seg--messages', style: `width:${pct(messagesMb)}` }),
          el('div', { class: 'ag-storage-seg ag-storage-seg--models', style: `width:${pct(modelsMb)}` }),
          el('div', { class: 'ag-storage-seg ag-storage-seg--cache', style: `width:${pct(appCacheMb)}` }),
        ),
        el(
          'div',
          { class: 'ag-storage-legend' },
          legendRow('ag-storage-seg--media', s.storageMedia, mediaMb),
          legendRow('ag-storage-seg--messages', s.storageMessages, messagesMb),
          legendRow('ag-storage-seg--models', s.storageModels, modelsMb),
          legendRow('ag-storage-seg--cache', s.storageFree, appCacheMb),
        ),
        el(
          'button',
          {
            class: 'ag-pill-btn ag-pill-btn--primary',
            onClick: () => {
              settingsStore.clearMediaCache();
              showToast(s.cacheClearedToast);
            },
          },
          `${s.clearCacheBtn} (${(mediaMb + appCacheMb).toFixed(1)} MB)`,
        ),
      ),
    );

    // 2. Auto-download toggles
    root.appendChild(
      createSettingsToggleGroup([
        [s.autoDownloadMedia, 'autoDownloadMedia'],
        [s.autoDownloadPhotos, 'autoDownloadPhotos'],
        [s.autoDownloadVoice, 'autoDownloadVoice'],
      ]),
    );
  };

  const u1 = settingsStore.subscribe(render);
  const u2 = i18nStore.subscribe(render);
  render();

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    u1();
    u2();
  };

  return root;
}
