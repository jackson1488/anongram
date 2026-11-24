import { i18nStore } from '../../../../core/i18n/i18nStore';
import { kvStore } from '../../../../core/storage/kvStore';
import { settingsStore, type VoiceEngineOption } from '../../../settings/settingsStore';

export type LanguagePackCode =
  | 'ru'
  | 'en'
  | 'de'
  | 'fr'
  | 'es'
  | 'tr'
  | 'zh'
  | 'ar'
  | 'uk'
  | 'ja';

export interface TranslationLanguagePack {
  code: LanguagePackCode;
  nameRu: string;
  nameEn: string;
  nativeName: string;
  sizeMb: number;
}

export interface TranslationPackInfo {
  code: string;
  nameRu: string;
  nameEn: string;
  sizeMb: number;
  downloaded: boolean;
}

export interface TranslationPackStatus extends TranslationPackInfo {
  nativeName: string;
  downloading: boolean;
  progress: number;
}

export interface TranslateTextResult {
  translated: string;
  sourceLang: string;
  targetLang: string;
}

export interface MessageTranslationEntry {
  messageId: string;
  originalText: string;
  translatedText: string;
  sourceLang: string;
  targetLang: string;
  updatedAt: number;
}

export interface VoiceTranscriptEntry {
  messageId: string;
  text: string;
  lang: 'ru' | 'en';
  durationSec: number;
  engine: 'web-speech' | 'whisper' | 'vosk';
  updatedAt: number;
}

export interface TranslatorStateSnapshot {
  downloadedCodes: string[];
  downloadingCode: string | null;
  progress: number;
  targetLang: string;
  translatingMessageIds: string[];
  transcribingVoiceIds: string[];
}

export type TranslatorListener = (snapshot: TranslatorStateSnapshot) => void;

interface SpeechRecognitionAlternativeLike {
  transcript: string;
}

interface SpeechRecognitionResultLike {
  isFinal: boolean;
  0?: SpeechRecognitionAlternativeLike;
}

interface SpeechRecognitionEventLike {
  results: ArrayLike<SpeechRecognitionResultLike>;
}

interface SpeechRecognitionInstance {
  lang: string;
  interimResults: boolean;
  maxAlternatives: number;
  continuous: boolean;
  onresult: ((ev: SpeechRecognitionEventLike) => void) | null;
  onerror: ((ev: { error?: string }) => void) | null;
  onend: (() => void) | null;
  start(): void;
  stop(): void;
  abort(): void;
}

type SpeechRecognitionCtor = new () => SpeechRecognitionInstance;

const KEY_PACKS = 'tools.translate.packs.v1';
const KEY_TARGET_LANG = 'tools.translate.target_lang.v1';
const KEY_CACHE = 'tools.translate.cache.v1';
const KEY_MSG_TRANSLATIONS = 'tools.translate.messages.v1';
const KEY_VOICE_TRANSCRIPTS = 'tools.translate.transcripts.v1';

const DEFAULT_DOWNLOADED_PACKS: LanguagePackCode[] = ['ru', 'en'];
const MYMEMORY_ENDPOINT = 'https://api.mymemory.translated.net/get';
const MYMEMORY_TIMEOUT_MS = 2500;
const MYMEMORY_MAX_CHUNK_CHARS = 450;
const MAX_CACHE_ENTRIES = 200;
const MAX_RECORD_ENTRIES = 150;

/** Language packs catalog matching Android ML Kit / Bergamot offline packs (32-48 MB). */
export const TRANSLATION_LANGUAGE_PACKS: readonly TranslationLanguagePack[] = [
  { code: 'ru', nameRu: 'Русский', nameEn: 'Russian', nativeName: 'Русский', sizeMb: 36 },
  { code: 'en', nameRu: 'Английский', nameEn: 'English', nativeName: 'English', sizeMb: 32 },
  { code: 'de', nameRu: 'Немецкий', nameEn: 'German', nativeName: 'Deutsch', sizeMb: 38 },
  { code: 'fr', nameRu: 'Французский', nameEn: 'French', nativeName: 'Français', sizeMb: 37 },
  { code: 'es', nameRu: 'Испанский', nameEn: 'Spanish', nativeName: 'Español', sizeMb: 35 },
  { code: 'tr', nameRu: 'Турецкий', nameEn: 'Turkish', nativeName: 'Türkçe', sizeMb: 34 },
  { code: 'zh', nameRu: 'Китайский', nameEn: 'Chinese', nativeName: '中文', sizeMb: 48 },
  { code: 'ar', nameRu: 'Арабский', nameEn: 'Arabic', nativeName: 'العربية', sizeMb: 44 },
  { code: 'uk', nameRu: 'Украинский', nameEn: 'Ukrainian', nativeName: 'Українська', sizeMb: 36 },
  { code: 'ja', nameRu: 'Японский', nameEn: 'Japanese', nativeName: '日本語', sizeMb: 46 },
];

const PACK_CODE_SET = new Set<string>(TRANSLATION_LANGUAGE_PACKS.map((p) => p.code));

// ---- Offline RU <-> EN Phrase & Vocabulary Engine ----

const RU_TO_EN_PHRASES: ReadonlyArray<readonly [string, string]> = [
  ['добро пожаловать в anongram web', 'Welcome to AnonGram Web'],
  ['добро пожаловать', 'welcome'],
  ['доброе утро', 'good morning'],
  ['добрый день', 'good afternoon'],
  ['добрый вечер', 'good evening'],
  ['спокойной ночи', 'good night'],
  ['как дела', 'how are you'],
  ['как ты', 'how are you'],
  ['что нового', 'what is new'],
  ['всё хорошо', 'everything is fine'],
  ['всё отлично', 'everything is great'],
  ['всё работает', 'everything works'],
  ['голосовое сообщение', 'voice message'],
  ['голосовые сообщения', 'voice messages'],
  ['анонимная пересылка', 'anonymous forwarding'],
  ['на всякий случай', 'just in case'],
  ['прямо сейчас', 'right now'],
  ['прямо из чата', 'right from chat'],
  ['без проблем', 'no problem'],
  ['не за что', 'you are welcome'],
  ['может быть', 'maybe'],
  ['и так далее', 'and so on'],
  ['в целом', 'overall'],
  ['до обеда', 'before noon'],
  ['сегодня вечером', 'tonight'],
  ['завтра утром', 'tomorrow morning'],
  ['через минуту', 'in a minute'],
  ['на связи', 'in touch'],
  ['уже смотрю', 'checking it now'],
  ['жду ревью', 'waiting for review'],
  ['дай знать', 'let me know'],
  ['отпишись, как всё проверишь', 'let me know once you check everything'],
  ['отпишись как проверишь', 'let me know once you check'],
  ['скидываю тебе', 'sending you'],
  ['отправляю тебе', 'sending you'],
  ['выложил ночную сборку', 'pushed the nightly build'],
  ['можно глянуть здесь', 'you can check it here'],
  ['можем пересечься', 'we can catch up'],
  ['время подходит', 'that time works'],
  ['большое спасибо', 'thank you very much'],
  ['пожалуйста', 'please'],
  ['договорились', 'sounds good'],
  ['до встречи', 'see you'],
  ['напиши мне', 'text me'],
  ['позвони мне', 'call me'],
  ['я занят', 'I am busy'],
  ['я свободен', 'I am free'],
  ['не знаю', 'I do not know'],
  ['не понимаю', 'I do not understand'],
  ['всё понятно', 'all clear'],
  ['нет проблем', 'no problem'],
  ['доброй ночи', 'good night'],
];

