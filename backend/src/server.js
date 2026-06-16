import express from 'express';
import cors from 'cors';
import helmet from 'helmet';
import morgan from 'morgan';
import { config, assertRuntimeConfig } from './config.js';
import { pool } from './db/pool.js';
import { migrate } from './db/migrate.js';
import { cachePing } from './cache/redis.js';
import { authRouter } from './routes/auth.js';
import { notificationsRouter } from './routes/notifications.js';

assertRuntimeConfig();
await migrate();

const app = express();

app.use(helmet());
app.use(cors({ origin: config.corsOrigin === '*' ? true : config.corsOrigin }));
app.use(express.json({ limit: '1mb' }));
app.use(morgan('dev'));

app.get('/health', async (_req, res) => {
  const db = await pool.query('select 1 as ok');
  const redis = await cachePing();
  res.json({
    ok: true,
    db: db.rows[0].ok === 1,
    redis
  });
});

app.use('/api/auth', authRouter);
app.use('/api/notifications', notificationsRouter);

app.use((error, _req, res, _next) => {
  const status = error.name === 'ZodError' ? 400 : 500;
  console.error('[api-error]', {
    status,
    name: error.name,
    message: error.message,
    stack: config.nodeEnv === 'production' ? undefined : error.stack
  });
  res.status(status).json({
    ok: false,
    message: error.message || 'Unexpected backend error'
  });
});

app.listen(config.port, () => {
  console.log(`Verified Credential backend listening on :${config.port}`);
});
