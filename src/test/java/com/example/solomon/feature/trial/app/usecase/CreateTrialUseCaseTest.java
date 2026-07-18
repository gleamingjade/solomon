package com.example.solomon.feature.trial.app.usecase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.example.solomon.KafkaTestSupport;
import com.example.solomon.TestContainersConfig;
import com.example.solomon.feature.chat.adapter.in.kafka.TrialCreatedConsumer;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.member.domain.repository.MemberRepository;
import com.example.solomon.feature.trial.application.port.in.usecase.CreateTrialUseCase;
import com.example.solomon.feature.trial.application.port.in.usecase.dto.CreateTrialCommand;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@SpringBootTest
public class CreateTrialUseCaseTest {

    // This test is only for the usecase. consumer have to speak when spoken to.
    @MockitoBean
    private TrialCreatedConsumer trialCreatedConsumer;

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

        // when
        createTrialUseCase.execute(new CreateTrialCommand(m.getId(), "issueTitle", "nickname"));

        // then
        assertThat(kafkaTestSupport.pollRecords("cdc-mysql.localdb.trial")).anyMatch(record -> {
            System.out.println(record.value());
            return record.value().contains("\"issue_title\":\"issueTitle\"");
        });
    }

}
