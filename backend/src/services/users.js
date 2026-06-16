import { query } from '../db/pool.js';

export function providerFromFirebase(decodedToken) {
  return decodedToken.firebase?.sign_in_provider || 'firebase';
}

export async function upsertUserFromFirebase(decodedToken, verificationSource) {
  const firebaseUid = decodedToken.uid;
  const provider = providerFromFirebase(decodedToken);
  const email = decodedToken.email || null;
  const phone = decodedToken.phone_number || null;
  const displayName = decodedToken.name || null;
  const emailVerified = Boolean(decodedToken.email_verified);
  const anonymous = provider === 'anonymous';

  const result = await query(
    `insert into users (
      firebase_uid, email, phone_number, display_name, email_verified,
      anonymous, auth_provider, verification_source
    ) values ($1,$2,$3,$4,$5,$6,$7,$8)
    on conflict (firebase_uid) do update set
      email = excluded.email,
      phone_number = excluded.phone_number,
      display_name = excluded.display_name,
      email_verified = excluded.email_verified,
      anonymous = excluded.anonymous,
      auth_provider = excluded.auth_provider,
      verification_source = excluded.verification_source,
      updated_at = now()
    returning *`,
    [firebaseUid, email, phone, displayName, emailVerified, anonymous, provider, verificationSource]
  );
  return toProfile(result.rows[0]);
}

export async function upsertNotificationToken(firebaseUid, token, platform = 'android') {
  await query(
    `insert into notification_tokens (firebase_uid, token, platform)
     values ($1,$2,$3)
     on conflict (token) do update set
       firebase_uid = excluded.firebase_uid,
       platform = excluded.platform,
       updated_at = now()`,
    [firebaseUid, token, platform]
  );
}

export async function findLatestNotificationToken(firebaseUid) {
  const result = await query(
    `select token from notification_tokens
     where firebase_uid = $1
     order by updated_at desc
     limit 1`,
    [firebaseUid]
  );
  return result.rows[0]?.token || null;
}

export async function recordVerificationEvent(event) {
  await query(
    `insert into verification_events
     (firebase_uid, subject, credential_type, nonce, success, reason)
     values ($1,$2,$3,$4,$5,$6)`,
    [
      event.firebaseUid || null,
      event.subject || null,
      event.credentialType,
      event.nonce || null,
      event.success,
      event.reason || null
    ]
  );
}

export function toProfile(row) {
  return {
    uid: row.firebase_uid,
    email: row.email,
    phoneNumber: row.phone_number,
    emailVerified: row.email_verified,
    authProvider: row.auth_provider,
    verificationSource: row.verification_source
  };
}
