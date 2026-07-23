package com.example.solomon.feature.chat.infra.messaging;

import java.util.UUID;

import com.example.solomon.common.adapter.in.messaging.kafka.DebeziumEnvelope;
import com.fasterxml.jackson.databind.JsonNode;

public record ChatMessageCreatedEvent(
        UUID trialId,
        String content,
        Long sequence) {

    public static ChatMessageCreatedEvent from(DebeziumEnvelope envelope) {
        JsonNode after = envelope.payload().after();

        return new ChatMessageCreatedEvent(
                UUID.fromString(after.get("trial_id").asText()),
                after.get("content").asText(),
                after.get("sequence").asLong());
    }

}