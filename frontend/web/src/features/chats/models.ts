import { i18nStore } from '../../core/i18n/i18nStore';
import type { AppLanguage } from '../../core/i18n/types';

export type ChatFolder = 'all' | 'unread' | 'pinned';

export type AttachmentType =
  | 'photo'
  | 'album'
  | 'file'
  | 'music'
  | 'voice'
  | 'video_circle'
  | 'geo'
  | 'contact';

export interface MessageAttachment {
  type: AttachmentType;
  title: string;
  subtitle?: string;
  durationSec?: number;
  previewUrl?: string;
  fileSizeLabel?: string;
  isVideo?: boolean;
  itemCount?: number;
}

export interface ReplyPreview {
  messageId: string;
  senderName: string;
  snippet: string;
}

export interface ChatMessage {
  id: string;
  chatId: string;
  isOutgoing: boolean;
  senderName: string;
  text: string;
  timestamp: string;
  createdAt: number;
  replyTo?: ReplyPreview;
  attachment?: MessageAttachment;
  reactions?: Record<string, number>;
  isEdited?: boolean;
  isUnread?: boolean;
}

export interface ChatThread {
  id: string;
  title: string;
  avatarEmoji: string;
  avatarColor: string;
  statusText: string;
  isOnline: boolean;
  isPinned: boolean;
  unreadCount: number;
  lastMessageEpochMs?: number;
  securityLabel?: string;
  fingerprint?: string;
  isSavedMessages?: boolean;
  isArchived?: boolean;
  isOtpBurn?: boolean;
}

export interface ChatPreviewResult {
  text: string;
  isAttachmentFallback: boolean;
  isFilteredProtocol: boolean;
  isEmpty: boolean;
}

const PROTOCOL_KEYWORDS_CI = [
  'ml-kem',
  'kyber',
  'ratchet',
  'e2ee',
  'sha-256',
  'zk-snark',
  'obfs4',
  'x3dh',
  'chacha20',
  '0x',
] as const;

const ATTACHMENT_FALLBACK_LABELS: Record<
  AppLanguage,
  {
    photo: string;
    video: string;
    album: string;
    voice: string;
    videoCircle: string;
    geo: string;
    music: string;
    file: string;
    contact: string;
  }
> = {
  ru: {
    photo: 'Фото',
    video: 'Видео',
    album: 'Альбом',
    voice: 'Голосовое сообщение',
    videoCircle: 'Видеосообщение',
    geo: 'Геолокация',
    music: 'Музыка',
    file: 'Файл',
    contact: 'Контакт',
  },
  en: {
    photo: 'Photo',
    video: 'Video',
    album: 'Album',
    voice: 'Voice message',
    videoCircle: 'Video message',
    geo: 'Location',
    music: 'Music',
    file: 'File',
    contact: 'Contact',
  },
};

/**
 * Mirrors Android ChatListItemCard.kt protocol-string detection so crypto/handshake
 * status banners never leak into chat-list previews.
 */
export function isProtocolPreviewText(text: string, securityLabel?: string): boolean {
  const clean = text.trim();
  if (!clean) return false;
  if (securityLabel && securityLabel.trim().length > 0) {
    if (clean.toLowerCase() === securityLabel.trim().toLowerCase()) {
      return true;
    }
  }
  const lower = clean.toLowerCase();
  if (PROTOCOL_KEYWORDS_CI.some((kw) => lower.includes(kw))) {
    return true;
  }
  return clean.includes('SAS');
}

/** Returns trimmed message text, or empty string if it is blank or matches protocol jargon. */
export function filterProtocolPreviewText(
  text?: string | null,
  securityLabel?: string,
): string {
  const clean = (text ?? '').trim();
  if (!clean || isProtocolPreviewText(clean, securityLabel)) {
    return '';
  }
  return clean;
}

/**
 * Localized category fallback label for an attachment
 * (mirrors Android ChatListItemCard.kt attachmentFallbackLabel).
 */
export function getAttachmentFallbackLabel(
  attachment?: MessageAttachment | null,
  lang: AppLanguage = i18nStore.language,
): string {
  if (!attachment) return '';
  const labels = ATTACHMENT_FALLBACK_LABELS[lang];
  switch (attachment.type) {
    case 'photo':
      return attachment.isVideo ? labels.video : labels.photo;
    case 'album':
      return labels.album;
    case 'voice':
      return labels.voice;
    case 'video_circle':
      return labels.videoCircle;
    case 'geo':
      return labels.geo;
    case 'music':
      return labels.music;
    case 'file':
      return labels.file;
    case 'contact':
      return labels.contact;
    default:
      return '';
  }
}

/**
 * Rich single-line attachment summary for list cards (mirrors Android ChatPreviewVisuals.kt),
 * falling back to localized type labels when metadata is absent.
 */
export function formatAttachmentPreviewText(
  attachment?: MessageAttachment | null,
  lang: AppLanguage = i18nStore.language,
): string {
  if (!attachment) return '';
  const fallback = getAttachmentFallbackLabel(attachment, lang);
  const title = attachment.title.trim();

  switch (attachment.type) {
    case 'album':
      if (attachment.itemCount && attachment.itemCount > 1) {
        return lang === 'ru'
          ? `${attachment.itemCount} фото`
          : `${attachment.itemCount} photos`;
      }
      return fallback;
    case 'music':
      return title && title.toLowerCase() !== fallback.toLowerCase()
        ? `${fallback}: ${title}`
        : fallback;
    case 'file':
      return title || fallback;
    case 'photo':
    case 'voice':
    case 'video_circle':
    case 'geo':
    case 'contact':
    default:
      return fallback;
  }
}

/**
 * Resolves a chat-list preview string from a message, filtering protocol strings and
 * falling back to localized attachment labels.
 */
export function resolveMessagePreview(
  message?: Pick<ChatMessage, 'text' | 'attachment'> | null,
  options?: {
    securityLabel?: string;
    lang?: AppLanguage;
    fallbackEmpty?: string;
  },
): ChatPreviewResult {
  const lang = options?.lang ?? i18nStore.language;
  const rawText = (message?.text ?? '').trim();
  const isFilteredProtocol = rawText.length > 0 && isProtocolPreviewText(rawText, options?.securityLabel);

  if (rawText.length > 0 && !isFilteredProtocol) {
    return {
      text: rawText,
      isAttachmentFallback: false,
      isFilteredProtocol: false,
      isEmpty: false,
    };
  }

  const attLabel = getAttachmentFallbackLabel(message?.attachment, lang);
  if (attLabel) {
    return {
      text: attLabel,
      isAttachmentFallback: true,
      isFilteredProtocol,
      isEmpty: false,
    };
  }

  return {
    text: options?.fallbackEmpty ?? '',
    isAttachmentFallback: false,
    isFilteredProtocol,
    isEmpty: true,
  };
}

