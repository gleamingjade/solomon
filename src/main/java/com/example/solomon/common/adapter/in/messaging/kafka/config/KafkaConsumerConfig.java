package com.example.solomon.common.adapter.in.messaging.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import com.example.solomon.common.adapter.in.messaging.kafka.DebeziumEnvelope;
import com.example.solomon.feature.trial.domain.event.TrialCreatedEvent;

import static com.example.solomon.common.adapter.in.messaging.kafka.config.KafkaTopics.TRIAL_CREATED_EVENT;

import lombok.extern.slf4j.Slf4j;

// Referenced by https://docs.spring.io/spring-kafka/reference/kafka/container-factory.html
@Slf4j
@Configuration
public class KafkaConsumerConfig {

    private final String bootstrapServers;

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public KafkaConsumerConfig(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                               @Autowired KafkaTemplate<Object, Object> kafkaTemplate) {
        this.bootstrapServers = bootstrapServers;
        this.kafkaTemplate = kafkaTemplate;
    }

    private Map<String, Object> getBaseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return props;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TrialCreatedEvent> trialCreatedConsumerFactory() {
        Map<String, Object> props = getBaseProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, TRIAL_CREATED_EVENT + "-consumer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        // Wrapping in ErrorHandlingDeserializer means a malformed record's deserialization
        // failure surfaces as a DeserializationException routed through the same
        // CommonErrorHandler below (DLT etc.), instead of raw JsonDeserializer throwing inside
        // poll() and potentially killing the whole container.
        DefaultKafkaConsumerFactory<String, TrialCreatedEvent> consumerFactory = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>(TrialCreatedEvent.class, false)));

        ConcurrentKafkaListenerContainerFactory<String, TrialCreatedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // KafkaTopicConfig creates this topic with 2 partitions, matched to our 2 app-server
        // instances. Each instance's container should only run 1 consumer thread here, so the
        // 2 instances together hold exactly 1 partition each instead of over-subscribing.
        factory.setConcurrency(1);

        // If processing fails, the error record is automatically published to the
        // dead-letter topic with a ".DLT" suffix appended to the original topic name. If the DLT
        // publish itself fails, the offset is never committed, so Kafka keeps redelivering the
        // same record - we don't drop it, but that does mean the partition stalls until it's
        // resolved (see recoveryFailed below).
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));

        // A deserialization failure will never succeed on retry (the bytes don't change), so skip
        // straight to the DLT instead of wasting 3 retries on it.
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        errorHandler.setRetryListeners(new RetryListener() {

            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                if (deliveryAttempt == 1) {
                    log.error("trial-created-event listener failed, topic={}, partition={}, offset={}",
                            record.topic(), record.partition(), record.offset(), ex);
                }
            }

            @Override
            public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
                log.error("Failed to publish trial-created-event record to DLT, topic={}, partition={}, "
                                + "offset={} - offset will not advance, partition will stall until resolved",
                        record.topic(), record.partition(), record.offset(), failure);
            }
        });

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DebeziumEnvelope> chatMessageCreatedConsumerFactory() {
        Map<String, Object> props = getBaseProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "chat-message-created-topic-consumer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, DebeziumEnvelope> consumerFactory = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new JsonDeserializer<>(DebeziumEnvelope.class, false));

        ConcurrentKafkaListenerContainerFactory<String, DebeziumEnvelope> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

}
