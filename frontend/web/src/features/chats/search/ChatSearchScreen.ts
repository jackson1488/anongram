import { clear, el } from '../../../core/ui/dom';
import { icons } from '../../../core/ui/icons';
import { createChatListItemCard } from '../mainmenu/components/ChatListItemCard';
import { createEmptyChatsState } from '../mainmenu/components/EmptyChatsState';
import { mainMenuT } from '../mainmenu/i18n';
import { chatStore } from '../chatStore';
import '../mainmenu/mainmenu.css';

export interface ChatSearchResults {
  chatIds: string[];
  messages: Array<{ chatId: string; messageId: string; text: string; timestamp: string }>;
}

/** Port of ChatSearchViewModel: filter chats by title and messages by text. */
export function searchChats(query: string): ChatSearchResults {
  const q = query.trim().toLowerCase();
  if (!q) return { chatIds: [], messages: [] };
  const chatIds = chatStore.threads.filter((t) => t.title.toLowerCase().includes(q)).map((t) => t.id);
  const messages: ChatSearchResults['messages'] = [];
  for (const t of chatStore.threads) {
    for (const m of chatStore.getMessages(t.id)) {
      if (m.text.toLowerCase().includes(q)) {
        messages.push({ chatId: t.id, messageId: m.id, text: m.text, timestamp: m.timestamp });
      }
    }
  }
  return { chatIds, messages };
}

export function createChatSearchScreen(onSelectChat: (chatId: string) => void, onBack: () => void): HTMLElement {
  const input = el('input', { type: 'text', placeholder: mainMenuT().searchChats }) as HTMLInputElement;
  const results = el('div', { class: 'ag-scroll-list' });
  const render = () => {
    const mt = mainMenuT();
    clear(results);
    if (!input.value.trim()) {
      results.appendChild(createEmptyChatsState(mt.searchHint));
      return;
    }
    const r = searchChats(input.value);
    if (r.chatIds.length === 0 && r.messages.length === 0) {
      results.appendChild(createEmptyChatsState(mt.searchEmpty));
      return;
    }
    if (r.chatIds.length) {
      results.appendChild(el('div', { class: 'ag-mm-section' }, mt.searchChatsSection));
      for (const id of r.chatIds) {
        const th = chatStore.getThread(id);
        if (th) results.appendChild(createChatListItemCard(th, false, () => onSelectChat(id)));
      }
    }
    if (r.messages.length) {
      results.appendChild(el('div', { class: 'ag-mm-section' }, mt.searchMessagesSection));
      for (const m of r.messages) {
        const th = chatStore.getThread(m.chatId);
        results.appendChild(
          el(
            'div',
            { class: 'ag-chat-row', onClick: () => onSelectChat(m.chatId) },
            el(
              'div',
              { class: 'ag-chat-row__body' },
              el(
                'div',
                { class: 'ag-chat-row__top' },
                el('span', { class: 'ag-chat-row__title' }, th?.title ?? ''),
                el('span', { class: 'ag-chat-row__time' }, m.timestamp),
              ),
              el('div', { class: 'ag-chat-row__preview' }, m.text),
            ),
          ),
        );
      }
    }
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
