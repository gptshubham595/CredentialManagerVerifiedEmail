# End-to-End Status

## Code-complete

- Android email/password sign-up and sign-in with Firebase email verification.
- Android Google sign-in with Credential Manager and Firebase Auth.
- Android phone OTP with Firebase Auth.
- Android anonymous sign-in with Firebase Auth.
- Android passkey create/sign-in client calls through Credential Manager.
- Android verified email DigitalCredential request through Credential Manager.
- Android hardened biometric local gate with Keystore invalidation and expiry.
- Android Retrofit backend client.
- Android FCM service for notifications.
- Railway-ready Node backend.
- Backend Postgres migrations for users, notification tokens, verification events.
- Backend Redis session cache support.
- Backend Firebase Admin session/custom-token support.
- Backend app JWT issuance.
- Backend notification token registration and test notification endpoint.

## Runtime configuration required

- `backend/.env` or Railway variables must contain real Firebase Admin values:
  - `FIREBASE_CLIENT_EMAIL`
  - `FIREBASE_PRIVATE_KEY`
- Railway must attach Postgres and Redis services so `DATABASE_URL` and `REDIS_URL` are available.
- Android release builds should pass the deployed backend URL:

```bash
./gradlew :app:assembleDebug -PBACKEND_BASE_URL=https://your-api.up.railway.app/
```

## Production security work still required

The verified-email endpoint is wired end-to-end, but production cryptographic trust is not complete until `backend/src/services/digitalCredentialVerifier.js` is replaced with full SD-JWT verification:

- Validate issuer metadata and trust policy.
- Validate signature chain / issuer public key.
- Validate key-binding proof.
- Validate nonce/audience/replay protection.
- Validate disclosed `email` and verified-email claim.

Until then, `DIGITAL_CREDENTIAL_TRUST_MODE=dev` is only for local Android/backend wiring. Production should keep `DIGITAL_CREDENTIAL_TRUST_MODE=strict`.
