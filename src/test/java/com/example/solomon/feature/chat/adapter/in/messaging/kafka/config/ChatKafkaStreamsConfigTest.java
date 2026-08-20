package com.example.solomon.feature.chat.adapter.in.messaging.kafka.config;

import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.solomon.KafkaTestSupport;
import com.example.solomon.SlicedSpringContextTest;
import com.example.solomon.TestContainersConfig;
import com.example.solomon.common.adapter.in.messaging.kafka.config.KafkaStreamsConfig;
import com.github.f4b6a3.uuid.UuidCreator;

import static com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaTopicConfig.CDC_SCYLLA_CHAT_MESSAGE;
import static com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaTopicConfig.CHAT_MESSAGE_CREATED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@SlicedSpringContextTest
@Import({
        TestContainersConfig.class,
        KafkaStreamsConfig.class,
        ChatKafkaStreamsConfig.class
})
class ChatKafkaStreamsConfigTest {

    // Kafka Streams treats a missing source topic as fatal on startup (MissingSourceTopicException
    // -> SHUTDOWN_CLIENT, no retry). In production the topic exists because Debezium has already
    // been producing CDC events to it; here nothing does that, so it must be created before the
    // Streams app (a Spring bean) starts. A static @BeforeAll runs before SpringExtension loads the
    // context, so it's the right place for this.
    @BeforeAll
    static void createCdcTopic() throws Exception {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, TestContainersConfig.KAFKA.getBootstrapServers());

        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(List.of(new NewTopic(CDC_SCYLLA_CHAT_MESSAGE, 1, (short) 1))).all().get();
        } catch (ExecutionException e) {
            if (!(e.getCause() instanceof TopicExistsException)) {
                throw e;
            }
        }
    }

    @Autowired
    private KafkaTestSupport kafkaTestSupport;

    @Test
    void chatMessageCdcStream_republishesInsertedRowAsChatMessageCreatedEvent() throws Exception {
        // Given
        UUID trialId = UuidCreator.getTimeOrderedEpoch();

        String debeziumEnvelope = """
                {
                  "payload": {
                    "op": "c",
                    "after": {
                      "trial_id": "%s",
                      "content": "hello",
                      "sequence": 1,
                      "type": "USER"
                    }
                  }
                }
                """.formatted(trialId);

        // When
        try (KafkaProducer<String, String> producer = rawStringProducer()) {
            producer.send(new ProducerRecord<>(CDC_SCYLLA_CHAT_MESSAGE, trialId.toString(), debeziumEnvelope)).get();
        }

        // Then
        ConsumerRecords<String, String> records = kafkaTestSupport.pollRecords(CHAT_MESSAGE_CREATED_EVENT);

        assertThat(records).isNotEmpty();
        assertThat(records.iterator().next().value()).contains(trialId.toString());
    }

    private KafkaProducer<String, String> rawStringProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, TestContainersConfig.KAFKA.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        return new KafkaProducer<>(props);
    }

}