package com.example.solomon.feature.trial.domain.event;

import java.util.UUID;

public record TrialJoinedEvent(UUID trialId, String nickname) {

    public static final String EVENT_TYPE = "TrialJoinedEvent";

}
