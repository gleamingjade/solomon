package com.example.solomon.feature.chat.domain.event;

import java.util.UUID;

import com.example.solomon.feature.chat.domain.entity.MessageType;

public record ChatMessageCreatedEvent(
        UUID trialId,
        String content,
        Long sequence,
        MessageType type,
        String serverId) {

}
