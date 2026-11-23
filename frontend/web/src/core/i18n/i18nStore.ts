import { kvStore } from '../storage/kvStore';
import { EN_STRINGS } from './en';
import { RU_STRINGS } from './ru';
import type { AnonGramStrings, AppLanguage } from './types';

const KEY_LANG = 'app.language';

type LangListener = (lang: AppLanguage, strings: AnonGramStrings) => void;

class I18nStore {
  private lang: AppLanguage;
  private listeners = new Set<LangListener>();

  constructor() {
    const saved = kvStore.getString(KEY_LANG, '');
    if (saved === 'ru' || saved === 'en') {
      this.lang = saved;
    } else {
      const navLang = (navigator.language || 'ru').toLowerCase();
      this.lang = navLang.startsWith('ru') ? 'ru' : 'en';
    }
    document.documentElement.lang = this.lang;
  }

  get language(): AppLanguage {
    return this.lang;
  }

  get t(): AnonGramStrings {
    return this.lang === 'ru' ? RU_STRINGS : EN_STRINGS;
  }

  setLanguage(lang: AppLanguage): void {
    if (this.lang === lang) return;
    this.lang = lang;
    kvStore.setString(KEY_LANG, lang);
    document.documentElement.lang = lang;
    const strings = this.t;
    for (const fn of this.listeners) {
      fn(lang, strings);
    }
  }

  subscribe(fn: LangListener): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }
}

export const i18nStore = new I18nStore();

// ---- Per-module dictionary registry (mirrors Android per-module i18n contracts) ----
export type ModuleDict = Record<string, string>;
const modules = new Map<string, { ru: ModuleDict; en: ModuleDict }>();

export function registerI18nModule(name: string, dict: { ru: ModuleDict; en: ModuleDict }): void {
  modules.set(name, dict);
}

/** Get a module dictionary resolved for the current language (falls back to en). */
export function moduleStrings(name: string): ModuleDict {
  const m = modules.get(name);
  if (!m) return {};
  return { ...m.en, ...(i18nStore.language === 'ru' ? m.ru : m.en) };
}

function lookupPath(obj: unknown, path: string): string | undefined {
  let cur: unknown = obj;
  for (const part of path.split('.')) {
    if (cur && typeof cur === 'object' && part in (cur as Record<string, unknown>)) {
      cur = (cur as Record<string, unknown>)[part];
    } else return undefined;
  }
  return typeof cur === 'string' ? cur : undefined;
}

/**
 * Translate a key. Forms: 'common.back' (core dictionary path),
 * 'module:key' (registered module), or bare 'key' (searched in all modules).
 * `{name}` placeholders replaced from params.
 */
export function tr(key: string, params?: Record<string, string | number>): string {
  let out: string | undefined;
  const colon = key.indexOf(':');
  if (colon > 0) {
    out = moduleStrings(key.slice(0, colon))[key.slice(colon + 1)];
  } else {
    out = lookupPath(i18nStore.t, key);
    if (out === undefined) {
      for (const name of modules.keys()) {
        const v = moduleStrings(name)[key];
        if (v !== undefined) { out = v; break; }
      }
    }
  }
  let res = out ?? key;
  if (params) for (const [k, v] of Object.entries(params)) res = res.split(`{${k}}`).join(String(v));
  return res;
}
