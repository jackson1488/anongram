import { i18nStore } from '../../core/i18n/i18nStore';
import type { AppLanguage } from '../../core/i18n/types';
import { kvStore } from '../../core/storage/kvStore';

export interface ChatDraft {
  chatId: string;
  text: string;
  replyToMessageId?: string;
  replyToSubIndex?: number;
  replySnippet?: string;
  voiceUri?: string;
  videoUri?: string;
  updatedAtEpochMs: number;
}

const KEY_DRAFTS = 'chats.drafts.v1';

const SERVICE_PREFIX_RE = /^(?:ответ|reply|черновик|draft)\s*:\s*/i;

const DRAFT_FALLBACK_LABELS: Record<
  AppLanguage,
  { reply: string; voice: string; video: string }
> = {
  ru: {
    reply: 'В ответ на сообщение',
    voice: 'Голосовое сообщение',
    video: 'Видеосообщение',
  },
  en: {
    reply: 'Replying to a message',
    voice: 'Voice message',
    video: 'Video message',
  },
};

/** Returns true when a draft carries no typed text, reply target, or recorded media. */
export function isDraftEmpty(draft?: ChatDraft | null): boolean {
  if (!draft) return true;
  return (
    !draft.text.trim() &&
    !draft.replyToMessageId &&
    !draft.voiceUri &&
    !draft.videoUri
  );
}

/**
 * Mirrors Android ChatPreviewVisuals.cleanDraftPreviewText:
 * strips accidental service prefixes ("Ответ: ", "Reply: ") and resolves localized fallbacks.
 */
export function cleanDraftPreviewText(
  draft?: ChatDraft | null,
  lang: AppLanguage = i18nStore.language,
): string | null {
  if (!draft || isDraftEmpty(draft)) return null;

  const normalize = (raw: string): string =>
    raw
      .replace(/[\r\n]+/g, ' ')
      .trim()
      .replace(SERVICE_PREFIX_RE, '')
      .trim();

  const typed = normalize(draft.text);
  if (typed) return typed;

  const snippet = draft.replySnippet ? normalize(draft.replySnippet) : '';
  if (snippet) return snippet;

  const labels = DRAFT_FALLBACK_LABELS[lang];
  if (draft.replyToMessageId) return labels.reply;
  if (draft.voiceUri) return labels.voice;
  if (draft.videoUri) return labels.video;
  return null;
}

class DraftsStore {
  private map: Record<string, ChatDraft>;
  private listeners = new Set<() => void>();

  constructor() {
    const loaded = kvStore.getJson<Record<string, ChatDraft> | null>(KEY_DRAFTS, null);
    this.map = loaded && typeof loaded === 'object' && !Array.isArray(loaded) ? loaded : {};
  }

  getDraft(chatId: string): ChatDraft | undefined {
    const d = this.map[chatId];
    if (isDraftEmpty(d)) return undefined;
    return d;
  }

  getAllDrafts(): Record<string, ChatDraft> {
    const out: Record<string, ChatDraft> = {};
    for (const [chatId, draft] of Object.entries(this.map)) {
      if (!isDraftEmpty(draft)) {
        out[chatId] = draft;
      }
    }
    return out;
  }

  setDraft(
    chatId: string,
    text: string,
    replyToMessageId?: string,
    replySnippet?: string,
    extras?: {
      replyToSubIndex?: number;
      voiceUri?: string;
      videoUri?: string;
    },
  ): void {
    const nextDraft: ChatDraft = {
      chatId,
      text,
      replyToMessageId,
      replyToSubIndex: extras?.replyToSubIndex,
      replySnippet,
      voiceUri: extras?.voiceUri,
      videoUri: extras?.videoUri,
      updatedAtEpochMs: Date.now(),
    };
    if (isDraftEmpty(nextDraft)) {
      if (this.map[chatId]) {
        delete this.map[chatId];
        this.persist();
      }
      return;
    }
    this.map[chatId] = nextDraft;
    this.persist();
  }

  /** Removes a stale reply reference from a draft (e.g. after clearing chat history). */
  clearReplyTarget(chatId: string): void {
    const current = this.map[chatId];
    if (!current || (!current.replyToMessageId && !current.replySnippet)) return;
    const updated: ChatDraft = {
      ...current,
      replyToMessageId: undefined,
      replyToSubIndex: undefined,
      replySnippet: undefined,
    };
    if (isDraftEmpty(updated)) {
      delete this.map[chatId];
    } else {
      this.map[chatId] = updated;
    }
    this.persist();
  }

  clearDraft(chatId: string): void {
    if (!this.map[chatId]) return;
    delete this.map[chatId];
    this.persist();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private persist(): void {
    kvStore.setJson(KEY_DRAFTS, this.map);
    for (const fn of this.listeners) fn();
  }
}

export const draftsStore = new DraftsStore();

