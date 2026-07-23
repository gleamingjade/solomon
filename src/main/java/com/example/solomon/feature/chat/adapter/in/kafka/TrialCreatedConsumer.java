package com.example.solomon.feature.chat.adapter.in.kafka;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.adapter.in.messaging.kafka.DebeziumEnvelope;
import com.example.solomon.feature.chat.application.in.usecase.NotifyTrialCreatedUsecase;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrialCreatedConsumer {

    private final NotifyTrialCreatedUsecase notifyTrialCreatedUsecase;

    @KafkaListener(topics = "cdc-mysql.localdb.trial-created", containerFactory = "trialCreatedConsumerFactory")
    public void consume(DebeziumEnvelope envelope) {
        UUID trialId = UUID.fromString(envelope.payload().after().get("id").asText());

        notifyTrialCreatedUsecase.execute(trialId);
    }

}
