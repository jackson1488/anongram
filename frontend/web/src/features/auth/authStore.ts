import { generate12Words, normalizeSeed, validateMnemonic } from '../../core/crypto/bip39';
import { deriveIdentity, type UserIdentity } from '../../core/crypto/identity';
import { kvStore } from '../../core/storage/kvStore';

const KEY_IDENTITY = 'auth.identity';

export type AuthStatus = 'loading' | 'unauthenticated' | 'authenticated';

export interface AnonIdentity extends UserIdentity {
  avatarUrl?: string;
  bio?: string;
}

export interface ProfilePatch {
  displayName?: string;
  username?: string;
  bio?: string;
  avatarUrl?: string;
}

type AuthListener = (status: AuthStatus, identity: AnonIdentity | null) => void;

class AuthStore {
  private status: AuthStatus = 'loading';
  private currentIdentity: AnonIdentity | null = null;
  private listeners = new Set<AuthListener>();

  constructor() {
    const saved = kvStore.getJson<AnonIdentity | null>(KEY_IDENTITY, null);
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

  get identity(): AnonIdentity | null {
    return this.currentIdentity;
  }

  async generateDraftIdentity(
    displayName: string,
    avatarIndex = 0,
  ): Promise<AnonIdentity> {
    const seed = await generate12Words();
    return deriveIdentity(seed, displayName, '', avatarIndex);
  }

  confirmIdentity(identity: AnonIdentity): void {
    this.currentIdentity = identity;
    this.status = 'authenticated';
    kvStore.setJson(KEY_IDENTITY, identity);
    this.emit();
  }

  async restoreFromSeed(rawSeed: string): Promise<AnonIdentity | null> {
    const words = normalizeSeed(rawSeed);
    if (words.length !== 12 || !(await validateMnemonic(words))) return null;
    const identity = await deriveIdentity(words.slice(0, 12), 'GhostWalker', '', 0);
    this.confirmIdentity(identity);
    return identity;
  }

  updateProfile(patchOrName: ProfilePatch | string, legacyUsername?: string): void {
    if (!this.currentIdentity) return;
    const patch: ProfilePatch =
      typeof patchOrName === 'string'
        ? { displayName: patchOrName, username: legacyUsername }
        : patchOrName;

    const nextDisplayName =
      patch.displayName !== undefined
        ? patch.displayName.trim() || this.currentIdentity.displayName
        : this.currentIdentity.displayName;

    let nextUsername = this.currentIdentity.username;
    if (patch.username !== undefined) {
      const cleanUser = patch.username.trim().replace(/^@+/, '');
      nextUsername = cleanUser ? `@${cleanUser}` : this.currentIdentity.username;
    }

    const nextBio =
      patch.bio !== undefined ? patch.bio.trim() : this.currentIdentity.bio;

    const nextAvatarUrl =
      patch.avatarUrl !== undefined ? patch.avatarUrl : this.currentIdentity.avatarUrl;

    this.currentIdentity = {
      ...this.currentIdentity,
      displayName: nextDisplayName,
      username: nextUsername,
      bio: nextBio,
      avatarUrl: nextAvatarUrl,
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