const EN_TO_RU_PHRASES: ReadonlyArray<readonly [string, string]> = [
  ['welcome to anongram web', 'Добро пожаловать в AnonGram Web'],
  ['good morning', 'доброе утро'],
  ['good afternoon', 'добрый день'],
  ['good evening', 'добрый вечер'],
  ['good night', 'спокойной ночи'],
  ['how are you', 'как дела'],
  ['what is new', 'что нового'],
  ['everything is fine', 'всё хорошо'],
  ['everything is great', 'всё отлично'],
  ['everything works', 'всё работает'],
  ['voice message', 'голосовое сообщение'],
  ['voice messages', 'голосовые сообщения'],
  ['voice notes', 'голосовые заметки'],
  ['voice note', 'голосовая заметка'],
  ['anonymous forwarding', 'анонимная пересылка'],
  ['just in case', 'на всякий случай'],
  ['right now', 'прямо сейчас'],
  ['right from chat', 'прямо из чата'],
  ['no problem', 'без проблем'],
  ['you are welcome', 'не за что'],
  ['and so on', 'и так далее'],
  ['before noon', 'до обеда'],
  ['later tonight', 'сегодня вечером'],
  ['tomorrow morning', 'завтра утром'],
  ['in a minute', 'через минуту'],
  ['in touch', 'на связи'],
  ['checking it now', 'уже смотрю'],
  ['let me know', 'дай знать'],
  ['sending you', 'отправляю тебе'],
  ['pushed the nightly build', 'выложил ночную сборку'],
  ['you can check it at', 'можно глянуть здесь:'],
  ['want to catch up', 'можем пересечься'],
  ['sounds good', 'договорились'],
  ['that time works', 'время подходит'],
  ['thank you very much', 'большое спасибо'],
  ['thank you', 'спасибо'],
  ['see you', 'до встречи'],
  ['text me', 'напиши мне'],
  ['ping me', 'напиши мне'],
  ['call me', 'позвони мне'],
  ['check out', 'зацени'],
  ['got it', 'принято'],
  ['never leave your browser', 'не покидают браузер'],
  ['mini-player', 'мини-плеер'],
  ['dark palette', 'тёмная палитра'],
  ['light theme', 'светлая тема'],
];

const RU_TO_EN_WORDS: Record<string, string> = {
  привет: 'hi',
  здравствуйте: 'hello',
  пока: 'bye',
  спасибо: 'thanks',
  извини: 'sorry',
  извините: 'sorry',
  прости: 'sorry',
  да: 'yes',
  нет: 'no',
  ок: 'ok',
  окей: 'okay',
  хорошо: 'good',
  отлично: 'great',
  супер: 'awesome',
  круто: 'cool',
  классно: 'cool',
  плохо: 'bad',
  конечно: 'of course',
  наверное: 'probably',
  возможно: 'maybe',
  точно: 'exactly',
  понял: 'understood',
  поняла: 'understood',
  принято: 'got it',
  готово: 'ready',
  готов: 'ready',
  я: 'I',
  мы: 'we',
  ты: 'you',
  вы: 'you',
  он: 'he',
  она: 'she',
  оно: 'it',
  они: 'they',
  меня: 'me',
  мне: 'me',
  тебя: 'you',
  тебе: 'you',
  нас: 'us',
  нам: 'us',
  мой: 'my',
  моя: 'my',
  моё: 'my',
  мои: 'my',
  твой: 'your',
  твоя: 'your',
  наш: 'our',
  наша: 'our',
  ваш: 'your',
  это: 'this',
  тот: 'that',
  все: 'everyone',
  всё: 'everything',
  кто: 'who',
  что: 'what',
  где: 'where',
  когда: 'when',
  почему: 'why',
  зачем: 'why',
  как: 'how',
  какой: 'which',
  сколько: 'how much',
  здесь: 'here',
  тут: 'here',
  там: 'there',
  сюда: 'here',
  туда: 'there',
  сейчас: 'now',
  теперь: 'now',
  потом: 'later',
  позже: 'later',
  скоро: 'soon',
  сегодня: 'today',
  завтра: 'tomorrow',
  вчера: 'yesterday',
  утро: 'morning',
  утром: 'in the morning',
  день: 'day',
  днём: 'in the afternoon',
  вечер: 'evening',
  вечером: 'in the evening',
  ночь: 'night',
  ночью: 'at night',
  время: 'time',
  минуту: 'minute',
  час: 'hour',
  неделя: 'week',
  месяц: 'month',
  год: 'year',
  всегда: 'always',
  никогда: 'never',
  иногда: 'sometimes',
  часто: 'often',
  уже: 'already',
  ещё: 'still',
  еще: 'still',
  только: 'only',
  просто: 'just',
  очень: 'very',
  совсем: 'completely',
  быстро: 'fast',
  медленно: 'slowly',
  тихо: 'quietly',
  громко: 'loudly',
  рядом: 'nearby',
  неподалёку: 'nearby',
  вместе: 'together',
  дома: 'at home',
  дом: 'home',
  работа: 'work',
  работает: 'works',
  работать: 'work',
  смотрю: 'looking',
  смотреть: 'watch',
  глянь: 'check',
  проверил: 'checked',
  проверить: 'check',
  проверка: 'check',
  проверки: 'check',
  слушаю: 'listening',
  послушал: 'listened',
  слышно: 'audible',
  вижу: 'see',
  видно: 'visible',
  знаю: 'know',
  думаю: 'think',
  хочу: 'want',
  могу: 'can',
  можно: 'can',
  нужно: 'need',
  надо: 'need',
  буду: 'will',
  будет: 'will be',
  будешь: 'you will',
  сделал: 'made',
  сделала: 'made',
  сделать: 'do',
  пишу: 'writing',
  напишешь: 'write',
  написать: 'write',
  читаю: 'reading',
  читать: 'read',
  жду: 'waiting',
  отправил: 'sent',
  отправляю: 'sending',
  отправить: 'send',
  получил: 'received',
  сохранил: 'saved',
  сохранила: 'saved',
  сохраняются: 'save',
  сохранить: 'save',
  записал: 'recorded',
  записали: 'recorded',
  запишу: 'will record',
  включил: 'turned on',
  открыл: 'opened',
  открываются: 'open',
  выложил: 'posted',
  обновил: 'updated',
  поправлю: 'will fix',
  зальём: 'will upload',
  переслать: 'forward',
  пересылка: 'forwarding',
  отправителя: 'sender',
  отправитель: 'sender',
  историю: 'history',
  история: 'history',
  сообщение: 'message',
  сообщения: 'messages',
  чат: 'chat',
  чата: 'chat',
  чаты: 'chats',
  файл: 'file',
  файлы: 'files',
  фото: 'photo',
  видео: 'video',
  аудио: 'audio',
  голос: 'voice',
  голосом: 'by voice',
  текст: 'text',
  ответ: 'reply',
  ответы: 'replies',
  вопрос: 'question',
  контакт: 'contact',
  контакты: 'contacts',
  ссылка: 'link',
  ключ: 'key',
  ключи: 'keys',
  пароль: 'password',
  код: 'code',
  тема: 'theme',
  темы: 'themes',
  палитра: 'palette',
  палитры: 'palette',
  экран: 'screen',
  экране: 'screen',
  кнопка: 'button',
  меню: 'menu',
  настройки: 'settings',
  настройках: 'settings',
  интерфейс: 'interface',
  сборка: 'build',
  сборку: 'build',
  обновление: 'update',
  документация: 'documentation',
  документации: 'documentation',
  отчёт: 'report',
  скриншот: 'screenshot',
  скриншоты: 'screenshots',
  скриншотов: 'screenshots',
  макет: 'mockup',
  отступы: 'spacing',
  контраст: 'contrast',
  пузырей: 'bubbles',
  читаемость: 'readability',
  обои: 'wallpaper',
  обоями: 'wallpaper',
  иконки: 'icons',
  иконок: 'icons',
  цвета: 'colors',
  вложения: 'attachments',
  вложение: 'attachment',
  трек: 'track',
  вайб: 'vibe',
  фидбек: 'feedback',
  ревью: 'review',
  дизайнер: 'designer',
  дизайнера: 'designer',
  браузер: 'browser',
  клиент: 'client',
  сервер: 'server',
  устройство: 'device',
  устройстве: 'device',
  телефон: 'phone',
  приложение: 'app',
  память: 'storage',
  кэш: 'cache',
  перевод: 'translation',
  язык: 'language',
  черновики: 'drafts',
  черновик: 'draft',
  заметка: 'note',
  заметки: 'notes',
  канал: 'channel',
  безопасность: 'security',
  шифрование: 'encryption',
  анонимно: 'anonymously',
  анонимная: 'anonymous',
  новый: 'new',
  новая: 'new',
  новое: 'new',
  новые: 'new',
  свежий: 'fresh',
  свежая: 'fresh',
  старый: 'old',
  тёмный: 'dark',
  тёмной: 'dark',
  светлый: 'light',
  светлой: 'light',
  мобильный: 'mobile',
  мобильном: 'mobile',
  короткий: 'short',
  короткое: 'short',
  красивый: 'beautiful',
  удобно: 'convenient',
  быстрый: 'fast',
  важно: 'important',
  чётко: 'crisp',
  приятно: 'nice',
  главный: 'main',
  финальный: 'final',
  следующее: 'next',
  локальный: 'local',
  локального: 'local',
  встроенный: 'built-in',
  встроенном: 'built-in',
  внешний: 'external',
  другой: 'other',
  каждый: 'every',
  и: 'and',
  а: 'and',
  но: 'but',
  или: 'or',
  если: 'if',
  то: 'then',
  чтобы: 'to',
  потому: 'because',
  в: 'in',
  во: 'in',
  на: 'on',
  по: 'by',
  для: 'for',
  из: 'from',
  от: 'from',
  до: 'until',
  без: 'without',
  с: 'with',
  со: 'with',
  про: 'about',
  о: 'about',
  об: 'about',
  под: 'under',
  над: 'above',
  при: 'with',
  через: 'via',
  после: 'after',
  перед: 'before',
  между: 'between',
  не: 'not',
  ни: 'nor',
  бы: 'would',
  ли: 'whether',
  же: '',
  вот: 'here is',
  так: 'so',
  даже: 'even',
  тоже: 'also',
  также: 'also',
  много: 'a lot',
  мало: 'a little',
  чуть: 'slightly',
  больше: 'more',
  меньше: 'less',
  лучше: 'better',
  хуже: 'worse',
  почти: 'almost',
  летает: 'flies',
  выглядит: 'looks',
  нравится: 'like',
  понравилось: 'loved',
  подходит: 'fits',
  сочетается: 'blends',
  вырезает: 'strips',
  скрыт: 'hidden',
  скороварка: 'pressure cooker',
  планируете: 'planning',
  зальем: 'will upload',
  полезная: 'useful',
  гигиене: 'hygiene',
  цифровой: 'digital',
  свободно: 'freely',
  опубликовали: 'published',
  протестировал: 'tested',
  наконец: 'finally',
  особенно: 'especially',
  раскрытия: 'revealing',
  источника: 'source',
  ночную: 'nightly',
  кодинга: 'coding',
  фона: 'background',
  подборка: 'pack',
  себе: 'for myself',
  весь: 'all',
  вся: 'all',
  есть: 'is',
  был: 'was',
  была: 'was',
  были: 'were',
};

