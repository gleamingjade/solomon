package com.example.solomon.common.infra.messaging.kafka;

import com.fasterxml.jackson.databind.JsonNode;

public interface CdcHandler {

    String supports();

    void handle(JsonNode after);

}