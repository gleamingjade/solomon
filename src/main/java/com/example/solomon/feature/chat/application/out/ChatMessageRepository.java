package com.example.solomon.feature.chat.application.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.solomon.feature.chat.domain.entity.ChatMessage;

public interface ChatMessageRepository {

    public ChatMessage save(ChatMessage chatMessage);

    // All messages must share the same trial_id partition - Scylla only guarantees atomicity
    // (all-or-nothing) for a batch within a single partition.
    public List<ChatMessage> saveAllInBatch(List<ChatMessage> chatMessages);

    public Optional<ChatMessage> findLatestByTrialId(UUID trialId);

}
