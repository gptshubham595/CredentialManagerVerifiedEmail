# Verified Credential Backend

Railway-ready Express API for the Android verified credential demo.

## What it provides

- Firebase Admin session verification
- App JWT issuance
- Postgres user/session/token storage
- Redis session cache
- FCM notification token registration
- Verified-email DigitalCredential endpoint shape

The verified-email endpoint is strict by default. Set `DIGITAL_CREDENTIAL_TRUST_MODE=dev` only for local wiring. Production must replace `src/services/digitalCredentialVerifier.js` with full SD-JWT issuer/signature/key-binding/nonce verification.

## Local

```bash
cp .env.example .env
npm install
npm run migrate
npm run dev
```

Android emulator default base URL is `http://10.0.2.2:8080/`.

## Railway

1. Create a Railway project.
2. Add a Postgres service and a Redis service.
3. Deploy this `backend/` folder.
4. Add env vars from `.env.example`.
5. Run `npm run migrate` once from Railway shell or locally against Railway `DATABASE_URL`.

Set Android build property when building a real APK:

```bash
./gradlew :app:assembleDebug -PBACKEND_BASE_URL=https://your-api.up.railway.app/
```
