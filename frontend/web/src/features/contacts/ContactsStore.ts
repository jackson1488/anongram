import { kvStore } from '../../core/storage/kvStore';

export interface Contact {
  id: string;
  name: string;
  firstName: string;
  lastName: string;
  handle: string;
  fingerprint: string;
  emoji: string;
  color: string;
  isOnline: boolean;
  isSasVerified: boolean;
  sharePhone: boolean;
  note: string;
}

export interface AddContactInput {
  firstName: string;
  lastName?: string;
  fingerprint?: string;
  sharePhone?: boolean;
  note?: string;
}

const KEY_CONTACTS = 'contacts.list.v1';

const INITIAL_CONTACTS: Contact[] = [
  {
    id: 'ghost_node',
    name: 'Ghost Node #42',
    firstName: 'Ghost Node',
    lastName: '#42',
    handle: '@ghost42',
    fingerprint: '0x4D9F…88E1',
    emoji: '🕶️',
    color: '#8B5CF6',
    isOnline: true,
    isSasVerified: true,
    sharePhone: false,
    note: '',
  },
  {
    id: 'maya_cipher',
    name: 'Maya Cipher',
    firstName: 'Maya',
    lastName: 'Cipher',
    handle: '@maya_e2ee',
    fingerprint: '0x1B77…D3A0',
    emoji: '⚡',
    color: '#EC4899',
    isOnline: false,
    isSasVerified: true,
    sharePhone: false,
    note: '',
  },
  {
    id: 'zero_lab',
    name: 'Zero-Knowledge Lab',
    firstName: 'Zero-Knowledge',
    lastName: 'Lab',
    handle: '@zklab',
    fingerprint: '0xA50C…12F7',
    emoji: '🔐',
    color: '#10B981',
    isOnline: true,
    isSasVerified: false,
    sharePhone: false,
    note: '',
  },
  {
    id: 'relay_alpha',
    name: 'Onion Relay Alpha',
    firstName: 'Onion Relay',
    lastName: 'Alpha',
    handle: '@relay_v3',
    fingerprint: '0x77E0…B4A9',
    emoji: '🧅',
    color: '#F59E0B',
    isOnline: false,
    isSasVerified: false,
    sharePhone: false,
    note: '',
  },
];

export function contactMonogram(name: string): string {
  const parts = name
    .trim()
    .split(/\s+/)
    .filter(Boolean);
  if (parts.length === 0) return 'AG';
  return parts
    .slice(0, 2)
    .map((p) => p[0].toUpperCase())
    .join('');
}

function randomHex(len: number): string {
  const chars = '0123456789ABCDEF';
  let out = '';
  for (let i = 0; i < len; i++) {
    out += chars[Math.floor(Math.random() * chars.length)];
  }
  return out;
}

class ContactsStore {
  private items: Contact[];
  private listeners = new Set<() => void>();

  constructor() {
    const saved = kvStore.getJson<Contact[] | null>(KEY_CONTACTS, null);
    this.items = saved && Array.isArray(saved) && saved.length > 0 ? saved : [...INITIAL_CONTACTS];
  }

  private persist(): void {
    kvStore.setJson(KEY_CONTACTS, this.items);
    for (const fn of this.listeners) fn();
  }

  subscribe(fn: () => void): () => void {
    this.listeners.add(fn);
    return () => this.listeners.delete(fn);
  }

  getAll(): Contact[] {
    return this.items;
  }

  getById(id: string): Contact | undefined {
    return this.items.find((c) => c.id === id);
  }

  search(query: string): Contact[] {
    const q = query.trim().toLowerCase();
    if (!q) return this.items;
    return this.items.filter(
      (c) =>
        c.name.toLowerCase().includes(q) ||
        c.handle.toLowerCase().includes(q) ||
        c.fingerprint.toLowerCase().includes(q),
    );
  }

  addContact(input: AddContactInput): Contact {
    const first = input.firstName.trim();
    const last = (input.lastName ?? '').trim();
    const fullName = [first, last].filter(Boolean).join(' ') || 'AnonContact';
    const fp =
      input.fingerprint && input.fingerprint.trim()
        ? input.fingerprint.trim()
        : `0x${randomHex(4)}…${randomHex(4)}`;
    const slug = fullName.toLowerCase().replace(/[^a-z0-9а-яё]+/gi, '_').replace(/^_|_$/g, '');
    const contact: Contact = {
      id: `c_${Date.now().toString(36)}_${randomHex(3).toLowerCase()}`,
      name: fullName,
      firstName: first || fullName,
      lastName: last,
      handle: `@${slug || 'anon'}`,
      fingerprint: fp,
      emoji: contactMonogram(fullName),
      color: '#6366F1',
      isOnline: false,
      isSasVerified: false,
      sharePhone: Boolean(input.sharePhone),
      note: (input.note ?? '').trim(),
    };
    this.items = [contact, ...this.items];
    this.persist();
    return contact;
  }

  updateContact(id: string, input: AddContactInput): Contact | undefined {
    const existing = this.getById(id);
    if (!existing) return undefined;
    const first = input.firstName.trim();
    const last = (input.lastName ?? '').trim();
    const fullName = [first, last].filter(Boolean).join(' ') || existing.name;
    const updated: Contact = {
      ...existing,
      name: fullName,
      firstName: first || existing.firstName,
      lastName: last,
      fingerprint:
        input.fingerprint && input.fingerprint.trim() ? input.fingerprint.trim() : existing.fingerprint,
      sharePhone: input.sharePhone !== undefined ? input.sharePhone : existing.sharePhone,
      note: input.note !== undefined ? input.note.trim() : existing.note,
    };
    this.items = this.items.map((c) => (c.id === id ? updated : c));
    this.persist();
    return updated;
  }

  toggleSasVerified(id: string): void {
    this.items = this.items.map((c) =>
      c.id === id ? { ...c, isSasVerified: !c.isSasVerified } : c,
    );
    this.persist();
  }

  deleteContact(id: string): void {
    this.items = this.items.filter((c) => c.id !== id);
    this.persist();
  }
}

export const contactsStore = new ContactsStore();
