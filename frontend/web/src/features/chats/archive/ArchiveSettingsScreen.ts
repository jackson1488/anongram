import { i18nStore } from '../../../core/i18n/i18nStore';
import { clear, el } from '../../../core/ui/dom';
import { allIcons as icons } from '../../../core/ui/icons';
import { iconButton, screenSurface, switchControl } from '../../../core/ui/components';
import { archiveSettingsStore } from './ArchiveSettingsStore';
import { archiveT } from './i18n/ArchiveI18n';
import './archive.css';

function createSettingCard(
  header: string,
  label: string,
  initial: boolean,
  description: string,
  onChange: (enabled: boolean) => void,
): HTMLElement {
  const sw = switchControl(initial, onChange);
  const card = el(
    'div',
    {
      class: 'ag-archive-setting-card',
      onclick: (e: Event) => {
        if ((e.target as HTMLElement).closest('.ag-switch')) return;
        const next = !sw.get();
        sw.set(next);
        onChange(next);
      },
    },
    el('div', { class: 'ag-archive-setting-card__header' }, header),
    el(
      'div',
      { class: 'ag-archive-setting-card__row' },
      el('div', { class: 'ag-archive-setting-card__label' }, label),
      sw.node,
    ),
  );

  return el(
    'div',
    { class: 'ag-archive-setting-group' },
    card,
    el('div', { class: 'ag-archive-setting-desc' }, description),
  );
}

export function createArchiveSettingsScreen(onBack: () => void): HTMLElement {
  const root = screenSurface();

  const render = () => {
    clear(root);
    const s = archiveT();
    const state = archiveSettingsStore.getState();

    const header = el(
      'div',
      { class: 'ag-archive-header' },
      iconButton(icons.arrowLeft(20), onBack, s.back),
      el('div', { class: 'ag-archive-header__title' }, s.archiveSettingsTitle),
    );

    const body = el(
      'div',
      { class: 'ag-archive-settings-body' },
      createSettingCard(
        s.chatsWithNotificationsHeader,
        s.alwaysKeepArchivedTitle,
        state.keepArchivedMuted,
        s.alwaysKeepArchivedDesc,
        (v) => archiveSettingsStore.setKeepArchivedMuted(v),
      ),
      createSettingCard(
        s.newChatsFromStrangersHeader,
        s.autoArchiveTitle,
        state.autoArchiveNonContacts,
        s.autoArchiveDesc,
        (v) => archiveSettingsStore.setAutoArchiveNonContacts(v),
      ),
      createSettingCard(
        s.mainListVisibilityHeader,
        s.showArchiveInMainListTitle,
        state.showArchiveInMainList,
        s.showArchiveInMainListDesc,
        (v) => archiveSettingsStore.setShowArchiveInMainList(v),
      ),
    );

    root.append(header, body);
  };

  render();
  const unsubStore = archiveSettingsStore.subscribe(render);
  const unsubI18n = i18nStore.subscribe(render);

  const observer = new MutationObserver(() => {
    if (!root.isConnected && document.body) {
      // keep alive until explicitly detached from DOM after having been mounted
    }
  });
  void observer;

  (root as HTMLElement & { destroy?: () => void }).destroy = () => {
    unsubStore();
    unsubI18n();
  };

  return root;
}