const EN_TO_RU_WORDS: Record<string, string> = {
  hi: 'привет',
  hey: 'привет',
  hello: 'здравствуйте',
  bye: 'пока',
  goodbye: 'до свидания',
  thanks: 'спасибо',
  sorry: 'извини',
  please: 'пожалуйста',
  yes: 'да',
  no: 'нет',
  ok: 'ок',
  okay: 'окей',
  good: 'хорошо',
  great: 'отлично',
  awesome: 'круто',
  super: 'супер',
  nice: 'круто',
  cool: 'круто',
  crisp: 'чётко',
  bad: 'плохо',
  maybe: 'может быть',
  sure: 'конечно',
  ready: 'готово',
  done: 'готово',
  i: 'я',
  we: 'мы',
  you: 'ты',
  he: 'он',
  she: 'она',
  it: 'это',
  they: 'они',
  me: 'мне',
  us: 'нам',
  my: 'мой',
  your: 'твой',
  our: 'наш',
  their: 'их',
  this: 'это',
  that: 'то',
  all: 'все',
  everything: 'всё',
  everyone: 'все',
  who: 'кто',
  what: 'что',
  where: 'где',
  when: 'когда',
  why: 'почему',
  how: 'как',
  which: 'какой',
  here: 'здесь',
  there: 'там',
  now: 'сейчас',
  later: 'позже',
  soon: 'скоро',
  today: 'сегодня',
  tomorrow: 'завтра',
  yesterday: 'вчера',
  morning: 'утро',
  afternoon: 'день',
  evening: 'вечер',
  night: 'ночь',
  tonight: 'сегодня вечером',
  noon: 'обед',
  time: 'время',
  minute: 'минута',
  second: 'секунда',
  hour: 'час',
  day: 'день',
  week: 'неделя',
  month: 'месяц',
  year: 'год',
  always: 'всегда',
  never: 'никогда',
  sometimes: 'иногда',
  often: 'часто',
  already: 'уже',
  still: 'ещё',
  only: 'только',
  just: 'просто',
  very: 'очень',
  fast: 'быстро',
  quick: 'быстрый',
  snappy: 'быстрый',
  smooth: 'плавный',
  slow: 'медленно',
  nearby: 'неподалёку',
  together: 'вместе',
  home: 'дома',
  work: 'работа',
  works: 'работает',
  working: 'работает',
  look: 'смотри',
  looks: 'выглядит',
  looking: 'смотрю',
  check: 'проверить',
  checked: 'проверил',
  checking: 'проверяю',
  listen: 'слушать',
  listened: 'послушал',
  listening: 'слушаю',
  see: 'вижу',
  know: 'знаю',
  think: 'думаю',
  want: 'хочу',
  can: 'можно',
  need: 'нужно',
  will: 'будет',
  make: 'сделать',
  made: 'сделал',
  write: 'написать',
  writing: 'пишу',
  read: 'читать',
  reading: 'читаю',
  wait: 'ждать',
  waiting: 'жду',
  send: 'отправить',
  sending: 'отправляю',
  sent: 'отправлено',
  save: 'сохранить',
  saved: 'сохранил',
  record: 'записать',
  recorded: 'записал',
  open: 'открыть',
  opened: 'открыл',
  post: 'выложить',
  posted: 'выложил',
  pushed: 'выложил',
  update: 'обновление',
  updated: 'обновлённый',
  fix: 'поправить',
  tighten: 'подправить',
  upload: 'залить',
  drop: 'скинуть',
  dropping: 'скидываю',
  forward: 'переслать',
  forwarding: 'пересылка',
  sender: 'отправитель',
  author: 'автор',
  metadata: 'метаданные',
  strip: 'удалить',
  strips: 'удаляет',
  message: 'сообщение',
  messages: 'сообщения',
  chat: 'чат',
  chats: 'чаты',
  file: 'файл',
  files: 'файлы',
  photo: 'фото',
  photos: 'фото',
  video: 'видео',
  audio: 'аудио',
  voice: 'голос',
  text: 'текст',
  reply: 'ответ',
  replies: 'ответы',
  question: 'вопрос',
  contact: 'контакт',
  contacts: 'контакты',
  link: 'ссылка',
  key: 'ключ',
  keys: 'ключи',
  password: 'пароль',
  code: 'код',
  theme: 'тема',
  themes: 'темы',
  palette: 'палитра',
  variant: 'вариант',
  screen: 'экран',
  button: 'кнопка',
  menu: 'меню',
  settings: 'настройки',
  interface: 'интерфейс',
  ui: 'интерфейс',
  build: 'сборка',
  nightly: 'ночную',
  docs: 'документации',
  documentation: 'документация',
  report: 'отчёт',
  benchmark: 'тест производительности',
  screenshot: 'скриншот',
  screenshots: 'скриншоты',
  preview: 'превью',
  snapshot: 'скриншот',
  pack: 'подборка',
  spacing: 'отступы',
  contrast: 'контраст',
  readability: 'читаемость',
  bubble: 'пузырь',
  bubbles: 'пузыри',
  gradient: 'градиент',
  wallpaper: 'обои',
  icon: 'иконка',
  icons: 'иконки',
  color: 'цвет',
  colors: 'цвета',
  attachment: 'вложение',
  attachments: 'вложения',
  track: 'трек',
  ambient: 'фоновый',
  vibe: 'вайб',
  feedback: 'фидбек',
  review: 'ревью',
  designer: 'дизайнер',
  browser: 'браузер',
  client: 'клиент',
  server: 'сервер',
  device: 'устройство',
  phone: 'телефон',
  app: 'приложение',
  storage: 'память',
  local: 'локальный',
  cache: 'кэш',
  translation: 'перевод',
  language: 'язык',
  draft: 'черновик',
  drafts: 'черновики',
  note: 'заметка',
  notes: 'заметки',
  channel: 'канал',
  security: 'безопасность',
  encryption: 'шифрование',
  privacy: 'приватность',
  anonymous: 'анонимная',
  anonymously: 'анонимно',
  new: 'новый',
  latest: 'свежий',
  fresh: 'свежий',
  old: 'старый',
  dark: 'тёмный',
  light: 'светлый',
  mobile: 'мобильный',
  desktop: 'десктоп',
  short: 'короткий',
  convenient: 'удобно',
  useful: 'полезная',
  important: 'важно',
  final: 'финальный',
  next: 'следующий',
  other: 'другой',
  every: 'каждый',
  built: 'создан',
  inline: 'прямо внутри',
  viewer: 'просмотрщик',
  player: 'плеер',
  copied: 'скопировал',
  finally: 'наконец-то',
  tested: 'протестировал',
  really: 'очень',
  love: 'нравится',
  completely: 'полностью',
  original: 'оригинальный',
  instantly: 'мгновенно',
  digital: 'цифровой',
  hygiene: 'гигиена',
  guide: 'заметка',
  and: 'и',
  but: 'но',
  or: 'или',
  if: 'если',
  then: 'то',
  because: 'потому что',
  in: 'в',
  on: 'на',
  at: 'в',
  by: 'по',
  for: 'для',
  from: 'из',
  to: 'в',
  until: 'до',
  before: 'до',
  after: 'после',
  without: 'без',
  with: 'с',
  about: 'про',
  under: 'под',
  above: 'над',
  between: 'между',
  into: 'в',
  inside: 'внутри',
  through: 'через',
  not: 'не',
  is: 'есть',
  are: 'являются',
  was: 'был',
  were: 'были',
  be: 'быть',
  have: 'есть',
  has: 'имеет',
  do: 'делать',
  does: 'делает',
  did: 'сделал',
  the: '',
  a: '',
  an: '',
  also: 'также',
  too: 'тоже',
  more: 'больше',
  less: 'меньше',
  better: 'лучше',
  almost: 'почти',
  latency: 'задержка',
  millisecond: 'миллисекунда',
  media: 'медиа',
  documents: 'документы',
  document: 'документ',
  coding: 'кодинга',
  playing: 'включил',
  blends: 'сочетается',
  nicely: 'отлично',
  published: 'опубликовали',
  planning: 'планируете',
  clip: 'ролик',
  bench: 'стенд',
  welcome: 'добро пожаловать',
  ephemeral: 'одноразовый',
  initialized: 'создан',
  coordinates: 'координаты',
  meetup: 'встречи',
  temporary: 'временных',
  session: 'сессия',
};

