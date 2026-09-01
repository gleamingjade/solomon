// Deployment only. Talks to Upstash's REST API — given Lambda's short-lived execution model,
// a request-based HTTP call avoids the connection-pool management overhead a persistent TCP
// connection would need.
const UPSTASH_REDIS_REST_URL = process.env.UPSTASH_REDIS_REST_URL;
const UPSTASH_REDIS_REST_TOKEN = process.env.UPSTASH_REDIS_REST_TOKEN;

async function redisCommand(...args) {
  const path = args.map(encodeURIComponent).join('/');
  const url = `${UPSTASH_REDIS_REST_URL}/${path}`;

  try {
    const res = await fetch(url, {
      headers: {
        Authorization: `Bearer ${UPSTASH_REDIS_REST_TOKEN}`,
      },
    });

    const text = await res.text();

    if (!res.ok) {
      throw new Error(`HTTP ${res.status}: ${text}`);
    }

    return JSON.parse(text).result;
  } catch (e) {
    console.error('Fetch error:', e);
    console.error('Cause:', e.cause);
    throw e;
  }
}

export const redisGet = (key) => redisCommand('GET', key);
export const redisSet = (key, value) => redisCommand('SET', key, value);
export const redisExists = (key) => redisCommand('EXISTS', key);
export const redisSmembers = (key) => redisCommand('SMEMBERS', key);
export const redisIncr = (key) => redisCommand('INCR', key);
export const redisDecr = (key) => redisCommand('DECR', key);
