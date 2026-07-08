package com.example.solomon.feature.trial.infra.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.app.dto.DebeziumEnvelope;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrialCreatedConsumer {

    @KafkaListener(topics = "mysql.localdb.trial-created", containerFactory = "trialCreatedConsumer")
    public void consume(DebeziumEnvelope envelope) {
        // TrialCreatedEvent 발송 
    }

}
