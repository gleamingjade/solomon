package com.example.solomon.feature.trial.adapter.in.messaging.kafka.config;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.example.solomon.common.adapter.in.messaging.kafka.DebeziumEnvelope;
import com.example.solomon.common.adapter.in.messaging.kafka.config.KafkaTopics;
import com.example.solomon.feature.trial.domain.event.TrialCreatedEvent;
import com.example.solomon.feature.trial.domain.event.TrialJoinedEvent;

// Relays trial's own outbox rows out of the shared MySQL outbox CDC topic into their own clean
// topics. Other features writing to the same outbox table register their own independent
// .stream(CDC_MYSQL_OUTBOX) here instead of extending this one, so aggregates stay decoupled from
// each other (see KafkaStreamsConfig for the shared StreamsBuilder these all register onto).
@Configuration
public class TrialKafkaStreamsConfig {

        @Bean
        public KStream<String, DebeziumEnvelope> trialCdcStream(
                        @Qualifier("cdcStreamsBuilder") StreamsBuilder streamsBuilder) {
                JsonSerde<DebeziumEnvelope> debeziumSerde = new JsonSerde<>(DebeziumEnvelope.class);

                KStream<String, DebeziumEnvelope> stream = streamsBuilder.stream(KafkaTopics.CDC_MYSQL_OUTBOX,
                                Consumed.with(Serdes.String(), debeziumSerde))
                                .filter((key, envelope) -> envelope != null && envelope.payload() != null
                                                && envelope.payload().after() != null);

                stream.split()
                                .branch(
                                                (key, envelope) -> TrialCreatedEvent.EVENT_TYPE
                                                                .equals(envelope.getValFromAfter("event_type").asText()),
                                                Branched.withConsumer(ks -> ks
                                                                .selectKey((key, envelope) -> envelope
                                                                                .getValFromAfter("aggregate_id").asText())
                                                                .mapValues(envelope -> envelope
                                                                                .getValFromAfter("payload").asText())
                                                                .to(TrialKafkaTopics.TRIAL_CREATED_EVENT,
                                                                                Produced.with(Serdes.String(), Serdes.String()))))
                                .branch(
                                                (key, envelope) -> TrialJoinedEvent.EVENT_TYPE
                                                                .equals(envelope.getValFromAfter("event_type").asText()),
                                                Branched.withConsumer(ks -> ks
                                                                .selectKey((key, envelope) -> envelope
                                                                                .getValFromAfter("aggregate_id").asText())
                                                                .mapValues(envelope -> envelope
                                                                                .getValFromAfter("payload").asText())
                                                                .to(TrialKafkaTopics.TRIAL_JOINED_EVENT,
                                                                                Produced.with(Serdes.String(), Serdes.String()))));
                return stream;
        }

}
