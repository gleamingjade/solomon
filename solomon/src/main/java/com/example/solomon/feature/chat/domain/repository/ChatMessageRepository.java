package com.example.solomon.feature.chat.domain.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;

import com.example.solomon.feature.chat.domain.entity.ChatMessage;
import com.example.solomon.feature.chat.domain.entity.ChatMessageKey;

public interface ChatMessageRepository
        extends CassandraRepository<ChatMessage, ChatMessageKey> {
}