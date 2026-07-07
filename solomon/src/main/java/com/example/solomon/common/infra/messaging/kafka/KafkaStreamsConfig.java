package com.example.solomon.common.infra.messaging.kafka;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

@Configuration
public class KafkaStreamsConfig {

        @Bean
        public KStream<String, DebeziumEnvelope> debeziumStream(StreamsBuilder streamsBuilder) {
                JsonSerde<DebeziumEnvelope> debeziumSerde = new JsonSerde<>(DebeziumEnvelope.class);

                KStream<String, DebeziumEnvelope> stream = streamsBuilder.stream("mysql.localdb.trial",
                                Consumed.with(Serdes.String(), debeziumSerde))
                                .filter((key, envelope) -> envelope != null && envelope.payload() != null);

                stream.split()
                                .branch(
                                                (key, envelope) -> "c".equals(envelope.payload().op()),
                                                Branched.withConsumer(ks -> ks.to("mysql.localdb.trial-created",
                                                                Produced.with(Serdes.String(), debeziumSerde))))
                                .branch(
                                                (key, envelope) -> "u".equals(envelope.payload().op()),
                                                Branched.withConsumer(ks -> ks.to("trial-updated",
                                                                Produced.with(Serdes.String(), debeziumSerde))));
                return stream;
        }

}
