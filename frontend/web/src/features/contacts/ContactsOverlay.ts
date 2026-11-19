import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { chatStore } from '../chats/chatStore';

interface ContactItem {
  id: string;
  name: string;
  handle: string;
  emoji: string;
  color: string;
}

const CONTACTS: ContactItem[] = [
  { id: 'ghost_node', name: 'Ghost Node #42', handle: '@ghost42', emoji: '🕶️', color: '#8B5CF6' },
  { id: 'maya_cipher', name: 'Maya Cipher', handle: '@maya_e2ee', emoji: '⚡', color: '#EC4899' },
  { id: 'zero_lab', name: 'Zero-Knowledge Lab', handle: '@zklab', emoji: '🔐', color: '#10B981' },
  { id: 'relay_alpha', name: 'Onion Relay Alpha', handle: '@relay_v3', emoji: '🧅', color: '#F59E0B' },
];

export function openContactsOverlay(onOpenChat: (chatId: string) => void): void {
  const overlay = el('div', {
    class: 'ag-modal-overlay',
    onClick: (e: Event) => {
      if (e.target === overlay) overlay.remove();
    },
  });

  let query = '';
  const box = el('div', { class: 'ag-card ag-forward-modal', style: 'max-width:420px;' });
  overlay.appendChild(box);

  const render = () => {
    clear(box);
    const searchInput = el('input', {
      type: 'text',
      placeholder: 'Поиск контакта или Public ID...',
      value: query,
      onInput: (e: Event) => {
        query = (e.target as HTMLInputElement).value;
        renderList();
      },
    }) as HTMLInputElement;

    box.appendChild(
      el(
        'div',
        { class: 'ag-forward-modal__head' },
        el('h3', { style: 'margin:0;font-size:18px;' }, 'Контакты'),
        el('button', { class: 'ag-icon-btn', onClick: () => overlay.remove() }, icons.close(18)),
      ),
    );
    box.appendChild(el('div', { class: 'ag-search-pill', style: 'margin-bottom:10px;' }, icons.search(16), searchInput));

    const list = el('div', { class: 'ag-forward-list' });
    box.appendChild(list);

    const renderList = () => {
      clear(list);
      const q = query.trim().toLowerCase();
      for (const c of CONTACTS.filter(
        (x) => !q || x.name.toLowerCase().includes(q) || x.handle.toLowerCase().includes(q),
      )) {
        list.appendChild(
          el(
            'div',
            { class: 'ag-chat-row' },
            el('div', { class: 'ag-avatar ag-avatar--sm', style: `background:${c.color}22` }, c.emoji),
            el(
              'div',
              { class: 'ag-chat-row__body' },
              el('div', { class: 'ag-chat-row__title' }, c.name),
              el('div', { class: 'ag-chat-row__preview' }, c.handle),
            ),
            el(
              'button',
              {
                class: 'ag-chip',
                onClick: () => {
                  const id = chatStore.ensureChat(c.id, c.name, c.emoji, c.color, false);
                  overlay.remove();
                  onOpenChat(id);
                },
              },
              'Чат',
            ),
            el(
              'button',
              {
                class: 'ag-chip',
                title: 'OTP-чат (сгораемый)',
                onClick: () => {
                  const id = chatStore.ensureChat(
                    `otp_${c.id}`,
                    `${c.name} (OTP)`,
                    '🔥',
                    '#EF4444',
                    true,
                  );
                  overlay.remove();
                  onOpenChat(id);
                },
              },
              icons.flame(13),
              'OTP',
            ),
          ),
        );
      }
    };
    renderList();
  };

  render();
  document.body.appendChild(overlay);
}
