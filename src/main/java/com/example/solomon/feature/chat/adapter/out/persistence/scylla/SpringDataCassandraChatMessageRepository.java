package com.example.solomon.feature.chat.adapter.out.persistence.scylla;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.cassandra.repository.CassandraRepository;

import com.example.solomon.feature.chat.domain.entity.ChatMessage;
import com.example.solomon.feature.chat.domain.entity.ChatMessageKey;

public interface SpringDataCassandraChatMessageRepository
        extends CassandraRepository<ChatMessage, ChatMessageKey> {

    Optional<ChatMessage> findTopByKeyTrialIdOrderByKeyIdDesc(UUID trialId);

}