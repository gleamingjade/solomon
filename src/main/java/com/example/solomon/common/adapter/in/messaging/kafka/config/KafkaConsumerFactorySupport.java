package com.example.solomon.common.adapter.in.messaging.kafka.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.serializer.DeserializationException;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;
import org.springframework.util.backoff.FixedBackOff;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class KafkaConsumerFactorySupport {

    private final String bootstrapServers;

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public KafkaConsumerFactorySupport(@Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
                                       KafkaTemplate<Object, Object> kafkaTemplate) {
        this.bootstrapServers = bootstrapServers;
        this.kafkaTemplate = kafkaTemplate;
    }

    private Map<String, Object> getBaseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        // Transactionally-produced messages land in the log either way, committed or aborted -
        // only a marker record tells them apart. Without this, a consumer would happily process
        // messages belonging to an aborted (rolled-back) transaction too. Harmless for topics
        // nothing ever produces to transactionally.
        props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        return props;
    }

    // If processing fails, the error record is automatically published to the dead-letter topic
    // with a ".DLT" suffix appended to the original topic name. If the DLT publish itself fails,
    // the offset is never committed, so Kafka keeps redelivering the same record - we don't drop
    // it, but that does mean the partition stalls until it's resolved (see recoveryFailed below).
    private DefaultErrorHandler buildErrorHandler(String listenerLabel) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                new DeadLetterPublishingRecoverer(kafkaTemplate), new FixedBackOff(1000L, 3L));

        // A deserialization failure will never succeed on retry (the bytes don't change), so skip
        // straight to the DLT instead of wasting 3 retries on it.
        errorHandler.addNotRetryableExceptions(DeserializationException.class);

        errorHandler.setRetryListeners(new RetryListener() {

            @Override
            public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
                if (deliveryAttempt == 1) {
                    log.error("{} listener failed, topic={}, partition={}, offset={}",
                            listenerLabel, record.topic(), record.partition(), record.offset(), ex);
                }
            }

            @Override
            public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
                log.error("Failed to publish {} record to DLT, topic={}, partition={}, offset={} - offset will "
                                + "not advance, partition will stall until resolved",
                        listenerLabel, record.topic(), record.partition(), record.offset(), failure);
            }
        });

        return errorHandler;
    }

    public <T> ConcurrentKafkaListenerContainerFactory<String, T> buildFactory(Class<T> valueType, String topic) {
        Map<String, Object> props = getBaseProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, topic + "-consumer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, T> consumerFactory = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(new JsonDeserializer<>(valueType, false)));

        ConcurrentKafkaListenerContainerFactory<String, T> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setConcurrency(1);
        factory.setCommonErrorHandler(buildErrorHandler(topic));

        return factory;
    }

}