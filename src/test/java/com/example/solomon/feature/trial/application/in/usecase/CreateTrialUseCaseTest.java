package com.example.solomon.feature.trial.application.in.usecase;

import com.example.solomon.KafkaTestSupport;
import com.example.solomon.SlicedSpringContextTest;
import com.example.solomon.TestContainersConfig;
import com.example.solomon.TestDataCleanupExtension;
import com.example.solomon.common.adapter.out.persistence.jpa.config.JpaConfig;
import com.example.solomon.feature.member.adapter.out.persistence.jpa.SpringDataJpaMemberRepositoryAdapter;
import com.example.solomon.feature.member.application.out.MemberRepository;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.trial.adapter.out.persistence.jpa.SpringDataJpaTrialRepositoryAdapter;
import com.example.solomon.feature.trial.application.in.usecase.dto.CreateTrialCommand;
import com.example.solomon.feature.trial.application.out.TrialRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;

import static org.assertj.core.api.Assertions.assertThat;


@Slf4j
@SlicedSpringContextTest
@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, JpaConfig.class, SpringDataJpaMemberRepositoryAdapter.class, SpringDataJpaTrialRepositoryAdapter.class})
@ExtendWith(TestDataCleanupExtension.class)
class CreateTrialUseCaseTest {

    @Autowired
    private KafkaTestSupport kafkaTestSupport;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TrialRepository trialRepository;

    @Test
    void testCreateTrialUseCase() {
        CreateTrialUseCase createTrialUseCase = new CreateTrialUseCase(trialRepository, memberRepository);

        // Given
        Member m = memberRepository.save(Member.create("email", "picture"));

        // When
        createTrialUseCase.execute(new CreateTrialCommand(m.getId(), "issueTitle", "nickname"));

        // Debezium only sees committed binlog events, so force a commit here before polling Kafka
        // instead of relying on @DataJpaTest's default end-of-test rollback.
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Then
        assertThat(kafkaTestSupport.pollRecords(TestContainersConfig.MYSQL_CDC_TOPIC_PREFIX
                + "." + TestContainersConfig.MYSQL_DATABASE
                + "." + TestContainersConfig.MYSQL_TRIAL_TABLE)).anyMatch(record -> {
            log.info(record.value());
            return record.value().contains("\"issue_title\":\"issueTitle\"");
        });
    }


}