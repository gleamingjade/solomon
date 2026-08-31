# Chat Persistence: Scylla → RocksDB + Kafka

How chat message storage was redesigned after building out [[Chat Routing System Design]] — and why Scylla turned out to be the wrong tool for it.

## Original Constraint: Why Async Chat Is Dangerous Here

Solomon's chat exists to let two people argue their side of a dispute so an AI can judge who's right. That framing makes one failure mode catastrophic:

If a chat message is persisted **asynchronously** — fire the WebSocket to the other user, then write to the DB in the background — a real edge case emerges under load: the DB write queue backs up, the other user is offline and never gets the WebSocket fanout, then reconnects and loads "recent messages" from the DB before the backed-up write lands. They now believe they're caught up on the latest state when they aren't, and keep arguing on top of a broken context. For a service whose entire output is "who was right, based on what was said," a broken message-ordering guarantee is close to a broken product.

The fix: split chat-message work into **essential** work (persisting the message itself — must be synchronous, no exceptions) and **derived** work (chat list preview updates, unread counts — safe to be async). Only the essential path needs to be fast *and* safe to do synchronously without killing latency at scale.

The fastest structure for synchronous writes is an **LSM-tree** (write-optimized, unlike a B-tree which is read-optimized) — that's what led to picking **Scylla**: LSM-tree storage, and built for horizontal scaling if write volume ever outgrows one node.

## Why Scylla Turned Out to Be the Wrong Fit

Scylla's value proposition is two things bundled together: an LSM-tree engine (fast writes) *and* horizontal sharding across nodes (scale writes past what one machine can do).

Once [[Chat Routing System Design]] was actually built out, the second half stopped being useful for this workload. The whole point of "same-server locality" is that **a single trial's chat is always read and written by exactly one server** — there's no scenario where one trial's message volume needs to be sharded across multiple nodes. Scylla's headline feature was being paid for but never exercised.

Meanwhile, this project runs partly on a local mini PC to keep costs down (Spring Boot + chat servers local, MySQL on Aurora Serverless v2, Redis on Upstash, LLM via AWS Nova) — running an actual Scylla cluster (even just to get real replication/sharding benefit, you want multiple nodes) is a heavy, expensive thing to operate for a benefit that isn't being used.

## New Design: Kafka First, RocksDB Second

**RocksDB** gives the same core property that motivated picking Scylla in the first place — it's a genuine LSM-tree engine. The difference is it's embedded, local, no network hop — which is exactly what "one server owns this trial" wants.

The write path:

```
client sends message
 → owning server produces to Kafka (this is the durability commit point)
 → consumer(s) apply it to local RocksDB
 → only commit the Kafka offset once the RocksDB write succeeds
```

Kafka is the source of truth; RocksDB is a fast local materialized view derived *from* Kafka, not the other way around. Derived-work consumers (chat list preview, unread count, `Trial.onNewChatMessage`) subscribe to the same Kafka topic directly, as siblings to the RocksDB-writing consumer.

## Ownership: Who Does What

Every chat server subscribes to the topic. Every message carries the producing server's `SERVER_ID` (the same env var `WebSocketConfig` already uses) — since the message only ever gets produced by the server that owns the trial's live connections (same-server locality guarantees that), stamping the producer's own id is enough; no lookup needed.

Each consuming server compares that id to its own:

- **Owner** (`serverId == my SERVER_ID`): apply to local RocksDB, then run derived work (`Trial.onNewChatMessage`, chat list preview, unread count). These two steps happen as one sequential unit — the Kafka offset only commits once *both* succeed. If the RocksDB write fails, derived work never runs and the whole thing retries together on the next poll; since the RocksDB write is a keyed `put`, redoing it is harmless.
- **Non-owner**: apply to local RocksDB only. No derived work — that only ever runs once, on the owner, so there's no duplicate-processing risk to guard against there.

### Fanout stays outside the retry loop

The WebSocket fanout to the other participant happens immediately on message receipt, before any of the above — not gated on Kafka or RocksDB. If it were inside the retry loop, a RocksDB hiccup would cause the same message to be delivered to the client multiple times. Only the persistence/derived-work side is safe to retry, because it's built to be idempotent; fanout isn't, so it just happens once, up front.

## Replication Without Full Broadcast

Having every server apply every trial's messages to its own local RocksDB isn't just for the owner's benefit — non-owners doing the same write is what gives durability without a hand-built replication protocol. If the owning server dies, another server already has a near-current local copy and can take over without replaying Kafka from scratch.

But literally broadcasting to *every* server means each server's load scales with total system-wide chat volume, not its own share — that gets expensive as the cluster grows. Instead of full broadcast, each server only replicates to a small, fixed set of **neighbors by server id** (a ring, wrapping at the ends) — e.g. server 1 replicates to servers 2 and 3; server 2 replicates to servers 1 and 3; generally, within roughly ±2 of its own id. This keeps replication cost constant (a fixed handful of copies) no matter how large the cluster gets, the same idea consistent-hashing ring topologies (Cassandra/DynamoDB-style) use to place a fixed replica count instead of replicating to the whole ring.

This needs to feed back into the failover/reassignment logic in [[Chat Routing System Design]]: when a trial's owning server dies and a new one has to be picked, prefer one of that server's designated neighbors first — they already hold a near-current local copy, so failover doesn't need a full Kafka replay to get back to a servable state.

## References

- [[Chat Routing System Design]]
- [[Kafka&CDC]]
- [[Service Overview]]
- `feature/trial/domain/entity/Trial.java` (`onNewChatMessage`)
- `feature/chat/adapter/out/websocket/StompChatMessagePublisher.java`
