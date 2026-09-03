package com.example.solomon.feature.chat.adapter.out.persistence.rocksdb;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rocksdb.Options;
import org.rocksdb.ReadOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.RocksIterator;
import org.rocksdb.Slice;
import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PreDestroy;

// Local, per-server materialized view of chat messages, built from Kafka (see the
// "Chat Persistence" wiki doc). RocksDB is not the source of truth here - Kafka is.
// Keys are laid out by ChatMessageRocksKey so "latest N" / "before X" reads are just
// ordered iteration, no query engine involved.
@Component
public class RocksDbChatMessageStore {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final RocksDB db;

    public RocksDbChatMessageStore(RocksDbProperties props) {
        RocksDB.loadLibrary();

        try {
            Files.createDirectories(Path.of(props.path()));

            this.db = RocksDB.open(new Options().setCreateIfMissing(true), props.path());
        } catch (RocksDBException | IOException e) {
            throw new IllegalStateException("Failed to open RocksDB at " + props.path(), e);
        }
    }

    @PreDestroy
    public void close() {
        db.close();
    }

    public void put(ChatMessageCreatedEvent event) {
        try {
            db.put(ChatMessageRocksKey.of(event.trialId(), event.sequence()), serialize(event));
        } catch (RocksDBException e) {
            throw new IllegalStateException("Failed to write chat message to RocksDB", e);
        }
    }

    public Optional<Long> findLatestSequence(UUID trialId) {
        return findLatest(trialId).map(ChatMessageCreatedEvent::sequence);
    }

    public Optional<ChatMessageCreatedEvent> findLatest(UUID trialId) {
        List<ChatMessageCreatedEvent> latest = findBefore(trialId, Long.MAX_VALUE, 1);
        return latest.isEmpty() ? Optional.empty() : Optional.of(latest.get(0));
    }

    public List<ChatMessageCreatedEvent> findRecent(UUID trialId, int limit) {
        return findBefore(trialId, Long.MAX_VALUE, limit);
    }

    // Returns up to `limit` messages with sequence < beforeSequenceExclusive, newest first.
    public List<ChatMessageCreatedEvent> findBefore(UUID trialId, long beforeSequenceExclusive, int limit) {
        byte[] lowerBound = ChatMessageRocksKey.prefix(trialId);
        byte[] seekFrom = ChatMessageRocksKey.of(trialId, beforeSequenceExclusive - 1);

        List<ChatMessageCreatedEvent> results = new ArrayList<>();

        try (Slice lowerBoundSlice = new Slice(lowerBound);
                ReadOptions readOptions = new ReadOptions().setIterateLowerBound(lowerBoundSlice);
                RocksIterator iterator = db.newIterator(readOptions)) {

            iterator.seekForPrev(seekFrom);

            while (iterator.isValid() && results.size() < limit) {
                results.add(deserialize(iterator.value()));
                iterator.prev();
            }
        }

        return results;
    }

    private byte[] serialize(ChatMessageCreatedEvent event) {
        try {
            return objectMapper.writeValueAsBytes(event);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize chat message.", e);
        }
    }

    private ChatMessageCreatedEvent deserialize(byte[] value) {
        try {
            return objectMapper.readValue(value, ChatMessageCreatedEvent.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to deserialize chat message.", e);
        }
    }

}
