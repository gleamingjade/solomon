package com.example.solomon.feature.chat.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.cassandra.core.CassandraBatchOperations;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.stereotype.Repository;

import com.example.solomon.feature.chat.adapter.out.persistence.scylla.SpringDataCassandraChatMessageRepository;
import com.example.solomon.feature.chat.application.out.ChatMessageRepository;
import com.example.solomon.feature.chat.domain.entity.ChatMessage;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryAdapter implements ChatMessageRepository {

    private final SpringDataCassandraChatMessageRepository chatMessageRepository;

    private final CassandraOperations cassandraOperations;

    @Override
    public ChatMessage save(ChatMessage chatMessage) {
        return chatMessageRepository.save(chatMessage);
    }

    @Override
    public List<ChatMessage> saveAllInBatch(List<ChatMessage> chatMessages) {
        CassandraBatchOperations batchOperations = cassandraOperations.batchOps();
        batchOperations.insert(chatMessages);
        batchOperations.execute();

        return chatMessages;
    }

    @Override
    public Optional<ChatMessage> findLatestByTrialId(UUID trialId) {
        return chatMessageRepository.findTopByKeyTrialIdOrderByKeyIdDesc(trialId);
    }

}
