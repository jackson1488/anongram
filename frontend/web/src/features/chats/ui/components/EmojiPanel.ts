import { kvStore } from '../../../../core/storage/kvStore';
import { clear, el } from '../../../../core/ui/dom';
import { icons, tgIcons } from '../../../../core/ui/icons';
import { chatScreenT } from '../../i18n';

const KEY_RECENT_EMOJIS = 'chats.recent_emojis.v1';
const MAX_RECENT_EMOJIS = 24;
const DEFAULT_RECENT = ['👍', '🔥', '❤️', '😂', '😮', '😢', '🎉', '🛡️'];

interface EmojiEntry {
  char: string;
  keywords: string;
}

type CategoryId = 'recent' | 'smileys' | 'gestures' | 'hearts' | 'stickers';

const SMILEYS: EmojiEntry[] = [
  { char: '😀', keywords: 'smile grin happy улыбка радость' },
  { char: '😃', keywords: 'smile happy joy улыбка смех' },
  { char: '😄', keywords: 'laugh happy smile смех радость' },
  { char: '😁', keywords: 'grin teeth happy ухмылка' },
  { char: '😆', keywords: 'laugh xd haha хаха смех' },
  { char: '😅', keywords: 'sweat nervous laugh пот неловко' },
  { char: '🤣', keywords: 'rofl lol laugh ржач смех' },
  { char: '😂', keywords: 'joy tears lol laugh слезы смех' },
  { char: '🙂', keywords: 'slight smile ок норм' },
  { char: '😉', keywords: 'wink подмигивание' },
  { char: '😊', keywords: 'blush cute warm мило смущение' },
  { char: '😇', keywords: 'angel halo ангел' },
  { char: '🥰', keywords: 'love hearts adore любовь' },
  { char: '😍', keywords: 'heart eyes love влюблен' },
  { char: '🤩', keywords: 'star struck wow восторг звезда' },
  { char: '😘', keywords: 'kiss love поцелуй' },
  { char: '😎', keywords: 'cool sunglasses крутой очки' },
  { char: '🤓', keywords: 'nerd glasses ботан очки' },
  { char: '🧐', keywords: 'monocle inspect think монокль' },
  { char: '🤔', keywords: 'think hmm вопрос думы' },
  { char: '🤫', keywords: 'shh secret quiet тсс секрет' },
  { char: '🤭', keywords: 'oops giggle ой' },
  { char: '😮', keywords: 'wow surprised шок удивление' },
  { char: '🤯', keywords: 'mind blown explode взрыв' },
  { char: '😢', keywords: 'cry sad tear грусть слеза' },
  { char: '😭', keywords: 'sob cry sad плач рыдания' },
  { char: '😤', keywords: 'angry huff пар злость' },
  { char: '😡', keywords: 'mad angry rage злость гнев' },
  { char: '🥳', keywords: 'party celebrate праздник' },
  { char: '😴', keywords: 'sleep zzz сон' },
  { char: '🤖', keywords: 'robot bot ai робот бот' },
  { char: '👻', keywords: 'ghost anon призрак аноним' },
];

const GESTURES: EmojiEntry[] = [
  { char: '👍', keywords: 'thumbs up like yes ок лайк да' },
  { char: '👎', keywords: 'thumbs down dislike no дизлайк нет' },
  { char: '👌', keywords: 'ok perfect окей' },
  { char: '✌️', keywords: 'peace victory мир победа' },
  { char: '🤞', keywords: 'fingers crossed luck удача' },
  { char: '🤟', keywords: 'love rock рок' },
  { char: '🤘', keywords: 'metal rock рок' },
  { char: '🤙', keywords: 'call me shaka звони' },
  { char: '👋', keywords: 'wave hi bye привет пока' },
  { char: '👏', keywords: 'clap applause браво хлопки' },
  { char: '🙌', keywords: 'hands celebrate ура' },
  { char: '🤝', keywords: 'handshake deal сделка рукопожатие' },
  { char: '🙏', keywords: 'pray please thanks спасибо прошу' },
  { char: '💪', keywords: 'strong muscle power сила' },
  { char: '🫶', keywords: 'heart hands love сердце руки' },
  { char: '🫡', keywords: 'salute respect честь' },
  { char: '👀', keywords: 'eyes look watch глаза смотрю' },
  { char: '🧠', keywords: 'brain smart мозг' },
  { char: '🛡️', keywords: 'shield security anon щит защита' },
  { char: '🔐', keywords: 'lock key crypto замок шифр' },
  { char: '🕶️', keywords: 'sunglasses anon dark очки аноним' },
  { char: '⚡', keywords: 'lightning fast zap молния быстро' },
  { char: '🔥', keywords: 'fire hot lit огонь топ' },
  { char: '🚀', keywords: 'rocket launch moon ракета' },
];

