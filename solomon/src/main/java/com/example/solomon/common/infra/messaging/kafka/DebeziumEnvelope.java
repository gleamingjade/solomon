package com.example.solomon.common.infra.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;

public record DebeziumEnvelope(Payload payload) {
        public record Payload(
                        JsonNode before,
                        JsonNode after,
                        String op) {
        }
}