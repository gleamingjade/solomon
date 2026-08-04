package com.example.solomon.feature.trial.adapter.in.messaging.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import com.example.solomon.common.adapter.in.messaging.kafka.config.KafkaConsumerFactorySupport;
import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaTopics;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;

import lombok.RequiredArgsConstructor;

// Container factory for the consumer living in feature/trial/adapter/in/messaging/kafka:
// ChatMessageCreatedEventConsumer (see ChatKafkaConsumerConfig for the chat feature's own
// consumer group on the same topic).
@Configuration
@RequiredArgsConstructor
public class TrialKafkaConsumerConfig {

    private final KafkaConsumerFactorySupport factorySupport;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageCreatedEvent> chatMessageCreatedEventTrialConsumerFactory() {
        return factorySupport.buildFactory(
                ChatMessageCreatedEvent.class,
                ChatKafkaTopics.CHAT_MESSAGE_CREATED_EVENT + "-trial-consumer",
                "chat-message-created-event (trial)");
    }

}