package com.example.solomon.feature.chat.adapter.in.messaging.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;

import com.example.solomon.common.adapter.in.messaging.kafka.config.KafkaConsumerFactorySupport;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;
import com.example.solomon.feature.trial.adapter.in.messaging.kafka.config.TrialKafkaTopics;
import com.example.solomon.feature.trial.domain.event.TrialCreatedEvent;
import com.example.solomon.feature.trial.domain.event.TrialJoinedEvent;

import lombok.RequiredArgsConstructor;

// Container factories for the consumers living in feature/chat/adapter/in/messaging/kafka:
// TrialCreatedEventConsumer, TrialJoinedEventConsumer, and chat's own ChatMessageCreatedEventConsumer.
@Configuration
@RequiredArgsConstructor
public class ChatKafkaConsumerConfig {

    private final KafkaConsumerFactorySupport factorySupport;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TrialCreatedEvent> trialCreatedEventConsumerFactory() {
        return factorySupport.buildFactory(
                TrialCreatedEvent.class,
                TrialKafkaTopics.TRIAL_CREATED_EVENT + "-consumer",
                "trial-created-event");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TrialJoinedEvent> trialJoinedEventConsumerFactory() {
        return factorySupport.buildFactory(
                TrialJoinedEvent.class,
                TrialKafkaTopics.TRIAL_JOINED_EVENT + "-consumer",
                "trial-joined-event");
    }

    // Consumed by the trial feature (advances the Trial aggregate's chat sequence, see
    // TrialKafkaConsumerConfig) and the chat feature (pushes the message over websocket) as two
    // independent consumer groups on the same "chat-message-created-event" topic, so each group
    // gets its own full copy of every message.
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, ChatMessageCreatedEvent> chatMessageCreatedEventConsumerFactory() {
        return factorySupport.buildFactory(
                ChatMessageCreatedEvent.class,
                ChatKafkaTopics.CHAT_MESSAGE_CREATED_EVENT + "-consumer",
                "chat-message-created-event");
    }

}