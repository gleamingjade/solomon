package com.example.solomon.feature.chat.app.dto;

import java.util.UUID;

public record CreateChatMessageCommand(UUID trialId, Long memberId, String content) {
}