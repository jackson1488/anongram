import { kvStore } from '../../../../core/storage/kvStore';
import type { AutoDeleteConfig } from '../model/UserProfileModels';

const KEY_BLOCKED_IDS = 'user_profile.blocked_ids.v1';
const KEY_MUTED_IDS = 'user_profile.muted_ids.v1';
const KEY_AUTO_DELETE = 'user_profile.auto_delete.v1';

type StoreListener = () => void;

class BlockedUsersStoreImpl {
  private blockedIds: Set<string>;
  private mutedIds: Set<string>;
  private autoDeleteMap: Record<string, AutoDeleteConfig>;
  private listeners = new Set<StoreListener>();

  constructor() {
    this.blockedIds = new Set(kvStore.getJson<string[]>(KEY_BLOCKED_IDS, []));
    this.mutedIds = new Set(kvStore.getJson<string[]>(KEY_MUTED_IDS, []));
    this.autoDeleteMap = kvStore.getJson<Record<string, AutoDeleteConfig>>(KEY_AUTO_DELETE, {});
  }

  getBlockedIds(): ReadonlySet<string> {
    return this.blockedIds;
  }

  isBlocked(peerId: string): boolean {
    return this.blockedIds.has(peerId);
  }

  block(peerId: string): void {
    if (this.blockedIds.has(peerId)) return;
    this.blockedIds.add(peerId);
    this.persistBlocked();
    this.emit();
  }

  unblock(peerId: string): void {
    if (!this.blockedIds.has(peerId)) return;
    this.blockedIds.delete(peerId);
    this.persistBlocked();
    this.emit();
  }

  toggle(peerId: string): boolean {
    const willBlock = !this.blockedIds.has(peerId);
    if (willBlock) {
      this.block(peerId);
    } else {
      this.unblock(peerId);
    }
    return willBlock;
  }

  isMuted(chatId: string): boolean {
    return this.mutedIds.has(chatId);
  }

  setMuted(chatId: string, muted: boolean): void {
    const has = this.mutedIds.has(chatId);
    if (has === muted) return;
    if (muted) this.mutedIds.add(chatId);
    else this.mutedIds.delete(chatId);
    kvStore.setJson(KEY_MUTED_IDS, Array.from(this.mutedIds));
    this.emit();
  }

  toggleMute(chatId: string): boolean {
    const next = !this.mutedIds.has(chatId);
    this.setMuted(chatId, next);
    return next;
  }

  getAutoDelete(chatId: string): AutoDeleteConfig {
    return this.autoDeleteMap[chatId] ?? { preset: 'off', seconds: 0 };
  }

  setAutoDelete(chatId: string, config: AutoDeleteConfig): void {
    this.autoDeleteMap = { ...this.autoDeleteMap, [chatId]: config };
    kvStore.setJson(KEY_AUTO_DELETE, this.autoDeleteMap);
    this.emit();
  }

  subscribe(fn: StoreListener): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private persistBlocked(): void {
    kvStore.setJson(KEY_BLOCKED_IDS, Array.from(this.blockedIds));
  }

  private emit(): void {
    for (const fn of this.listeners) fn();
  }
}

export const blockedUsersStore = new BlockedUsersStoreImpl();
