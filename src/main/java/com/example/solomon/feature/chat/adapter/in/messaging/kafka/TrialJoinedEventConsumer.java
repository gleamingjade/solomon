package com.example.solomon.feature.chat.adapter.in.messaging.kafka;

import com.example.solomon.feature.chat.application.in.usecase.CreateChatMessageUsecase;
import com.example.solomon.feature.chat.application.in.usecase.dto.CreateChatMessageCommand;
import com.example.solomon.feature.chat.domain.entity.MessageType;
import com.example.solomon.feature.trial.adapter.in.messaging.kafka.config.TrialKafkaTopics;
import com.example.solomon.feature.trial.domain.event.TrialJoinedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TrialJoinedEventConsumer {

    private final CreateChatMessageUsecase createChatMessageUsecase;

    @KafkaListener(topics = TrialKafkaTopics.TRIAL_JOINED_EVENT, containerFactory = "trialJoinedEventConsumerFactory")
    public void consume(TrialJoinedEvent event) {
        createChatMessageUsecase.execute(List.of(
                new CreateChatMessageCommand(
                        event.trialId(),
                        null,
                        event.nickname() + " came into play.",
                        MessageType.SYSTEM_MESSAGE_JOINED),
                new CreateChatMessageCommand(
                        event.trialId(),
                        null,
                        "Everybody came up, Please press the ready button below if you're ready to start",
                        MessageType.SYSTEM_MESSAGE_READY_REQUEST)));
    }

}