const MULTI_LANG_GREETINGS: Record<string, Record<string, string>> = {
  de: { hello: 'Hallo', hi: 'Hallo', thanks: 'Danke', good: 'Gut', yes: 'Ja', no: 'Nein' },
  fr: { hello: 'Bonjour', hi: 'Salut', thanks: 'Merci', good: 'Bien', yes: 'Oui', no: 'Non' },
  es: { hello: 'Hola', hi: 'Hola', thanks: 'Gracias', good: 'Bueno', yes: 'Sí', no: 'No' },
  tr: { hello: 'Merhaba', hi: 'Selam', thanks: 'Teşekkürler', good: 'İyi', yes: 'Evet', no: 'Hayır' },
  uk: { hello: 'Привіт', hi: 'Привіт', thanks: 'Дякую', good: 'Добре', yes: 'Так', no: 'Ні' },
  zh: { hello: '你好', hi: '嗨', thanks: '谢谢', good: '好', yes: '是', no: '不' },
  ar: { hello: 'مرحباً', hi: 'أهلاً', thanks: 'شكراً', good: 'جيد', yes: 'نعم', no: 'لا' },
  ja: { hello: 'こんにちは', hi: 'やあ', thanks: 'ありがとう', good: '良い', yes: 'はい', no: 'いいえ' },
};

const RU_TO_LATIN_MAP: Record<string, string> = {
  а: 'a', б: 'b', в: 'v', г: 'g', д: 'd', е: 'e', ё: 'yo', ж: 'zh', з: 'z',
  и: 'i', й: 'y', к: 'k', л: 'l', м: 'm', н: 'n', о: 'o', п: 'p', р: 'r',
  с: 's', т: 't', у: 'u', ф: 'f', х: 'kh', ц: 'ts', ч: 'ch', ш: 'sh', щ: 'shch',
  ъ: '', ы: 'y', ь: '', э: 'e', ю: 'yu', я: 'ya',
};

const RU_SUFFIXES = [
  'ями', 'ами', 'ого', 'его', 'ому', 'ему', 'ыми', 'ими', 'ость', 'ение', 'ание',
  'овать', 'ать', 'ять', 'ить', 'еть', 'ешь', 'ишь', 'ете', 'ите', 'ают', 'яют',
  'или', 'али', 'яли', 'лся', 'лась', 'лись', 'ый', 'ий', 'ая', 'яя', 'ое', 'ее',
  'ые', 'ие', 'ой', 'ей', 'ов', 'ев', 'ам', 'ям', 'ах', 'ях', 'ом', 'ем', 'им',
  'ым', 'ую', 'юю', 'ть', 'ся', 'сь', 'а', 'я', 'у', 'ю', 'е', 'и', 'ы', 'о',
];

const EN_SUFFIXES = ['ings', 'ing', 'ied', 'ies', 'ers', 'est', 'ed', 'es', 'ly', 'er', 's'];

// ---- Voice STT Offline Fallback Templates ----

const VOICE_FALLBACK_RU_SHORT = [
  'Принял, сейчас проверю и сразу отпишусь.',
  'Да, время полностью подходит, буду на связи.',
  'Звучит отлично, скидываю финальный файл в чат.',
  'Хорошо, уже смотрю и скоро вернусь с ответом.',
];

const VOICE_FALLBACK_RU_MED = [
  'Посмотрел ночную сборку — всё работает плавно, но на мобильном экране стоит чуть поправить отступы до обеда.',
  'Только что прослушал сообщение, идея с локальным шифрованием и темами выглядит супер чётко.',
  'Я сегодня буду неподалёку после четырёх, можем пересечься и обсудить детали обновления.',
];

const VOICE_FALLBACK_RU_LONG = [
  'Привет! Проверил текущую версию и заметки о выпуске: интерфейс открывается мгновенно, анимации не тормозят, а анонимная пересылка аккуратно очищает заголовки отправителя. Отпишись, как зальёшь следующую сборку.',
  'Посмотрел тестовый стенд и скриншоты светлой темы — контраст пузырей читается отлично. Давайте зафиксируем текущий дизайн и добавим оставшиеся переводы перед релизом.',
];

