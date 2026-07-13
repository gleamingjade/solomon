package com.example.solomon.feature.trial.application.port.in.usecase.dto;

public record CreateTrialCommand(Long memberId, String issueTitle, String nickname) {
}
