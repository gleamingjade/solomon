package com.example.solomon.feature.trial.app.usecase;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.example.solomon.TestContainersConfig;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.member.domain.repository.MemberRepository;
import com.example.solomon.feature.trial.app.dto.CreateTrialCommand;
import com.example.solomon.feature.trial.domain.repository.TrialRepository;

@ActiveProfiles("local")
@Import(TestContainersConfig.class)
@SpringBootTest
public class CreateTrialUseCaseTest {

    @Autowired
    private CreateTrialUseCase createTrialUseCase;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TrialRepository trialRepository;

    @Test
    void testCreate() {
        Member m = memberRepository.save(Member.create("email", "picture"));

        createTrialUseCase.create(new CreateTrialCommand(m.getId(), "issueTitle", "nickname"));

    }

}
