package com.example.solomon.feature.chat.adapter.in.messaging.kafka.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;

// See KafkaTemplateConfig (common) for the plain, shared KafkaTemplate. Only
// CreateChatMessageUsecase's multi-message batch needs a transactional producer - a transaction
// costs extra broker round trips per send (AddPartitionsToTxn, EndTxn on top of the plain
// Produce call) that nothing else needs to pay for - so it stays chat-local.
@Configuration
public class ChatKafkaProducerConfig {

    @Bean(name = "chatMessageTransactionalKafkaTemplate")
    public KafkaTemplate<Object, Object> chatMessageTransactionalKafkaTemplate(
            KafkaProperties kafkaProperties, @Value("${SERVER_ID}") String serverId) {
        DefaultKafkaProducerFactory<Object, Object> factory =
                new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());

        // Must be unique per app instance - SERVER_ID keeps two instances from fencing each other.
        // Spring appends a plain incrementing counter (no separator) to build the actual
        // transactional.id per pooled producer, e.g. prefix + "0", prefix + "1", ... Since
        // SERVER_ID itself ends in a digit ("서버-1", "서버-2", ...), omitting the trailing "-"
        // could let two different servers collide on the same final id, e.g. server "서버-1"'s
        // counter 23 and server "서버-12"'s counter 3 would both produce "...서버-123". The
        // trailing "-" pins down where SERVER_ID ends and Spring's counter begins.
        factory.setTransactionIdPrefix("chat-message-" + serverId + "-");

        return new KafkaTemplate<>(factory);
    }

}
