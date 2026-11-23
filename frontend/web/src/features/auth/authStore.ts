import { generate12Words, normalizeSeed, validateMnemonic } from '../../core/crypto/bip39';
import { deriveIdentity, type UserIdentity } from '../../core/crypto/identity';
import { kvStore } from '../../core/storage/kvStore';

const KEY_IDENTITY = 'auth.identity';

export type AuthStatus = 'loading' | 'unauthenticated' | 'authenticated';

type AuthListener = (status: AuthStatus, identity: UserIdentity | null) => void;

class AuthStore {
  private status: AuthStatus = 'loading';
  private currentIdentity: UserIdentity | null = null;
  private listeners = new Set<AuthListener>();

  constructor() {
    const saved = kvStore.getJson<UserIdentity | null>(KEY_IDENTITY, null);
    window.setTimeout(() => {
      if (saved && saved.publicId) {
        this.currentIdentity = saved;
        this.status = 'authenticated';
      } else {
        this.status = 'unauthenticated';
      }
      this.emit();
    }, 650);
  }

  get authStatus(): AuthStatus {
    return this.status;
  }

  get identity(): UserIdentity | null {
    return this.currentIdentity;
  }

  async generateDraftIdentity(
    displayName: string,
    avatarIndex = 0,
  ): Promise<UserIdentity> {
    const seed = await generate12Words();
    return deriveIdentity(seed, displayName, '', avatarIndex);
  }

  confirmIdentity(identity: UserIdentity): void {
    this.currentIdentity = identity;
    this.status = 'authenticated';
    kvStore.setJson(KEY_IDENTITY, identity);
    this.emit();
  }

  async restoreFromSeed(rawSeed: string): Promise<UserIdentity | null> {
    const words = normalizeSeed(rawSeed);
    if (words.length !== 12 || !(await validateMnemonic(words))) return null;
    const identity = await deriveIdentity(words.slice(0, 12), 'GhostWalker', '', 0);
    this.confirmIdentity(identity);
    return identity;
  }

  updateProfile(displayName: string, username: string): void {
    if (!this.currentIdentity) return;
    const cleanName = displayName.trim() || this.currentIdentity.displayName;
    const cleanUser = username.trim().replace(/^@+/, '');
    this.currentIdentity = {
      ...this.currentIdentity,
      displayName: cleanName,
      username: cleanUser ? `@${cleanUser}` : this.currentIdentity.username,
    };
    kvStore.setJson(KEY_IDENTITY, this.currentIdentity);
    this.emit();
  }

  logout(): void {
    this.currentIdentity = null;
    this.status = 'unauthenticated';
    kvStore.remove(KEY_IDENTITY);
    this.emit();
  }

  subscribe(fn: AuthListener): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private emit(): void {
    for (const fn of this.listeners) {
      fn(this.status, this.currentIdentity);
    }
  }
}

export const authStore = new AuthStore();
