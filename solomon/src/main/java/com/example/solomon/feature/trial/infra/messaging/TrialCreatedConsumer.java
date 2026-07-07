package com.example.solomon.feature.trial.infra.messaging;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.infra.messaging.kafka.CdcDispatcher;
import com.example.solomon.common.infra.messaging.kafka.DebeziumEnvelope;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrialCreatedConsumer {

    private final ObjectMapper objectMapper;
    private final CdcDispatcher cdcDispatcher;

    @KafkaListener(topics = "mysql.localdb.trial-created", containerFactory = "trialCreatedConsumer")
    public void consume(DebeziumEnvelope envelope) {

    }

}
