import 'dotenv/config';

function normalizePrivateKey(value) {
  if (!value) return undefined;
  let privateKey = value.trim();

  if (
    (privateKey.startsWith('"') && privateKey.endsWith('"')) ||
    (privateKey.startsWith("'") && privateKey.endsWith("'"))
  ) {
    privateKey = privateKey.slice(1, -1);
  }

  if (!privateKey.includes('BEGIN PRIVATE KEY')) {
    try {
      const decoded = Buffer.from(privateKey, 'base64').toString('utf8').trim();
      if (decoded.includes('BEGIN PRIVATE KEY')) {
        privateKey = decoded;
      }
    } catch {
      // Keep the original value and let Firebase Admin report the parse error.
    }
  }

  return privateKey.replace(/\\n/g, '\n');
}

export const config = {
  port: Number(process.env.PORT || 8080),
  nodeEnv: process.env.NODE_ENV || 'development',
  databaseUrl: process.env.DATABASE_URL,
  redisUrl: process.env.REDIS_URL,
  jwtSecret: process.env.JWT_SECRET || 'dev-only-change-me',
  jwtExpiresIn: process.env.JWT_EXPIRES_IN || '15m',
  corsOrigin: process.env.CORS_ORIGIN || '*',
  digitalCredentialTrustMode: process.env.DIGITAL_CREDENTIAL_TRUST_MODE || 'strict',
  firebase: {
    projectId: process.env.FIREBASE_PROJECT_ID,
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
    privateKey: normalizePrivateKey(process.env.FIREBASE_PRIVATE_KEY)
  }
};

export function assertRuntimeConfig() {
  if (!config.databaseUrl) {
    throw new Error('DATABASE_URL is required');
  }
  if (
    config.firebase.projectId &&
    (
      !config.firebase.clientEmail ||
      !config.firebase.privateKey ||
      config.firebase.clientEmail.includes('xxxxx') ||
      config.firebase.privateKey.includes('PASTE_SERVICE_ACCOUNT_PRIVATE_KEY_HERE') ||
      config.firebase.privateKey.includes('...')
    )
  ) {
    throw new Error(
      'Firebase Admin credentials are placeholders. Set FIREBASE_CLIENT_EMAIL and FIREBASE_PRIVATE_KEY from a Firebase service account JSON.'
    );
  }
  if (!config.jwtSecret || config.jwtSecret === 'dev-only-change-me') {
    console.warn('JWT_SECRET is using a development fallback. Set a strong secret before deploy.');
  }
}
