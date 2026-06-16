import { config, assertRuntimeConfig } from './config.js';
import { pool } from './db/pool.js';
import { cachePing } from './cache/redis.js';

assertRuntimeConfig();

const db = await pool.query('select 1 as ok');
const redis = await cachePing();

console.log(JSON.stringify({
  ok: db.rows[0].ok === 1,
  redis,
  nodeEnv: config.nodeEnv,
  digitalCredentialTrustMode: config.digitalCredentialTrustMode
}, null, 2));

await pool.end();
