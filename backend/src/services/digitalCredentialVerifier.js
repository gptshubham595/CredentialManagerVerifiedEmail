import { config } from '../config.js';

export class DigitalCredentialVerificationError extends Error {
  constructor(message) {
    super(message);
    this.name = 'DigitalCredentialVerificationError';
    this.statusCode = 400;
  }
}

function verificationError(message) {
  return new DigitalCredentialVerificationError(message);
}

function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4)) % 4), '=');
  return Buffer.from(padded, 'base64').toString('utf8');
}

function decodeBase64UrlJson(value) {
  return JSON.parse(decodeBase64Url(value));
}

function collectStringValues(value, output = []) {
  if (typeof value === 'string') {
    output.push(value);
    return output;
  }
  if (!value || typeof value !== 'object') return output;
  for (const child of Object.values(value)) {
    collectStringValues(child, output);
  }
  return output;
}

function assignClaim(claims, name, value) {
  if (typeof name !== 'string') return;
  if (
    [
      'email',
      'email_verified',
      'name',
      'given_name',
      'family_name',
      'picture',
      'hd'
    ].includes(name)
  ) {
    claims[name] = value;
  }
}

function claimsFromSdJwtDisclosures(value) {
  const claims = {};
  for (const disclosure of value.split('~').slice(1)) {
    if (!disclosure || disclosure.includes('.')) continue;
    try {
      const decoded = decodeBase64UrlJson(disclosure);
      if (Array.isArray(decoded) && decoded.length >= 3) {
        assignClaim(claims, decoded[1], decoded[2]);
      }
    } catch {
      // Ignore non-disclosure segments.
    }
  }
  return Object.keys(claims).length > 0 ? claims : null;
}

function findDecodedJsonPayloads(value) {
  const payloads = [];
  for (const candidate of collectStringValues(value)) {
    for (const segment of candidate.split('~')) {
      const jwtParts = segment.split('.');
      if (jwtParts.length === 3) {
        try {
          payloads.push(decodeBase64UrlJson(jwtParts[1]));
        } catch {
          // Ignore non-JWT segments.
        }
      }
    }
  }
  return payloads;
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

function findEmailCredentialClaims(value) {
  const direct = findEmailInObject(value);
  if (direct) return direct;

  for (const payload of findDecodedJsonPayloads(value)) {
    const fromPayload = findEmailInObject(payload);
    if (fromPayload) return fromPayload;
  }

  for (const candidate of collectStringValues(value)) {
    const disclosureClaims = claimsFromSdJwtDisclosures(candidate);
    if (disclosureClaims?.email) {
      return {
        email: disclosureClaims.email,
        emailVerified: disclosureClaims.email_verified !== false
      };
    }
  }

  return null;
}

function responseContainsNonce(value, nonce) {
  if (!nonce) return true;

  const raw = JSON.stringify(value);
  if (raw.includes(nonce)) return true;

  for (const payload of findDecodedJsonPayloads(value)) {
    if (JSON.stringify(payload).includes(nonce)) return true;
  }

  return false;
}

export async function verifyEmailDigitalCredential({ credentialJson, nonce }) {
  if (config.digitalCredentialTrustMode !== 'dev') {
    throw verificationError(
      'Digital credential verification is strict. Implement SD-JWT issuer/signature/key-binding/nonce validation or set DIGITAL_CREDENTIAL_TRUST_MODE=dev locally.'
    );
  }

  const parsed = JSON.parse(credentialJson);
  if (!responseContainsNonce(parsed, nonce)) {
     throw verificationError(
        "Nonce was not found in credential response. credentialJson=" + credentialJson
      );
  }

  const emailClaim = findEmailCredentialClaims(parsed);

  if (!emailClaim?.email) {
    throw verificationError('No email claim found in credential response.');
  }

  return {
    email: emailClaim.email,
    emailVerified: Boolean(emailClaim.emailVerified),
    issuer: 'dev-unverified'
  };
}
