package com.example.solomon.feature.trial.app.usecase;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.solomon.KafkaTestSupport;
import com.example.solomon.TestContainersConfig;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.member.domain.repository.MemberRepository;
import com.example.solomon.feature.trial.app.dto.CreateTrialCommand;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@SpringBootTest
public class CreateTrialUseCaseTest {

    @Autowired
    private KafkaTestSupport kafkaTestSupport;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CreateTrialUseCase createTrialUseCase;

    @Test
    void testCreateTrialUseCase() {
        Properties props = new Properties();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, TestContainersConfig.KAFKA.getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(List.of("cdc-mysql.localdb.trial"));

            // given
            Member m = memberRepository.save(Member.create("email", "picture"));

            // when
            createTrialUseCase.execute(new CreateTrialCommand(m.getId(), "issueTitle", "nickname"));

            // then
            assertThat(kafkaTestSupport.pollRecords("cdc-mysql.localdb.trial")).anyMatch(record -> {
                return record.value().contains("\"issue_title\":\"issueTitle\"");
            });
        }
    }

}
