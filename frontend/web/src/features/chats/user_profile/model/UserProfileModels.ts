import type { ChatMessage, ChatThread } from '../../models';

export type UserProfileTab = 'MEDIA' | 'FILES' | 'LINKS' | 'VOICE';

export type AutoDeletePreset = 'off' | '1h' | '24h' | '7d' | '30d' | 'custom';

export interface AutoDeleteConfig {
  preset: AutoDeletePreset;
  /** Duration in seconds (0 when 'off'). */
  seconds: number;
}

export interface UserProfileMediaItem {
  message: ChatMessage;
  previewUrl?: string;
  isVideo: boolean;
  isVideoCircle: boolean;
  title: string;
  durationSec?: number;
  itemKey: string;
}

export interface UserProfileFileItem {
  message: ChatMessage;
  title: string;
  subtitle?: string;
  fileSizeLabel?: string;
}

export interface UserProfileLinkItem {
  message: ChatMessage;
  url: string;
  domain: string;
}

export interface UserProfileAudioItem {
  message: ChatMessage;
  title: string;
  subtitle: string;
  durationSec?: number;
  isVoice: boolean;
}

export interface UserProfileBuckets {
  media: UserProfileMediaItem[];
  files: UserProfileFileItem[];
  links: UserProfileLinkItem[];
  voice: UserProfileAudioItem[];
}

export interface PeerCryptoMetadata {
  publicId: string;
  fingerprint: string;
  username: string;
  bio: string;
}

const URL_REGEX = /https?:\/\/[^\s<>"')]+/gi;

function fnv1aHash(input: string): number {
  let hash = 0x811c9dc5;
  for (let i = 0; i < input.length; i++) {
    hash ^= input.charCodeAt(i);
    hash = Math.imul(hash, 0x01000193);
  }
  return hash >>> 0;
}

function deterministicHex(seed: string, length: number): string {
  let out = '';
  let counter = 0;
  while (out.length < length) {
    const h = fnv1aHash(`${seed}:${counter++}`);
    out += h.toString(16).padStart(8, '0');
  }
  return out.slice(0, length);
}

/** Derive deterministic Public ID, Ed25519 fingerprint, and handle for a chat thread. */
export function derivePeerMetadata(thread: ChatThread): PeerCryptoMetadata {
  const hex = deterministicHex(`ag:${thread.id}`, 32);
  const publicId = `ag_${hex.slice(0, 16)}`;
  const fingerprint = (hex.toUpperCase().match(/.{1,4}/g) ?? []).join(' ');
  const cleanHandle = thread.title
    .toLowerCase()
    .replace(/[^a-z0-9_а-яё]/gi, '_')
    .replace(/_+/g, '_')
    .replace(/^_|_$/g, '')
    .slice(0, 20);
  const username = `@${cleanHandle || thread.id}`;
  const bio = thread.isSavedMessages
    ? 'E2EE local encrypted vault'
    : thread.isOtpBurn
      ? 'Ephemeral one-time burn channel'
      : 'AnonGram Tor Onion v3 • E2EE-v2';

  return { publicId, fingerprint, username, bio };
}

export function formatDurationSec(sec?: number): string {
  if (!sec || sec <= 0) return '0:00';
  const m = Math.floor(sec / 60);
  const s = Math.floor(sec % 60);
  return `${m}:${String(s).padStart(2, '0')}`;
}

export function extractProfileBuckets(messages: ChatMessage[]): UserProfileBuckets {
  const media: UserProfileMediaItem[] = [];
  const files: UserProfileFileItem[] = [];
  const links: UserProfileLinkItem[] = [];
  const voice: UserProfileAudioItem[] = [];

  // Show newest items first, matching Telegram / Android profile order
  for (let i = messages.length - 1; i >= 0; i--) {
    const msg = messages[i];
    const att = msg.attachment;

    if (att) {
      if (att.type === 'photo' || att.type === 'album' || att.type === 'video_circle') {
        media.push({
          message: msg,
          previewUrl: att.previewUrl,
          isVideo: att.type === 'video_circle',
          isVideoCircle: att.type === 'video_circle',
          title: att.title,
          durationSec: att.durationSec,
          itemKey: msg.id,
        });
      } else if (att.type === 'file') {
        files.push({
          message: msg,
          title: att.title,
          subtitle: att.subtitle,
          fileSizeLabel: att.fileSizeLabel,
        });
      } else if (att.type === 'voice' || att.type === 'music') {
        voice.push({
          message: msg,
          title: att.title,
          subtitle: att.subtitle ?? formatDurationSec(att.durationSec),
          durationSec: att.durationSec,
          isVoice: att.type === 'voice',
        });
      }
    }

    if (msg.text) {
      const matches = msg.text.match(URL_REGEX);
      if (matches) {
        for (const url of matches) {
          let domain = url;
          try {
            domain = new URL(url).hostname;
          } catch {
            // keep raw url
          }
          links.push({ message: msg, url, domain });
        }
      }
    }
  }

  return { media, files, links, voice };
}

export function getAvailableTabs(buckets: UserProfileBuckets): UserProfileTab[] {
  const tabs: UserProfileTab[] = [];
  if (buckets.media.length > 0) tabs.push('MEDIA');
  if (buckets.files.length > 0) tabs.push('FILES');
  if (buckets.links.length > 0) tabs.push('LINKS');
  if (buckets.voice.length > 0) tabs.push('VOICE');
  return tabs;
}
