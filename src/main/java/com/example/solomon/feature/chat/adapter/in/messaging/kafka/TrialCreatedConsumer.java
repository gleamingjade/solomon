package com.example.solomon.feature.chat.adapter.in.messaging.kafka;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.adapter.in.messaging.kafka.KafkaTopics;
import com.example.solomon.feature.chat.application.in.usecase.CreateChatMessageUsecase;
import com.example.solomon.feature.chat.application.in.usecase.dto.CreateChatMessageCommand;
import com.example.solomon.feature.trial.domain.event.TrialCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrialCreatedConsumer {

    private final CreateChatMessageUsecase createChatMessageUsecase;

    @KafkaListener(topics = KafkaTopics.TRIAL_CREATED_EVENT, containerFactory = "trialCreatedConsumerFactory")
    public void consume(TrialCreatedEvent event) {
        createChatMessageUsecase.execute(new CreateChatMessageCommand(
                event.trialId(), event.memberId(), event.nickname() + " came into play."));
    }

}
