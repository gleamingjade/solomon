package com.example.solomon.common.adapter.in.messaging.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DebeziumEnvelope(Payload payload) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payload(
            JsonNode before,
            JsonNode after,
            String op) {
    }

    public JsonNode getValFromAfter(String name) {
        return payload.after.get(name);
    }

}