package com.example.solomon.feature.trial.application.in.usecase;

import com.example.solomon.feature.member.application.out.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.solomon.common.application.out.OutboxRepository;
import com.example.solomon.common.domain.entity.jpa.Outbox;
import com.example.solomon.common.domain.exception.BusinessException;
import com.example.solomon.common.util.JsonUtils;
import com.example.solomon.feature.member.domain.exception.MemberException;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.trial.application.in.usecase.dto.CreateTrialCommand;
import com.example.solomon.feature.trial.application.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.event.TrialCreatedEvent;
import com.example.solomon.feature.trial.domain.exception.TrialException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateTrialUseCase {

    private final TrialRepository trialRepository;

    private final MemberRepository memberRepository;

    private final OutboxRepository outboxRepository;

    @Transactional
    public String execute(CreateTrialCommand command) {
        Member member = memberRepository
                .findById(command.memberId())
                .orElseThrow(() -> new BusinessException(MemberException.UNEXISTS_MEMBER));

        if (trialRepository.countUnTerminatedTrialByMemberId(member.getId()) > 0)
            throw new BusinessException(TrialException.ONGOING_TRIAL_EXISTS);

        Trial trial = trialRepository.save(Trial.create(member, command.issueTitle(), command.nickname()));

        outboxRepository.save(
                Outbox.create(
                        Trial.AGGREGATE_TYPE,
                        trial.getId().toString(),
                        TrialCreatedEvent.EVENT_TYPE,
                        JsonUtils.writeValueAsString(new TrialCreatedEvent(trial.getId(), member.getId(), command.nickname()))));

        return trial.getId().toString();
    }

}