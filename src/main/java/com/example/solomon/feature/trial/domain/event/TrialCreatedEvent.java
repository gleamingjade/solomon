package com.example.solomon.feature.trial.domain.event;

import java.util.UUID;

public record TrialCreatedEvent(UUID trialId, Long memberId, String nickname) {

    public static final String EVENT_TYPE = "TrialCreatedEvent";

}