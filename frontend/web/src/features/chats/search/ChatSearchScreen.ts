import { kvStore } from '../../../core/storage/kvStore';
import { clear, el } from '../../../core/ui/dom';
import { icons, tgIcons } from '../../../core/ui/icons';
import { contactsStore, type Contact } from '../../contacts/ContactsStore';
import { chatStore } from '../chatStore';
import { createChatListItemCard } from '../mainmenu/components/ChatListItemCard';
import { createEmptyChatsState } from '../mainmenu/components/EmptyChatsState';
import { mainMenuT } from '../mainmenu/i18n';
import '../mainmenu/mainmenu.css';

const KEY_RECENT_SEARCHES = 'chats.recent_searches.v1';
const MAX_RECENT_SEARCHES = 8;

export interface ChatSearchResults {
  chatIds: string[];
  contacts: Contact[];
  messages: Array<{ chatId: string; messageId: string; text: string; timestamp: string }>;
}

export function getRecentSearches(): string[] {
  const saved = kvStore.getJson<string[]>(KEY_RECENT_SEARCHES, []);
  return Array.isArray(saved) ? saved.filter((s) => typeof s === 'string' && s.trim().length > 0) : [];
}

export function addRecentSearch(query: string): string[] {
  const q = query.trim();
  if (!q) return getRecentSearches();
  const next = [q, ...getRecentSearches().filter((s) => s.toLowerCase() !== q.toLowerCase())].slice(
    0,
    MAX_RECENT_SEARCHES,
  );
  kvStore.setJson(KEY_RECENT_SEARCHES, next);
  return next;
}

export function clearRecentSearches(): void {
  kvStore.setJson(KEY_RECENT_SEARCHES, []);
}

/** Global search across chats, ContactsStore contacts, and message text snippets. */
export function searchChats(query: string): ChatSearchResults {
  const q = query.trim().toLowerCase();
  if (!q) return { chatIds: [], contacts: [], messages: [] };

  const chatIds = chatStore.threads
    .filter((t) => !t.isArchived && t.title.toLowerCase().includes(q))
    .map((t) => t.id);

  const chatIdSet = new Set(chatIds);
  const contacts = contactsStore.search(q).filter((c) => !chatIdSet.has(c.id));

  const messages: ChatSearchResults['messages'] = [];
  for (const t of chatStore.threads) {
    if (t.isArchived) continue;
    for (const m of chatStore.getMessages(t.id)) {
      if (m.text && m.text.toLowerCase().includes(q)) {
        messages.push({ chatId: t.id, messageId: m.id, text: m.text, timestamp: m.timestamp });
      }
    }
  }
  return { chatIds, contacts, messages };
}

function buildHighlightedSnippet(text: string, query: string): HTMLElement {
  const container = el('div', { class: 'ag-chat-row__preview' });
  const q = query.trim();
  if (!q) {
    container.textContent = text;
    return container;
  }
  const lower = text.toLowerCase();
  const idx = lower.indexOf(q.toLowerCase());
  if (idx === -1) {
    container.textContent = text;
    return container;
  }
  const start = Math.max(0, idx - 24);
  const prefix = start > 0 ? '…' : '';
  const before = prefix + text.slice(start, idx);
  const match = text.slice(idx, idx + q.length);
  const after = text.slice(idx + q.length);

  if (before) container.appendChild(document.createTextNode(before));
  container.appendChild(el('mark', { class: 'ag-search-highlight' }, match));
  if (after) container.appendChild(document.createTextNode(after));
  return container;
}

export interface GlobalSearchCallbacks {
  onSelectChat: (chatId: string, messageId?: string) => void;
  onPickQuery?: (query: string) => void;
}

