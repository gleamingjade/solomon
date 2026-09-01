// REDIS_MODE=tcp -> local (docker-compose), anything else (default) -> Upstash REST.
// Handler code above doesn't need to know which one got picked - just the six function signatures.
import * as redisTcp from './redis-tcp.js';
import * as redisRest from './redis-rest.js';

const impl = process.env.REDIS_MODE === 'tcp' ? redisTcp : redisRest;

export const redisGet = impl.redisGet;
export const redisSet = impl.redisSet;
export const redisExists = impl.redisExists;
export const redisSmembers = impl.redisSmembers;
export const redisIncr = impl.redisIncr;
export const redisDecr = impl.redisDecr;
