# Local Dev Environment vs. Deployment Environment

Why local development never talks to real cloud infrastructure, and how that principle was applied to the chat-routing Lambda specifically.

## The Principle

Deployment infra is expected to change over time — this is normal, not a sign something was designed wrong. Redis might move from Upstash to ElastiCache. MySQL might move from AWS to GCP. Whatever's cheapest/best fits at the time wins, and that decision should be revisitable without dread.

The trap to avoid: spinning up a "dev" instance of whatever the *current* prod vendor happens to be (e.g. a second Upstash database just for local development) and pointing local dev at that. It looks convenient, but it quietly **couples local development to today's prod vendor choice** — every time prod infra changes, local dev has to be re-plumbed to match, and "swap Redis providers" stops being a small, isolated decision.

The fix: local development is reproduced **entirely in Docker**, independent of whatever real vendor prod currently uses. This is the 12-factor app principle — backing services (DB, cache, queue) are attached resources, swapped via config/environment variables, never hardcoded into the app itself. The app shouldn't be able to tell, from its own code, whether it's talking to a Docker container or a managed cloud service — only its environment variables know that.

## Why This Doc Exists

This split is easy to forget the reasoning behind during later maintenance, especially for the chat-routing Lambda (see [[Chat Routing System Design]]) — it's the one piece of this system that runs on genuinely different infrastructure between local and prod (a plain container locally vs. AWS Lambda's own invocation model in prod), so "why does this have two entry points" isn't self-evident from the code alone. This records how and why it's split.

## Applied Example: The Chat-Routing Lambda

The Lambda that resolves `connect-info` (and reassigns a trial to a new server on failover) needs two independent things to differ between local and deployment — and they're solved by two *different* mechanisms, not one:

### Axis 1: Which Redis to talk to — solved by an env var

Redis access sits behind one small interface (`get`/`set`/`exists`/`smembers`/`incr`/`decr`), with two implementations:

```
redis/
  redis-rest.js   // Upstash REST API — used in deployment
  redis-tcp.js    // ioredis over plain TCP — used locally, against docker-compose's redis
  index.js        // picks one based on REDIS_MODE
```

The rest of the handler (`isAlive`, `getCount`, the handler itself) only ever calls the interface, never either implementation directly — same shape as a port/adapter split on the Spring Boot side.

### Axis 2: How the container gets invoked — solved by overriding the Docker command, not an env var

This one isn't a config value the app reads — it's *which file is the entrypoint*, because the two environments need fundamentally different front doors:

- **Real AWS Lambda**: nothing external calls this container like a normal server. AWS's own infrastructure drives it through the Lambda Runtime Interface Client (baked into the AWS-provided base image), using Lambda's internal invoke protocol.
- **Local `docker-compose`**: there's no AWS infrastructure to drive it, so something has to stand in — a small Express server that takes a normal HTTP request and calls the same handler function directly.

```
src/
  handler.js       // the actual logic — unchanged between environments
  lambda-entry.js  // exports.handler = require('./handler').handler — what real Lambda calls
  local-server.js  // tiny Express wrapper around handler() — what docker-compose calls
```

One image, one `Dockerfile`, one default `CMD` — `docker-compose` overrides it locally:

```dockerfile
FROM public.ecr.aws/lambda/nodejs:20
COPY package*.json ./
RUN npm ci --omit=dev
COPY src/ ./
CMD ["lambda-entry.handler"]
```

```yaml
# docker-compose-local.yml
chat-routing-lambda:
  build:
    context: ../lambda-chat-routing
  command: ["node", "src/local-server.js"]   # overrides the image's default CMD, local only
  ports:
    - "9000:8080"
  environment:
    REDIS_MODE: tcp
    REDIS_HOST: redis
    REDIS_PORT: 6379
  depends_on:
    redis:
      condition: service_healthy
```

When actually deployed to AWS Lambda, `docker-compose` isn't involved at all, so the image's built-in `CMD` (`lambda-entry.handler`) runs as-is, and `REDIS_MODE` gets set to whatever selects the Upstash REST implementation.

### The takeaway to remember

Two axes, two different mechanisms — don't conflate them:

| | Local | Deployment |
|---|---|---|
| Redis impl (env var) | `redis-tcp.js` via `redis` | `redis-rest.js` via Upstash |
| Entrypoint (Docker command) | `local-server.js` (Express) | `lambda-entry.js` (Lambda Runtime Interface Client) |

Same image, same handler code, same business logic either way — only the env var and the Docker command differ.

## References

- [[Chat Routing System Design]] — what this Lambda actually does (`connect-info`, failover reassignment) and why it has to live outside Spring Boot
- [[Chat Persistence: Scylla to RocksDB + Kafka]]
- `docker/docker-compose-local.yml`
- https://docs.aws.amazon.com/ko_kr/lambda/latest/dg/nodejs-image.html