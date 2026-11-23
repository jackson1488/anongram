import { i18nStore } from '../../core/i18n/i18nStore';

export interface ContactsStrings {
  title: string;
  searchPlaceholder: string;
  newContact: string;
  editContact: string;
  createBtn: string;
  saveBtn: string;
  addNewCardTitle: string;
  addNewCardSub: string;
  openQrBtn: string;
  emptyList: string;
  chatBtn: string;
  otpBtn: string;
  otpTooltip: string;
  online: string;
  lastSeenRecently: string;
  sasVerified: string;
  sasUnverified: string;
  verifySasBtn: string;
  unverifySasBtn: string;
  firstNamePlaceholder: string;
  lastNamePlaceholder: string;
  fingerprintPlaceholder: string;
  fingerprintLabel: string;
  sharePhoneLabel: string;
  sharePhoneHint: string;
  notePlaceholder: string;
  noteHint: string;
  numberHidden: string;
  phoneHint: string;
  detailsTitle: string;
  noteLabel: string;
  openChat: string;
  openOtpChat: string;
  editBtn: string;
  deleteBtn: string;
  deletedToast: string;
  createdToast: string;
  updatedToast: string;
  copiedToast: string;
  copyFingerprint: string;
}

const RU: ContactsStrings = {
  title: 'Контакты',
  searchPlaceholder: 'Поиск контакта или отпечатка...',
  newContact: 'Новый контакт',
  editContact: 'Изменить контакт',
  createBtn: 'Создать',
  saveBtn: 'Сохранить',
  addNewCardTitle: 'Добавить контакт',
  addNewCardSub: 'По имени, Public ID или QR-коду',
  openQrBtn: 'QR-хаб',
  emptyList: 'Контакты не найдены',
  chatBtn: 'Чат',
  otpBtn: 'OTP',
  otpTooltip: 'OTP-чат (сгораемый)',
  online: 'в сети',
  lastSeenRecently: 'был(а) недавно',
  sasVerified: 'SAS подтверждён',
  sasUnverified: 'SAS не подтверждён',
  verifySasBtn: 'Подтвердить SAS',
  unverifySasBtn: 'Снять SAS-метку',
  firstNamePlaceholder: 'Имя (обязательно)',
  lastNamePlaceholder: 'Фамилия (необязательно)',
  fingerprintPlaceholder: 'Public ID / Ed25519 (необязательно)',
  fingerprintLabel: 'Отпечаток Ed25519',
  sharePhoneLabel: 'Показать мой номер телефона',
  sharePhoneHint: 'Отключите, если не хотите, чтобы собеседник видел(а) Ваш номер телефона.',
  notePlaceholder: 'Заметка',
  noteHint: 'Заметку видите только Вы.',
  numberHidden: 'Номер скрыт',
  phoneHint: 'Номер телефона будет виден, когда собеседник добавит Вас в контакты.',
  detailsTitle: 'Контакт',
  noteLabel: 'Заметка',
  openChat: 'Написать сообщение',
  openOtpChat: 'OTP-комната (сгораемая)',
  editBtn: 'Изменить',
  deleteBtn: 'Удалить контакт',
  deletedToast: 'Контакт удалён',
  createdToast: 'Контакт добавлен',
  updatedToast: 'Контакт обновлён',
  copiedToast: 'Отпечаток скопирован',
  copyFingerprint: 'Скопировать отпечаток',
};

const EN: ContactsStrings = {
  title: 'Contacts',
  searchPlaceholder: 'Search contact or fingerprint...',
  newContact: 'New Contact',
  editContact: 'Edit Contact',
  createBtn: 'Create',
  saveBtn: 'Save',
  addNewCardTitle: 'Add Contact',
  addNewCardSub: 'By name, Public ID or QR code',
  openQrBtn: 'QR Hub',
  emptyList: 'No contacts found',
  chatBtn: 'Chat',
  otpBtn: 'OTP',
  otpTooltip: 'OTP Chat (burn-after-reading)',
  online: 'online',
  lastSeenRecently: 'last seen recently',
  sasVerified: 'SAS Verified',
  sasUnverified: 'SAS Unverified',
  verifySasBtn: 'Verify SAS',
  unverifySasBtn: 'Remove SAS mark',
  firstNamePlaceholder: 'First name (required)',
  lastNamePlaceholder: 'Last name (optional)',
  fingerprintPlaceholder: 'Public ID / Ed25519 (optional)',
  fingerprintLabel: 'Ed25519 Fingerprint',
  sharePhoneLabel: 'Share my phone number',
  sharePhoneHint: "Disable if you don't want them to see your phone number.",
  notePlaceholder: 'Notes',
  noteHint: 'Only you can see this note.',
  numberHidden: 'Number hidden',
  phoneHint: 'Phone number will be visible when the contact adds you back.',
  detailsTitle: 'Contact',
  noteLabel: 'Note',
  openChat: 'Send Message',
  openOtpChat: 'OTP Room (Burnable)',
  editBtn: 'Edit',
  deleteBtn: 'Delete Contact',
  deletedToast: 'Contact deleted',
  createdToast: 'Contact added',
  updatedToast: 'Contact updated',
  copiedToast: 'Fingerprint copied',
  copyFingerprint: 'Copy fingerprint',
};

export function contactsT(): ContactsStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}
