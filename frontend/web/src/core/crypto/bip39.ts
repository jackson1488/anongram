import { BIP39_WORDLIST } from './bip39Wordlist';

const WORDS = BIP39_WORDLIST;
const WORD_INDEX = new Map<string, number>(WORDS.map((w, i) => [w, i]));

async function sha256(bytes: Uint8Array): Promise<Uint8Array> {
  const buf = new Uint8Array(bytes).buffer;
  return new Uint8Array(await crypto.subtle.digest('SHA-256', buf));
}

/** 128-bit entropy + 4-bit SHA-256 checksum -> 12 words (BIP-39). */
export async function generate12Words(): Promise<string[]> {
  const entropy = new Uint8Array(16);
  crypto.getRandomValues(entropy);
  const hash = await sha256(entropy);
  let bits = '';
  for (const b of entropy) bits += b.toString(2).padStart(8, '0');
  bits += hash[0].toString(2).padStart(8, '0').slice(0, 4);
  const out: string[] = [];
  for (let i = 0; i < 12; i++) out.push(WORDS[parseInt(bits.slice(i * 11, i * 11 + 11), 2)]);
  return out;
}

export function normalizeSeed(raw: string): string[] {
  return raw
    .trim()
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean);
}

export function findSuggestions(prefix: string, limit = 6): string[] {
  const clean = prefix.trim().toLowerCase();
  if (clean.length < 1) return [];
  return WORDS.filter((w) => w.startsWith(clean)).slice(0, limit);
}

export function isValidWord(word: string): boolean {
  return WORD_INDEX.has(word.trim().toLowerCase());
}

/** Validates 12 known words and the BIP-39 checksum. */
export async function validateMnemonic(words: string[]): Promise<boolean> {
  if (words.length !== 12) return false;
  let bits = '';
  for (const w of words) {
    const idx = WORD_INDEX.get(w.trim().toLowerCase());
    if (idx === undefined) return false;
    bits += idx.toString(2).padStart(11, '0');
  }
  const entropy = new Uint8Array(16);
  for (let i = 0; i < 16; i++) entropy[i] = parseInt(bits.slice(i * 8, i * 8 + 8), 2);
  const hash = await sha256(entropy);
  return hash[0].toString(2).padStart(8, '0').slice(0, 4) === bits.slice(128, 132);
}
