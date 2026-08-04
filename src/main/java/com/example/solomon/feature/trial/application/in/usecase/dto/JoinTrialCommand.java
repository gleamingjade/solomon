package com.example.solomon.feature.trial.application.in.usecase.dto;

import java.util.UUID;

public record JoinTrialCommand(UUID trialId, Long memberId, String nickname) {
}
