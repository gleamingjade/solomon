package com.example.solomon.feature.chat.adapter.out.persistence.kafkastreams;

import java.nio.ByteBuffer;
import java.util.UUID;

// Kafka Streams' persistent store compares keys by their serialized bytes, so the sort order we
// need (per-trial messages ordered by sequence) has to be encoded into the byte layout itself:
// trialId (16 bytes, fixed) + sequence (8 bytes, big-endian). Big-endian makes byte comparison
// equal numeric comparison for non-negative longs, and the fixed UUID width means no separator
// or padding is needed to keep one trial's keys from bleeding into another's.
public record ChatMessageStoreKey(UUID trialId, long sequence) {

    private static final int TRIAL_ID_LENGTH = 16;
    private static final int KEY_LENGTH = TRIAL_ID_LENGTH + Long.BYTES;

    byte[] toBytes() {
        return ByteBuffer.allocate(KEY_LENGTH)
                .putLong(trialId.getMostSignificantBits())
                .putLong(trialId.getLeastSignificantBits())
                .putLong(sequence)
                .array();
    }

    static ChatMessageStoreKey fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        long mostSignificantBits = buffer.getLong();
        long leastSignificantBits = buffer.getLong();
        long sequence = buffer.getLong();

        return new ChatMessageStoreKey(new UUID(mostSignificantBits, leastSignificantBits), sequence);
    }

}