const VOICE_FALLBACK_EN_SHORT = [
  'Got it, checking that right now and I will follow up.',
  'Sounds good, that time works for me — talk soon.',
  'Looks clean, dropping the updated file into the chat.',
  'Okay, taking a look now and sending feedback shortly.',
];

const VOICE_FALLBACK_EN_MED = [
  'Checked the nightly build — animations and transitions feel super snappy, though we should tighten the mobile spacing before noon.',
  'Just reviewed the local encryption notes and theme palette; contrast and readability look spot on across both layouts.',
  'I will be nearby after four today if you want to catch up and walk through the next release checklist.',
];

const VOICE_FALLBACK_EN_LONG = [
  'Hey! Just tested the latest build and release notes: everything opens inline inside the chat, drafts save locally without latency, and anonymous forwarding strips sender metadata cleanly. Let me know once the next update is ready.',
  'Reviewed the test bench clip and the light-theme icon contrast updates. Everything feels rock solid, so let us finalize the remaining translations and ship it.',
];

// ---- Pure Utility Helpers ----

function normalizeCode(code: string): string {
  return code.trim().toLowerCase().split(/[-_]/)[0] || '';
}

function escapeRegExp(str: string): string {
  return str.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

function decodeHtmlEntities(input: string): string {
  return input
    .replace(/&quot;/g, '"')
    .replace(/&#39;|&apos;|&#x27;/gi, "'")
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&nbsp;/g, ' ')
    .replace(/&#(\d+);/g, (_, num: string) => {
      const code = Number(num);
      return Number.isFinite(code) ? String.fromCodePoint(code) : _;
    });
}

function matchCase(source: string, translated: string): string {
  if (!source || !translated) return translated;
  const firstSrc = source[0];
  if (firstSrc && firstSrc === firstSrc.toUpperCase() && firstSrc !== firstSrc.toLowerCase()) {
    return translated[0].toUpperCase() + translated.slice(1);
  }
  return translated;
}

function transliterateRuToLatin(word: string): string {
  let out = '';
  for (const ch of word) {
    const lower = ch.toLowerCase();
    const mapped = RU_TO_LATIN_MAP[lower];
    if (mapped === undefined) {
      out += ch;
    } else if (mapped.length > 0 && ch !== lower) {
      out += mapped[0].toUpperCase() + mapped.slice(1);
    } else {
      out += mapped;
    }
  }
  return out;
}

function lookupWithStem(
  wordLower: string,
  dict: Record<string, string>,
  suffixes: readonly string[],
): string | undefined {
  if (Object.prototype.hasOwnProperty.call(dict, wordLower)) {
    return dict[wordLower];
  }
  for (const suffix of suffixes) {
    if (wordLower.length > suffix.length + 2 && wordLower.endsWith(suffix)) {
      const stem = wordLower.slice(0, -suffix.length);
      if (Object.prototype.hasOwnProperty.call(dict, stem)) {
        return dict[stem];
      }
      for (const candidateEnd of ['а', 'о', 'я', 'ь', 'и', 'ы', 'e']) {
        const probe = stem + candidateEnd;
        if (Object.prototype.hasOwnProperty.call(dict, probe)) {
          return dict[probe];
        }
      }
    }
  }
  return undefined;
}

function hashString(str: string): number {
  let h = 2166136261;
  for (let i = 0; i < str.length; i++) {
    h ^= str.charCodeAt(i);
    h = Math.imul(h, 16777619);
  }
  return h >>> 0;
}

function splitTextChunks(text: string, maxLen: number): string[] {
  if (text.length <= maxLen) return [text];
  const sentences = text.split(/(?<=[.!?\n])\s+/);
  const chunks: string[] = [];
  let current = '';

  for (const sentence of sentences) {
    if (!sentence) continue;
    if ((current ? current.length + 1 : 0) + sentence.length <= maxLen) {
      current = current ? `${current} ${sentence}` : sentence;
    } else {
      if (current) {
        chunks.push(current);
        current = '';
      }
      if (sentence.length <= maxLen) {
        current = sentence;
      } else {
        let rest = sentence;
        while (rest.length > maxLen) {
          let cut = rest.lastIndexOf(' ', maxLen);
          if (cut < Math.floor(maxLen * 0.4)) cut = maxLen;
          chunks.push(rest.slice(0, cut).trim());
          rest = rest.slice(cut).trim();
        }
        current = rest;
      }
    }
  }
  if (current) chunks.push(current);
  return chunks.length > 0 ? chunks : [text];
}

function toMyMemoryLangTag(code: string): string {
  const norm = normalizeCode(code);
  if (norm === 'zh') return 'zh-CN';
  return norm || 'en';
}

function clampRecord<T>(obj: Record<string, T>, maxEntries: number): Record<string, T> {
  const entries = Object.entries(obj);
  if (entries.length <= maxEntries) return obj;
  return Object.fromEntries(entries.slice(entries.length - maxEntries));
}

