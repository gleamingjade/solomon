package com.example.solomon.feature.chat.adapter.in.messaging.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import com.example.solomon.common.adapter.in.messaging.kafka.config.KafkaConsumerFactorySupport;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;
import com.example.solomon.feature.trial.adapter.in.messaging.kafka.config.TrialKafkaTopicConfig;
import com.example.solomon.feature.trial.domain.event.TrialCreatedEvent;
import com.example.solomon.feature.trial.domain.event.TrialJoinedEvent;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class ChatKafkaConsumerConfig {

    private final KafkaConsumerFactorySupport factorySupport;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageCreatedEvent> chatMessageAppliedEventConsumerFactory() {
        return factorySupport.buildFactory(ChatMessageCreatedEvent.class, ChatKafkaTopicConfig.CHAT_MESSAGE_APPLIED_EVENT);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TrialCreatedEvent> trialCreatedEventConsumerFactory() {
        return factorySupport.buildFactory(TrialCreatedEvent.class, TrialKafkaTopicConfig.TRIAL_CREATED_EVENT);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TrialJoinedEvent> trialJoinedEventConsumerFactory() {
        return factorySupport.buildFactory(TrialJoinedEvent.class, TrialKafkaTopicConfig.TRIAL_JOINED_EVENT);
    }

}