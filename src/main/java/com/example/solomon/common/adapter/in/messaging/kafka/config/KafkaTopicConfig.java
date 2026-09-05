package com.example.solomon.common.adapter.in.messaging.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// CDC_MYSQL_OUTBOX is a shared entry point consumed by multiple features' own *KafkaStreamsConfig
// (see TrialKafkaStreamsConfig), so no single feature owns it - it's provisioned here instead.
// Kafka Streams treats a missing source topic on startup as fatal, so it must exist before the
// MySQL connector ever produces to it, not be created lazily by the connector.

// Referenced by https://docs.spring.io/spring-kafka/reference/kafka/configuring-topics.html
@Configuration
public class KafkaTopicConfig {

    public static final String CDC_MYSQL_OUTBOX = "cdc-mysql.localmysql.outbox";

    @Bean
    public NewTopic cdcMysqlOutboxTopic() {
        return TopicBuilder.name(CDC_MYSQL_OUTBOX)
                .partitions(6)
                .replicas(1)
                .build();
    }

}