package com.example.solomon.feature.trial.application.usecase.dto;

public record CreateTrialCommand(Long memberId, String issueTitle, String nickname) {
}
