import Redis from 'ioredis';
import { config } from '../config.js';

export const redis = config.redisUrl
  ? new Redis(config.redisUrl, { maxRetriesPerRequest: 2 })
  : null;

if (redis) {
  redis.on('error', (error) => {
    console.warn(`Redis unavailable: ${error.message}`);
  });
}

export async function cacheSetJson(key, value, ttlSeconds) {
  if (!redis) return;
  await redis.set(key, JSON.stringify(value), 'EX', ttlSeconds);
}

export async function cachePing() {
  if (!redis) return 'disabled';
  try {
    return await redis.ping();
  } catch (error) {
    return `unavailable: ${error.message}`;
  }
}
