/**
 * Browser History Back stack coordinator (`BackHandler` parity for mobile & desktop browsers).
 *
 * Keeps `window.history` entries synchronized with a logical stack of open UI layers
 * (tabs, mobile detail screens, UserProfile, overlays, context menus, selection mode, search, archive)
 * so hardware/gesture Back pops one layer at a time instead of exiting the web app.
 */

const STATE_KEY = '__agBack';
const BACK_SETTLE_TIMEOUT_MS = 250;

export interface BackHistoryEntry {
  /** Unique identifier for this layer (e.g. 'shell:mobile-chat', 'dom:user-profile'). */
  id: string;
  /** Optional semantic tag for debugging/telemetry (e.g. 'tab', 'overlay', 'menu'). */
  tag?: string;
  /** Invoked when browser Back (`popstate`) pops this layer. */
  onBack: () => void;
}

interface BackStatePayload {
  sessionId: string;
  depth: number;
  id?: string;
  tag?: string;
}

function isPlainRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function makeSessionId(): string {
  return `ag_${Date.now().toString(36)}_${Math.random().toString(36).slice(2, 8)}`;
}

/**
 * Coordinates a logical Back stack with `window.history.pushState` and `popstate`.
 *
 * Forward pushes (`pushState`) update synchronously when not awaiting an in-flight
 * `history.go(...)`, while shrinks (`history.go(-delta)`) coalesce in a microtask
 * so closing one layer and opening another in the same tick becomes a single
 * `replaceState` with zero async `popstate` races.
 */
export class BackHistory {
  private stack: BackHistoryEntry[] = [];
  private readonly sessionId: string;
  private knownBrowserDepth = 0;
  private lastCommittedId: string | undefined = undefined;
  private pendingBackTarget: number | null = null;
  private backFallbackTimer: number | null = null;
  private reconcileScheduled = false;
  private attached = false;

  constructor(autoAttach = true) {
    this.sessionId = makeSessionId();
    if (autoAttach && typeof window !== 'undefined') {
      this.attach();
    }
  }

  /** Attaches the `popstate` listener and stamps the current history entry as depth 0. */
  attach(): void {
    if (this.attached || typeof window === 'undefined') return;
    this.attached = true;
    this.knownBrowserDepth = 0;
    this.lastCommittedId = undefined;
    this.replaceBrowserState(0, undefined);
    window.addEventListener('popstate', this.onPopState);
  }

  /** Current number of active layers on the back stack. */
  get depth(): number {
    return this.stack.length;
  }

  /** Whether an entry with `id` is currently on the back stack. */
  has(id: string): boolean {
    return this.stack.some((entry) => entry.id === id);
  }

  /** Returns the topmost entry on the back stack, if any. */
  top(): BackHistoryEntry | undefined {
    return this.stack[this.stack.length - 1];
  }

  /**
   * Pushes a layer onto the top of the back stack (deduplicating any existing entry with the same `id`).
   * Returns a disposer that removes the layer programmatically when closed via UI.
   */
  push(entry: BackHistoryEntry): () => void {
    this.ensureAttached();
    this.stack = this.stack.filter((item) => item.id !== entry.id);
    this.stack.push(entry);
    if (
      this.stack.length > this.knownBrowserDepth &&
      !this.reconcileScheduled &&
      this.pendingBackTarget === null
    ) {
      this.reconcile();
    } else {
      this.scheduleReconcile();
    }
    return () => {
      this.remove(entry.id);
    };
  }

  /** Programmatically removes a layer by `id` (without invoking `onBack`) and syncs history. */
  remove(id: string): boolean {
    const idx = this.stack.findIndex((entry) => entry.id === id);
    if (idx === -1) return false;
    this.stack.splice(idx, 1);
    this.scheduleReconcile();
    return true;
  }