const HEARTS: EmojiEntry[] = [
  { char: '❤️', keywords: 'red heart love сердце любовь' },
  { char: '🧡', keywords: 'orange heart оранжевое сердце' },
  { char: '💛', keywords: 'yellow heart желтое сердце' },
  { char: '💚', keywords: 'green heart зеленое сердце' },
  { char: '🩵', keywords: 'light blue heart голубое сердце' },
  { char: '💙', keywords: 'blue heart синее сердце' },
  { char: '💜', keywords: 'purple heart фиолетовое сердце' },
  { char: '🖤', keywords: 'black heart черное сердце' },
  { char: '🤍', keywords: 'white heart белое сердце' },
  { char: '💔', keywords: 'broken heart разбитое сердце' },
  { char: '❤️‍🔥', keywords: 'heart on fire страсть' },
  { char: '💖', keywords: 'sparkling heart сияющее сердце' },
  { char: '💘', keywords: 'cupid heart стрела сердце' },
  { char: '✨', keywords: 'sparkles stars блеск магия' },
  { char: '🎉', keywords: 'party confetti праздник' },
  { char: '💎', keywords: 'gem diamond crystal алмаз' },
];

const STICKERS = ['🦊', '🐉', '🛸', '🕶️', '⚡', '🔐', '💎', '🌌', '🧅', '🛡️', '🎭', '👾'];
const KAOMOJI = [
  '( ͡° ͜ʖ ͡°)',
  '¯\\_(ツ)_/¯',
  '(╯°□°)╯︵ ┻━┻',
  '(づ｡◕‿‿◕｡)づ',
  '(•_•) ( •_•)>⌐■-■ (⌐■_■)',
  '(ノಠ益ಠ)ノ',
  '(☞ﾟヮﾟ)☞',
  'ʕ•ᴥ•ʔ',
];

const ALL_EMOJIS: EmojiEntry[] = [...SMILEYS, ...GESTURES, ...HEARTS];

function loadRecentEmojis(): string[] {
  const saved = kvStore.getJson<string[]>(KEY_RECENT_EMOJIS, DEFAULT_RECENT);
  return Array.isArray(saved) && saved.length > 0 ? saved : DEFAULT_RECENT;
}

function recordRecentEmoji(char: string): string[] {
  const prev = loadRecentEmojis().filter((e) => e !== char);
  const next = [char, ...prev].slice(0, MAX_RECENT_EMOJIS);
  kvStore.setJson(KEY_RECENT_EMOJIS, next);
  return next;
}

/**
 * Telegram Web A style Emoji & Stickers Picker:
 * - Category tabs (Recent, Smileys, Gestures, Hearts, Stickers/Kaomoji)
 * - Persistent emoji search input
 * - Persisted Recent emojis in kvStore
 */
