import { copyToClipboard, el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { showToast } from '../../core/ui/toast';
import { contactMonogram, contactsStore, type Contact } from './ContactsStore';
import { contactsT } from './i18n';

export interface ContactDetailsOptions {
  contact: Contact;
  onBack: () => void;
  onEdit: (contact: Contact) => void;
  onOpenChat: (contact: Contact, isOtp: boolean) => void;
  onDeleted: () => void;
}

export function createContactDetailsView(options: ContactDetailsOptions): HTMLElement {
  const s = contactsT();
  const c = contactsStore.getById(options.contact.id) ?? options.contact;
  const root = el('div', { style: 'display:flex;flex-direction:column;gap:12px;' });

  root.append(
    el(
      'div',
      { class: 'ag-contacts-head' },
      el(
        'div',
        { class: 'ag-contacts-head__left' },
        el('button', { class: 'ag-icon-btn', onClick: options.onBack }, icons.back(20)),
        el('h3', { class: 'ag-contacts-head__title' }, s.detailsTitle),
      ),
      el(
        'button',
        { class: 'ag-icon-btn', title: s.editBtn, onClick: () => options.onEdit(c) },
        icons.edit(18),
      ),
    ),
    el(
      'div',
      { class: 'ag-contact-details-hero' },
      el('div', { class: 'ag-contact-details-avatar' }, c.emoji || contactMonogram(c.name)),
      el('strong', { style: 'font-size:18px;color:var(--ag-text-primary);' }, c.name),
      el(
        'span',
        { style: 'font-size:13px;color:var(--ag-text-secondary);' },
        `${c.handle} • ${c.isOnline ? s.online : s.lastSeenRecently}`,
      ),
      el(
        'span',
        { class: `ag-contact-sas-badge ${c.isSasVerified ? '' : 'is-unverified'}` },
        c.isSasVerified ? s.sasVerified : s.sasUnverified,
      ),
    ),
    el(
      'div',
      { class: 'ag-contact-form-card' },
      el('div', { style: 'font-size:11px;font-weight:600;color:var(--ag-text-muted);text-transform:uppercase;' }, s.fingerprintLabel),
      el(
        'div',
        { style: 'display:flex;align-items:center;justify-content:space-between;gap:8px;' },
        el('code', { class: 'ag-ag1-code', style: 'flex:1;' }, c.fingerprint),
        el(
          'button',
          {
            class: 'ag-icon-btn',
            title: s.copyFingerprint,
            onClick: async () => {
              await copyToClipboard(c.fingerprint);
              showToast(s.copiedToast);
            },
          },
          icons.copy(16),
        ),
      ),
      c.note
        ? el(
            'div',
            { style: 'margin-top:4px;display:flex;flex-direction:column;gap:2px;' },
            el('div', { style: 'font-size:11px;font-weight:600;color:var(--ag-text-muted);text-transform:uppercase;' }, s.noteLabel),
            el('div', { style: 'font-size:13px;color:var(--ag-text-secondary);' }, c.note),
          )
        : el('span', null),
    ),
    el(
      'div',
      { class: 'ag-contact-details-btns' },
      el(
        'button',
        {
          class: 'ag-pill-btn ag-pill-btn--primary',
          onClick: () => options.onOpenChat(c, false),
        },
        icons.chats(17),
        s.openChat,
      ),
      el(
        'button',
        {
          class: 'ag-pill-btn ag-pill-btn--secondary',
          onClick: () => options.onOpenChat(c, true),
        },
        icons.flame(16),
        s.openOtpChat,
      ),
      el(
        'button',
        {
          class: 'ag-pill-btn ag-pill-btn--ghost',
          onClick: () => {
            contactsStore.toggleSasVerified(c.id);
          },
        },
        icons.check(16),
        c.isSasVerified ? s.unverifySasBtn : s.verifySasBtn,
      ),
      el(
        'button',
        {
          class: 'ag-contact-danger-btn',
          onClick: () => {
            contactsStore.deleteContact(c.id);
            showToast(s.deletedToast);
            options.onDeleted();
          },
        },
        icons.trash(16),
        s.deleteBtn,
      ),
    ),
  );

  return root;
}
