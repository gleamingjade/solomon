package com.example.solomon.feature.trial.infra.messaging;

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
import com.example.solomon.feature.trial.app.usecase.CreateTrialUseCase;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.repository.TrialRepository;

import java.time.Duration;

import org.awaitility.Awaitility;

@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@SpringBootTest
public class TrialCreatedConsumerTest {

    @Autowired
    private KafkaTestSupport kafkaTestSupport;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CreateTrialUseCase createTrialUseCase;

    @Test
    void testCreateTrialUseCase() {
        // given
        Member m = memberRepository.save(Member.create("email", "picture"));
        createTrialUseCase.execute(new CreateTrialCommand(m.getId(), "issueTitle", "nickname"));

        // when
        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(null);
    }

}
