package com.example.solomon.feature.chat.application.in.usecase.dto;

import java.util.UUID;

import com.example.solomon.feature.chat.domain.entity.MessageType;

public record CreateChatMessageCommand(UUID trialId, Long memberId, String content, MessageType type) {
}