export function renderRecentSearchesSection(
  container: HTMLElement,
  onPickQuery: (query: string) => void,
  onCleared?: () => void,
): boolean {
  const recent = getRecentSearches();
  if (recent.length === 0) return false;
  const mt = mainMenuT();

  const header = el(
    'div',
    { class: 'ag-mm-section ag-mm-section--row' },
    el('span', null, mt.recentSearches),
    el(
      'button',
      {
        class: 'ag-mm-section__action',
        onClick: () => {
          clearRecentSearches();
          onCleared?.();
        },
      },
      mt.clearRecent,
    ),
  );
  container.appendChild(header);

  const chips = el(
    'div',
    { class: 'ag-recent-searches' },
    ...recent.map((q) =>
      el(
        'button',
        {
          class: 'ag-chip ag-recent-chip',
          onClick: () => onPickQuery(q),
        },
        icons.search(12),
        el('span', null, q),
      ),
    ),
  );
  container.appendChild(chips);
  return true;
}

export function renderGlobalSearchResults(
  container: HTMLElement,
  query: string,
  callbacks: GlobalSearchCallbacks,
): void {
  const mt = mainMenuT();
  clear(container);

  const trimmed = query.trim();
  if (!trimmed) {
    const hasRecent = callbacks.onPickQuery
      ? renderRecentSearchesSection(container, callbacks.onPickQuery, () =>
          renderGlobalSearchResults(container, '', callbacks),
        )
      : false;
    if (!hasRecent) {
      container.appendChild(createEmptyChatsState(mt.searchHint));
    }
    return;
  }

  const r = searchChats(trimmed);
  if (r.chatIds.length === 0 && r.contacts.length === 0 && r.messages.length === 0) {
    container.appendChild(createEmptyChatsState(mt.searchEmpty));
    return;
  }

  const selectAndRecord = (chatId: string, messageId?: string) => {
    addRecentSearch(trimmed);
    callbacks.onSelectChat(chatId, messageId);
  };

  if (r.chatIds.length > 0) {
    container.appendChild(el('div', { class: 'ag-mm-section' }, mt.searchChatsSection));
    for (const id of r.chatIds) {
      const th = chatStore.getThread(id);
      if (th) container.appendChild(createChatListItemCard(th, false, () => selectAndRecord(id)));
    }
  }

  if (r.contacts.length > 0) {
    container.appendChild(el('div', { class: 'ag-mm-section' }, mt.searchContactsSection));
    for (const c of r.contacts) {
      container.appendChild(
        el(
          'div',
          {
            class: 'ag-chat-row',
            onClick: () => {
              const chatId = chatStore.ensureChat(c.id, c.name, c.emoji, c.color, false);
              selectAndRecord(chatId);
            },
          },
          el(
            'div',
            { class: 'ag-avatar', style: `background:${c.color}22;color:${c.color};` },
            c.emoji,
          ),
          el(
            'div',
            { class: 'ag-chat-row__body' },
            el(
              'div',
              { class: 'ag-chat-row__top' },
              el('span', { class: 'ag-chat-row__title' }, c.name),
            ),
            el('div', { class: 'ag-chat-row__preview' }, `${c.handle} · ${c.fingerprint}`),
          ),
        ),
      );
    }
  }

  if (r.messages.length > 0) {
    container.appendChild(el('div', { class: 'ag-mm-section' }, mt.searchMessagesSection));
    for (const m of r.messages) {
      const th = chatStore.getThread(m.chatId);
      container.appendChild(
        el(
          'div',
          { class: 'ag-chat-row', onClick: () => selectAndRecord(m.chatId, m.messageId) },
          el(
            'div',
            { class: 'ag-avatar', style: `background:${th?.avatarColor ?? '#0EA5E9'}22;` },
            th?.avatarEmoji ?? '💬',
          ),
          el(
            'div',
            { class: 'ag-chat-row__body' },
            el(
              'div',
              { class: 'ag-chat-row__top' },
              el('span', { class: 'ag-chat-row__title' }, th?.title ?? ''),
              el('span', { class: 'ag-chat-row__time' }, m.timestamp),
            ),
            buildHighlightedSnippet(m.text, trimmed),
          ),
        ),
      );
    }
  }
}

