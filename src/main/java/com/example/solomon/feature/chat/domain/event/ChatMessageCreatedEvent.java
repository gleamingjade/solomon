package com.example.solomon.feature.chat.domain.event;

import java.util.UUID;

import com.example.solomon.feature.chat.domain.entity.MessageType;

// serverId is the id of the server that produced this event (always the trial's owning server,
// per same-server locality) - consumers compare it against their own SERVER_ID to decide whether
// they're the owner (apply to RocksDB + fanout + derived work) or a replica (apply only). See
// the "Chat Persistence" wiki doc.
public record ChatMessageCreatedEvent(
        UUID trialId,
        String content,
        Long sequence,
        MessageType type,
        String serverId) {

}
