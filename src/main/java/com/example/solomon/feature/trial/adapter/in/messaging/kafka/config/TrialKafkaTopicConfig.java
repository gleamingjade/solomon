package com.example.solomon.feature.trial.adapter.in.messaging.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

// Trial produces onto both of these (see TrialKafkaStreamsConfig), so trial owns provisioning.
// Referenced by https://docs.spring.io/spring-kafka/reference/kafka/configuring-topics.html
@Configuration
public class TrialKafkaTopicConfig {

    public static final String TRIAL_CREATED_EVENT = "trial-created-event";

    public static final String TRIAL_JOINED_EVENT = "trial-joined-event";

    @Bean
    public NewTopic trialCreatedEventTopic() {
        return TopicBuilder.name(TRIAL_CREATED_EVENT)
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic trialJoinedEventTopic() {
        return TopicBuilder.name(TRIAL_JOINED_EVENT)
                .partitions(2)
                .replicas(1)
                .build();
    }

}
