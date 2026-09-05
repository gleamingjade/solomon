package com.example.solomon.feature.chat.adapter.in.messaging.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaTopicConfig;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;
import com.example.solomon.feature.trial.application.out.TrialRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMessageAppliedEventConsumer {

    private final TrialRepository trialRepository;

    @KafkaListener(topics = ChatKafkaTopicConfig.CHAT_MESSAGE_APPLIED_EVENT,
            containerFactory = "chatMessageAppliedEventConsumerFactory")
    public void consume(ChatMessageCreatedEvent event) {
        trialRepository.updateLastMessageIfNewer(event.trialId(), event.content(), event.sequence());
    }

}
