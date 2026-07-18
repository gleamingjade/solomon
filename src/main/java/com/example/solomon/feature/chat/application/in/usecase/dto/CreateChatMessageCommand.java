package com.example.solomon.feature.chat.application.in.usecase.dto;

import java.util.UUID;

public record CreateChatMessageCommand(UUID trialId, Long memberId, String content) {
}