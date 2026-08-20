package com.example.solomon.feature.chat.adapter.in.messaging.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.application.in.usecase.CreateChatMessageUsecase;
import com.example.solomon.feature.chat.application.in.usecase.dto.CreateChatMessageCommand;
import com.example.solomon.feature.chat.domain.entity.MessageType;
import com.example.solomon.feature.trial.adapter.in.messaging.kafka.config.TrialKafkaTopicConfig;
import com.example.solomon.feature.trial.domain.event.TrialCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrialCreatedEventConsumer {

    private final CreateChatMessageUsecase createChatMessageUsecase;

    @KafkaListener(topics = TrialKafkaTopicConfig.TRIAL_CREATED_EVENT, containerFactory = "trialCreatedEventConsumerFactory")
    public void consume(TrialCreatedEvent event) {
        createChatMessageUsecase.execute(new CreateChatMessageCommand(
                event.trialId(), event.memberId(), event.nickname() + " came into play.", MessageType.SYSTEM_MESSAGE_JOINED));
    }

}
