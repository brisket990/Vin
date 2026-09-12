import { randomInt } from 'node:crypto';

// Unambiguous alphabet (no 0/O, 1/I/L) so the code stays readable when a
// partner has to type it in by hand to join a household.
const ALPHABET = 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';

export function generateInviteCode(length = 8): string {
  let code = '';
  for (let i = 0; i < length; i++) {
    code += ALPHABET[randomInt(ALPHABET.length)];
  }
  return code;
}
