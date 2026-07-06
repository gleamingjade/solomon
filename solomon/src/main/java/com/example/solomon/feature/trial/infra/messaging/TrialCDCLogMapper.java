package com.example.solomon.feature.trial.infra.messaging;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.example.solomon.feature.trial.domain.event.TrialCreatedEvent;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class TrialCDCLogMapper {

    public TrialCreatedEvent toCreatedEvent(JsonNode after) {
        UUID id = UUID.fromString(after.get("id").asText());
        return new TrialCreatedEvent(id);
    }

    // public TrialUpdatedEvent toUpdatedEvent(JsonNode after) {
    // UUID id = UUID.fromString(after.get("id").asText());
    // String stage = after.get("stage").asText();

    // return new TrialUpdatedEvent(id, stage);
    // }

}
