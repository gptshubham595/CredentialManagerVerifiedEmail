import { pool } from './pool.js';

const statements = [
  `create table if not exists users (
    id bigserial primary key,
    firebase_uid text unique not null,
    email text,
    phone_number text,
    display_name text,
    email_verified boolean not null default false,
    anonymous boolean not null default false,
    auth_provider text not null,
    verification_source text not null,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
  )`,
  `create table if not exists notification_tokens (
    id bigserial primary key,
    firebase_uid text not null references users(firebase_uid) on delete cascade,
    token text unique not null,
    platform text not null default 'android',
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
  )`,
  `create table if not exists verification_events (
    id bigserial primary key,
    firebase_uid text,
    subject text,
    credential_type text not null,
    nonce text,
    success boolean not null,
    reason text,
    created_at timestamptz not null default now()
  )`
];

export async function migrate() {
  for (const statement of statements) {
    await pool.query(statement);
  }
}

if (import.meta.url === `file://${process.argv[1]}`) {
  migrate()
    .then(() => {
      console.log('Migrations complete');
      return pool.end();
    })
    .catch(async (error) => {
      console.error(error);
      await pool.end();
      process.exit(1);
    });
}
