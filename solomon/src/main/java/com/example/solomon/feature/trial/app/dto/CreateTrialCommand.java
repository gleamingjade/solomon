package com.example.solomon.feature.trial.app.dto;

public record CreateTrialCommand(Long memberId, String issueTitle, String nickname) {
}