  /**
   * Declaratively reconciles the ordered back stack (bottom -> top) with the given entries.
   * Pushes are applied immediately when safe; pops/replacements coalesce in a microtask.
   */
  sync(entries: readonly BackHistoryEntry[]): void {
    this.ensureAttached();
    const deduped: BackHistoryEntry[] = [];
    const seen = new Set<string>();
    for (let i = entries.length - 1; i >= 0; i--) {
      const entry = entries[i];
      if (!seen.has(entry.id)) {
        seen.add(entry.id);
        deduped.push(entry);
      }
    }
    deduped.reverse();

    const prevTopId = this.stack[this.stack.length - 1]?.id;
    const nextTopId = deduped[deduped.length - 1]?.id;
    const lengthChanged = deduped.length !== this.stack.length;
    const contentChanged =
      lengthChanged ||
      prevTopId !== nextTopId ||
      deduped.some((entry, idx) => entry.id !== this.stack[idx]?.id);

    this.stack = deduped;

    if (
      this.stack.length > this.knownBrowserDepth &&
      !this.reconcileScheduled &&
      this.pendingBackTarget === null
    ) {
      this.reconcile();
    } else if (contentChanged || this.stack.length !== this.knownBrowserDepth) {
      this.scheduleReconcile();
    }
  }

  /** Clears all layers and returns browser history back to depth 0 without firing `onBack`. */
  clear(): void {
    this.stack = [];
    if (this.knownBrowserDepth > 0 || this.pendingBackTarget !== null) {
      this.scheduleReconcile();
    } else {
      this.lastCommittedId = undefined;
      if (this.attached) this.replaceBrowserState(0, undefined);
    }
  }

  /** Detaches the `popstate` listener and cancels any pending timers. */
  dispose(): void {
    this.clearBackFallback();
    this.reconcileScheduled = false;
    this.stack = [];
    if (this.attached && typeof window !== 'undefined') {
      window.removeEventListener('popstate', this.onPopState);
    }
    this.attached = false;
  }

  private ensureAttached(): void {
    if (!this.attached && typeof window !== 'undefined') {
      this.attach();
    }
  }

  private scheduleReconcile(): void {
    if (this.reconcileScheduled) return;
    this.reconcileScheduled = true;
    queueMicrotask(() => {
      this.reconcileScheduled = false;
      this.reconcile();
    });
  }

  private reconcile(): void {
    if (!this.attached || typeof window === 'undefined') return;
    if (this.pendingBackTarget !== null) return;

    const desiredDepth = this.stack.length;

    if (desiredDepth > this.knownBrowserDepth) {
      while (this.knownBrowserDepth < desiredDepth) {
        const nextDepth = this.knownBrowserDepth + 1;
        const entry = this.stack[nextDepth - 1];
        this.pushBrowserState(nextDepth, entry);
        this.knownBrowserDepth = nextDepth;
        this.lastCommittedId = entry?.id;
      }
      return;
    }

    if (desiredDepth === this.knownBrowserDepth) {
      const topEntry = this.stack[desiredDepth - 1];
      if (topEntry?.id !== this.lastCommittedId) {
        this.replaceBrowserState(desiredDepth, topEntry);
        this.lastCommittedId = topEntry?.id;
      }
      return;
    }

    // desiredDepth < knownBrowserDepth: step browser history back to match logical stack.
    const delta = desiredDepth - this.knownBrowserDepth;
    this.pendingBackTarget = desiredDepth;
    this.armBackFallback();
    try {
      window.history.go(delta);
    } catch {
      this.clearBackFallback();
      this.pendingBackTarget = null;
      this.knownBrowserDepth = desiredDepth;
      const topEntry = this.stack[desiredDepth - 1];
      this.lastCommittedId = topEntry?.id;
    }
  }

