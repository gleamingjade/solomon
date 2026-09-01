import Redis from 'ioredis';

// Local dev only. Connects straight to docker-compose's redis over plain Redis protocol (TCP).
const client = new Redis({
  host: process.env.REDIS_HOST,
  port: Number(process.env.REDIS_PORT) || 6379,
});

export const redisGet = (key) => client.get(key);
export const redisSet = (key, value) => client.set(key, value);
export const redisExists = (key) => client.exists(key);
export const redisSmembers = (key) => client.smembers(key);
export const redisIncr = (key) => client.incr(key);
export const redisDecr = (key) => client.decr(key);
