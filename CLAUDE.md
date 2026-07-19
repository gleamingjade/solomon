# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

"Solomon" is a Spring Boot 3.5 (Java 17) monolith. Domain: a trial/interview-style app where a `Trial` has `TrialMember`s and an associated chat room. Chat messages get fanned out to connected clients over WebSocket/STOMP. Package root: `com.example.solomon`.

The project is mid-refactor toward a consistent hexagonal (ports & adapters) package layout — see "Architecture" below for the two conventions currently coexisting in the tree.

## Commands

```bash
./gradlew build          # compile + test + assemble
./gradlew test           # run all tests (JUnit 5 / useJUnitPlatform)
./gradlew test --tests "com.example.solomon.feature.trial.domain.entity.TrialTest"   # single test class
./gradlew test --tests "*.CreateTrialUseCaseTest.testCreateTrial"                     # single test method
./gradlew check          # test + verification tasks
```

Tests rely on Testcontainers (MySQL, ScyllaDB, Kafka, a custom Debezium Connect image) and require Docker to be running locally — see `src/test/java/com/example/solomon/TestContainersConfig.java`. There is no separate lint/format task configured.

To run the full local stack outside of tests (MySQL, Scylla, Kafka, Kafka Connect w/ Debezium, Kafka UI):

```bash
docker compose -f docker/docker-compose-local.yml up
```

Run the app with the `local` profile so it picks up `src/main/resources/config/local/*.yml` (kafka/mysql/redis/scylla connection settings):

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Architecture

### Data flow: MySQL/Scylla → Debezium/CDC → Kafka → consumers

This is the core architectural idea of the project (see `wiki/Kafka&CDC.md`):

- Writes go to MySQL (via JPA, e.g. `Trial`/`TrialMember`/`Member`) or Scylla (chat messages).
- Debezium (MySQL connector) and the Scylla CDC source connector tail the binlog / CDC log and publish change events to Kafka topics named `cdc-mysql.<db>.<table>` / `cdc-scylla.<keyspace>.<table>`.
- Application code consumes these via `@KafkaListener` (e.g. `TrialCreatedConsumer` listens on `cdc-mysql.localdb.trial-created`), deserializing into `common/infra/messaging/kafka/DebeziumEnvelope` (a generic `{payload: {before, after, op}}` wrapper over Debezium's JSON, ignoring unknown fields).
- Rather than the app explicitly publishing "X was created" events, downstream effects (chat server allocation, fanout, etc.) are triggered reactively off the CDC stream. This is a deliberate choice recorded in commit history/wiki: CDC replaces bespoke application events where the CDC pipeline already exists.
- Each Kafka consumer group gets its own `ConcurrentKafkaListenerContainerFactory` built by hand in `common/config/kafka/KafkaConsumerConfig` (not the Spring Boot YAML auto-config), because different consumer groups need independent error handling. Every factory wires a `DeadLetterPublishingRecoverer` + `DefaultErrorHandler` with a fixed backoff (records that repeatedly fail land on the `<topic>.DLT` topic).

### Chat delivery

- Two separate Redis instances are configured, not one: "session" Redis (`SessionRedisConfig`, used for Spring Session / `RedisSecurityContextRepository`) and "broker" Redis (`BrokerRedisConfig`, used for app-level caches like the chat sequence counter). Don't merge these — they're intentionally split by concern, with distinct connection factories (`@Qualifier("brokerRedisConnectionFactory")`, etc.) and distinct properties classes (`BrokerRedisProperties`, `SessionRedisProperties`).
- `ChatMessageSeqRepository` (Redis-backed) hands out a per-trial monotonic sequence number for each chat message before it's persisted (see `CreateChatMessageUsecase`).
- One chat room lives on exactly one app server at a time. `AllocateChatServerUsecase` / `ChatServerMappingRepository` are meant to hash-assign a trial's chat room to a server so all participants land on the same instance and can share a subscription — this is still a stub (see TODOs in `TrialCreatedConsumer` and `AllocateChatServerUsecase`).
- Outbound delivery to clients is abstracted behind `ChatMessageSender.fanout(ChatMessage)` (domain-level interface), implemented by `StompChatMessageSender` using `SimpMessageSendingOperations` over `/user` (see `WebSocketConfig`: app prefix `/pub`, user prefix `/user`, endpoint `/ws`). This implementation is currently a stub.

### Package layout — two conventions in flight

The `trial` feature reflects the target hexagonal layout:
```
feature/trial/adapter/in/web/...          # inbound adapters (web DTOs)
feature/trial/adapter/out/persistence/jpa/... # outbound adapters (Spring Data JPA repos + adapter impl)
feature/trial/application/port/in/usecase/... # use cases (inbound ports) + their command DTOs
feature/trial/application/port/out/...        # outbound ports (repository interfaces)
feature/trial/domain/entity/...               # JPA entities / domain model
feature/trial/domain/exception/...
```

The `chat` feature has not yet been migrated and mixes the old and new styles — e.g. `adapter/in/kafka`, `application/in/usecase`, `application/out` (no `port` segment), plus a residual `domain/repository` and `infra/messaging` / `infra/websocket` that should conceptually be adapters. When touching `chat`, prefer following the `trial` package shape for any *new* code rather than extending the older pattern, unless you're doing the migration itself.

`member` is the least developed feature (just domain entities + a repository port) and is treated as a shared/supporting domain referenced by `trial` (`Member`, `MemberRepository`).

`common/` holds cross-cutting infra with no feature ownership: JPA base entities (`UuidBaseEntity`, `IdBaseEntity`, `BaseTimeEntity`), Redis/Kafka/JPA `@Configuration` classes, the WebSocket config, the Debezium envelope type, and app-wide exception types (`AppException` + per-feature `*Exception` enums implementing a shared `ExceptionInfo`).

### Testing conventions (see `wiki/Test Infra Explained.md`)

- Prefer sliced/lightweight tests over full `@SpringBootTest` where possible — `SlicedSpringContextTest` is a meta-annotation (`@SpringJUnitConfig` + `ConfigDataApplicationContextInitializer` + `PropertyPlaceholderConfiguration`) for tests that need config/`@Value` resolution without booting the whole context. Full `@SpringBootTest` is reserved for tests that genuinely need the CDC pipeline end-to-end (e.g. `TrialCreatedConsumerTest`).
- `TestContainersConfig` boots MySQL + Scylla + Kafka in parallel (`Startables.deepStart`) plus a Debezium Connect container built from a custom Dockerfile (base image + the Scylla CDC source connector jar added on top). It registers both the MySQL and Scylla source connectors against Debezium automatically via `SmartInitializingSingleton`, gated by an `AtomicBoolean` so registration happens once per test JVM even though the config is `@Import`ed into multiple test classes.
- `@ServiceConnection` works for MySQL but **not** Scylla/Cassandra with this Testcontainers module — Scylla connection properties are set manually via `System.setProperty(...)` in `initScyllaProperties()`. If you add Scylla-backed tests, don't assume `@ServiceConnection` wiring applies.
- Scylla requires the CDC-enabled table to exist *before* the Scylla source connector is registered (`initScyllaSchema()` creates `chat_message` with `WITH cdc = {'enabled': true}` up front).
- `KafkaTestSupport.pollRecords(topic)` and `DebeziumTestSupport` (thin HTTP client over the Kafka Connect REST API) are the way to assert on CDC/Kafka behavior in tests, using Awaitility rather than fixed sleeps for async assertions (per the wiki, this is deliberate — async behavior should be tested by awaiting the actual condition, not fixed delays).