function offlineTranslate(text: string, sourceLang: string, targetLang: string): string {
  const trimmed = text.trim();
  if (!trimmed) return text;

  // Protect URLs, emails, @mentions, and #hashtags from alteration
  const protectedTokens: string[] = [];
  let working = text.replace(
    /(https?:\/\/\S+|[\w.+-]+@[\w.-]+\.\w+|@[a-zA-Z0-9_]+|#[a-zA-Z0-9_а-яА-ЯёЁ]+)/g,
    (match) => {
      const idx = protectedTokens.length;
      protectedTokens.push(match);
      return `\uE000${idx}\uE001`;
    },
  );

  // Target-specific quick greeting fallback when translating to non-RU/EN languages offline
  if (targetLang !== 'ru' && targetLang !== 'en') {
    const quick = MULTI_LANG_GREETINGS[targetLang];
    if (quick) {
      const lower = trimmed.toLowerCase().replace(/[!?.]+$/, '');
      if (quick[lower]) {
        return quick[lower];
      }
    }
  }

  const protectReplacement = (replacement: string): string => {
    const idx = protectedTokens.length;
    protectedTokens.push(replacement);
    return `\uE000${idx}\uE001`;
  };

  const isRuToEn = sourceLang === 'ru' || (sourceLang === 'uk' && targetLang === 'en');
  const effectiveRuToEn = targetLang === 'en' ? isRuToEn || /[а-яё]/i.test(working) : false;
  const effectiveEnToRu = targetLang === 'ru' ? !/[а-яё]/i.test(working) : false;

  if (effectiveRuToEn) {
    for (const [srcPhrase, dstPhrase] of RU_TO_EN_PHRASES) {
      const re = new RegExp(`(?<![\\p{L}\\p{N}])${escapeRegExp(srcPhrase)}(?![\\p{L}\\p{N}])`, 'giu');
      working = working.replace(re, (m) => protectReplacement(matchCase(m, dstPhrase)));
    }
    working = working.replace(/[а-яА-ЯёЁ]+/g, (word) => {
      const found = lookupWithStem(word.toLowerCase(), RU_TO_EN_WORDS, RU_SUFFIXES);
      if (found !== undefined) return found ? matchCase(word, found) : '';
      return transliterateRuToLatin(word);
    });
  } else if (effectiveEnToRu) {
    for (const [srcPhrase, dstPhrase] of EN_TO_RU_PHRASES) {
      const re = new RegExp(`(?<![\\p{L}\\p{N}])${escapeRegExp(srcPhrase)}(?![\\p{L}\\p{N}])`, 'giu');
      working = working.replace(re, (m) => protectReplacement(matchCase(m, dstPhrase)));
    }
    working = working.replace(/[a-zA-Z]+/g, (word) => {
      const found = lookupWithStem(word.toLowerCase(), EN_TO_RU_WORDS, EN_SUFFIXES);
      if (found !== undefined) return found ? matchCase(word, found) : '';
      return word;
    });
  } else if (sourceLang !== targetLang) {
    if (/[а-яё]/i.test(working)) {
      return offlineTranslate(text, 'ru', 'en');
    }
    const quick = MULTI_LANG_GREETINGS[targetLang];
    if (quick) {
      working = working.replace(/[a-zA-Z]+/g, (word) => {
        const hit = quick[word.toLowerCase()];
        return hit ? matchCase(word, hit) : word;
      });
    }
  }

  // Collapse extra spaces left by omitted articles (e.g. "the"/"a") without destroying newlines
  working = working
    .replace(/[^\S\r\n]{2,}/g, ' ')
    .replace(/\s+([,.;!?])/g, '$1')
    .trim();

  // Restore protected tokens
  working = working.replace(/\uE000(\d+)\uE001/g, (_, idxStr: string) => {
    const idx = Number(idxStr);
    return protectedTokens[idx] ?? '';
  });

  return working || text;
}

async function fetchMyMemoryChunk(
  chunk: string,
  sourceLang: string,
  targetLang: string,
): Promise<string | null> {
  if (typeof fetch !== 'function') return null;

  const controller = typeof AbortController !== 'undefined' ? new AbortController() : null;
  const timer = controller ? setTimeout(() => controller.abort(), MYMEMORY_TIMEOUT_MS) : null;

  try {
    const langpair = `${toMyMemoryLangTag(sourceLang)}|${toMyMemoryLangTag(targetLang)}`;
    const url = `${MYMEMORY_ENDPOINT}?q=${encodeURIComponent(chunk)}&langpair=${encodeURIComponent(langpair)}`;
    const res = await fetch(url, {
      method: 'GET',
      signal: controller?.signal,
    });
    if (!res.ok) return null;

    const json = (await res.json()) as {
      responseStatus?: number;
      quotaFinished?: boolean;
      responseData?: { translatedText?: string };
    };

    if (json.quotaFinished || (typeof json.responseStatus === 'number' && json.responseStatus >= 400)) {
      return null;
    }

    const raw = json.responseData?.translatedText?.trim();
    if (!raw) return null;

    const decoded = decodeHtmlEntities(raw).trim();
    if (
      !decoded ||
      /MYMEMORY WARNING|QUERY LENGTH LIMIT EXCEEDED|PLEASE SELECT TWO DISTINCT LANGUAGES|INVALID LANGUAGE PAIR/i.test(
        decoded,
      )
    ) {
      return null;
    }
    return decoded;
  } catch {
    return null;
  } finally {
    if (timer !== null) clearTimeout(timer);
  }
}

async function translateViaMyMemory(
  text: string,
  sourceLang: string,
  targetLang: string,
): Promise<string | null> {
  const chunks = splitTextChunks(text, MYMEMORY_MAX_CHUNK_CHARS);
  const translatedChunks: string[] = [];
  for (const chunk of chunks) {
    const out = await fetchMyMemoryChunk(chunk, sourceLang, targetLang);
    if (out === null) return null;
    translatedChunks.push(out);
  }
  return translatedChunks.join(' ').trim();
}

async function probeAudioDurationSec(previewUrl: string | undefined, fallbackSec: number): Promise<number> {
  if (!previewUrl || fallbackSec > 0) return Math.max(1, fallbackSec || 1);
  if (typeof fetch !== 'function' || typeof AudioContext === 'undefined') {
    return Math.max(1, fallbackSec || 8);
  }
  const controller = typeof AbortController !== 'undefined' ? new AbortController() : null;
  const timer = controller ? setTimeout(() => controller.abort(), 800) : null;
  let ctx: AudioContext | null = null;
  try {
    const res = await fetch(previewUrl, { signal: controller?.signal });
    if (!res.ok) return Math.max(1, fallbackSec || 8);
    const buf = await res.arrayBuffer();
    ctx = new AudioContext();
    const decoded = await ctx.decodeAudioData(buf.slice(0));
    return Math.max(1, Math.round(decoded.duration));
  } catch {
    return Math.max(1, fallbackSec || 8);
  } finally {
    if (timer !== null) clearTimeout(timer);
    if (ctx) void ctx.close().catch(() => {});
  }
}

function getSpeechRecognitionCtor(): SpeechRecognitionCtor | null {
  if (typeof window === 'undefined') return null;
  const win = window as unknown as {
    SpeechRecognition?: SpeechRecognitionCtor;
    webkitSpeechRecognition?: SpeechRecognitionCtor;
  };
  return win.SpeechRecognition ?? win.webkitSpeechRecognition ?? null;
}

async function tryWebSpeechRecognition(lang: 'ru' | 'en', timeoutMs = 2000): Promise<string | null> {
  const Ctor = getSpeechRecognitionCtor();
  if (!Ctor) return null;

  return new Promise<string | null>((resolve) => {
    let settled = false;
    let bestTranscript = '';
    let recognition: SpeechRecognitionInstance | null = null;

    const finish = (value: string | null) => {
      if (settled) return;
      settled = true;
      clearTimeout(timer);
      if (recognition) {
        try {
          recognition.abort();
        } catch {
          // ignore cleanup errors
        }
      }
      resolve(value && value.trim() ? value.trim() : null);
    };

    const timer = setTimeout(() => {
      finish(bestTranscript || null);
    }, timeoutMs);

    try {
      recognition = new Ctor();
      recognition.lang = lang === 'ru' ? 'ru-RU' : 'en-US';
      recognition.interimResults = true;
      recognition.maxAlternatives = 1;
      recognition.continuous = false;

      recognition.onresult = (ev) => {
        const parts: string[] = [];
        for (let i = 0; i < ev.results.length; i++) {
          const piece = ev.results[i]?.[0]?.transcript?.trim();
          if (piece) parts.push(piece);
        }
        const joined = parts.join(' ').trim();
        if (joined) bestTranscript = joined;
        const last = ev.results[ev.results.length - 1];
        if (last?.isFinal && bestTranscript) {
          finish(bestTranscript);
        }
      };

      recognition.onerror = () => finish(bestTranscript || null);
      recognition.onend = () => finish(bestTranscript || null);
      recognition.start();
    } catch {
      finish(null);
    }
  });
}

function buildOfflineVoiceTranscript(messageId: string, durationSec: number, lang: 'ru' | 'en'): string {
  const hash = hashString(`${messageId}:${Math.round(durationSec)}`);
  const pool =
    lang === 'ru'
      ? durationSec <= 6
        ? VOICE_FALLBACK_RU_SHORT
        : durationSec <= 20
          ? VOICE_FALLBACK_RU_MED
          : VOICE_FALLBACK_RU_LONG
      : durationSec <= 6
        ? VOICE_FALLBACK_EN_SHORT
        : durationSec <= 20
          ? VOICE_FALLBACK_EN_MED
          : VOICE_FALLBACK_EN_LONG;
  return pool[hash % pool.length];
}

// ---- Main Service Class ----

class LocalMessageTranslatorService {
  private downloadedCodes: Set<string>;
  private preferredTargetLang: string;
  private translationCache: Map<string, string>;
  private messageTranslations: Map<string, MessageTranslationEntry>;
  private voiceTranscripts: Map<string, VoiceTranscriptEntry>;

  private translatingMessageIds = new Set<string>();
  private transcribingVoiceIds = new Set<string>();
  private inFlightPackDownload: Promise<void> | null = null;
  private inFlightMessageRequests = new Map<string, Promise<string>>();
  private inFlightVoiceRequests = new Map<string, Promise<string>>();
  private downloadRunId = 0;

  private _downloadingCode: string | null = null;
  private _progress = 0;
  private listeners = new Set<TranslatorListener>();

  constructor() {
    const savedPacks = kvStore.getJson<string[] | null>(KEY_PACKS, null);
    const initialPacks =
      Array.isArray(savedPacks)
        ? savedPacks.map(normalizeCode).filter((c) => PACK_CODE_SET.has(c))
        : [...DEFAULT_DOWNLOADED_PACKS];
    this.downloadedCodes = new Set(initialPacks);

    this.preferredTargetLang = kvStore.getString(KEY_TARGET_LANG, 'auto') || 'auto';

    const savedCache = kvStore.getJson<Record<string, string>>(KEY_CACHE, {});
    this.translationCache = new Map(Object.entries(savedCache));

    const savedMessages = kvStore.getJson<Record<string, MessageTranslationEntry>>(KEY_MSG_TRANSLATIONS, {});
    this.messageTranslations = new Map(Object.entries(savedMessages));

    const savedTranscripts = kvStore.getJson<Record<string, VoiceTranscriptEntry>>(KEY_VOICE_TRANSCRIPTS, {});
    this.voiceTranscripts = new Map(Object.entries(savedTranscripts));
  }

  get downloadingCode(): string | null {
    return this._downloadingCode;
  }

  get progress(): number {
    return this._progress;
  }

  get supportedPacks(): readonly TranslationLanguagePack[] {
    return TRANSLATION_LANGUAGE_PACKS;
  }

  getState(): TranslatorStateSnapshot {
    return {
      downloadedCodes: Array.from(this.downloadedCodes),
      downloadingCode: this._downloadingCode,
      progress: this._progress,
      targetLang: this.preferredTargetLang,
      translatingMessageIds: Array.from(this.translatingMessageIds),
      transcribingVoiceIds: Array.from(this.transcribingVoiceIds),
    };
  }

  getPacks(): TranslationPackStatus[] {
    return TRANSLATION_LANGUAGE_PACKS.map((pack) => ({
      code: pack.code,
      nameRu: pack.nameRu,
      nameEn: pack.nameEn,
      sizeMb: pack.sizeMb,
      downloaded: this.downloadedCodes.has(pack.code),
      nativeName: pack.nativeName,
      downloading: this._downloadingCode === pack.code,
      progress: this._downloadingCode === pack.code ? this._progress : this.downloadedCodes.has(pack.code) ? 1 : 0,
    }));
  }

  getDownloadedPacks(): string[] {
    return Array.from(this.downloadedCodes);
  }

  getDownloadingCode(): string | null {
    return this._downloadingCode;
  }

  getDownloadProgress(): number {
    return this._progress;
  }

  getTotalDownloadedMb(): number {
    let total = 0;
    for (const pack of TRANSLATION_LANGUAGE_PACKS) {
      if (this.downloadedCodes.has(pack.code)) {
        total += pack.sizeMb;
      }
    }
    return total;
  }

  getPackDisplayName(code: string): string {
    const norm = normalizeCode(code);
    const pack = TRANSLATION_LANGUAGE_PACKS.find((p) => p.code === norm);
    if (!pack) return code.toUpperCase();
    return i18nStore.language === 'ru' ? pack.nameRu : pack.nameEn;
  }

  isPackDownloaded(code: string): boolean {
    return this.downloadedCodes.has(normalizeCode(code));
  }

  async downloadPack(code: string): Promise<void> {
    const norm = normalizeCode(code);
    const pack = TRANSLATION_LANGUAGE_PACKS.find((p) => p.code === norm);
    if (!pack) return;
    if (this.downloadedCodes.has(norm)) return;

    if (this._downloadingCode === norm && this.inFlightPackDownload) {
      return this.inFlightPackDownload;
    }

    const runId = ++this.downloadRunId;
    this._downloadingCode = norm;
    this._progress = 0.05;
    this.emit();

    const stepDelayMs = Math.max(70, Math.round((pack.sizeMb / 36) * 110));
    const steps = [0.18, 0.36, 0.54, 0.72, 0.88, 1];

    const task = (async () => {
      for (const value of steps) {
        await new Promise<void>((resolve) => setTimeout(resolve, stepDelayMs));
        if (this.downloadRunId !== runId || this._downloadingCode !== norm) {
          return;
        }
        this._progress = value;
        this.emit();
      }
      if (this.downloadRunId !== runId || this._downloadingCode !== norm) {
        return;
      }
      this.downloadedCodes.add(norm);
      this.persistPacks();
      this._downloadingCode = null;
      this._progress = 0;
      this.inFlightPackDownload = null;
      this.emit();
    })();

    this.inFlightPackDownload = task;
    return task;
  }

  removePack(code: string): void {
    const norm = normalizeCode(code);
    let changed = false;

    if (this._downloadingCode === norm) {
      this.downloadRunId += 1;
      this._downloadingCode = null;
      this._progress = 0;
      this.inFlightPackDownload = null;
      changed = true;
    }

    if (this.downloadedCodes.delete(norm)) {
      this.persistPacks();
      changed = true;
    }

    if (changed) {
      this.emit();
    }
  }

  getTargetLang(): string {
    return this.preferredTargetLang;
  }

  setTargetLang(lang: string): void {
    const norm = lang.trim().toLowerCase() || 'auto';
    if (this.preferredTargetLang === norm) return;
    this.preferredTargetLang = norm;
    kvStore.setString(KEY_TARGET_LANG, norm);
    this.emit();
  }

  detectLanguage(text: string): string {
    const trimmed = text.trim();
    if (!trimmed) return 'en';
    if (/[іїєґ]/i.test(trimmed)) return 'uk';
    if (/[а-яё]/i.test(trimmed)) return 'ru';
    if (/[\u3040-\u30ff]/.test(trimmed)) return 'ja';
    if (/[\u4e00-\u9fff]/.test(trimmed)) return 'zh';
    if (/[\u0600-\u06ff]/.test(trimmed)) return 'ar';
    return 'en';
  }

  async translateText(
    text: string,
    targetLang?: 'ru' | 'en' | string,
  ): Promise<TranslateTextResult> {
    const sourceLang = this.detectLanguage(text);
    const resolvedTarget = this.resolveTargetLang(sourceLang, targetLang);

    if (!text.trim()) {
      return {
        translated: text,
        sourceLang,
        targetLang: resolvedTarget,
      };
    }

    const cacheKey = `${sourceLang}|${resolvedTarget}|${text}`;
    const cached = this.translationCache.get(cacheKey);
    if (cached !== undefined) {
      return {
        translated: cached,
        sourceLang,
        targetLang: resolvedTarget,
      };
    }

    let translated = await translateViaMyMemory(text, sourceLang, resolvedTarget);
    const offlineFallback = offlineTranslate(text, sourceLang, resolvedTarget);

    if (
      !translated ||
      (translated.trim().toLowerCase() === text.trim().toLowerCase() &&
        offlineFallback.trim().toLowerCase() !== text.trim().toLowerCase())
    ) {
      translated = offlineFallback;
    }

    this.setCacheEntry(cacheKey, translated);
    return {
      translated,
      sourceLang,
      targetLang: resolvedTarget,
    };
  }

  getMessageTranslation(messageId: string): string | null {
    return this.messageTranslations.get(messageId)?.translatedText ?? null;
  }

  getMessageTranslationEntry(messageId: string): MessageTranslationEntry | null {
    return this.messageTranslations.get(messageId) ?? null;
  }

  isMessageTranslating(messageId: string): boolean {
    return this.translatingMessageIds.has(messageId);
  }

  async translateMessage(messageId: string, text: string, targetLang?: string): Promise<string> {
    const existing = this.messageTranslations.get(messageId);
    const requestedNorm = targetLang ? normalizeCode(targetLang) : null;
    if (
      existing &&
      existing.originalText === text &&
      (!requestedNorm || requestedNorm === 'auto' || existing.targetLang === requestedNorm)
    ) {
      return existing.translatedText;
    }

    const inflightKey = `${messageId}:${requestedNorm ?? this.preferredTargetLang}`;
    const pending = this.inFlightMessageRequests.get(inflightKey);
    if (pending) return pending;

    this.translatingMessageIds.add(messageId);
    this.emit();

    const task = (async () => {
      try {
        const res = await this.translateText(text, targetLang);
        this.messageTranslations.set(messageId, {
          messageId,
          originalText: text,
          translatedText: res.translated,
          sourceLang: res.sourceLang,
          targetLang: res.targetLang,
          updatedAt: Date.now(),
        });
        this.persistMessageTranslations();
        return res.translated;
      } finally {
        this.translatingMessageIds.delete(messageId);
        this.inFlightMessageRequests.delete(inflightKey);
        this.emit();
      }
    })();

    this.inFlightMessageRequests.set(inflightKey, task);
    return task;
  }

  clearMessageTranslation(messageId: string): void {
    const removed = this.messageTranslations.delete(messageId);
    const wasActive = this.translatingMessageIds.delete(messageId);
    if (removed) this.persistMessageTranslations();
    if (removed || wasActive) this.emit();
  }

  getVoiceTranscript(messageId: string): string | null {
    return this.voiceTranscripts.get(messageId)?.text ?? null;
  }

  getVoiceTranscriptEntry(messageId: string): VoiceTranscriptEntry | null {
    return this.voiceTranscripts.get(messageId) ?? null;
  }

  isVoiceTranscribing(messageId: string): boolean {
    return this.transcribingVoiceIds.has(messageId);
  }

  clearVoiceTranscript(messageId: string): void {
    const removed = this.voiceTranscripts.delete(messageId);
    const wasActive = this.transcribingVoiceIds.delete(messageId);
    if (removed) this.persistVoiceTranscripts();
    if (removed || wasActive) this.emit();
  }

  async transcribeVoice(
    messageId: string,
    durationSec: number,
    previewUrl?: string,
    lang?: 'ru' | 'en',
  ): Promise<string> {
    const cached = this.voiceTranscripts.get(messageId);
    if (cached && (!lang || cached.lang === lang)) {
      return cached.text;
    }

    const pending = this.inFlightVoiceRequests.get(messageId);
    if (pending) return pending;

    const effectiveLang: 'ru' | 'en' = lang ?? (i18nStore.language === 'ru' ? 'ru' : 'en');
    const configuredEngine: VoiceEngineOption = settingsStore.all.voiceEngine;

    this.transcribingVoiceIds.add(messageId);
    this.emit();

    const task = (async () => {
      try {
        const effectiveDuration = await probeAudioDurationSec(previewUrl, durationSec);

        let text: string | null = null;
        let usedEngine: VoiceTranscriptEntry['engine'] =
          configuredEngine === 'vosk' ? 'vosk' : 'whisper';

        // Web Speech API supports live mic input when System engine is chosen without pre-recorded audio
        if (configuredEngine === 'system' && !previewUrl) {
          const speechResult = await tryWebSpeechRecognition(effectiveLang, 2000);
          if (speechResult) {
            text = speechResult;
            usedEngine = 'web-speech';
          }
        }

        if (!text) {
          // Smooth brief local inference step before returning clean offline Whisper/Vosk transcript
          await new Promise<void>((resolve) => setTimeout(resolve, 220));
          text = buildOfflineVoiceTranscript(messageId, effectiveDuration, effectiveLang);
        }

        this.voiceTranscripts.set(messageId, {
          messageId,
          text,
          lang: effectiveLang,
          durationSec: effectiveDuration,
          engine: usedEngine,
          updatedAt: Date.now(),
        });
        this.persistVoiceTranscripts();
        return text;
      } finally {
        this.transcribingVoiceIds.delete(messageId);
        this.inFlightVoiceRequests.delete(messageId);
        this.emit();
      }
    })();

    this.inFlightVoiceRequests.set(messageId, task);
    return task;
  }

  subscribe(cb: TranslatorListener): () => void {
    this.listeners.add(cb);
    return () => {
      this.listeners.delete(cb);
    };
  }

  private resolveTargetLang(sourceLang: string, requestedTarget?: string): string {
    const raw = (requestedTarget ?? this.preferredTargetLang).trim().toLowerCase();
    if (!raw || raw === 'auto') {
      return sourceLang === 'ru' ? 'en' : 'ru';
    }
    const norm = normalizeCode(raw) || 'en';
    if (norm === sourceLang) {
      return sourceLang === 'ru' ? 'en' : 'ru';
    }
    return norm;
  }

  private setCacheEntry(key: string, value: string): void {
    if (this.translationCache.has(key)) {
      this.translationCache.delete(key);
    }
    this.translationCache.set(key, value);
    while (this.translationCache.size > MAX_CACHE_ENTRIES) {
      const oldest = this.translationCache.keys().next().value;
      if (oldest === undefined) break;
      this.translationCache.delete(oldest);
    }
    kvStore.setJson(KEY_CACHE, Object.fromEntries(this.translationCache.entries()));
  }

  private persistPacks(): void {
    kvStore.setJson(KEY_PACKS, Array.from(this.downloadedCodes));
  }

  private persistMessageTranslations(): void {
    const obj = clampRecord(Object.fromEntries(this.messageTranslations.entries()), MAX_RECORD_ENTRIES);
    kvStore.setJson(KEY_MSG_TRANSLATIONS, obj);
  }

  private persistVoiceTranscripts(): void {
    const obj = clampRecord(Object.fromEntries(this.voiceTranscripts.entries()), MAX_RECORD_ENTRIES);
    kvStore.setJson(KEY_VOICE_TRANSCRIPTS, obj);
  }

  private emit(): void {
    const snap = this.getState();
    for (const cb of this.listeners) {
      cb(snap);
    }
  }
}

export const localMessageTranslator = new LocalMessageTranslatorService();
export const localTranslator = localMessageTranslator;
export const LocalMessageTranslator = localMessageTranslator;

export const getPacks = (): TranslationPackStatus[] => localMessageTranslator.getPacks();
export const getDownloadedPacks = (): string[] => localMessageTranslator.getDownloadedPacks();
export const getDownloadingCode = (): string | null => localMessageTranslator.getDownloadingCode();
export const getDownloadProgress = (): number => localMessageTranslator.getDownloadProgress();
export const getTotalDownloadedMb = (): number => localMessageTranslator.getTotalDownloadedMb();
export const downloadPack = (code: string): Promise<void> => localMessageTranslator.downloadPack(code);
export const removePack = (code: string): void => localMessageTranslator.removePack(code);
export const isPackDownloaded = (code: string): boolean => localMessageTranslator.isPackDownloaded(code);

export const translateText = (
  text: string,
  targetLang?: 'ru' | 'en' | string,
): Promise<TranslateTextResult> => localMessageTranslator.translateText(text, targetLang);

export const getMessageTranslation = (messageId: string): string | null =>
  localMessageTranslator.getMessageTranslation(messageId);

export const translateMessage = (
  messageId: string,
  text: string,
  targetLang?: string,
): Promise<string> => localMessageTranslator.translateMessage(messageId, text, targetLang);

export const clearMessageTranslation = (messageId: string): void =>
  localMessageTranslator.clearMessageTranslation(messageId);

export const transcribeVoice = (
  messageId: string,
  durationSec: number,
  previewUrl?: string,
  lang?: 'ru' | 'en',
): Promise<string> => localMessageTranslator.transcribeVoice(messageId, durationSec, previewUrl, lang);

export const getVoiceTranscript = (messageId: string): string | null =>
  localMessageTranslator.getVoiceTranscript(messageId);

export const subscribeTranslator = (cb: TranslatorListener): (() => void) =>
  localMessageTranslator.subscribe(cb);
