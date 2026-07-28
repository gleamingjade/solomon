package com.example.solomon.feature.chat.application.in.usecase;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.Row;
import com.example.solomon.DebeziumTestSupport;
import com.example.solomon.KafkaTestSupport;
import com.example.solomon.SlicedSpringContextTest;
import com.example.solomon.TestContainersConfig;
import com.example.solomon.feature.chat.application.in.usecase.dto.CreateChatMessageCommand;
import com.github.f4b6a3.uuid.UuidCreator;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.cassandra.DataCassandraTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SlicedSpringContextTest
@Import({
        TestContainersConfig.class,
        CreateChatMessageUsecase.class
})
@DataCassandraTest
@ImportAutoConfiguration({
        KafkaAutoConfiguration.class,
})
class CreateChatMessageUsecaseTest {

    @Autowired
    private KafkaTestSupport kafkaTestSupport;

    @Autowired
    private DebeziumTestSupport debeziumTestSupport;

    @Autowired
    private CreateChatMessageUsecase createChatMessageUsecase;

    @Autowired
    private CqlSession cqlSession;

    @Test
    void testScyllaConnectorStatus() {
        System.out.println(
                debeziumTestSupport.getConnectorStatus("scylla-source-connector"));
    }

    @Test
    void testCreateChatMessageUsecase() throws InterruptedException {
        CreateChatMessageCommand command = new CreateChatMessageCommand(UuidCreator.getTimeOrderedEpoch(), 1L, "test");

        createChatMessageUsecase.execute(command);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<Row> rows = cqlSession.execute("""
                            SELECT *
                            FROM localscylla.chat_message_scylla_cdc_log
                            """).all();

                    assertThat(rows).isNotEmpty();
                });

        assertThat(kafkaTestSupport.pollRecords("cdc-scylla.localscylla.chat_message")).anyMatch(record -> {
            System.out.println(record.value());
            return true;
        });

    }

}