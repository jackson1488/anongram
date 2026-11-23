import { i18nStore } from '../../core/i18n/i18nStore';

export interface ProfileStrings {
  title: string;
  publicIdLabel: string;
  ed25519Label: string;
  fingerprintLabel: string;
  copyPublicId: string;
  copiedPublicId: string;
  seedTitle: string;
  seedValue: string;
  quantumTitle: string;
  quantumValue: string;
  verifyTitle: string;
  verifyValue: string;
  accountTitle: string;
  accountValue: string;
  logoutBtn: string;
  logoutConfirm: string;
}

const RU: ProfileStrings = {
  title: 'Крипто-Профиль',
  publicIdLabel: 'Public ID',
  ed25519Label: 'Публичный ключ Ed25519',
  fingerprintLabel: 'Отпечаток',
  copyPublicId: 'Копировать Public ID',
  copiedPublicId: 'Public ID скопирован',
  seedTitle: 'Seed-фраза',
  seedValue: '12 слов · BIP-39 · 100% Offline',
  quantumTitle: 'Квантовая защита',
  quantumValue: 'Гибридный бандл ML-DSA-87',
  verifyTitle: 'Верификация',
  verifyValue: 'Blind Relay Hash · SHA-256',
  accountTitle: 'Аккаунт',
  accountValue: 'Без номера телефона, email и паролей',
  logoutBtn: 'Выйти из аккаунта (стереть ключи)',
  logoutConfirm: 'Выйти и стереть локальные ключи из браузера?',
};

const EN: ProfileStrings = {
  title: 'Crypto Profile',
  publicIdLabel: 'Public ID',
  ed25519Label: 'Ed25519 Public Key',
  fingerprintLabel: 'Fingerprint',
  copyPublicId: 'Copy Public ID',
  copiedPublicId: 'Public ID copied',
  seedTitle: 'Seed Phrase',
  seedValue: '12 words · BIP-39 · 100% Offline',
  quantumTitle: 'Quantum Protection',
  quantumValue: 'ML-DSA-87 Hybrid Bundle',
  verifyTitle: 'Verification',
  verifyValue: 'Blind Relay Hash · SHA-256',
  accountTitle: 'Account',
  accountValue: 'No phone number, email or passwords',
  logoutBtn: 'Log out (wipe local keys)',
  logoutConfirm: 'Log out and wipe local keys from this browser?',
};

export function profileT(): ProfileStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}
