package com.example.solomon.common.adapter.in.messaging.kafka.config;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

// Defining any bean of type KafkaTemplate/ProducerFactory makes Spring Boot's own
// KafkaAutoConfiguration back off entirely for both - its @ConditionalOnMissingBean checks are
// raw-type (checked directly against KafkaTemplate.class/ProducerFactory.class), not scoped to a
// bean name. So once a feature needs a second, transactional template (see
// ChatKafkaProducerConfig), the plain default one has to be re-provided explicitly too, shared
// here since it's used across features (DLT publishing in KafkaConsumerFactorySupport, every
// single-message producer in trial and chat alike) - not something chat should own.
@Configuration
public class KafkaTemplateConfig {

    @Bean(name = "kafkaTemplate")
    public KafkaTemplate<Object, Object> kafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties()));
    }

}
