import jwt from 'jsonwebtoken';
import { config } from '../config.js';

export function issueAppJwt(profile) {
  return jwt.sign(
    {
      sub: profile.uid,
      email: profile.email,
      phoneNumber: profile.phoneNumber,
      provider: profile.authProvider,
      verificationSource: profile.verificationSource
    },
    config.jwtSecret,
    { expiresIn: config.jwtExpiresIn }
  );
}
