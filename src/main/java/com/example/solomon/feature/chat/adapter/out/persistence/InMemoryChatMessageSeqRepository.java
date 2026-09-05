package com.example.solomon.feature.chat.adapter.out.persistence;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.adapter.out.persistence.kafkastreams.ChatMessageStateStoreQueries;
import com.example.solomon.feature.chat.application.out.ChatMessageSeqRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class InMemoryChatMessageSeqRepository implements ChatMessageSeqRepository {

    private final ChatMessageStateStoreQueries chatMessageStateStoreQueries;

    private final Map<UUID, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public Long next(UUID trialId) {
        return counters
                .computeIfAbsent(trialId, id -> new AtomicLong(chatMessageStateStoreQueries.findLatestSequence(id).orElse(0L)))
                .incrementAndGet();
    }

}
