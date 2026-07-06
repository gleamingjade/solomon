package com.example.solomon.common.infra.messaging.kafka;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class CdcDispatcher {

    private final Map<String, CdcHandler> handlerMap;

    public CdcDispatcher(List<CdcHandler> handlers) {
        this.handlerMap = handlers.stream().collect(Collectors.toMap(
                CdcHandler::supports,
                Function.identity()));
    }

    public void dispatch(DebeziumEnvelope envelope) {
        String op = envelope.payload().op();
        JsonNode after = envelope.payload().after();

        if (op == null || after == null)
            return;

        handlerMap.get(op).handle(after);
    }
}