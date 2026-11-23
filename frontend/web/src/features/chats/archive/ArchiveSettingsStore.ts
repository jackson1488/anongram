import { kvStore } from '../../../core/storage/kvStore';

const KEY_KEEP_ARCHIVED_MUTED = 'chats.archive.keepMuted.v1';
const KEY_AUTO_ARCHIVE_NON_CONTACTS = 'chats.archive.autoNonContacts.v1';
const KEY_SHOW_IN_MAIN_LIST = 'chats.archive.showInMainList.v1';

export interface ArchiveSettingsState {
  keepArchivedMuted: boolean;
  autoArchiveNonContacts: boolean;
  showArchiveInMainList: boolean;
}

type ArchiveSettingsListener = (state: ArchiveSettingsState) => void;

class ArchiveSettingsStore {
  private state: ArchiveSettingsState;
  private listeners = new Set<ArchiveSettingsListener>();

  constructor() {
    this.state = {
      keepArchivedMuted: kvStore.getBool(KEY_KEEP_ARCHIVED_MUTED, false),
      autoArchiveNonContacts: kvStore.getBool(KEY_AUTO_ARCHIVE_NON_CONTACTS, false),
      showArchiveInMainList: kvStore.getBool(KEY_SHOW_IN_MAIN_LIST, true),
    };
  }

  getState(): ArchiveSettingsState {
    return { ...this.state };
  }

  setKeepArchivedMuted(enabled: boolean): void {
    if (this.state.keepArchivedMuted === enabled) return;
    this.state = { ...this.state, keepArchivedMuted: enabled };
    kvStore.setBool(KEY_KEEP_ARCHIVED_MUTED, enabled);
    this.emit();
  }

  setAutoArchiveNonContacts(enabled: boolean): void {
    if (this.state.autoArchiveNonContacts === enabled) return;
    this.state = { ...this.state, autoArchiveNonContacts: enabled };
    kvStore.setBool(KEY_AUTO_ARCHIVE_NON_CONTACTS, enabled);
    this.emit();
  }

  setShowArchiveInMainList(enabled: boolean): void {
    if (this.state.showArchiveInMainList === enabled) return;
    this.state = { ...this.state, showArchiveInMainList: enabled };
    kvStore.setBool(KEY_SHOW_IN_MAIN_LIST, enabled);
    this.emit();
  }

  subscribe(fn: ArchiveSettingsListener): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private emit(): void {
    const snap = this.getState();
    for (const fn of this.listeners) fn(snap);
  }
}

export const archiveSettingsStore = new ArchiveSettingsStore();
