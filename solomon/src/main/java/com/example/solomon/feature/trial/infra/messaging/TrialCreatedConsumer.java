package com.example.solomon.feature.trial.infra.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.app.usecase.CreateChatMessageUsecase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrialCreatedConsumer {

    private final CreateChatMessageUsecase usecase;

    @KafkaListener(topics = "mysql.localdb.trial", groupId = "trial-derivative")
    public void consume(String message) {
        System.out.println(message);
    }

}
