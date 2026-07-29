package com.example.solomon;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;

public class KafkaTestSupport {

    private final String bootstrapServers;

    public KafkaTestSupport(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public ConsumerRecords<String, String> pollRecords(String topicName) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of(topicName));

            return Awaitility.await()
                    .atMost(Duration.ofSeconds(120))
                    .pollInterval(Duration.ZERO)
                    .until(() -> consumer.poll(Duration.ofMillis(200)),
                            records -> !records.isEmpty());
        }
    }

}
