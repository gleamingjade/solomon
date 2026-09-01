import {
  redisGet,
  redisSet,
  redisExists,
  redisSmembers,
  redisIncr,
  redisDecr,
} from './redis/index.js';

const SERVERS_KEY = 'servers';

const healthKey = (id) => `server:health:${id}`;
const countKey = (id) => `server:${id}:count`;
const trialServerKey = (id) => `trial:${id}:server`;

async function isAlive(serverId) {
  const exists = await redisExists(healthKey(serverId));
  return Number(exists) === 1;
}

async function getCount(serverId) {
  const count = await redisGet(countKey(serverId));
  const parsed = parseInt(count, 10);
  return isNaN(parsed) || parsed < 0 ? 0 : parsed;
}

function response(statusCode, body) {
  return {
    statusCode,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  };
}

export const handler = async (event) => {
  const trialId = event.queryStringParameters?.trialId;

  if (!trialId) {
    return response(400, { message: 'trialId is required' });
  }

  const currentServerId = await redisGet(trialServerKey(trialId));

  if (!currentServerId) {
    return response(404, { message: `No server allocated for trial ${trialId}` });
  }

  if (await isAlive(currentServerId)) {
    return response(200, { serverId: currentServerId });
  }

  // Current server is dead -> reassign.
  const serverIds = await redisSmembers(SERVERS_KEY);

  if (!serverIds || !Array.isArray(serverIds) || serverIds.length === 0) {
    return response(503, { message: 'No available server' });
  }

  const healthChecks = await Promise.all(
    serverIds.map(async (id) => ({ id, alive: await isAlive(id) }))
  );

  const aliveServerIds = healthChecks.filter((item) => item.alive).map((item) => item.id);

  if (aliveServerIds.length === 0) {
    return response(503, { message: 'No available server' });
  }

  const withCounts = await Promise.all(
    aliveServerIds.map(async (id) => ({ id, count: await getCount(id) }))
  );

  withCounts.sort((a, b) => a.count - b.count);

  const newServerId = withCounts[0].id;

  await redisIncr(countKey(newServerId));
  await redisDecr(countKey(currentServerId));
  await redisSet(trialServerKey(trialId), newServerId);

  return response(200, { serverId: newServerId });
};
