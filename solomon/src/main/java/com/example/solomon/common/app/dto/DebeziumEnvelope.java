package com.example.solomon.common.app.dto;

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

}