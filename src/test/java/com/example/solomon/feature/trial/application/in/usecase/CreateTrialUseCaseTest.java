package com.example.solomon.feature.trial.application.in.usecase;

import com.example.solomon.SlicedSpringContextTest;
import com.example.solomon.TestContainersConfig;
import com.example.solomon.common.adapter.out.persistence.jpa.SpringDataJpaOutboxRepository;
import com.example.solomon.common.adapter.out.persistence.jpa.SpringDataJpaOutboxRepositoryAdapter;
import com.example.solomon.common.adapter.out.persistence.jpa.config.JpaConfig;
import com.example.solomon.common.application.out.OutboxRepository;
import com.example.solomon.common.util.JsonUtils;
import com.example.solomon.feature.member.adapter.out.persistence.jpa.SpringDataJpaMemberRepositoryAdapter;
import com.example.solomon.feature.member.application.out.MemberRepository;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.trial.adapter.out.persistence.jpa.SpringDataJpaTrialRepositoryAdapter;
import com.example.solomon.feature.trial.application.in.usecase.dto.CreateTrialCommand;
import com.example.solomon.feature.trial.application.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.event.TrialCreatedEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;


@SlicedSpringContextTest
@DataJpaTest
@ActiveProfiles("test")
@Import({TestContainersConfig.class, JpaConfig.class, SpringDataJpaMemberRepositoryAdapter.class,
        SpringDataJpaTrialRepositoryAdapter.class, SpringDataJpaOutboxRepositoryAdapter.class})
class CreateTrialUseCaseTest {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TrialRepository trialRepository;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private SpringDataJpaOutboxRepository springDataJpaOutboxRepository;

    @Test
    void testCreateTrialUseCase() {
        CreateTrialUseCase createTrialUseCase = new CreateTrialUseCase(trialRepository, memberRepository, outboxRepository);

        // Given
        Member m = memberRepository.save(Member.create("email", "picture"));

        // When
        String trialId = createTrialUseCase.execute(new CreateTrialCommand(m.getId(), "issueTitle", "nickname"));

        // Then
        assertThat(springDataJpaOutboxRepository.findAll()).anySatisfy(outbox -> {
            assertThat(outbox.getAggregateType()).isEqualTo(Trial.AGGREGATE_TYPE);
            assertThat(outbox.getAggregateId()).isEqualTo(trialId);
            assertThat(outbox.getEventType()).isEqualTo(TrialCreatedEvent.EVENT_TYPE);

            TrialCreatedEvent event = JsonUtils.readValue(outbox.getPayload(), TrialCreatedEvent.class);
            assertThat(event.trialId()).isEqualTo(UUID.fromString(trialId));
            assertThat(event.memberId()).isEqualTo(m.getId());
            assertThat(event.nickname()).isEqualTo("nickname");
        });
    }

}