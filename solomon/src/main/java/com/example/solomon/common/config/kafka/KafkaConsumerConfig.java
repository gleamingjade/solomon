package com.example.solomon.common.config.kafka;

import java.util.HashMap;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
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
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import com.example.solomon.common.app.dto.DebeziumEnvelope;

// Referenced by https://docs.spring.io/spring-kafka/reference/kafka/container-factory.html
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

    // @formatter:off
    // In this project, we don't have enough money for letting consumer occupy their own host.
    // In other words, we got multiple groups so we have to go with like this seperate way 
    // than using yml auto configuration so that the consumer can have their own way to handle the event.
    // @formatter:on
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DebeziumEnvelope> trialCreatedConsumer() {
        Map<String, Object> props = getBaseProps();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "trial-created-topic-consumer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        DefaultKafkaConsumerFactory<String, DebeziumEnvelope> consumerFactory = new DefaultKafkaConsumerFactory<>(props,
                new StringDeserializer(), new JsonDeserializer<>(DebeziumEnvelope.class, false));

        ConcurrentKafkaListenerContainerFactory<String, DebeziumEnvelope> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);

        // If processing fails, the error record is automatically published to the
        // dead-letter topic with a ".DLT" suffix appended to the original topic name.
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, new FixedBackOff(1000L, 3L));

        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

}
