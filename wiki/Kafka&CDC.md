# Kafka & CDC
## CDC (Change Data Capture)

A technique that reads a database's **commit log (binlog/WAL) directly** and streams out the actual committed changes as events.

Implementing the outbox pattern without CDC (i.e. polling) has these problems:

- Latency tied to the polling interval
- Processed rows have to be deleted/marked to avoid reprocessing
- Polling itself puts load on the DB

Tailing the outbox table with CDC removes the need for polling entirely — the event goes out the moment the row is committed. It also eliminates the classic **dual-write problem**, where application code commits to the DB but then fails to publish the message (or vice versa). Since CDC only ever reads from the commit log, anything not committed to the DB never becomes an event, and anything committed always does.

This is commonly implemented together with Kafka (e.g. Debezium, a Kafka Connect source connector, reads the binlog and publishes to a Kafka topic).

## Kafka Basics

A message broker. Writes are **appended to a log on disk** — sequential writes make it fast, and persisting to disk (rather than memory) makes it durable. Consumed messages aren't deleted immediately either (they stay for the configured retention period), so multiple independent consumer groups can each read from the beginning on their own.

## Core Concepts

### Topic

The "subject" of a message — where it gets published to. e.g. `trial-created-event`.

### Partition

A topic is split into multiple partitions. Which partition a message lands on is decided by **hashing the partition key**.

> Example: for a chat message topic, the partition key should be the **chat room id**. That way, messages from the same room always land on the same partition, guaranteeing processing order. Pick the wrong key (e.g. random) and messages from the same room can scatter across partitions, breaking ordering.

### Producer

The side that **writes** messages to a topic. The producer decides which partition a message goes to (key hashing, or a custom partitioner). The `acks` setting controls how many brokers must replicate a message before it's considered successful — a durability/speed trade-off.

### Consumer / Consumer Group

The side that **reads** messages piled up in partitions. Whether consumers are grouped together completely changes the behavior.

- **Same group**: consumers in the group split the topic's partitions between them. Each consumer processes a different subset of messages (work-sharing).
- **Different groups**: groups are independent of each other. Every group receives the entire set of messages on the topic (replication).

> Example: say there are 3 consumers —
> - Group A (logging), Group B (actual processing), Group C (indexing into Elasticsearch), **each its own group** → all three receive every single message.
> - If those same 3 were put in **one group** instead → messages get split between them, and now some messages never get logged, some never make it to Elasticsearch.
>
> Rule of thumb: are these consumers splitting up the *same* piece of work (same group), or does each of them need to see the *entire* stream for its own separate purpose (different groups)?

## References

- [Debezium MySQL connector](https://debezium.io/documentation/reference/stable/connectors/mysql.html#mysql-example-configuration)
- [Scylla CDC source connector](https://github.com/scylladb/scylla-cdc-source-connector)
- [Kafka configuration reference](https://kafka.apache.org/43/configuration/)
- [Spring Kafka integration](https://docs.spring.io/spring-kafka/reference/kafka.html)

For basic config/Spring integration, the two docs above plus a search engine or an AI assistant get you most of the way there without much trouble.