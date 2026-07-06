package com.example.solomon.feature.trial.domain.event;

import java.util.UUID;

// https://debezium.io/documentation/reference/stable/connectors/mysql.html#mysql-create-events
public record TrialCreatedEvent(UUID trialId) {
}