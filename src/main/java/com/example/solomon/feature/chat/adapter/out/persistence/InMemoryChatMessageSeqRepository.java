package com.example.solomon.feature.chat.adapter.out.persistence;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.adapter.out.persistence.rocksdb.RocksDbChatMessageStore;
import com.example.solomon.feature.chat.application.out.ChatMessageSeqRepository;

import lombok.RequiredArgsConstructor;

// Same-server locality guarantees only the owning server ever writes to a given trial at a time,
// so an in-memory counter is enough - no need for a shared counter (e.g. Redis INCR) across
// servers. The only gap this leaves is failover/restart, where a server sees a trial for the
// first time and its Map has no entry yet; computeIfAbsent recovers the starting point from
// RocksDB (the last sequence this server, or a neighbor replica, actually persisted) in that case.
@Component
@RequiredArgsConstructor
public class InMemoryChatMessageSeqRepository implements ChatMessageSeqRepository {

    private final RocksDbChatMessageStore rocksDbChatMessageStore;

    private final Map<UUID, AtomicLong> counters = new ConcurrentHashMap<>();

    @Override
    public Long next(UUID trialId) {
        return counters
                .computeIfAbsent(trialId, id -> new AtomicLong(rocksDbChatMessageStore.findLatestSequence(id).orElse(0L)))
                .incrementAndGet();
    }

}
