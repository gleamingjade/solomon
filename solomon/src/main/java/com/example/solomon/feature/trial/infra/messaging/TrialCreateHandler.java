package com.example.solomon.feature.trial.infra.messaging;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import com.example.solomon.common.infra.messaging.kafka.CdcHandler;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrialCreateHandler implements CdcHandler {

    private final ApplicationEventPublisher publisher;
    private final TrialCDCLogMapper mapper;

    @Override
    public String supports() {
        return "c";
    }

    @Override
    public void handle(JsonNode after) {
        publisher.publishEvent(
                mapper.toCreatedEvent(after));
    }
}