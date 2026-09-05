# Kafka Overall Maintenance Guide

Reference guide for how Kafka is set up and used across the backend: what topics exist, how the two Kafka Streams apps are structured, where shared config lives, and the reasoning behind partition/replication counts. Update the topic inventory whenever a new topic is added.

## Deployment environment (why the numbers below look the way they do)

- **One Kafka broker**, run as a single Docker container - not a multi-broker cluster. This caps every topic's replication factor at 1 (replication factor can never exceed the number of live brokers; Kafka refuses to create a topic otherwise).
- **App instances**: starting with 3 Spring Boot containers via Docker, all on the same physical machine (a local mini PC) - not yet real, physically separate servers. Partition counts are still chosen with future horizontal scaling in mind, because partition count is expensive to change later (see "Partition count guidance" below).
- `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false` on the broker - Kafka Streams treats a missing source topic as fatal, so every topic a Streams app reads from must be declared as a `NewTopic` bean up front, not lazily auto-created.

### A note on "odd numbers" - two different things people conflate

The "prefer an odd count" rule applies to **quorum/consensus nodes** (a ZooKeeper ensemble, or a KRaft controller quorum) - odd counts avoid tie votes and don't waste a node's worth of fault tolerance (going from 3 to 4 nodes doesn't improve fault tolerance; going from 3 to 5 does). This has nothing to do with:

- **Broker count in general** - a single broker can serve any number of consumer instances; there's no relationship between broker count and consumer/app instance count at all.
- **Topic partition count** - no quorum/voting happens here. Partition count is purely a parallelism knob, sized against however many *consumer instances* you expect to run (see below), not against broker count and not "odd vs even."

## Common Kafka infrastructure (shared across features)

| File | Purpose |
|---|---|
| `common/adapter/in/messaging/kafka/config/KafkaConsumerFactorySupport.java` | Base config for plain `@KafkaListener` container factories - bootstrap servers, JSON deserialization, dead-letter-topic error handling, retry, and the `groupId = topic + "-consumer"` naming convention. Every plain listener's container factory is built via `buildFactory(...)` here. |
| `common/adapter/in/messaging/kafka/config/KafkaStreamsConfig.java` | Defines the `outboxStreamsBuilder` bean - the shared Kafka Streams app (`application.id = solomon-cdc-streams`) that CDC/outbox-relay topologies register onto. Stateless, no state stores, no app-wide config beyond bootstrap servers. |
| `common/adapter/in/messaging/kafka/config/KafkaTopicConfig.java` | Declares the shared MySQL outbox CDC topic (`cdc-mysql.localmysql.outbox`). |

## Current topic inventory

| Topic | Partitions | Replicas | Keyed by | Produced by | Consumed by |
|---|---|---|---|---|---|
| `cdc-mysql.localmysql.outbox` | 6 | 1 | outbox row's own key (Debezium) | Debezium (MySQL CDC) | `TrialKafkaStreamsConfig` (via `outboxStreamsBuilder`) |
| `trial-created-event` | 6 | 1 | `trialId` | `TrialKafkaStreamsConfig` (relayed from outbox) | `TrialCreatedEventConsumer` |
| `trial-joined-event` | 6 | 1 | `trialId` | `TrialKafkaStreamsConfig` (relayed from outbox) | `TrialJoinedEventConsumer` |
| `chat-message-created-event` | 12 | 1 | `trialId` | `CreateChatMessageUsecase` | `ChatKafkaStreamsConfig`'s chat message state store topology (via `chatMessageStreamsBuilder`) |
| `chat-message-applied-event` | 12 | 1 | `trialId` | `ChatMessageStoreProcessor` (forwards after fanout) | `ChatMessageAppliedEventConsumer` |

## Kafka Streams apps

There are two independent Kafka Streams applications, each with its own `StreamsBuilderFactoryBean` and `application.id` - see [[Chat Persistence: Kafka Streams State Store]] for the full rationale on why chat gets its own dedicated app instead of sharing the CDC one.

- **`outboxStreamsBuilder`** (`application.id = solomon-cdc-streams`, defined in `KafkaStreamsConfig`) - stateless CDC-relay topologies register onto this. Currently just `TrialKafkaStreamsConfig`, which filters/branches the shared outbox topic by `event_type` and republishes each into its own clean topic (`trial-created-event`, `trial-joined-event`). Other features writing to the same outbox table should register their own independent `.stream(CDC_MYSQL_OUTBOX)` branch here rather than extending `TrialKafkaStreamsConfig`, to keep aggregates decoupled.
- **`chatMessageStreamsBuilder`** (`application.id = solomon-chat-streams`, defined in `ChatKafkaStreamsConfig`) - the stateful chat message state store topology. Kept separate because it needs app-wide `StreamsConfig` properties (`num.standby.replicas`, `application.server`) the stateless CDC relay doesn't need and shouldn't be forced to carry.

## Consumer group conventions

- **Plain `@KafkaListener`s** (`KafkaConsumerFactorySupport.buildFactory`): `groupId = topic + "-consumer"`, identical across every app instance. One partition is owned by exactly one instance at a time within that group, so each message is processed exactly once cluster-wide. This is the right shape for anything that should run once per event regardless of which instance handles it (e.g. `TrialCreatedEventConsumer`, `ChatMessageAppliedEventConsumer`).
- **Kafka Streams apps**: `application.id` acts as the group id, shared by every instance running that app. Kafka Streams' own task assignor splits partitions into tasks and distributes active/standby ownership within that one group - this is the shape needed when a topology also needs replicated local state (the chat message store).

## Partition count guidance

- Size partition count against the number of **consumer instances** you expect to run, not broker count and not "odd vs even." A partition count that's a multiple of your instance count distributes evenly with nobody idle; a highly composite number (e.g. 6, 12) stays evenly divisible across several plausible future instance counts (2, 3, 4, 6, 12 for a count of 12).
- **Increasing partition count later is risky for topics where a single key accumulates an ordered history** - `chat-message-created-event` is the clearest example. Changing partition count changes `hash(key) % partitions`, so a trial's older messages (hashed under the old partition count) and newer ones (hashed under the new count) can end up in different partitions/tasks, splitting one trial's history apart. Pick generously up front for these.
- **Lower risk for topics where each key only ever produces one event** - `trial-created-event` / `trial-joined-event` fall here (a given `trialId` produces exactly one created-event and one joined-event, never an accumulating sequence), so there's no history to split if partition count changes later. Still sized generously here for consistency, but it's a much smaller decision.
- `chat-message-applied-event` is also lower-risk to resize later despite carrying `trialId` per message and the same volume as `chat-message-created-event` - `TrialRepository.updateLastMessageIfNewer` is a conditional update guarded by `sequence`, so out-of-order delivery (from any cause, including a future repartition) can't corrupt it.

## Replication factor guidance

- Capped by live broker count - cannot exceed it at topic creation time (`InvalidReplicationFactorException`). With one broker, every topic must be `replicas(1)`.
- Unrelated to partition count and unrelated to consumer/app instance count.
- When the broker fleet eventually grows past one node, replication factor can be raised topic-by-topic via a partition reassignment (not simply flipping the `NewTopic` bean's value - existing topics need an explicit reassignment plan).

## References

- [[Chat Persistence: Kafka Streams State Store]]
- `common/adapter/in/messaging/kafka/config/KafkaConsumerFactorySupport.java`
- `common/adapter/in/messaging/kafka/config/KafkaStreamsConfig.java`
- `common/adapter/in/messaging/kafka/config/KafkaTopicConfig.java`
- `feature/trial/adapter/in/messaging/kafka/config/TrialKafkaTopicConfig.java`
- `feature/trial/adapter/in/messaging/kafka/config/TrialKafkaStreamsConfig.java`
- `feature/chat/adapter/in/messaging/kafka/config/ChatKafkaTopicConfig.java`
- `feature/chat/adapter/in/messaging/kafka/config/ChatKafkaStreamsConfig.java`
- `feature/chat/adapter/in/messaging/kafka/config/ChatKafkaConsumerConfig.java`
