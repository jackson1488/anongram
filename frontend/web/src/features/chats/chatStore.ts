import { kvStore } from '../../core/storage/kvStore';
import type { ChatMessage, ChatThread, MessageAttachment, ReplyPreview } from './models';

const KEY_THREADS = 'chats.threads.v1';
const KEY_MESSAGES = 'chats.messages.v1';

const INITIAL_THREADS: ChatThread[] = [
  {
    id: 'saved',
    title: 'Избранное',
    avatarEmoji: '🔖',
    avatarColor: '#0EA5E9',
    statusText: 'локальное хранилище',
    isOnline: true,
    isPinned: true,
    unreadCount: 0,
    isSavedMessages: true,
  },
  {
    id: 'ghost_node',
    title: 'Ghost Node #42',
    avatarEmoji: '🕶️',
    avatarColor: '#8B5CF6',
    statusText: 'в сети',
    isOnline: true,
    isPinned: true,
    unreadCount: 2,
  },
  {
    id: 'anongram_core',
    title: 'AnonGram Core',
    avatarEmoji: '🛡️',
    avatarColor: '#06B6D4',
    statusText: 'канал протокола',
    isOnline: true,
    isPinned: false,
    unreadCount: 1,
  },
  {
    id: 'maya_cipher',
    title: 'Maya Cipher',
    avatarEmoji: '⚡',
    avatarColor: '#EC4899',
    statusText: 'был(а) недавно',
    isOnline: false,
    isPinned: false,
    unreadCount: 0,
  },
  {
    id: 'zero_lab',
    title: 'Zero-Knowledge Lab',
    avatarEmoji: '🔐',
    avatarColor: '#10B981',
    statusText: '12 участников',
    isOnline: true,
    isPinned: false,
    unreadCount: 0,
  },
];

const now = Date.now();
const INITIAL_MESSAGES: Record<string, ChatMessage[]> = {
  saved: [
    {
      id: 'm_saved_1',
      chatId: 'saved',
      isOutgoing: true,
      senderName: 'Вы',
      text: 'Личные заметки, ключи и черновики хранятся локально.',
      timestamp: '14:10',
      createdAt: now - 3600_000,
    },
  ],
  ghost_node: [
    {
      id: 'm_gn_1',
      chatId: 'ghost_node',
      isOutgoing: false,
      senderName: 'Ghost Node #42',
      text: 'Привет! Проверил новый веб-клиент на TypeScript — открывается мгновенно.',
      timestamp: '18:02',
      createdAt: now - 900_000,
    },
    {
      id: 'm_gn_2',
      chatId: 'ghost_node',
      isOutgoing: false,
      senderName: 'Ghost Node #42',
      text: 'И пересылка сообщений по протоколу полностью анонимная: никаких плашек «Переслано от».',
      timestamp: '18:04',
      createdAt: now - 820_000,
      attachment: {
        type: 'voice',
        title: 'Голосовое сообщение',
        durationSec: 18,
      },
    },
  ],
  anongram_core: [
    {
      id: 'm_ac_1',
      chatId: 'anongram_core',
      isOutgoing: false,
      senderName: 'AnonGram Core',
      text: 'Добро пожаловать в AnonGram Web. Поддержка мобильной версии 1-в-1 с Android и двухпанельного режима для ПК.',
      timestamp: '16:40',
      createdAt: now - 2400_000,
      reactions: { '🔥': 4, '🛡️': 3 },
    },
  ],
  maya_cipher: [
    {
      id: 'm_mc_1',
      chatId: 'maya_cipher',
      isOutgoing: true,
      senderName: 'Вы',
      text: 'Зацени цветовой круг в настройках оформления чатов!',
      timestamp: '12:15',
      createdAt: now - 7200_000,
    },
    {
      id: 'm_mc_2',
      chatId: 'maya_cipher',
      isOutgoing: false,
      senderName: 'Maya Cipher',
      text: 'Код темы AG1 скопировался за секунду, очень удобно.',
      timestamp: '12:20',
      createdAt: now - 7000_000,
    },
  ],
  zero_lab: [
    {
      id: 'm_zl_1',
      chatId: 'zero_lab',
      isOutgoing: false,
      senderName: 'Zero-Knowledge Lab',
      text: 'Спецификация сквозного шифрования E2EE-v2 опубликована.',
      timestamp: 'Вчера',
      createdAt: now - 86400_000,
      attachment: {
        type: 'file',
        title: 'AnonGram_Protocol_v2.pdf',
        fileSizeLabel: '1.4 MB',
      },
    },
  ],
};

