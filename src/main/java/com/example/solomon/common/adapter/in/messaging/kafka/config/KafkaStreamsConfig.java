package com.example.solomon.common.adapter.in.messaging.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.StreamsConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.KafkaStreamsConfiguration;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;

@Configuration
public class KafkaStreamsConfig {

        private final String bootstrapServers;

        public KafkaStreamsConfig(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers) {
                this.bootstrapServers = bootstrapServers;
        }

        @Bean(name = "outboxStreamsBuilder")
        public StreamsBuilderFactoryBean outboxStreamsBuilder() {
                Map<String, Object> props = new HashMap<>();
                props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
                props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                props.put(StreamsConfig.APPLICATION_ID_CONFIG, "solomon-cdc-streams");

                return new StreamsBuilderFactoryBean(new KafkaStreamsConfiguration(props));
        }

}
