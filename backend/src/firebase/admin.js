import admin from 'firebase-admin';
import { config } from '../config.js';

function credential() {
  if (config.firebase.projectId && config.firebase.clientEmail && config.firebase.privateKey) {
    return admin.credential.cert({
      projectId: config.firebase.projectId,
      clientEmail: config.firebase.clientEmail,
      privateKey: config.firebase.privateKey
    });
  }
  return admin.credential.applicationDefault();
}

export function firebaseAdmin() {
  if (!admin.apps.length) {
    admin.initializeApp({
      credential: credential(),
      projectId: config.firebase.projectId
    });
  }
  return admin;
}

export async function verifyFirebaseIdToken(idToken) {
  return firebaseAdmin().auth().verifyIdToken(idToken);
}

export async function createOrUpdateVerifiedEmailUser(email) {
  const auth = firebaseAdmin().auth();
  try {
    const user = await auth.getUserByEmail(email);
    if (!user.emailVerified) {
      await auth.updateUser(user.uid, { emailVerified: true });
    }
    return auth.getUser(user.uid);
  } catch (error) {
    if (error.code !== 'auth/user-not-found') throw error;
    return auth.createUser({ email, emailVerified: true });
  }
}

export async function createCustomToken(uid, claims = {}) {
  return firebaseAdmin().auth().createCustomToken(uid, claims);
}

export async function sendFcmToToken(token, notification, data = {}) {
  return firebaseAdmin().messaging().send({
    token,
    notification,
    data
  });
}
