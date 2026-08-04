package com.example.solomon.common.adapter.in.messaging.kafka.config;

// Only the outbox CDC source topic lives here - it's the shared entry point any feature's own
// *KafkaStreamsConfig may read from (see TrialKafkaStreamsConfig). Each feature's own topics
// (e.g. TrialKafkaTopics, ChatKafkaTopics) live next to the event type they carry.
public final class KafkaTopics {

    public static final String CDC_MYSQL_OUTBOX = "cdc-mysql.localmysql.outbox";

    private KafkaTopics() {
    }

}