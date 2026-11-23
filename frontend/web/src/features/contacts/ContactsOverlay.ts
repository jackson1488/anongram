import { i18nStore } from '../../core/i18n/i18nStore';
import { clear, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { chatStore } from '../chats/chatStore';
import { openQrHubOverlay } from '../qr/QrHubOverlay';
import { createContactDetailsView } from './ContactDetailsModal';
import { contactMonogram, contactsStore, type Contact } from './ContactsStore';
import { createNewContactView } from './NewContactScreen';
import { contactsT } from './i18n';
import './contacts.css';

type ModalMode =
  | { type: 'list' }
  | { type: 'new' }
  | { type: 'edit'; contact: Contact }
  | { type: 'details'; contact: Contact };

export function openContactsOverlay(onOpenChat: (chatId: string) => void): void {
  const overlay = el('div', {
    class: 'ag-modal-overlay',
    onClick: (e: Event) => {
      if (e.target === overlay) closeOverlay();
    },
  });

  let query = '';
  let mode: ModalMode = { type: 'list' };
  const box = el('div', { class: 'ag-card ag-contacts-modal' });
  overlay.appendChild(box);

  let unsubContacts: (() => void) | null = null;
  let unsubI18n: (() => void) | null = null;

  const closeOverlay = () => {
    unsubContacts?.();
    unsubI18n?.();
    overlay.remove();
  };

  const openChatForContact = (c: Contact, isOtp: boolean) => {
    const id = isOtp
      ? chatStore.ensureChat(`otp_${c.id}`, `${c.name} (OTP)`, '🔥', c.color, true)
      : chatStore.ensureChat(c.id, c.name, c.emoji || contactMonogram(c.name), c.color, false);
    closeOverlay();
    onOpenChat(id);
  };

  const render = () => {
    clear(box);
    const s = contactsT();

    if (mode.type === 'new') {
      box.appendChild(
        createNewContactView({
          onBack: () => {
            mode = { type: 'list' };
            render();
          },
          onSubmit: (input) => {
            contactsStore.addContact(input);
            showToast(s.createdToast);
            mode = { type: 'list' };
            render();
          },
        }),
      );
      return;
    }

    if (mode.type === 'edit') {
      const target = mode.contact;
      box.appendChild(
        createNewContactView({
          existing: target,
          onBack: () => {
            const latest = contactsStore.getById(target.id) ?? target;
            mode = { type: 'details', contact: latest };
            render();
          },
          onSubmit: (input) => {
            const updated = contactsStore.updateContact(target.id, input);
            showToast(s.updatedToast);
            mode = updated ? { type: 'details', contact: updated } : { type: 'list' };
            render();
          },
        }),
      );
      return;
    }

    if (mode.type === 'details') {
      const current = contactsStore.getById(mode.contact.id);
      if (!current) {
        mode = { type: 'list' };
        render();
        return;
      }
      box.appendChild(
        createContactDetailsView({
          contact: current,
          onBack: () => {
            mode = { type: 'list' };
            render();
          },
          onEdit: (c) => {
            mode = { type: 'edit', contact: c };
            render();
          },
          onOpenChat: openChatForContact,
          onDeleted: () => {
            mode = { type: 'list' };
            render();
          },
        }),
      );
      return;
    }

    // List mode (persistent search input)
    const list = el('div', { class: 'ag-contacts-list' });

    const renderList = () => {
      clear(list);
      const items = contactsStore.search(query);
      if (items.length === 0) {
        list.appendChild(
          el(
            'div',
            { style: 'text-align:center;padding:28px 12px;color:var(--ag-text-muted);font-size:13px;' },
            s.emptyList,
          ),
        );
        return;
      }

      for (const c of items) {
        list.appendChild(
          el(
            'div',
            {
              class: 'ag-contact-row',
              onClick: () => {
                mode = { type: 'details', contact: c };
                render();
              },
            },
            el(
              'div',
              { class: 'ag-contact-avatar' },
              c.emoji || contactMonogram(c.name),
              c.isOnline ? el('span', { class: 'ag-contact-online-dot' }) : el('span', null),
            ),
            el(
              'div',
              { class: 'ag-contact-row__body' },
              el(
                'div',
                { class: 'ag-contact-row__name-wrap' },
                el('span', { class: 'ag-contact-row__name' }, c.name),
                c.isSasVerified ? el('span', { class: 'ag-contact-sas-badge' }, 'SAS') : el('span', null),
              ),
              el('div', { class: 'ag-contact-row__sub' }, `${c.fingerprint} • ${c.handle}`),
            ),
            el(
              'div',
              { class: 'ag-contact-row__actions' },
              el(
                'button',
                {
                  class: 'ag-chip',
                  onClick: (e: Event) => {
                    e.stopPropagation();
                    openChatForContact(c, false);
                  },
                },
                s.chatBtn,
              ),
              el(
                'button',
                {
                  class: 'ag-chip',
                  title: s.otpTooltip,
                  onClick: (e: Event) => {
                    e.stopPropagation();
                    openChatForContact(c, true);
                  },
                },
                icons.flame(13),
                s.otpBtn,
              ),
            ),
          ),
        );
      }
    };

    const searchInput = el('input', {
      type: 'text',
      placeholder: s.searchPlaceholder,
      value: query,
      onInput: (e: Event) => {
        query = (e.target as HTMLInputElement).value;
        renderList();
      },
    }) as HTMLInputElement;

    box.append(
      el(
        'div',
        { class: 'ag-contacts-head' },
        el('h3', { class: 'ag-contacts-head__title' }, s.title),
        el(
          'div',
          { style: 'display:flex;align-items:center;gap:6px;' },
          el(
            'button',
            {
              class: 'ag-pill-btn ag-pill-btn--primary',
              style: 'min-height:34px;padding:0 14px;font-size:13px;',
              onClick: () => {
                mode = { type: 'new' };
                render();
              },
            },
            s.newContact,
          ),
          el('button', { class: 'ag-icon-btn', onClick: closeOverlay }, icons.close(18)),
        ),
      ),
      el('div', { class: 'ag-search-pill' }, icons.search(16), searchInput),
      el(
        'div',
        { class: 'ag-contacts-actions-card' },
        el(
          'div',
          { class: 'ag-contacts-actions-card__meta' },
          el('strong', null, s.addNewCardTitle),
          el('span', null, s.addNewCardSub),
        ),
        el(
          'div',
          { class: 'ag-contacts-actions-card__btns' },
          el(
            'button',
            {
              class: 'ag-chip',
              onClick: () => {
                closeOverlay();
                openQrHubOverlay();
              },
            },
            icons.qr(14),
            s.openQrBtn,
          ),
        ),
      ),
      list,
    );

    renderList();
  };

  unsubContacts = contactsStore.subscribe(render);
  unsubI18n = i18nStore.subscribe(render);
  render();
  document.body.appendChild(overlay);
}
