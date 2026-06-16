import { Router } from 'express';
import { z } from 'zod';
import { sendFcmToToken, verifyFirebaseIdToken } from '../firebase/admin.js';
import { findLatestNotificationToken, upsertNotificationToken } from '../services/users.js';

export const notificationsRouter = Router();

const registerSchema = z.object({
  firebaseIdToken: z.string().min(20),
  fcmToken: z.string().min(20),
  platform: z.string().default('android')
});

const testSchema = z.object({
  firebaseIdToken: z.string().min(20),
  title: z.string().default('Verified Credential'),
  body: z.string().default('Backend notification is connected.')
});

notificationsRouter.post('/register', async (req, res, next) => {
  try {
    const body = registerSchema.parse(req.body);
    const decoded = await verifyFirebaseIdToken(body.firebaseIdToken);
    await upsertNotificationToken(decoded.uid, body.fcmToken, body.platform);
    res.json({ ok: true, message: 'Notification token registered.' });
  } catch (error) {
    next(error);
  }
});

notificationsRouter.post('/test', async (req, res, next) => {
  try {
    const body = testSchema.parse(req.body);
    const decoded = await verifyFirebaseIdToken(body.firebaseIdToken);
    const token = await findLatestNotificationToken(decoded.uid);
    if (!token) {
      res.status(404).json({ ok: false, message: 'No notification token registered.' });
      return;
    }
    const messageId = await sendFcmToToken(token, {
      title: body.title,
      body: body.body
    });
    res.json({ ok: true, message: messageId });
  } catch (error) {
    next(error);
  }
});