export function createEmojiPanel(onEmoji: (em: string) => void, onSticker: (st: string) => void): HTMLElement {
  const cs = chatScreenT();
  let activeTab: CategoryId = 'recent';
  let query = '';
  let recent = loadRecentEmojis();

  const searchInput = el('input', {
    type: 'text',
    class: 'ag-emoji-search__input',
    placeholder: cs.emojiSearchPlaceholder,
  }) as HTMLInputElement;

  const clearBtn = el(
    'button',
    {
      class: 'ag-icon-btn ag-emoji-search__clear',
      style: 'display:none;',
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: () => {
        searchInput.value = '';
        query = '';
        clearBtn.style.display = 'none';
        renderBody();
        searchInput.focus();
      },
    },
    icons.close(14),
  );

  const searchRow = el(
    'div',
    { class: 'ag-emoji-search' },
    icons.search(14),
    searchInput,
    clearBtn,
  );

  const tabsRow = el('div', { class: 'ag-emoji-tabs' });
  const body = el('div', { class: 'ag-emoji-body' });

  const tabDefs: Array<{ id: CategoryId; label: string; icon: () => SVGElement | string }> = [
    { id: 'recent', label: cs.emojiRecent, icon: () => tgIcons.clock(16) },
    { id: 'smileys', label: cs.emojiSmileys, icon: () => icons.smile(16) },
    { id: 'gestures', label: cs.emojiGestures, icon: () => '👍' },
    { id: 'hearts', label: cs.emojiHearts, icon: () => tgIcons.heart(16) },
    { id: 'stickers', label: cs.emojiStickers, icon: () => tgIcons.smileSticker(16) },
  ];

  const tabButtons = new Map<CategoryId, HTMLElement>();
  const sectionRefs = new Map<CategoryId, HTMLElement>();

  const updateTabHighlight = () => {
    for (const [id, btn] of tabButtons) {
      btn.classList.toggle('is-active', id === activeTab);
    }
  };

  for (const tab of tabDefs) {
    const btn = el(
      'button',
      {
        class: `ag-emoji-tab${tab.id === activeTab ? ' is-active' : ''}`,
        title: tab.label,
        onMouseDown: (e: Event) => e.preventDefault(),
        onClick: () => {
          activeTab = tab.id;
          updateTabHighlight();
          if (query.trim()) {
            searchInput.value = '';
            query = '';
            clearBtn.style.display = 'none';
            renderBody();
          }
          const sec = sectionRefs.get(tab.id);
          sec?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        },
      },
      tab.icon(),
    );
    tabButtons.set(tab.id, btn);
    tabsRow.appendChild(btn);
  }

  const pickEmoji = (em: string) => {
    recent = recordRecentEmoji(em);
    onEmoji(em);
    if (!query.trim()) {
      const recentGrid = sectionRefs.get('recent')?.querySelector('.ag-emoji-grid');
      if (recentGrid) {
        clear(recentGrid as HTMLElement);
        for (const r of recent) {
          recentGrid.appendChild(makeEmojiBtn(r));
        }
      }
    }
  };

  const makeEmojiBtn = (em: string) =>
    el(
      'button',
      {
        class: 'ag-emoji-btn',
        onMouseDown: (e: Event) => e.preventDefault(),
        onClick: () => pickEmoji(em),
      },
      em,
    );

  const renderSection = (id: CategoryId, title: string, emojis: string[]) => {
    const sec = el(
      'div',
      { class: 'ag-emoji-section', 'data-cat': id },
      el('div', { class: 'ag-emoji-section__title' }, title),
      el('div', { class: 'ag-emoji-grid' }, ...emojis.map(makeEmojiBtn)),
    );
    sectionRefs.set(id, sec);
    return sec;
  };

  const renderBody = () => {
    clear(body);
    sectionRefs.clear();
    const q = query.trim().toLowerCase();

    if (q) {
      const seen = new Set<string>();
      const matched: string[] = [];
      for (const entry of ALL_EMOJIS) {
        if (!seen.has(entry.char) && (entry.char.includes(q) || entry.keywords.toLowerCase().includes(q))) {
          seen.add(entry.char);
          matched.push(entry.char);
        }
      }
      if (matched.length === 0) {
        body.appendChild(el('div', { class: 'ag-emoji-empty' }, cs.emojiEmpty));
        return;
      }
      body.appendChild(el('div', { class: 'ag-emoji-grid' }, ...matched.map(makeEmojiBtn)));
      return;
    }

    body.appendChild(renderSection('recent', cs.emojiRecent, recent));
    body.appendChild(renderSection('smileys', cs.emojiSmileys, SMILEYS.map((e) => e.char)));
    body.appendChild(renderSection('gestures', cs.emojiGestures, GESTURES.map((e) => e.char)));
    body.appendChild(renderSection('hearts', cs.emojiHearts, HEARTS.map((e) => e.char)));

    const stickersSec = el(
      'div',
      { class: 'ag-emoji-section', 'data-cat': 'stickers' },
      el('div', { class: 'ag-emoji-section__title' }, cs.emojiStickers),
      el(
        'div',
        { class: 'ag-stickers-row' },
        ...STICKERS.map((st) =>
          el(
            'button',
            {
              class: 'ag-sticker-btn',
              onMouseDown: (e: Event) => e.preventDefault(),
              onClick: () => onSticker(st),
            },
            st,
          ),
        ),
      ),
      el(
        'div',
        { class: 'ag-kaomoji-grid' },
        ...KAOMOJI.map((km) =>
          el(
            'button',
            {
              class: 'ag-kaomoji-btn',
              onMouseDown: (e: Event) => e.preventDefault(),
              onClick: () => pickEmoji(km),
            },
            km,
          ),
        ),
      ),
    );
    sectionRefs.set('stickers', stickersSec);
    body.appendChild(stickersSec);
  };

  searchInput.addEventListener('input', () => {
    query = searchInput.value;
    clearBtn.style.display = query ? 'inline-flex' : 'none';
    renderBody();
  });

  renderBody();

  return el('div', { class: 'ag-drawer-panel ag-emoji-panel' }, tabsRow, searchRow, body);
}
