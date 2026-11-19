const PREFIX = 'anongram.';

export const kvStore = {
  getString(key: string, fallback = ''): string {
    try {
      const val = window.localStorage.getItem(PREFIX + key);
      return val !== null ? val : fallback;
    } catch {
      return fallback;
    }
  },

  setString(key: string, value: string): void {
    try {
      window.localStorage.setItem(PREFIX + key, value);
    } catch {
      // ignore quota / private mode errors
    }
  },

  getNumber(key: string, fallback: number): number {
    const raw = this.getString(key, '');
    if (!raw) return fallback;
    const parsed = Number(raw);
    return Number.isFinite(parsed) ? parsed : fallback;
  },

  setNumber(key: string, value: number): void {
    this.setString(key, String(value));
  },

  getBool(key: string, fallback: boolean): boolean {
    const raw = this.getString(key, '');
    if (raw === 'true') return true;
    if (raw === 'false') return false;
    return fallback;
  },

  setBool(key: string, value: boolean): void {
    this.setString(key, value ? 'true' : 'false');
  },

  getJson<T>(key: string, fallback: T): T {
    const raw = this.getString(key, '');
    if (!raw) return fallback;
    try {
      return JSON.parse(raw) as T;
    } catch {
      return fallback;
    }
  },

  setJson<T>(key: string, value: T): void {
    try {
      this.setString(key, JSON.stringify(value));
    } catch {
      // ignore
    }
  },

  remove(key: string): void {
    try {
      window.localStorage.removeItem(PREFIX + key);
    } catch {
      // ignore
    }
  },
};
