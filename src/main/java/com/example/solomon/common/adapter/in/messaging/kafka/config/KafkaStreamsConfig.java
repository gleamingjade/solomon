package com.example.solomon.common.adapter.in.messaging.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.Branched;
import org.apache.kafka.streams.kstream.Consumed;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Produced;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.example.solomon.common.adapter.in.messaging.kafka.DebeziumEnvelope;

// Referenced by https://docs.spring.io/spring-kafka/reference/streams.html#kafka-streams-example
@Configuration
public class KafkaStreamsConfig {

        private final String bootstrapServers;

        public KafkaStreamsConfig(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
                this.bootstrapServers = bootstrapServers;
        }

        private Map<String, Object> getBaseProps() {
                Map<String, Object> props = new HashMap<>();
                props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                return props;
        }

        @Bean(name = "cdcStreamsBuilder")
        public StreamsBuilderFactoryBean cdcStreamsBuilder() {
                Map<String, Object> props = getBaseProps();
                props.put(StreamsConfig.APPLICATION_ID_CONFIG, "solomon-cdc-streams");

                return new StreamsBuilderFactoryBean(new KafkaStreamsConfiguration(props));
        }

        @Bean
        public KStream<String, DebeziumEnvelope> cdcStream(
                        @Qualifier("cdcStreamsBuilder") StreamsBuilder streamsBuilder) {
                JsonSerde<DebeziumEnvelope> debeziumSerde = new JsonSerde<>(DebeziumEnvelope.class);

                KStream<String, DebeziumEnvelope> stream = streamsBuilder.stream("cdc-mysql.localdb.trial",
                                Consumed.with(Serdes.String(), debeziumSerde))
                                .filter((key, envelope) -> envelope != null && envelope.payload() != null);

                stream.split()
                                .branch(
                                                (key, envelope) -> envelope.payload().op() != null
                                                                && "c".equals(envelope.payload().op()),
                                                Branched.withConsumer(ks -> ks.to("cdc-mysql.localdb.trial-created",
                                                                Produced.with(Serdes.String(), debeziumSerde))))
                                .branch(
                                                (key, envelope) -> envelope.payload().op() != null
                                                                && "u".equals(envelope.payload().op()),
                                                Branched.withConsumer(ks -> ks.to("cdc-mysql.localdb.trial-updated",
                                                                Produced.with(Serdes.String(), debeziumSerde))));
                return stream;
        }

}