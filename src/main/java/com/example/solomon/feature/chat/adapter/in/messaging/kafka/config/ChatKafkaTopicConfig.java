package com.example.solomon.feature.chat.adapter.in.messaging.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// Chat produces onto CHAT_MESSAGE_CREATED_EVENT and consumes CDC_SCYLLA_CHAT_MESSAGE as a Kafka
// Streams source (see ChatKafkaStreamsConfig), so chat owns provisioning both - Streams treats a
// missing source topic on startup as fatal, so the CDC topic must exist before the connector ever
// produces to it, not be created lazily by the connector.
// Referenced by https://docs.spring.io/spring-kafka/reference/kafka/configuring-topics.html
@Configuration
public class ChatKafkaTopicConfig {

    public static final String CDC_SCYLLA_CHAT_MESSAGE = "cdc-scylla.localscylla.chat_message";

    public static final String CHAT_MESSAGE_CREATED_EVENT = "chat-message-created-event";

    @Bean
    public NewTopic chatMessageCreatedEventTopic() {
        return TopicBuilder.name(CHAT_MESSAGE_CREATED_EVENT)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic cdcScyllaChatMessageTopic() {
        return TopicBuilder.name(CDC_SCYLLA_CHAT_MESSAGE)
                .partitions(2)
                .replicas(1)
                .build();
    }

}
