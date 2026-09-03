package com.example.solomon.common.adapter.in.messaging.kafka.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;

import com.example.solomon.feature.chat.adapter.in.messaging.kafka.config.ChatKafkaProducerConfig;

import static org.assertj.core.api.Assertions.assertThat;

// Defining a KafkaTemplate bean here (KafkaTemplateConfig) and another one in
// ChatKafkaProducerConfig risks silently short-circuiting Spring Boot's own
// KafkaAutoConfiguration (its @ConditionalOnMissingBean checks are raw-type, so it backs off the
// instant *any* KafkaTemplate bean exists - see KafkaTemplateConfig's comment). This asserts the
// resulting wiring is actually what we intend, not just "it compiles".
class KafkaTemplateConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(KafkaAutoConfiguration.class))
            .withUserConfiguration(KafkaTemplateConfig.class, ChatKafkaProducerConfig.class)
            .withPropertyValues(
                    "spring.kafka.bootstrap-servers=localhost:9092",
                    "SERVER_ID=test-server");

    @Test
    void exposesExactlyTwoKafkaTemplates_plainAndTransactional() {
        contextRunner.run(context -> {
            assertThat(context.getBeansOfType(KafkaTemplate.class)).hasSize(2);

            KafkaTemplate<?, ?> plain = context.getBean("kafkaTemplate", KafkaTemplate.class);
            KafkaTemplate<?, ?> transactional = context.getBean("chatMessageTransactionalKafkaTemplate", KafkaTemplate.class);

            assertThat(plain.getTransactionIdPrefix()).isNull();
            assertThat(transactional.getTransactionIdPrefix()).isEqualTo("chat-message-test-server-");
        });
    }

}
