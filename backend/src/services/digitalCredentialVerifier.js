import { config } from '../config.js';

function decodeBase64UrlJson(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=');
  return JSON.parse(Buffer.from(padded, 'base64').toString('utf8'));
}

function findEmailInObject(value) {
  if (!value || typeof value !== 'object') return null;
  if (typeof value.email === 'string') {
    return {
      email: value.email,
      emailVerified: value.email_verified !== false
    };
  }
  for (const child of Object.values(value)) {
    const found = findEmailInObject(child);
    if (found) return found;
  }
  return null;
}

export async function verifyEmailDigitalCredential({ credentialJson, nonce }) {
  if (config.digitalCredentialTrustMode !== 'dev') {
    throw new Error(
      'Digital credential verification is strict. Implement SD-JWT issuer/signature/key-binding/nonce validation or set DIGITAL_CREDENTIAL_TRUST_MODE=dev locally.'
    );
  }

  const parsed = JSON.parse(credentialJson);
  const raw = JSON.stringify(parsed);
  if (nonce && !raw.includes(nonce)) {
    throw new Error('Nonce was not found in credential response.');
  }

  let emailClaim = findEmailInObject(parsed);

  if (!emailClaim) {
    const jwtLikeParts = raw.match(/[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g) || [];
    for (const token of jwtLikeParts) {
      try {
        const payload = decodeBase64UrlJson(token.split('.')[1]);
        emailClaim = findEmailInObject(payload);
        if (emailClaim) break;
      } catch {
        // Ignore non-JWT fragments in the credential envelope.
      }
    }
  }

  if (!emailClaim?.email) {
    throw new Error('No email claim found in credential response.');
  }

  return {
    email: emailClaim.email,
    emailVerified: Boolean(emailClaim.emailVerified),
    issuer: 'dev-unverified'
  };
}
