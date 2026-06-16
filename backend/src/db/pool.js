import pg from 'pg';
import { config } from '../config.js';

if (!config.databaseUrl) {
  throw new Error('DATABASE_URL is required. Add a Railway Postgres service or set DATABASE_URL manually.');
}

export const pool = new pg.Pool({
  connectionString: config.databaseUrl,
  ssl: config.nodeEnv === 'production' ? { rejectUnauthorized: false } : undefined
});

export async function query(text, params) {
  return pool.query(text, params);
}
