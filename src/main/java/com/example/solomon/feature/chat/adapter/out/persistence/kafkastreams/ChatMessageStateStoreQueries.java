package com.example.solomon.feature.chat.adapter.out.persistence.kafkastreams;

import java.util.Optional;
import java.util.UUID;

import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StoreQueryParameters;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.stereotype.Component;

import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaStreamsConfig;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatMessageStateStoreQueries {

    @Qualifier("chatMessageStreamsBuilder")
    private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;

    public Optional<Long> findLatestSequence(UUID trialId) {
        ReadOnlyKeyValueStore<ChatMessageStoreKey, ChatMessageCreatedEvent> store = store();

        ChatMessageStoreKey from = new ChatMessageStoreKey(trialId, 0L);
        ChatMessageStoreKey to = new ChatMessageStoreKey(trialId, Long.MAX_VALUE);

        try (KeyValueIterator<ChatMessageStoreKey, ChatMessageCreatedEvent> iterator = store.reverseRange(from, to)) {
            if (!iterator.hasNext()) {
                return Optional.empty();
            }

            KeyValue<ChatMessageStoreKey, ChatMessageCreatedEvent> latest = iterator.next();

            return Optional.of(latest.value.sequence());
        }
    }

    private ReadOnlyKeyValueStore<ChatMessageStoreKey, ChatMessageCreatedEvent> store() {
        KafkaStreams kafkaStreams = streamsBuilderFactoryBean.getKafkaStreams();

        return kafkaStreams.store(StoreQueryParameters.fromNameAndType(
                ChatKafkaStreamsConfig.CHAT_MESSAGE_STORE_NAME, QueryableStoreTypes.keyValueStore()));
    }

}
