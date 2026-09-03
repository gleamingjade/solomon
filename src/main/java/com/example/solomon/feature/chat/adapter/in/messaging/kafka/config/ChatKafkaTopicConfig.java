package com.example.solomon.feature.chat.adapter.in.messaging.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// Referenced by https://docs.spring.io/spring-kafka/reference/kafka/configuring-topics.html
@Configuration
public class ChatKafkaTopicConfig {

    public static final String CHAT_MESSAGE_CREATED_EVENT = "chat-message-created-event";

    @Bean
    public NewTopic chatMessageCreatedEventTopic() {
        return TopicBuilder.name(CHAT_MESSAGE_CREATED_EVENT)
                .partitions(2)
                .replicas(1)
                .build();
    }

}
