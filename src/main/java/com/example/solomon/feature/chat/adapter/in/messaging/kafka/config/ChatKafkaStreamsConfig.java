package com.example.solomon.feature.chat.adapter.in.messaging.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.example.solomon.feature.chat.adapter.in.messaging.kafka.ChatMessageStoreProcessor;
import com.example.solomon.feature.chat.adapter.out.persistence.kafkastreams.ChatMessageStoreKey;
import com.example.solomon.feature.chat.adapter.out.persistence.kafkastreams.ChatMessageStoreKeySerde;
import com.example.solomon.feature.chat.application.out.ChatMessagePublisher;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;

@Configuration
public class ChatKafkaStreamsConfig {

    public static final String CHAT_MESSAGE_STORE_NAME = "chat-message-store";

    private final ChatMessagePublisher chatMessagePublisher;

    private final String bootstrapServers;

    private final String serverId;

    public ChatKafkaStreamsConfig(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${SERVER_ID}") String serverId,
            ChatMessagePublisher chatMessagePublisher) {
        this.bootstrapServers = bootstrapServers;
        this.serverId = serverId;
        this.chatMessagePublisher = chatMessagePublisher;
    }

    @Bean(name = "chatMessageStreamsBuilder")
    public StreamsBuilderFactoryBean chatMessageStreamsBuilder() {
        Map<String, Object> props = new HashMap<>();
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "solomon-chat-streams");
        props.put(StreamsConfig.NUM_STANDBY_REPLICAS_CONFIG, 2);
        // Lets queryMetadataForKey() resolve which instance owns a given key. The port is a
        // placeholder never dialed directly - callers use HostInfo#host() as the owning
        // instance's SERVER_ID and connect via the existing /ws-{serverId} STOMP endpoint (see
        // WebSocketConfig), not by dialing this host:port pair.
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, serverId + ":0");

        return new StreamsBuilderFactoryBean(new KafkaStreamsConfiguration(props));
    }

    @Bean
    public KStream<String, ChatMessageCreatedEvent> chatMessageStream(
            @Qualifier("chatMessageStreamsBuilder") StreamsBuilder streamsBuilder) {
        JsonSerde<ChatMessageCreatedEvent> eventSerde = new JsonSerde<>(ChatMessageCreatedEvent.class);

        StoreBuilder<KeyValueStore<ChatMessageStoreKey, ChatMessageCreatedEvent>> storeBuilder = Stores
                .keyValueStoreBuilder(
                        Stores.persistentKeyValueStore(CHAT_MESSAGE_STORE_NAME),
                        new ChatMessageStoreKeySerde(), eventSerde)
                // The persistence contract here is synchronous, no-async-gap writes (see the
                // "Chat Persistence" wiki doc) - Streams' default in-memory cache would batch
                // writes before they land in the store/changelog, which breaks that guarantee.
                .withCachingDisabled();

        streamsBuilder.addStateStore(storeBuilder);

        KStream<String, ChatMessageCreatedEvent> stream = streamsBuilder.stream(
                ChatKafkaTopicConfig.CHAT_MESSAGE_CREATED_EVENT,
                Consumed.with(Serdes.String(), eventSerde));

        KStream<String, ChatMessageCreatedEvent> applied = stream.process(
                () -> new ChatMessageStoreProcessor(chatMessagePublisher), CHAT_MESSAGE_STORE_NAME);

        applied.to(ChatKafkaTopicConfig.CHAT_MESSAGE_APPLIED_EVENT, Produced.with(Serdes.String(), eventSerde));

        return stream;
    }

}
