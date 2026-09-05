package com.example.solomon.feature.chat.adapter.out.persistence.kafkastreams;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;

public final class ChatMessageStoreKeySerde implements Serde<ChatMessageStoreKey> {

    @Override
    public Serializer<ChatMessageStoreKey> serializer() {
        return (topic, key) -> key.toBytes();
    }

    @Override
    public Deserializer<ChatMessageStoreKey> deserializer() {
        return (topic, bytes) -> ChatMessageStoreKey.fromBytes(bytes);
    }

}