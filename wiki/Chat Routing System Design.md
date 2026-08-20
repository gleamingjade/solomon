# Chat Routing System Design

Message routing and failover strategy for the 1:1 single-session chat service.

## Context & Strategy

This service's chat is strictly 1:1 — the business rule is **a user can be connected to only one chat room at a time**.

### Why broker-based routing doesn't fit here

A typical multi-room chat system assigns users to random chat servers and routes messages between servers through a broker (Redis Pub/Sub, Kafka, etc). Applying that pattern to a service where every user only ever sits in a single room causes two problems:

- **Unnecessary network overhead** — every message has to go through the broker just to find which server the other user is on.
- **Wasted resources / extra latency** — the two users in the same room are likely to land on different servers, so a simple direct delivery isn't possible.

### Core strategy: "Same-Server Locality"

So instead, this system guarantees that **both users in the same chat room (trial) are always assigned to the same chat server**.

That completely skips server-to-server routing — messages get delivered via local in-memory dispatch on the same server, which maximizes latency and cuts infra cost.

## Architecture Flow

### ① Server registration & health check

Each chat server registers itself and its liveness in Redis on startup.

- **Heartbeat**: each chat server periodically pings Redis with `SET server:health:{server_id} "alive" EX 10` (10s TTL).

### ② Trial creation & server allocation

When a user creates a new trial (chat room), the main API server queries Redis and picks the chat server with the fewest currently-connected users/trials.

The chosen server is then tracked in Redis:

- `trial:{trial_id}:server` → `{server_id}`
- `server:{server_id}:count` → number of trials currently connected

### ③ Connection & direct dispatch

When a user connects to a chat room, they ask the main API server for connection info, get back the WebSocket URL of the server mapped in `trial:{trial_id}:server`, and connect to it directly.

- **Direct dispatch**: since both users of the same trial live in the same chat server's session memory, a published message is delivered straight to the local WebSocket session — no broker hop needed.

## Failover Strategy

When a chat server goes down, the client, the main API server, and Redis cooperate to detect the failure and self-heal by reassigning to a healthy server.

### ① Fault detection

- **Server down**: if the chat server crashes or is killed, the client's WebSocket connection drops (`onclose` fires).
- **TTL expiry**: if a chat server stops sending heartbeats, its `server:health:{server_id}` key in Redis auto-expires after 10s.

### ② Client-driven reconnection

The client doesn't try to figure out *why* the connection dropped (refresh, flaky network, server down, whatever).

On any reconnect attempt, it unconditionally asks the main API server for the trial's latest connection URL: `GET /api/chat/connect-info?trialId={trial_id}`.

### ③ API server's failover handling

When the main API server gets that request, it branches based on Redis state:

- **Existing server still alive**: if `server:health:{server_id}` is still valid in Redis, return that server's WebSocket URL as-is.
- **Existing server confirmed down (TTL expired)**: if the health key is gone, treat the old server as dead —
  1. Pick a new server from the currently-alive ones with the fewest users.
  2. Atomically update `trial:{trial_id}:server` to the new server id.
  3. Return the new server's WebSocket URL.

### ④ Handling the grace period (TTL delay)

Right after a server dies (before the 10s TTL has actually elapsed), a client request might come in while the API server still thinks the old server is alive — so it hands back a URL that's already dead.

- **Client-side exponential backoff**: if connecting to that URL fails, the client waits with backoff (1s, 2s, 4s, ...) before asking the main API server again.
- Within 2-3 retries, Redis's TTL naturally expires, so the client eventually gets the recovered/reassigned server's URL and reconnects safely.
