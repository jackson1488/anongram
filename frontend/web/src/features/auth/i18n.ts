import { i18nStore } from '../../core/i18n/i18nStore';

export interface AuthStrings {
  tagline: string;
  feat1Title: string; feat1Sub: string;
  feat2Title: string; feat2Sub: string;
  feat3Title: string; feat3Sub: string;
  createCta: string;
  restoreCta: string;
  createTitle: string;
  step1: string; step2: string; step3: string; step4: string;
  generateKeys: string;
  generating: string;
  toBackup: string;
  namePlaceholder: string;
  backupTitle: string;
  backupWarning: string;
  copyWords: string;
  wordsCopied: string;
  confirmCta: string;
  quizTitle: string;
  quizDesc: string;
  quizWord: (n: number) => string;
  quizMismatch: string;
  cancel: string;
  confirm: string;
  restoreTitle: string;
  restoreDesc: string;
  wordPlaceholder: string;
  paste: string;
  pasted: string;
  pasteShort: string;
  pasteFail: string;
  restoreFillAll: string;
  restoreInvalidWord: (n: number) => string;
  restoreBadChecksum: string;
  restoreCta2: string;
}

const RU: AuthStrings = {
  tagline: 'Zero-Knowledge Blind Relay Messenger',
  feat1Title: '100% анонимность', feat1Sub: 'Без привязки к SIM-картам, email или паспортам',
  feat2Title: 'Слепой релей', feat2Sub: 'Сервер видит только зашифрованные конверты',
  feat3Title: 'Квантовая защита', feat3Sub: '12 слов BIP-39 + криптография Ed25519 & ML-DSA',
  createCta: 'Создать анонимную личность',
  restoreCta: 'Войти по Seed-фразе (12 слов)',
  createTitle: 'Создание личности',
  step1: '1. Генерация 12 слов BIP-39 энтропии',
  step2: '2. Деривация мастер-ключа Ed25519',
  step3: '3. Пост-квантовый отпечаток ML-DSA-87',
  step4: '4. Анонимный ID сформирован локально',
  generateKeys: 'Сгенерировать ключи',
  generating: 'Генерация ключей...',
  toBackup: 'Перейти к резервной фразе',
  namePlaceholder: 'GhostWalker',
  backupTitle: 'Резервная копия (12 слов)',
  backupWarning: 'Никому не показывайте эти 12 слов. При потере фразы восстановить доступ к аккаунту невозможно.',
  copyWords: 'Скопировать 12 слов',
  wordsCopied: '12 слов скопированы в буфер обмена',
  confirmCta: 'Я записал, проверить и войти',
  quizTitle: 'Проверка памяти',
  quizDesc: 'Введите 3-е и 7-е слово вашей seed-фразы, чтобы подтвердить сохранение.',
  quizWord: (n) => `Слово #${n}`,
  quizMismatch: 'Слова не совпадают. Проверьте 3-е и 7-е слово.',
  cancel: 'Отмена',
  confirm: 'Подтвердить',
  restoreTitle: 'Восстановление (12 слов)',
  restoreDesc: 'Введите 12 слов мнемоники BIP-39 по порядку или вставьте всю фразу из буфера.',
  wordPlaceholder: 'слово',
  paste: 'Вставить фразу',
  pasted: 'Вставлено 12 слов мнемоники',
  pasteShort: 'В буфере меньше 12 слов',
  pasteFail: 'Вставьте фразу прямо в первый слот (Ctrl+V)',
  restoreFillAll: 'Заполните все 12 слов seed-фразы',
  restoreInvalidWord: (n) => `Слово #${n} не из словаря BIP-39`,
  restoreBadChecksum: 'Неверная контрольная сумма seed-фразы',
  restoreCta2: 'Восстановить доступ',
};

const EN: AuthStrings = {
  tagline: 'Zero-Knowledge Blind Relay Messenger',
  feat1Title: '100% anonymity', feat1Sub: 'No SIM cards, email or passports required',
  feat2Title: 'Blind relay', feat2Sub: 'The server only sees encrypted envelopes',
  feat3Title: 'Quantum protection', feat3Sub: '12 BIP-39 words + Ed25519 & ML-DSA cryptography',
  createCta: 'Create anonymous identity',
  restoreCta: 'Sign in with Seed phrase (12 words)',
  createTitle: 'Create identity',
  step1: '1. Generating 12 BIP-39 entropy words',
  step2: '2. Deriving Ed25519 master key',
  step3: '3. Post-quantum ML-DSA-87 fingerprint',
  step4: '4. Anonymous ID generated locally',
  generateKeys: 'Generate keys',
  generating: 'Generating keys...',
  toBackup: 'Go to backup phrase',
  namePlaceholder: 'GhostWalker',
  backupTitle: 'Backup (12 words)',
  backupWarning: 'Never show these 12 words to anyone. If the phrase is lost, the account cannot be recovered.',
  copyWords: 'Copy 12 words',
  wordsCopied: '12 words copied to clipboard',
  confirmCta: 'I wrote it down, verify and sign in',
  quizTitle: 'Memory check',
  quizDesc: 'Enter the 3rd and 7th word of your seed phrase to confirm you saved it.',
  quizWord: (n) => `Word #${n}`,
  quizMismatch: 'Words do not match. Check the 3rd and 7th word.',
  cancel: 'Cancel',
  confirm: 'Confirm',
  restoreTitle: 'Restore (12 words)',
  restoreDesc: 'Enter the 12 BIP-39 mnemonic words in order or paste the whole phrase from the clipboard.',
  wordPlaceholder: 'word',
  paste: 'Paste phrase',
  pasted: '12 mnemonic words pasted',
  pasteShort: 'Clipboard has fewer than 12 words',
  pasteFail: 'Paste the phrase directly into the first slot (Ctrl+V)',
  restoreFillAll: 'Fill in all 12 seed phrase words',
  restoreInvalidWord: (n) => `Word #${n} is not in the BIP-39 wordlist`,
  restoreBadChecksum: 'Invalid seed phrase checksum',
  restoreCta2: 'Restore access',
};

export function authT(): AuthStrings {
  return i18nStore.language === 'ru' ? RU : EN;
}
