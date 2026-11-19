export const AVATAR_GRADIENTS: Array<[string, string]> = [
  ['#8B5CF6', '#3B82F6'],
  ['#EC4899', '#8B5CF6'],
  ['#10B981', '#06B6D4'],
  ['#F59E0B', '#EF4444'],
  ['#6366F1', '#A855F7'],
];

export interface UserIdentity {
  publicId: string;
  fingerprint: string;
  ed25519PublicKey: string;
  displayName: string;
  username: string;
  avatarIndex: number;
  seedPhrase: string[];
  createdAt: number;
}

async function sha256Hex(input: string): Promise<string> {
  const encoded = new TextEncoder().encode(input);
  const digest = await crypto.subtle.digest('SHA-256', encoded);
  return Array.from(new Uint8Array(digest))
    .map((b) => b.toString(16).padStart(2, '0'))
    .join('');
}

export async function deriveIdentity(
  seedPhrase: string[],
  displayName: string,
  username = '',
  avatarIndex = 0,
): Promise<UserIdentity> {
  const normalized = seedPhrase.map((w) => w.trim().toLowerCase()).join(' ');
  const hash = await sha256Hex(`anongram:v1:${normalized}`);
  const edHash = await sha256Hex(`anongram:ed25519:${normalized}`);
  const publicId = `ag_${hash.slice(0, 16)}`;
  const fingerprint = hash
    .slice(0, 32)
    .toUpperCase()
    .match(/.{1,4}/g)!
    .join(' ');
  const cleanName = displayName.trim() || 'GhostWalker';
  const cleanUser = username.trim().replace(/^@+/, '');
  return {
    publicId,
    fingerprint,
    ed25519PublicKey: `ed25519:${edHash.slice(0, 24)}`,
    displayName: cleanName,
    username: cleanUser ? `@${cleanUser}` : `@${hash.slice(0, 8)}`,
    avatarIndex: avatarIndex % AVATAR_GRADIENTS.length,
    seedPhrase: normalized.split(' '),
    createdAt: Date.now(),
  };
}
