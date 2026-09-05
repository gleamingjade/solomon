# Chat Persistence: Kafka Streams State Store

How chat messages are persisted and how chat session routing works.

## Kafka Streams apps

The backend runs two independent Kafka Streams applications, each with its own `StreamsBuilderFactoryBean`:

- **`outboxStreamsBuilder`** (`common/adapter/in/messaging/kafka/config/KafkaStreamsConfig.java`, `application.id = solomon-cdc-streams`) — relays the shared MySQL outbox CDC topic into feature-specific clean topics (e.g. `TrialKafkaStreamsConfig` producing `trial-created-event` / `trial-joined-event`). Stateless, no state stores.
- **`chatMessageStreamsBuilder`** (`feature/chat/adapter/in/messaging/kafka/config/ChatKafkaStreamsConfig.java`, `application.id = solomon-chat-streams`) — the chat message state store topology below. Kept separate because it's stateful and needs app-wide `StreamsConfig` properties (`num.standby.replicas`, `application.server`) the CDC relay doesn't need.

Every chat server instance runs both apps.

## Chat message state store

`ChatKafkaStreamsConfig.chatMessageStream(...)` reads `chat-message-created-event` (keyed by `trialId`) and feeds it into `ChatMessageStoreProcessor`, which writes into a persistent Kafka Streams key-value store (`CHAT_MESSAGE_STORE_NAME = "chat-message-store"`).

**Key layout**: `ChatMessageStoreKey(UUID trialId, long sequence)`, serialized by `ChatMessageStoreKeySerde` into `trialId (16 bytes) + sequence (8 bytes, big-endian)`. Big-endian keeps byte comparison equal to numeric comparison, so range queries over a trial's messages come back in order with no separator or padding needed.

**Changelog**: Kafka Streams backs the store with a compacted changelog topic. Compaction only discards *old values for a repeated key* — since `sequence` never repeats for a given `trialId`, nothing is ever compacted away; the changelog behaves like a normal unbounded log while still giving Kafka Streams' restore machinery for free.

**Caching disabled**: the store is built with `.withCachingDisabled()`. Kafka Streams' default in-memory cache batches writes before they land in the store/changelog — this system requires every chat message write to be synchronous with no gap, so caching stays off.

**Standby replicas**: `num.standby.replicas = 2`. Kafka Streams' own group-membership/assignor decides which instances hold standbys, dynamically, from whichever instances are currently alive — so failover reassignment always draws from the live set, not a fixed set computed when the original owner was picked. The actual number of standbys realized is capped at `min(num.standby.replicas, live instances - 1)`. Changing this value requires a rolling redeploy with every instance on the same setting.

**Processor logic** (`ChatMessageStoreProcessor.process`):

```java
store.put(new ChatMessageStoreKey(event.trialId(), event.sequence()), event);
chatMessagePublisher.publish(event);       // WebSocket fanout
trial.onNewChatMessage(event.content(), event.sequence());
trialRepository.save(trial);
```

No ownership check is needed here. Kafka Streams only ever calls `process()` on the *active* task instance for a given partition — standby instances replay the changelog to stay warm without ever invoking the processor. That guarantee is what makes fanout and the `Trial` update run exactly once, on the right instance, automatically.

## Sequence generation

`InMemoryChatMessageSeqRepository` keeps an in-memory `AtomicLong` per `trialId`, safe because same-server locality guarantees only the owning instance ever writes for a given trial at a time. On first use for a trial (failover/restart), it seeds the counter via `ChatMessageStateStoreQueries.findLatestSequence`, which does a local Interactive Query (`reverseRange` on the state store) against the same instance's own copy — this only works because the caller is the active/standby owner for that trial.

## Session routing

`ChatConnectionController` exposes `GET /api/chat/connect-info?trialId={uuid}`, which asks the chat Streams app who owns that trial and returns their `SERVER_ID`:

```java
KeyQueryMetadata metadata = kafkaStreams.queryMetadataForKey(
        ChatKafkaStreamsConfig.CHAT_MESSAGE_STORE_NAME, trialId.toString(), Serdes.String().serializer());
String serverId = metadata.activeHost().host();
```

`application.server` is set to `"${SERVER_ID}:0"` in `ChatKafkaStreamsConfig` purely so `HostInfo.host()` carries the owning instance's `SERVER_ID` — the port is a placeholder, never dialed directly. The client takes that `serverId` and connects to the existing `/ws-{serverId}` STOMP endpoint (`WebSocketConfig`), same as today.

Any instance can answer a `connect-info` request, not just the owner — Kafka Streams' rebalance protocol shares task-assignment metadata across the whole consumer group, so there's no need to route the HTTP request itself to a specific instance first.

**Failover**: if the owning instance dies, its session timeout in the consumer group protocol triggers a rebalance, which reassigns the task to a live instance (promoting a standby if one exists, so it's already warm). A client that loses its WebSocket connection re-requests `connect-info`; once the rebalance completes, that call returns the new owner's `serverId` and the client reconnects there.

## System messages

`TrialCreatedEventConsumer` / `TrialJoinedEventConsumer` (`feature/chat/adapter/in/messaging/kafka/`) remain plain `@KafkaListener`s on a shared consumer group (`ChatKafkaConsumerConfig`), consuming `trial-created-event` / `trial-joined-event` and producing system `ChatMessageCreatedEvent`s via `CreateChatMessageUsecase`. They don't need to run on any particular instance or do any ownership check — a shared consumer group already guarantees exactly one instance processes each event, and whatever `ChatMessageCreatedEvent` they produce gets its ownership resolved independently, downstream, by the chat message state store topology based on `trialId`.

(`trial-created-event` / `trial-joined-event` are already keyed by `trialId` — `TrialKafkaStreamsConfig` re-keys the CDC envelope to `aggregate_id`, which is `trial.getId().toString()` for both events.)

## Not yet implemented

- Migrating an ended trial's messages out to MySQL and deleting them from the state store (`KeyValueStore.delete(key)` is available and logging-enabled, so deletes propagate to the changelog as tombstones — the migration/delete flow itself isn't built).
- Message history pagination (the store's byte-ordered keys support `range()`/`reverseRange()` for this, not wired up to an API yet).

## References

- `common/adapter/in/messaging/kafka/config/KafkaStreamsConfig.java`
- `feature/trial/adapter/in/messaging/kafka/config/TrialKafkaStreamsConfig.java`
- `feature/chat/adapter/in/messaging/kafka/config/ChatKafkaStreamsConfig.java`
- `feature/chat/adapter/in/messaging/kafka/ChatMessageStoreProcessor.java`
- `feature/chat/adapter/out/persistence/kafkastreams/ChatMessageStoreKey.java`
- `feature/chat/adapter/out/persistence/kafkastreams/ChatMessageStateStoreQueries.java`
- `feature/chat/adapter/in/web/ChatConnectionController.java`
- `feature/chat/adapter/websocket/config/WebSocketConfig.java`
