import { Router } from 'express';
import { z } from 'zod';
import { cacheSetJson } from '../cache/redis.js';
import { createCustomToken, createOrUpdateVerifiedEmailUser, verifyFirebaseIdToken } from '../firebase/admin.js';
import { issueAppJwt } from '../services/jwt.js';
import { recordVerificationEvent, upsertNotificationToken, upsertUserFromFirebase } from '../services/users.js';
import { verifyEmailDigitalCredential } from '../services/digitalCredentialVerifier.js';

export const authRouter = Router();

const sessionSchema = z.object({
  firebaseIdToken: z.string().min(20),
  fcmToken: z.string().optional().nullable()
});

const verifiedEmailSchema = z.object({
  credentialJson: z.string().min(10),
  nonce: z.string().min(8),
  fcmToken: z.string().optional().nullable()
});

authRouter.post('/session', async (req, res, next) => {
  try {
    const body = sessionSchema.parse(req.body);
    const decoded = await verifyFirebaseIdToken(body.firebaseIdToken);
    const profile = await upsertUserFromFirebase(decoded, decoded.firebase?.sign_in_provider || 'firebase');
    if (body.fcmToken) {
      await upsertNotificationToken(decoded.uid, body.fcmToken, 'android');
    }
    const appJwt = issueAppJwt(profile);
    await cacheSetJson(`session:${decoded.uid}`, profile, 15 * 60);
    res.json({ appJwt, profile });
  } catch (error) {
    next(error);
  }
});

authRouter.post('/verified-email', async (req, res, next) => {
  try {
    const body = verifiedEmailSchema.parse(req.body);
    const verified = await verifyEmailDigitalCredential(body);
    if (!verified.emailVerified) {
      throw new Error('Credential email is not marked verified.');
    }

    const firebaseUser = await createOrUpdateVerifiedEmailUser(verified.email);
    const firebaseCustomToken = await createCustomToken(firebaseUser.uid, {
      verificationSource: 'verified_email_credential'
    });

    const decodedLike = {
      uid: firebaseUser.uid,
      email: verified.email,
      email_verified: true,
      firebase: { sign_in_provider: 'verified_email_credential' }
    };
    const profile = await upsertUserFromFirebase(decodedLike, 'verified_email_credential');
    if (body.fcmToken) {
      await upsertNotificationToken(firebaseUser.uid, body.fcmToken, 'android');
    }
    await recordVerificationEvent({
      firebaseUid: firebaseUser.uid,
      subject: verified.email,
      credentialType: 'email',
      nonce: body.nonce,
      success: true,
      reason: verified.issuer
    });

    const appJwt = issueAppJwt(profile);
    await cacheSetJson(`session:${firebaseUser.uid}`, profile, 15 * 60);
    res.json({ firebaseCustomToken, appJwt, profile });
  } catch (error) {
    await recordVerificationEvent({
      credentialType: 'email',
      nonce: req.body?.nonce,
      success: false,
      reason: error.message
    }).catch(() => {});
    next(error);
  }
});
