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
  isSavedMessages?: boolean;
  isArchived?: boolean;
  isOtpBurn?: boolean;
}
