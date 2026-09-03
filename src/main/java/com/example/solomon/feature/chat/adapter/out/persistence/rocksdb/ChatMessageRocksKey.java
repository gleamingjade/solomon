package com.example.solomon.feature.chat.adapter.out.persistence.rocksdb;

import java.nio.ByteBuffer;
import java.util.UUID;

// RocksDB is a raw byte key-value store with no query engine, so the sort order we need
// (per-trial messages ordered by sequence) has to be encoded into the key layout itself:
// trialId (16 bytes, fixed) + sequence (8 bytes, big-endian). Big-endian makes byte comparison
// equal numeric comparison for non-negative longs, and the fixed UUID width means no separator
// or padding is needed to keep one trial's keys from bleeding into another's.
final class ChatMessageRocksKey {

    private static final int TRIAL_ID_LENGTH = 16;
    private static final int KEY_LENGTH = TRIAL_ID_LENGTH + Long.BYTES;

    private ChatMessageRocksKey() {
    }

    static byte[] of(UUID trialId, long sequence) {
        return ByteBuffer.allocate(KEY_LENGTH)
                .putLong(trialId.getMostSignificantBits())
                .putLong(trialId.getLeastSignificantBits())
                .putLong(sequence)
                .array();
    }

    static byte[] prefix(UUID trialId) {
        return ByteBuffer.allocate(TRIAL_ID_LENGTH)
                .putLong(trialId.getMostSignificantBits())
                .putLong(trialId.getLeastSignificantBits())
                .array();
    }

}
