import {
  createCipheriv,
  createDecipheriv,
  createHash,
  randomBytes,
} from 'node:crypto';

const ALGORITHM = 'aes-256-gcm';
const IV_LENGTH = 12;

/**
 * Derives a stable 32-byte key from the configured secret (any length string)
 * so operators can provide a plain passphrase in API_KEY_ENCRYPTION_SECRET
 * rather than having to generate an exact 32-byte value themselves.
 */
function deriveKey(secret: string): Buffer {
  return createHash('sha256').update(secret, 'utf8').digest();
}

/**
 * Encrypts a plaintext secret (e.g. a user-supplied AI provider API key) for
 * storage at rest. Output is a single base64 string encoding iv + authTag +
 * ciphertext, safe to store directly in a text column.
 */
export function encryptSecret(plaintext: string, secret: string): string {
  const key = deriveKey(secret);
  const iv = randomBytes(IV_LENGTH);
  const cipher = createCipheriv(ALGORITHM, key, iv);
  const ciphertext = Buffer.concat([
    cipher.update(plaintext, 'utf8'),
    cipher.final(),
  ]);
  const authTag = cipher.getAuthTag();
  return Buffer.concat([iv, authTag, ciphertext]).toString('base64');
}

/**
 * Decrypts a value produced by encryptSecret. Throws if the secret has
 * changed or the stored value has been tampered with (GCM auth tag check).
 */
export function decryptSecret(encoded: string, secret: string): string {
  const key = deriveKey(secret);
  const raw = Buffer.from(encoded, 'base64');
  const iv = raw.subarray(0, IV_LENGTH);
  const authTag = raw.subarray(IV_LENGTH, IV_LENGTH + 16);
  const ciphertext = raw.subarray(IV_LENGTH + 16);
  const decipher = createDecipheriv(ALGORITHM, key, iv);
  decipher.setAuthTag(authTag);
  const plaintext = Buffer.concat([
    decipher.update(ciphertext),
    decipher.final(),
  ]);
  return plaintext.toString('utf8');
}

/**
 * Returns a display-safe hint for a secret (e.g. "sk-...ab12") so the app can
 * show the user which key is configured without ever re-exposing the value.
 */
export function maskSecret(plaintext: string): string {
  if (plaintext.length <= 8) return '••••';
  return `${plaintext.slice(0, 4)}…${plaintext.slice(-4)}`;
}
