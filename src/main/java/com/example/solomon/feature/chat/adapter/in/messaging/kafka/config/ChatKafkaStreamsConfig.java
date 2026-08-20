package com.example.solomon.feature.chat.adapter.in.messaging.kafka.config;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.example.solomon.common.adapter.in.messaging.kafka.DebeziumEnvelope;
import com.example.solomon.feature.chat.domain.event.ChatMessageCreatedEvent;

// Scylla has no outbox table to piggyback a transaction on, so instead of an
// application-written outbox row, we branch directly off the row-level CDC log and treat
// an insert ("c") as the "chat message created" event. See KafkaStreamsConfig for the shared
// StreamsBuilder this registers onto.
@Configuration
public class ChatKafkaStreamsConfig {

        @Bean
        public KStream<String, DebeziumEnvelope> chatMessageCdcStream(
                        @Qualifier("cdcStreamsBuilder") StreamsBuilder streamsBuilder) {
                JsonSerde<DebeziumEnvelope> debeziumSerde = new JsonSerde<>(DebeziumEnvelope.class);
                JsonSerde<ChatMessageCreatedEvent> chatMessageCreatedEventSerde = new JsonSerde<>(
                                ChatMessageCreatedEvent.class);

                KStream<String, DebeziumEnvelope> stream = streamsBuilder.stream(ChatKafkaTopicConfig.CDC_SCYLLA_CHAT_MESSAGE,
                                Consumed.with(Serdes.String(), debeziumSerde))
                                .filter((key, envelope) -> envelope != null && envelope.payload() != null
                                                && envelope.payload().after() != null
                                                && "c".equals(envelope.payload().op()));

                stream.selectKey((key, envelope) -> envelope.getValFromAfter("trial_id").asText())
                                .mapValues(ChatMessageCreatedEvent::from)
                                .to(ChatKafkaTopicConfig.CHAT_MESSAGE_CREATED_EVENT,
                                                Produced.with(Serdes.String(), chatMessageCreatedEventSerde));

                return stream;
        }

}