export function createChatSearchScreen(
  onSelectChat: (chatId: string, messageId?: string) => void,
  onBack: () => void,
): HTMLElement {
  const input = el('input', { type: 'text', placeholder: mainMenuT().searchChats }) as HTMLInputElement;
  const results = el('div', { class: 'ag-scroll-list' });

  const render = () => {
    renderGlobalSearchResults(results, input.value, {
      onSelectChat,
      onPickQuery: (q) => {
        input.value = q;
        render();
        input.focus();
      },
    });
  };

  input.addEventListener('input', render);
  render();

  return el(
    'div',
    { class: 'ag-list-pane ag-mm' },
    el(
      'div',
      { class: 'ag-search-wrap ag-search-row' },
      el('button', { class: 'ag-icon-btn', onClick: onBack }, icons.back(20)),
      el('div', { class: 'ag-search-pill', style: 'flex:1;min-width:0;' }, icons.search(16), input),
    ),
    results,
  );
}

/**
 * In-chat message search bar with match counter (`1 / N`), prev/next arrows, and Esc to close.
 * Persistent input element is never recreated on updates and never steals focus.
 */
export function createInChatSearchBar(
  chatId: string,
  onJumpToMessage: (messageId: string) => void,
  onClose: () => void,
): HTMLElement {
  const mt = mainMenuT();
  let matchIds: string[] = [];
  let activeIdx = -1;

  const input = el('input', {
    type: 'text',
    class: 'ag-inchat-search__input',
    placeholder: mt.inChatSearchPlaceholder,
  }) as HTMLInputElement;

  const counterEl = el('span', { class: 'ag-inchat-search__count' }, '');
  const prevBtn = el(
    'button',
    {
      class: 'ag-icon-btn ag-inchat-search__nav',
      disabled: true,
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: () => step(-1),
    },
    tgIcons.chevronUp(18),
  ) as HTMLButtonElement;

  const nextBtn = el(
    'button',
    {
      class: 'ag-icon-btn ag-inchat-search__nav',
      disabled: true,
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: () => step(1),
    },
    tgIcons.chevronDown(18),
  ) as HTMLButtonElement;

  const closeBtn = el(
    'button',
    {
      class: 'ag-icon-btn',
      onMouseDown: (e: Event) => e.preventDefault(),
      onClick: () => cleanupAndClose(),
    },
    icons.close(18),
  );

  const syncUi = () => {
    const hasQuery = input.value.trim().length > 0;
    const total = matchIds.length;
    if (!hasQuery) {
      counterEl.textContent = '';
    } else if (total === 0) {
      counterEl.textContent = '0 / 0';
    } else {
      counterEl.textContent = `${activeIdx + 1} / ${total}`;
    }
    prevBtn.disabled = total === 0;
    nextBtn.disabled = total === 0;
  };

  const runSearch = () => {
    const q = input.value.trim().toLowerCase();
    if (!q) {
      matchIds = [];
      activeIdx = -1;
      syncUi();
      return;
    }
    const msgs = chatStore.getMessages(chatId);
    matchIds = msgs.filter((m) => m.text && m.text.toLowerCase().includes(q)).map((m) => m.id);
    if (matchIds.length > 0) {
      activeIdx = matchIds.length - 1;
      onJumpToMessage(matchIds[activeIdx]);
    } else {
      activeIdx = -1;
    }
    syncUi();
  };

  const step = (delta: number) => {
    if (matchIds.length === 0) return;
    activeIdx = (activeIdx + delta + matchIds.length) % matchIds.length;
    syncUi();
    onJumpToMessage(matchIds[activeIdx]);
  };

  const onDocKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Escape') {
      e.preventDefault();
      cleanupAndClose();
    }
  };

  function cleanupAndClose() {
    document.removeEventListener('keydown', onDocKeyDown);
    onClose();
  }

  input.addEventListener('input', runSearch);
  input.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.key === 'Enter') {
      e.preventDefault();
      step(e.shiftKey ? -1 : 1);
    } else if (e.key === 'Escape') {
      e.preventDefault();
      cleanupAndClose();
    }
  });
  document.addEventListener('keydown', onDocKeyDown);

  const root = el(
    'div',
    { class: 'ag-inchat-search' },
    el('div', { class: 'ag-search-pill ag-inchat-search__pill' }, icons.search(16), input, counterEl),
    prevBtn,
    nextBtn,
    closeBtn,
  );

  (root as unknown as { __cleanup?: () => void }).__cleanup = () => {
    document.removeEventListener('keydown', onDocKeyDown);
  };

  return root;
}
