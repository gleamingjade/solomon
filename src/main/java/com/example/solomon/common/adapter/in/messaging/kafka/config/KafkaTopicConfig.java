package com.example.solomon.common.adapter.in.messaging.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// Referenced by https://docs.spring.io/spring-kafka/reference/kafka/configuring-topics.html
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic trialCreatedEventTopic() {
        return TopicBuilder.name(KafkaTopics.TRIAL_CREATED_EVENT)
                .partitions(2)
                .replicas(1)
                .build();
    }

}