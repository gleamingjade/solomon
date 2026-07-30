package com.example.solomon.common.adapter.in.messaging.kafka.config;

public final class KafkaTopics {

    public static final String CDC_MYSQL_OUTBOX = "cdc-mysql.localmysql.outbox";

    public static final String CDC_SCYLLA_CHAT_MESSAGE = "cdc-scylla.localscylla.chat_message";

    public static final String TRIAL_CREATED_EVENT = "trial-created-event";

    private KafkaTopics() {
    }

}