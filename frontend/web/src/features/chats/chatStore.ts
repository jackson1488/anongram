import { i18nStore } from '../../core/i18n/i18nStore';
import type { AppLanguage } from '../../core/i18n/types';
import { kvStore } from '../../core/storage/kvStore';
import { draftsStore } from '../drafts/draftsStore';
import { blockedUsersStore } from './user_profile/data/BlockedUsersStore';
import {
  type ChatMessage,
  type ChatPreviewResult,
  type ChatThread,
  type MessageAttachment,
  type ReplyPreview,
  filterProtocolPreviewText,
  formatAttachmentPreviewText,
  getAttachmentFallbackLabel,
  isProtocolPreviewText,
  resolveMessagePreview,
} from './models';

export {
  filterProtocolPreviewText,
  formatAttachmentPreviewText,
  getAttachmentFallbackLabel,
  isProtocolPreviewText,
  resolveMessagePreview,
};

const KEY_THREADS = 'chats.threads.v1';
const KEY_MESSAGES = 'chats.messages.v1';
const MAX_PINNED_CHATS = 10;

const MIN = 60_000;
const HOUR = 3_600_000;
const DAY = 86_400_000;

type ChatListener = () => void;

function formatTime(date = new Date()): string {
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

function dayStartMs(epochMs: number): number {
  const d = new Date(epochMs);
  return new Date(d.getFullYear(), d.getMonth(), d.getDate()).getTime();
}

function escapeXml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function makeSvgPreviewDataUrl(
  title: string,
  badge: string,
  accentHex: string,
  bgStartHex: string,
  bgEndHex: string,
): string {
  const svg = [
    '<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 360">',
    '<defs>',
    '<linearGradient id="g" x1="0" y1="0" x2="1" y2="1">',
    `<stop offset="0%" stop-color="${bgStartHex}"/>`,
    `<stop offset="100%" stop-color="${bgEndHex}"/>`,
    '</linearGradient>',
    '</defs>',
    '<rect width="640" height="360" rx="24" fill="url(#g)"/>',
    `<circle cx="520" cy="72" r="110" fill="${accentHex}" fill-opacity="0.18"/>`,
    `<circle cx="110" cy="300" r="130" fill="${accentHex}" fill-opacity="0.14"/>`,
    `<path d="M0 250 Q180 185 340 235 T640 205 L640 360 L0 360 Z" fill="${accentHex}" fill-opacity="0.22"/>`,
    `<rect x="36" y="34" width="160" height="32" rx="16" fill="${accentHex}" fill-opacity="0.24"/>`,
    `<text x="116" y="55" text-anchor="middle" fill="#F8FAFC" font-family="system-ui,sans-serif" font-size="14" font-weight="700">${escapeXml(badge)}</text>`,
    `<text x="36" y="295" fill="#F8FAFC" font-family="system-ui,sans-serif" font-size="26" font-weight="700">${escapeXml(title)}</text>`,
    `<text x="36" y="325" fill="#CBD5E1" font-family="system-ui,sans-serif" font-size="14">AnonGram Encrypted Media Preview</text>`,
    '</svg>',
  ].join('');
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
}

function makeTextDataUrl(content: string, mime = 'text/plain'): string {
  return `data:${mime};charset=utf-8,${encodeURIComponent(content)}`;
}

function buildInitialMessages(now: number, lang: AppLanguage): Record<string, ChatMessage[]> {
  const ru = lang === 'ru';
  const selfName = ru ? 'Вы' : 'You';
  const todayStart = dayStartMs(now);
  const d1 = todayStart - 1 * DAY;
  const d2 = todayStart - 2 * DAY;

  let seq = 0;
  const msg = (
    chatId: string,
    isOutgoing: boolean,
    senderName: string,
    text: string,
    createdAt: number,
    opts?: {
      replyTo?: ChatMessage;
      attachment?: MessageAttachment;
      reactions?: Record<string, number>;
      isEdited?: boolean;
      isUnread?: boolean;
    },
  ): ChatMessage => {
    seq += 1;
    const reply = opts?.replyTo;
    return {
      id: `seed_${chatId}_${seq}`,
      chatId,
      isOutgoing,
      senderName,
      text,
      timestamp: formatTime(new Date(createdAt)),
      createdAt,
      replyTo: reply
        ? {
            messageId: reply.id,
            senderName: reply.senderName,
            snippet:
              reply.text ||
              getAttachmentFallbackLabel(reply.attachment, lang) ||
              '📎',
          }
        : undefined,
      attachment: opts?.attachment,
      reactions: opts?.reactions,
      isEdited: opts?.isEdited,
      isUnread: opts?.isUnread,
    };
  };

  // --- Saved Messages ---
  const saved: ChatMessage[] = [
    msg(
      'saved',
      true,
      selfName,
      ru
        ? 'Личные заметки, черновики и файлы хранятся только локально на этом устройстве.'
        : 'Personal notes, drafts, and files are stored strictly locally on this device.',
      d1 + 11 * HOUR + 10 * MIN,
    ),
    msg(
      'saved',
      true,
      selfName,
      ru ? 'Схема резервного маршрута.' : 'Backup relay route layout.',
      d1 + 14 * HOUR + 20 * MIN,
      {
        attachment: {
          type: 'photo',
          title: ru ? 'Схема маршрута' : 'Relay Map',
          fileSizeLabel: '380 KB',
          previewUrl: makeSvgPreviewDataUrl(
            ru ? 'Схема маршрута #3' : 'Relay Map #3',
            ru ? 'ФОТО' : 'PHOTO',
            '#0EA5E9',
            '#0F172A',
            '#1E293B',
          ),
        },
      },
    ),
    msg(
      'saved',
      true,
      selfName,
      '',
      d1 + 18 * HOUR + 45 * MIN,
      {
        attachment: {
          type: 'file',
          title: 'backup_checklist.txt',
          fileSizeLabel: '2.4 KB',
          previewUrl: makeTextDataUrl(
            ru
              ? 'Чек-лист резервной копии AnonGram:\n1. Записать seed-фразу на бумагу.\n2. Проверить пароль локального хранилища.\n3. Сохранить тему AG1.'
              : 'AnonGram Backup Checklist:\n1. Write down seed phrase offline.\n2. Verify local vault passphrase.\n3. Export AG1 theme code.',
          ),
        },
      },
    ),
    msg(
      'saved',
      true,
      selfName,
      ru
        ? 'Полезная заметка по цифровой гигиене: https://anongram.example/privacy'
        : 'Useful digital hygiene guide: https://anongram.example/privacy',
      now - 38 * MIN,
      { reactions: { '🔖': 1 } },
    ),
    msg(
      'saved',
      true,
      selfName,
      ru
        ? 'Черновики ответов и файлы не покидают браузер.'
        : 'Reply drafts and saved files never leave your browser.',
      now - 18 * MIN,
    ),
  ];

  // --- Ghost Node #42 (rich multi-day back-and-forth with grouped bubbles & attachments) ---
  const gn1 = msg(
    'ghost_node',
    false,
    'Ghost Node #42',
    ru
      ? 'Привет! Наконец-то протестировал веб-клиент — интерфейс летает.'
      : 'Hey! Finally tested the web client — the UI feels super snappy.',
    d2 + 10 * HOUR + 12 * MIN,
  );
  const gn2 = msg(
    'ghost_node',
    false,
    'Ghost Node #42',
    ru
      ? 'Особенно понравилось, что пересылка работает без раскрытия источника.'
      : 'Really love how forwarding strips the original sender completely.',
    d2 + 10 * HOUR + 13 * MIN,
  );
  const gn3 = msg(
    'ghost_node',
    true,
    selfName,
    ru
      ? 'Привет! Да, анонимная пересылка сразу вырезает отправителя и историю.'
      : 'Hey! Yes, anonymous forwarding strips author metadata right away.',
    d2 + 10 * HOUR + 16 * MIN,
    { replyTo: gn1, reactions: { '👍': 1, '🔥': 1 } },
  );
  const gn4 = msg(
    'ghost_node',
    true,
    selfName,
    ru
      ? 'Скидываю тебе макет новой тёмной палитры для проверки.'
      : 'Sending you a preview of the new dark palette to check out.',
    d2 + 10 * HOUR + 17 * MIN,
  );
  const gn5 = msg(
    'ghost_node',
    true,
    selfName,
    ru ? 'Вот скриншот темы Obsidian Violet.' : 'Here is the Obsidian Violet theme screenshot.',
    d2 + 10 * HOUR + 18 * MIN,
    {
      attachment: {
        type: 'photo',
        title: 'Obsidian Violet',
        fileSizeLabel: '420 KB',
        previewUrl: makeSvgPreviewDataUrl(
          'Obsidian Violet',
          ru ? 'ТЕМА' : 'THEME',
          '#8B5CF6',
          '#111827',
          '#1E1B4B',
        ),
      },
    },
  );
  const gn6 = msg(
    'ghost_node',
    false,
    'Ghost Node #42',
    ru
      ? 'Выглядит супер чётко! Контраст пузырей и читаемость отличные.'
      : 'Looks super crisp! Bubble contrast and readability are spot on.',
    d2 + 10 * HOUR + 24 * MIN,
  );
  const gn7 = msg(
    'ghost_node',
    true,
    selfName,
    ru
      ? 'Записал короткий трек для фона во время кодинга.'
      : 'Recorded a short ambient track for late-night coding.',
    d1 + 13 * HOUR + 5 * MIN,
  );
  const gn8 = msg(
    'ghost_node',
    true,
    selfName,
    '',
    d1 + 13 * HOUR + 6 * MIN,
    {
      attachment: {
        type: 'music',
        title: 'Midnight Onion',
        subtitle: 'Ghost Ensemble',
        durationSec: 214,
      },
    },
  );
  const gn9 = msg(
    'ghost_node',
    false,
    'Ghost Node #42',
    ru
      ? 'Классный вайб! Включил в мини-плеере прямо из чата.'
      : 'Awesome vibe! Playing it in the mini-player right from chat.',
    d1 + 13 * HOUR + 14 * MIN,
    { reactions: { '🔥': 2 } },
  );
  const gn10 = msg(
    'ghost_node',
    false,
    'Ghost Node #42',
    ru
      ? 'Завтра буду неподалёку, можем пересечься после 16:00.'
      : 'I will be nearby tomorrow — want to catch up after 16:00?',
    d1 + 17 * HOUR + 40 * MIN,
    {
      attachment: {
        type: 'geo',
        title: ru ? 'пр. Чуй 265, Бишкек' : '265 Chuy Ave, Bishkek',
        subtitle: '42.8759° N, 74.5900° E',
      },
    },
  );
  const gn11 = msg(
    'ghost_node',
    true,
    selfName,
    ru
      ? 'Договорились, время подходит. Напишешь, как будешь выходить.'
      : 'Sounds good, that time works. Ping me when you head out.',
    d1 + 17 * HOUR + 46 * MIN,
  );
  const gn12 = msg(
    'ghost_node',
    true,
    selfName,
    ru
      ? 'Доброе утро! Выложил ночную сборку, можно глянуть здесь: https://anongram.example/builds'
      : 'Morning! Pushed the nightly build, you can check it at https://anongram.example/builds',
    now - 95 * MIN,
  );
  const gn13 = msg(
    'ghost_node',
    false,
    'Ghost Node #42',
    ru
      ? 'Уже смотрю! Сейчас запишу короткий фидбек голосом.'
      : 'Checking it now! Sending quick voice feedback.',
    now - 68 * MIN,
  );
  const gn14 = msg(
    'ghost_node',
    false,
    'Ghost Node #42',
    '',
    now - 66 * MIN,
    {
      attachment: {
        type: 'voice',
        title: ru ? 'Голосовое сообщение' : 'Voice message',
        durationSec: 18,
      },
    },
  );
  const gn15 = msg(
    'ghost_node',
    true,
    selfName,
    ru
      ? 'Послушал, поправлю отступы на мобильном экране до обеда.'
      : 'Listened through — I will tighten the mobile spacing before noon.',
    now - 42 * MIN,
    { isEdited: true },
  );
  const gn16 = msg(
    'ghost_node',
    true,
    selfName,
    ru ? 'Вот свежая подборка скриншотов UI.' : 'Here is the updated UI snapshot pack.',
    now - 40 * MIN,
    {
      attachment: {
        type: 'album',
        title: ru ? 'Подборка UI (4 фото)' : 'UI Snapshots (4 photos)',
        fileSizeLabel: ru ? '4 фото' : '4 photos',
        itemCount: 4,
        previewUrl: makeSvgPreviewDataUrl(
          ru ? 'UI Альбом • 4 фото' : 'UI Album • 4 photos',
          ru ? 'АЛЬБОМ' : 'ALBUM',
          '#06B6D4',
          '#0F172A',
          '#164E63',
        ),
      },
    },
  );
  const gn17 = msg(
    'ghost_node',
    false,
    'Ghost Node #42',
    ru
      ? 'И ещё глянь вложения: я обновил цвета иконок для светлой темы.'
      : 'Also check the updated light-theme icon contrast when you get a chance.',
    now - 5 * MIN,
    { isUnread: true },
  );
  const gn18 = msg(
    'ghost_node',
    false,
    'Ghost Node #42',
    ru
      ? 'Отпишись, как всё проверишь — жду ревью!'
      : 'Let me know once you review everything!',
    now - 2 * MIN,
    { isUnread: true },
  );
  const ghostNode = [
    gn1, gn2, gn3, gn4, gn5, gn6, gn7, gn8, gn9,
    gn10, gn11, gn12, gn13, gn14, gn15, gn16, gn17, gn18,
  ];

  // --- AnonGram Core ---
  const ac1 = msg(
    'anongram_core',
    false,
    'AnonGram Core',
    ru
      ? 'Добро пожаловать в AnonGram Web! Интерфейс адаптирован под мобильный режим и двухпанельный десктоп.'
      : 'Welcome to AnonGram Web! Built for seamless mobile navigation and two-pane desktop flow.',
    d1 + 12 * HOUR + 15 * MIN,
  );
  const ac2 = msg(
    'anongram_core',
    true,
    selfName,
    ru
      ? 'Круто! Как здесь устроена работа с черновиками и пересылкой?'
      : 'Nice! How do drafts and message forwarding work here?',
    d1 + 12 * HOUR + 22 * MIN,
  );
  const ac3 = msg(
    'anongram_core',
    false,
    'AnonGram Core',
    ru
      ? 'Черновики сохраняются мгновенно на устройстве, а при пересылке автор всегда полностью скрыт.'
      : 'Drafts save instantly on-device, and forwarded messages always strip the original author.',
    d1 + 12 * HOUR + 25 * MIN,
    { replyTo: ac2, reactions: { '🔥': 4, '🛡️': 3 } },
  );
  const ac4 = msg(
    'anongram_core',
    true,
    selfName,
    ru
      ? 'А файлы и голосовые открываются прямо внутри чата?'
      : 'Do documents and voice notes open inline inside the chat?',
    now - 34 * MIN,
  );
  const ac5 = msg(
    'anongram_core',
    false,
    'AnonGram Core',
    ru
      ? 'Да! Все медиа, документы и аудио открываются во встроенном просмотрщике и мини-плеере.'
      : 'Yes! All media, documents, and audio open in the built-in viewer and mini-player.',
    now - 25 * MIN,
    {
      isUnread: true,
      attachment: {
        type: 'file',
        title: 'release_notes.txt',
        fileSizeLabel: '4.1 KB',
        previewUrl: makeTextDataUrl(
          ru
            ? 'AnonGram Web — заметки о выпуске:\n- Полноценный просмотрщик изображений и документов\n- Локальные черновики с автоматической сортировкой\n- 100% анонимная пересылка без заголовков автора'
            : 'AnonGram Web Release Notes:\n- Built-in media & document viewer\n- Instant local drafts with activity sorting\n- 100% anonymous forwarding with zero author headers',
        ),
      },
    },
  );
  const anongramCore = [ac1, ac2, ac3, ac4, ac5];

  // --- Maya Cipher ---
  const mayaCipher: ChatMessage[] = [
    msg(
      'maya_cipher',
      true,
      selfName,
      ru
        ? 'Зацени цветовой круг в настройках оформления чатов!'
        : 'Check out the HSV color wheel in Chat Appearance settings!',
      d1 + 11 * HOUR + 15 * MIN,
    ),
    msg(
      'maya_cipher',
      false,
      'Maya Cipher',
      ru
        ? 'Код темы AG1 скопировался за секунду, очень удобно.'
        : 'Copied the AG1 theme code in a second — super convenient.',
      d1 + 11 * HOUR + 20 * MIN,
    ),
    msg(
      'maya_cipher',
      false,
      'Maya Cipher',
      ru ? 'Сделала ещё вариант в бирюзовых тонах.' : 'Put together a teal variant too.',
      d1 + 11 * HOUR + 21 * MIN,
      {
        attachment: {
          type: 'photo',
          title: 'Aurora Teal',
          fileSizeLabel: '310 KB',
          previewUrl: makeSvgPreviewDataUrl(
            'Aurora Teal',
            ru ? 'ПАЛИТРА' : 'PALETTE',
            '#EC4899',
            '#0F172A',
            '#311042',
          ),
        },
      },
    ),
    msg(
      'maya_cipher',
      true,
      selfName,
      ru
        ? 'Супер, градиент пузырей отлично сочетается с обоями!'
        : 'Awesome, the bubble gradient blends nicely with the wallpaper!',
      d1 + 11 * HOUR + 28 * MIN,
      { reactions: { '❤️': 1 } },
    ),
    msg(
      'maya_cipher',
      true,
      selfName,
      ru
        ? 'Кину контакты нашего дизайнера на всякий случай.'
        : 'Dropping our designer contact here just in case.',
      now - 55 * MIN,
    ),
    msg(
      'maya_cipher',
      true,
      selfName,
      '',
      now - 54 * MIN,
      {
        attachment: {
          type: 'contact',
          title: ru ? 'Вера Криптон' : 'Vera Krypton',
          subtitle: '@vera_design',
        },
      },
    ),
    msg(
      'maya_cipher',
      false,
      'Maya Cipher',
      ru ? 'Спасибо, сохранила контакт себе!' : 'Thanks, saved the contact!',
      now - 45 * MIN,
    ),
  ];

  // --- Zero-Knowledge Lab ---
  const zeroLab: ChatMessage[] = [
    msg(
      'zero_lab',
      false,
      'Zero-Knowledge Lab',
      ru
        ? 'Опубликовали свежий отчёт по производительности локального хранилища.'
        : 'Published the latest benchmark report for local storage.',
      d2 + 15 * HOUR + 5 * MIN,
    ),
    msg(
      'zero_lab',
      false,
      'Zero-Knowledge Lab',
      '',
      d2 + 15 * HOUR + 6 * MIN,
      {
        attachment: {
          type: 'file',
          title: 'Architecture_Notes.pdf',
          fileSizeLabel: '1.4 MB',
        },
      },
    ),
    msg(
      'zero_lab',
      true,
      selfName,
      ru
        ? 'Отлично, уже открыл PDF. Задержка чтения меньше миллисекунды!'
        : 'Nice, opened the PDF already. Read latency is under a millisecond!',
      d2 + 15 * HOUR + 14 * MIN,
    ),
    msg(
      'zero_lab',
      false,
      'Zero-Knowledge Lab',
      ru ? 'Записали короткое демо со стенда.' : 'Recorded a quick clip from the test bench.',
      d1 + 16 * HOUR + 10 * MIN,
      {
        attachment: {
          type: 'video_circle',
          title: ru ? 'Видеокружок со стенда' : 'Test Bench Video Note',
          durationSec: 14,
        },
      },
    ),
    msg(
      'zero_lab',
      true,
      selfName,
      ru ? 'Видно отлично, стенд работает ровно.' : 'Looks smooth, bench numbers are rock solid.',
      d1 + 16 * HOUR + 18 * MIN,
    ),
    msg(
      'zero_lab',
      true,
      selfName,
      ru
        ? 'Когда планируете выложить следующее обновление документации?'
        : 'When are you planning to post the next docs update?',
      now - 82 * MIN,
    ),
    msg(
      'zero_lab',
      false,
      'Zero-Knowledge Lab',
      ru
        ? 'Сегодня вечером зальём финальный вариант сюда в чат.'
        : 'We will drop the final version in this chat later tonight.',
      now - 75 * MIN,
    ),
  ];

  // --- OTP Vault (OTP-Burn demo) ---
  const otpVault: ChatMessage[] = [
    msg(
      'otp_vault',
      false,
      'OTP Vault',
      ru
        ? 'Создан одноразовый канал связи. Заметки активны только в текущей локальной сессии.'
        : 'One-time ephemeral channel initialized. Notes remain strictly in this local session.',
      d1 + 19 * HOUR + 5 * MIN,
    ),
    msg(
      'otp_vault',
      true,
      selfName,
      ru
        ? 'Принято, буду использовать этот чат для временных координат.'
        : 'Got it, using this channel for temporary meetup coordinates.',
      d1 + 19 * HOUR + 12 * MIN,
    ),
    msg(
      'otp_vault',
      true,
      selfName,
      ru ? 'Где встречаемся для передачи резервной копии?' : 'Where are we meeting for the offline handoff?',
      now - 108 * MIN,
    ),
    msg(
      'otp_vault',
      false,
      'OTP Vault',
      '',
      now - 100 * MIN,
      {
        isUnread: true,
        attachment: {
          type: 'geo',
          title: ru ? 'Точка встречи: Парк Победы' : 'Meetup Point: Victory Park',
          subtitle: '42.8453° N, 74.6078° E',
        },
      },
    ),
  ];

  // --- Onion Relay Logs (Archived demo) ---
  const archivedRelay: ChatMessage[] = [
    msg(
      'archived_relay',
      true,
      selfName,
      ru ? 'Проверяю статус резервного узла за вчера.' : 'Checking yesterday status for the backup relay.',
      d1 + 20 * HOUR + 10 * MIN,
    ),
    msg(
      'archived_relay',
      false,
      'Onion Relay Logs',
      ru
        ? 'Трёхшаговый маршрут стабильный, потерь пакетов нет.'
        : 'Three-hop relay route stable with zero packet loss.',
      d1 + 20 * HOUR + 14 * MIN,
    ),
    msg(
      'archived_relay',
      false,
      'Onion Relay Logs',
      '',
      d1 + 20 * HOUR + 15 * MIN,
      {
        attachment: {
          type: 'file',
          title: 'relay_status.json',
          fileSizeLabel: '1.2 KB',
          previewUrl: makeTextDataUrl(
            JSON.stringify(
              {
                node: 'onion-relay-03',
                hops: 3,
                latencyMs: 42,
                packetLoss: 0,
                status: 'healthy',
              },
              null,
              2,
            ),
            'application/json',
          ),
        },
      },
    ),
  ];

  return {
    saved,
    ghost_node: ghostNode,
    anongram_core: anongramCore,
    maya_cipher: mayaCipher,
    zero_lab: zeroLab,
    otp_vault: otpVault,
    archived_relay: archivedRelay,
  };
}

function buildInitialThreads(
  messagesMap: Record<string, ChatMessage[]>,
  now: number,
  lang: AppLanguage,
): ChatThread[] {
  const ru = lang === 'ru';
  const lastTime = (id: string, fallback: number): number => {
    const list = messagesMap[id];
    return list && list.length > 0 ? list[list.length - 1].createdAt : fallback;
  };

  return [
    {
      id: 'saved',
      title: ru ? 'Избранное' : 'Saved Messages',
      avatarEmoji: '🔖',
      avatarColor: '#0EA5E9',
      statusText: ru ? 'локальное хранилище' : 'local vault',
      isOnline: true,
      isPinned: true,
      unreadCount: 0,
      lastMessageEpochMs: lastTime('saved', now - 18 * MIN),
      fingerprint: '0x0000…SAVED',
      isSavedMessages: true,
    },
    {
      id: 'ghost_node',
      title: 'Ghost Node #42',
      avatarEmoji: '🕶️',
      avatarColor: '#8B5CF6',
      statusText: ru ? 'в сети' : 'online',
      isOnline: true,
      isPinned: true,
      unreadCount: 2,
      lastMessageEpochMs: lastTime('ghost_node', now - 2 * MIN),
      fingerprint: '0x4D9F…88E1',
    },
    {
      id: 'anongram_core',
      title: 'AnonGram Core',
      avatarEmoji: '🛡️',
      avatarColor: '#06B6D4',
      statusText: ru ? 'канал протокола' : 'protocol channel',
      isOnline: true,
      isPinned: false,
      unreadCount: 1,
      lastMessageEpochMs: lastTime('anongram_core', now - 25 * MIN),
      fingerprint: '0x7F2A…9C01',
    },
    {
      id: 'maya_cipher',
      title: 'Maya Cipher',
      avatarEmoji: '⚡',
      avatarColor: '#EC4899',
      statusText: ru ? 'был(а) недавно' : 'last seen recently',
      isOnline: false,
      isPinned: false,
      unreadCount: 0,
      lastMessageEpochMs: lastTime('maya_cipher', now - 45 * MIN),
      fingerprint: '0x1B77…D3A0',
    },
    {
      id: 'zero_lab',
      title: 'Zero-Knowledge Lab',
      avatarEmoji: '🔐',
      avatarColor: '#10B981',
      statusText: ru ? '12 участников' : '12 members',
      isOnline: true,
      isPinned: false,
      unreadCount: 0,
      lastMessageEpochMs: lastTime('zero_lab', now - 75 * MIN),
      fingerprint: '0xA50C…12F7',
    },
    {
      id: 'otp_vault',
      title: 'OTP Vault',
      avatarEmoji: '🔥',
      avatarColor: '#F59E0B',
      statusText: ru ? 'одноразовый блокнот' : 'one-time pad',
      isOnline: true,
      isPinned: false,
      unreadCount: 1,
      lastMessageEpochMs: lastTime('otp_vault', now - 100 * MIN),
      fingerprint: '0xE210…7B44',
      isOtpBurn: true,
    },
    {
      id: 'archived_relay',
      title: 'Onion Relay Logs',
      avatarEmoji: '🧅',
      avatarColor: '#64748B',
      statusText: ru ? 'архивный узел' : 'archived relay',
      isOnline: false,
      isPinned: false,
      unreadCount: 0,
      lastMessageEpochMs: lastTime('archived_relay', now - 4 * HOUR),
      fingerprint: '0x99C2…04FF',
      isArchived: true,
    },
  ];
}

class ChatStore {
  private threadList: ChatThread[];
  private messagesMap: Record<string, ChatMessage[]>;
  private isOnionOnline = true;
  private listeners = new Set<ChatListener>();

  constructor() {
    const savedThreads = kvStore.getJson<ChatThread[] | null>(KEY_THREADS, null);
    const savedMessages = kvStore.getJson<Record<string, ChatMessage[]> | null>(KEY_MESSAGES, null);

    if (Array.isArray(savedThreads) && savedMessages && typeof savedMessages === 'object' && !Array.isArray(savedMessages)) {
      this.threadList = savedThreads;
      this.messagesMap = savedMessages;
    } else {
      const now = Date.now();
      const lang = i18nStore.language;
      const seededMessages = buildInitialMessages(now, lang);
      this.messagesMap =
        savedMessages && typeof savedMessages === 'object' && !Array.isArray(savedMessages)
          ? savedMessages
          : seededMessages;
      this.threadList = Array.isArray(savedThreads)
        ? savedThreads
        : buildInitialThreads(this.messagesMap, now, lang);
    }
  }

  get onionOnline(): boolean {
    return this.isOnionOnline;
  }

  toggleOnionOnline(): void {
    this.isOnionOnline = !this.isOnionOnline;
    this.emit();
  }

  /**
   * Mirrors Android ChatPreviewVisuals.sortSessionsWithDrafts:
   * pinned chats (up to MAX_PINNED_CHATS) remain at the top, and unpinned chats
   * sort dynamically by max(lastMessageEpochMs, draft.updatedAtEpochMs).
   */
  get threads(): ChatThread[] {
    const drafts = draftsStore.getAllDrafts();
    const pinned: ChatThread[] = [];
    const unpinned: ChatThread[] = [];

    for (const thread of this.threadList) {
      if (thread.isPinned && pinned.length < MAX_PINNED_CHATS) {
        pinned.push(thread);
      } else {
        unpinned.push(thread);
      }
    }

    const activityTime = (t: ChatThread): number => {
      const msgTime = this.getLastMessage(t.id)?.createdAt ?? t.lastMessageEpochMs ?? 0;
      const draftTime = drafts[t.id]?.updatedAtEpochMs ?? 0;
      return Math.max(msgTime, draftTime);
    };

    pinned.sort((a, b) => activityTime(b) - activityTime(a));
    unpinned.sort((a, b) => activityTime(b) - activityTime(a));

    return [...pinned, ...unpinned];
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

  getLastMessagePreview(chatId: string, fallbackEmpty?: string): ChatPreviewResult {
    const thread = this.getThread(chatId);
    const last = this.getLastMessage(chatId);
    return resolveMessagePreview(last, {
      securityLabel: thread?.securityLabel ?? thread?.statusText,
      fallbackEmpty,
    });
  }

  markRead(chatId: string): void {
    const idx = this.threadList.findIndex((t) => t.id === chatId);
    const list = this.messagesMap[chatId];
    const hasUnreadMsgs = Boolean(list?.some((m) => m.isUnread));
    const hasUnreadCount = idx !== -1 && this.threadList[idx].unreadCount > 0;
    if (!hasUnreadCount && !hasUnreadMsgs) return;

    if (idx !== -1 && hasUnreadCount) {
      this.threadList[idx] = { ...this.threadList[idx], unreadCount: 0 };
    }
    if (list && hasUnreadMsgs) {
      this.messagesMap = {
        ...this.messagesMap,
        [chatId]: list.map((m) => (m.isUnread ? { ...m, isUnread: false } : m)),
      };
    }
    this.persist();
    this.emit();
  }

  togglePin(chatId: string): void {
    const target = this.threadList.find((t) => t.id === chatId);
    if (!target) return;
    if (!target.isPinned) {
      const pinnedCount = this.threadList.filter((t) => t.isPinned && !t.isArchived).length;
      if (pinnedCount >= MAX_PINNED_CHATS) return;
    }
    this.threadList = this.threadList.map((t) =>
      t.id === chatId ? { ...t, isPinned: !t.isPinned } : t,
    );
    this.persist();
    this.emit();
  }

  toggleArchive(chatId: string): void {
    this.threadList = this.threadList.map((t) =>
      t.id === chatId ? { ...t, isArchived: !t.isArchived, isPinned: false } : t,
    );
    this.persist();
    this.emit();
  }

  ensureChat(
    id: string,
    title: string,
    avatarEmoji: string,
    avatarColor: string,
    isOtpBurn: boolean,
  ): string {
    const existing = this.threadList.find((t) => t.id === id);
    if (!existing) {
      const ru = i18nStore.language === 'ru';
      const now = Date.now();
      this.threadList = [
        {
          id,
          title,
          avatarEmoji,
          avatarColor,
          statusText: isOtpBurn
            ? ru
              ? 'сгораемый OTP-канал'
              : 'ephemeral OTP channel'
            : ru
              ? 'в сети'
              : 'online',
          isOnline: true,
          isPinned: false,
          unreadCount: 0,
          lastMessageEpochMs: now,
          isOtpBurn,
        },
        ...this.threadList,
      ];
      this.persist();
      this.emit();
    }
    return id;
  }

  sendMessage(
    chatId: string,
    text: string,
    replyTo?: ReplyPreview,
    attachment?: MessageAttachment,
  ): ChatMessage {
    const createdAt = Date.now();
    const senderName = i18nStore.language === 'ru' ? 'Вы' : 'You';
    const msg: ChatMessage = {
      id: `m_${createdAt}_${Math.random().toString(36).slice(2, 6)}`,
      chatId,
      isOutgoing: true,
      senderName,
      text: text.trim(),
      timestamp: formatTime(new Date(createdAt)),
      createdAt,
      replyTo,
      attachment,
    };
    const prev = this.messagesMap[chatId] ?? [];
    this.messagesMap = {
      ...this.messagesMap,
      [chatId]: [...prev, msg],
    };
    this.threadList = this.threadList.map((t) =>
      t.id === chatId
        ? { ...t, lastMessageEpochMs: createdAt, unreadCount: 0 }
        : t,
    );
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
    const nextList = list.filter((m) => m.id !== messageId);
    if (nextList.length === list.length) return;
    this.messagesMap = {
      ...this.messagesMap,
      [chatId]: nextList,
    };
    const nextLast = nextList[nextList.length - 1];
    if (nextLast) {
      this.threadList = this.threadList.map((t) =>
        t.id === chatId ? { ...t, lastMessageEpochMs: nextLast.createdAt } : t,
      );
    }
    this.persist();
    this.emit();
  }

  /** Updates an existing message's text, marks it as edited, and refreshes previews/replies. */
  editMessage(chatId: string, messageId: string, newText: string): void {
    const list = this.messagesMap[chatId];
    if (!list) return;
    const idx = list.findIndex((m) => m.id === messageId);
    if (idx === -1) return;

    const existing = list[idx];
    const trimmed = newText.trim();
    if (!trimmed && !existing.attachment) return;
    if (existing.text === trimmed) return;

    const updated: ChatMessage = {
      ...existing,
      text: trimmed,
      isEdited: true,
      replyTo: existing.replyTo ? { ...existing.replyTo } : undefined,
    };

    const replySnippet =
      trimmed ||
      getAttachmentFallbackLabel(updated.attachment, i18nStore.language) ||
      '📎';

    const nextList = list.map((m, i) => {
      if (i === idx) return updated;
      if (m.replyTo?.messageId === messageId) {
        return {
          ...m,
          replyTo: {
            ...m.replyTo,
            snippet: replySnippet,
          },
        };
      }
      return m;
    });

    this.messagesMap = {
      ...this.messagesMap,
      [chatId]: nextList,
    };

    if (idx === list.length - 1) {
      this.threadList = this.threadList.map((t) =>
        t.id === chatId ? { ...t, lastMessageEpochMs: updated.createdAt } : t,
      );
    }

    this.persist();
    this.emit();
  }

  /** Purges messages older than the chat's configured auto-delete duration. */
  purgeExpiredAutoDelete(chatId: string): void {
    const config = blockedUsersStore.getAutoDelete(chatId);
    const durationSeconds = config.seconds;
    if (config.preset === 'off' || durationSeconds <= 0) return;

    const list = this.messagesMap[chatId];
    if (!list || list.length === 0) return;

    const cutoff = Date.now() - durationSeconds * 1000;
    const nextList = list.filter((m) => m.createdAt >= cutoff);
    if (nextList.length === list.length) return;

    this.messagesMap = {
      ...this.messagesMap,
      [chatId]: nextList,
    };

    const nextLast = nextList[nextList.length - 1];
    this.threadList = this.threadList.map((t) =>
      t.id === chatId
        ? {
            ...t,
            lastMessageEpochMs: nextLast ? nextLast.createdAt : t.lastMessageEpochMs,
            unreadCount: nextList.length === 0 ? 0 : Math.min(t.unreadCount, nextList.length),
          }
        : t,
    );

    this.persist();
    this.emit();
  }

  /** Clears all messages in a chat and resets unread state while preserving the thread. */
  clearHistory(chatId: string): void {
    const hadKey = chatId in this.messagesMap;
    const hadMessages = (this.messagesMap[chatId]?.length ?? 0) > 0;
    const thread = this.threadList.find((t) => t.id === chatId);
    const hadUnread = (thread?.unreadCount ?? 0) > 0;

    if (!thread && !hadKey) return;

    if (hadMessages || !hadKey) {
      this.messagesMap = {
        ...this.messagesMap,
        [chatId]: [],
      };
    }
    if (thread && hadUnread) {
      this.threadList = this.threadList.map((t) =>
        t.id === chatId ? { ...t, unreadCount: 0 } : t,
      );
    }
    draftsStore.clearReplyTarget(chatId);

    if (hadMessages || hadUnread || !hadKey) {
      this.persist();
      this.emit();
    }
  }

  /** Removes a chat thread, its messages, and any associated draft completely. */
  deleteChat(chatId: string): void {
    const hadThread = this.threadList.some((t) => t.id === chatId);
    const hadMessages = chatId in this.messagesMap;

    if (hadThread) {
      this.threadList = this.threadList.filter((t) => t.id !== chatId);
    }
    if (hadMessages) {
      const nextMap = { ...this.messagesMap };
      delete nextMap[chatId];
      this.messagesMap = nextMap;
    }
    draftsStore.clearDraft(chatId);

    if (hadThread || hadMessages) {
      this.persist();
      this.emit();
    }
  }

  toggleReaction(chatId: string, messageId: string, emoji: string): void {
    const list = this.messagesMap[chatId] ?? [];
    this.messagesMap = {
      ...this.messagesMap,
      [chatId]: list.map((m) => {
        if (m.id !== messageId) return m;
        const next = { ...(m.reactions ?? {}) };
        next[emoji] = next[emoji] ? next[emoji] + 1 : 1;
        return { ...m, reactions: next };
      }),
    };
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

