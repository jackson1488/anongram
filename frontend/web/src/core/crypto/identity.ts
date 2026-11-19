export interface UserIdentity {
  publicId: string;
  fingerprint: string;
  displayName: string;
  username: string;
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
  username: string,
): Promise<UserIdentity> {
  const normalized = seedPhrase.map((w) => w.trim().toLowerCase()).join(' ');
  const hash = await sha256Hex(`anongram:v1:${normalized}`);
  const publicId = `ag_${hash.slice(0, 16)}`;
  const fingerprint = hash
    .slice(0, 32)
    .toUpperCase()
    .match(/.{1,4}/g)!
    .join(' ');
  const cleanName = displayName.trim() || 'Ghost';
  const cleanUser = username.trim().replace(/^@+/, '');
  return {
    publicId,
    fingerprint,
    displayName: cleanName,
    username: cleanUser ? `@${cleanUser}` : `@${hash.slice(0, 8)}`,
    seedPhrase: normalized.split(' '),
    createdAt: Date.now(),
  };
}