  private onPopState = (event: PopStateEvent): void => {
    const payload = this.readPayload(event.state);
    const stateDepth = payload ? Math.max(0, Math.floor(payload.depth)) : 0;
    this.knownBrowserDepth = stateDepth;
    this.lastCommittedId = payload?.id;

    if (this.pendingBackTarget !== null) {
      const expectedTarget = this.pendingBackTarget;
      this.clearBackFallback();
      this.pendingBackTarget = null;

      // If the user also pressed hardware Back while internal history.go was in flight,
      // honor the extra Back steps beyond expectedTarget.
      const extraUserPops = Math.max(0, Math.min(this.stack.length, expectedTarget - stateDepth));
      if (extraUserPops > 0) {
        this.popAndNotify(extraUserPops);
      }
      this.scheduleReconcile();
      return;
    }

    if (stateDepth < this.stack.length) {
      const pops = this.stack.length - stateDepth;
      this.popAndNotify(pops);
      this.scheduleReconcile();
    } else if (stateDepth > this.stack.length) {
      // Browser Forward landed on a closed overlay entry; snap back to actual stack depth.
      this.scheduleReconcile();
    }
  };

  private popAndNotify(count: number): void {
    const popped: BackHistoryEntry[] = [];
    for (let i = 0; i < count && this.stack.length > 0; i++) {
      const item = this.stack.pop();
      if (item) popped.push(item);
    }
    for (const item of popped) {
      try {
        item.onBack();
      } catch (err) {
        console.error('[BackHistory] onBack handler failed:', item.id, err);
      }
    }
  }

  private armBackFallback(): void {
    this.clearBackFallback();
    if (typeof window === 'undefined') return;
    this.backFallbackTimer = window.setTimeout(() => {
      this.backFallbackTimer = null;
      if (this.pendingBackTarget === null) return;
      this.pendingBackTarget = null;
      const actualDepth = this.readDepth(window.history.state);
      // Sync to whichever is smaller to prevent infinite history.go loops if browser refused navigation.
      this.knownBrowserDepth = Math.min(actualDepth, this.stack.length);
      const topEntry = this.stack[this.knownBrowserDepth - 1];
      this.lastCommittedId = topEntry?.id;
      if (this.stack.length > this.knownBrowserDepth) {
        this.scheduleReconcile();
      }
    }, BACK_SETTLE_TIMEOUT_MS);
  }

  private clearBackFallback(): void {
    if (this.backFallbackTimer !== null && typeof window !== 'undefined') {
      window.clearTimeout(this.backFallbackTimer);
      this.backFallbackTimer = null;
    }
  }

  private buildState(depth: number, entry?: BackHistoryEntry): Record<string, unknown> {
    const existing = typeof window !== 'undefined' ? window.history.state : null;
    const base: Record<string, unknown> = isPlainRecord(existing) ? { ...existing } : {};
    const payload: BackStatePayload = {
      sessionId: this.sessionId,
      depth,
      ...(entry ? { id: entry.id, ...(entry.tag ? { tag: entry.tag } : {}) } : {}),
    };
    base[STATE_KEY] = payload;
    return base;
  }

  private pushBrowserState(depth: number, entry?: BackHistoryEntry): void {
    try {
      window.history.pushState(this.buildState(depth, entry), '');
    } catch {
      /* ignore sandboxed history restrictions */
    }
  }

  private replaceBrowserState(depth: number, entry?: BackHistoryEntry): void {
    try {
      window.history.replaceState(this.buildState(depth, entry), '');
    } catch {
      /* ignore sandboxed history restrictions */
    }
  }

  private readPayload(state: unknown): BackStatePayload | null {
    if (!isPlainRecord(state)) return null;
    const raw = state[STATE_KEY];
    if (!isPlainRecord(raw)) return null;
    if (raw.sessionId !== this.sessionId || typeof raw.depth !== 'number') return null;
    return {
      sessionId: raw.sessionId,
      depth: raw.depth,
      id: typeof raw.id === 'string' ? raw.id : undefined,
      tag: typeof raw.tag === 'string' ? raw.tag : undefined,
    };
  }

  private readDepth(state: unknown): number {
    const payload = this.readPayload(state);
    return payload ? Math.max(0, Math.floor(payload.depth)) : 0;
  }
}

export const backHistory = new BackHistory();
