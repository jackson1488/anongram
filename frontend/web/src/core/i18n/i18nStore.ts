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
