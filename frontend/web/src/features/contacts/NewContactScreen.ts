import { el } from '../../core/ui/dom';
import { icons } from '../../core/ui/icons';
import { contactMonogram, type AddContactInput, type Contact } from './ContactsStore';
import { contactsT } from './i18n';

export interface NewContactScreenOptions {
  existing?: Contact;
  onBack: () => void;
  onSubmit: (input: AddContactInput) => void;
}

/**
 * Mirrors Android NewContactScreen.kt: create or edit a contact with
 * first name, last name, optional fingerprint/Public ID, share-phone toggle, and private note.
 */
export function createNewContactView(options: NewContactScreenOptions): HTMLElement {
  const s = contactsT();
  const isEdit = Boolean(options.existing);

  let firstName = options.existing?.firstName ?? '';
  let lastName = options.existing?.lastName ?? '';
  let fingerprint = options.existing?.fingerprint ?? '';
  let sharePhone = options.existing?.sharePhone ?? true;
  let note = options.existing?.note ?? '';

  const root = el('div', { style: 'display:flex;flex-direction:column;gap:12px;' });

  const avatarEl = el('div', { class: 'ag-contact-details-avatar', style: 'width:56px;height:56px;font-size:19px;' },
    contactMonogram(`${firstName} ${lastName}`),
  );

  const submitBtn = el(
    'button',
    {
      class: 'ag-pill-btn ag-pill-btn--primary',
      style: 'min-height:36px;padding:0 16px;',
      onClick: () => {
        if (!firstName.trim()) return;
        options.onSubmit({
          firstName: firstName.trim(),
          lastName: lastName.trim(),
          fingerprint: fingerprint.trim(),
          sharePhone,
          note: note.trim(),
        });
      },
    },
    isEdit ? s.saveBtn : s.createBtn,
  ) as HTMLButtonElement;

  const updateSubmitState = () => {
    const valid = firstName.trim().length > 0;
    submitBtn.disabled = !valid;
    submitBtn.style.opacity = valid ? '1' : '0.45';
    avatarEl.textContent = contactMonogram(`${firstName} ${lastName}`);
  };
  updateSubmitState();

  const firstInput = el('input', {
    type: 'text',
    class: 'ag-contact-field',
    placeholder: s.firstNamePlaceholder,
    value: firstName,
    onInput: (e: Event) => {
      firstName = (e.target as HTMLInputElement).value;
      updateSubmitState();
    },
  }) as HTMLInputElement;

  const lastInput = el('input', {
    type: 'text',
    class: 'ag-contact-field',
    placeholder: s.lastNamePlaceholder,
    value: lastName,
    onInput: (e: Event) => {
      lastName = (e.target as HTMLInputElement).value;
      updateSubmitState();
    },
  }) as HTMLInputElement;

  const fpInput = el('input', {
    type: 'text',
    class: 'ag-contact-field',
    placeholder: s.fingerprintPlaceholder,
    value: fingerprint,
    onInput: (e: Event) => {
      fingerprint = (e.target as HTMLInputElement).value;
    },
  }) as HTMLInputElement;

  const noteInput = el('textarea', {
    class: 'ag-contact-field',
    rows: '2',
    placeholder: s.notePlaceholder,
    style: 'resize:vertical;',
    onInput: (e: Event) => {
      note = (e.target as HTMLTextAreaElement).value;
    },
  }) as HTMLTextAreaElement;
  noteInput.value = note;

  const shareCheckbox = el('input', {
    type: 'checkbox',
    style: 'width:18px;height:18px;accent-color:var(--ag-accent);cursor:pointer;',
    onChange: (e: Event) => {
      sharePhone = (e.target as HTMLInputElement).checked;
    },
  }) as HTMLInputElement;
  shareCheckbox.checked = sharePhone;

  root.append(
    el(
      'div',
      { class: 'ag-contacts-head' },
      el(
        'div',
        { class: 'ag-contacts-head__left' },
        el('button', { class: 'ag-icon-btn', onClick: options.onBack }, icons.back(20)),
        el('h3', { class: 'ag-contacts-head__title' }, isEdit ? s.editContact : s.newContact),
      ),
      submitBtn,
    ),
    el(
      'div',
      { class: 'ag-contact-form-card' },
      el(
        'div',
        { class: 'ag-contact-form-profile' },
        avatarEl,
        el('div', { class: 'ag-contact-form-inputs' }, firstInput, lastInput),
      ),
      fpInput,
      el('div', { class: 'ag-contact-hint' }, `${s.numberHidden} • ${s.phoneHint}`),
    ),
    el(
      'label',
      { class: 'ag-contact-switch-row' },
      el(
        'div',
        { style: 'display:flex;flex-direction:column;gap:2px;' },
        el('strong', { style: 'font-size:13px;color:var(--ag-text-primary);' }, s.sharePhoneLabel),
        el('span', { style: 'font-size:11px;color:var(--ag-text-secondary);' }, s.sharePhoneHint),
      ),
      shareCheckbox,
    ),
    el(
      'div',
      { class: 'ag-contact-form-card' },
      noteInput,
      el('div', { class: 'ag-contact-hint' }, s.noteHint),
    ),
  );

  setTimeout(() => firstInput.focus(), 20);
  return root;
}