type ChatListener = () => void;

function formatTime(date = new Date()): string {
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

class ChatStore {
  private threadList: ChatThread[];
  private messagesMap: Record<string, ChatMessage[]>;
  private listeners = new Set<ChatListener>();

  constructor() {
    this.threadList = kvStore.getJson<ChatThread[]>(KEY_THREADS, INITIAL_THREADS);
    this.messagesMap = kvStore.getJson<Record<string, ChatMessage[]>>(KEY_MESSAGES, INITIAL_MESSAGES);
  }

  get threads(): ChatThread[] {
    return [...this.threadList].sort((a, b) => {
      if (a.isPinned !== b.isPinned) return a.isPinned ? -1 : 1;
      const la = this.getLastMessage(a.id)?.createdAt ?? 0;
      const lb = this.getLastMessage(b.id)?.createdAt ?? 0;
      return lb - la;
    });
  }

  getThread(chatId: string): ChatThread | undefined {
    return this.threadList.find((t) => t.id === chatId);
  }

  getMessages(chatId: string): ChatMessage[] {
    return this.messagesMap[chatId] ?? [];
  }

  getLastMessage(chatId: string): ChatMessage | undefined {
    const list = this.messagesMap[chatId];
    return list && list.length > 0 ? list[list.length - 1] : undefined;
  }

  markRead(chatId: string): void {
    const idx = this.threadList.findIndex((t) => t.id === chatId);
    if (idx === -1 || this.threadList[idx].unreadCount === 0) return;
    this.threadList[idx] = { ...this.threadList[idx], unreadCount: 0 };
    this.persist();
    this.emit();
  }

  togglePin(chatId: string): void {
    this.threadList = this.threadList.map((t) =>
      t.id === chatId ? { ...t, isPinned: !t.isPinned } : t,
    );
    this.persist();
    this.emit();
  }

  sendMessage(
    chatId: string,
    text: string,
    replyTo?: ReplyPreview,
    attachment?: MessageAttachment,
  ): ChatMessage {
    const msg: ChatMessage = {
      id: `m_${Date.now()}_${Math.random().toString(36).slice(2, 6)}`,
      chatId,
      isOutgoing: true,
      senderName: 'Вы',
      text: text.trim(),
      timestamp: formatTime(),
      createdAt: Date.now(),
      replyTo,
      attachment,
    };
    const prev = this.messagesMap[chatId] ?? [];
    this.messagesMap[chatId] = [...prev, msg];
    this.persist();
    this.emit();
    return msg;
  }

  /**
   * AnonGram Core Protocol: 100% anonymous forwarding.
   * Strips any original author/chat metadata completely.
   */
  forwardAnonymously(targetChatId: string, sourceMessage: ChatMessage): ChatMessage {
    return this.sendMessage(
      targetChatId,
      sourceMessage.text,
      undefined,
      sourceMessage.attachment,
    );
  }

  deleteMessage(chatId: string, messageId: string): void {
    const list = this.messagesMap[chatId] ?? [];
    this.messagesMap[chatId] = list.filter((m) => m.id !== messageId);
    this.persist();
    this.emit();
  }

  toggleReaction(chatId: string, messageId: string, emoji: string): void {
    const list = this.messagesMap[chatId] ?? [];
    this.messagesMap[chatId] = list.map((m) => {
      if (m.id !== messageId) return m;
      const next = { ...(m.reactions ?? {}) };
      next[emoji] = next[emoji] ? next[emoji] + 1 : 1;
      return { ...m, reactions: next };
    });
    this.persist();
    this.emit();
  }

  subscribe(fn: ChatListener): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  private persist(): void {
    kvStore.setJson(KEY_THREADS, this.threadList);
    kvStore.setJson(KEY_MESSAGES, this.messagesMap);
  }

  private emit(): void {
    for (const fn of this.listeners) fn();
  }
}

export const chatStore = new ChatStore();
