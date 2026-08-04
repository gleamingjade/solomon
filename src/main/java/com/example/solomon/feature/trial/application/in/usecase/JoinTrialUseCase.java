package com.example.solomon.feature.trial.application.in.usecase;

import com.example.solomon.common.application.out.OutboxRepository;
import com.example.solomon.common.domain.entity.jpa.Outbox;
import com.example.solomon.common.domain.exception.BusinessException;
import com.example.solomon.common.util.JsonUtils;
import com.example.solomon.feature.member.application.out.MemberRepository;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.member.domain.exception.MemberException;
import com.example.solomon.feature.trial.application.in.usecase.dto.JoinTrialCommand;
import com.example.solomon.feature.trial.application.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.event.TrialJoinedEvent;
import com.example.solomon.feature.trial.domain.exception.TrialException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JoinTrialUseCase {

    private final TrialRepository trialRepository;

    private final MemberRepository memberRepository;

    private final OutboxRepository outboxRepository;

    public void execute(JoinTrialCommand command) {
        Member member = memberRepository
                .findById(command.memberId())
                .orElseThrow(() -> new BusinessException(MemberException.UNEXISTS_MEMBER));

        if (trialRepository.countUnTerminatedTrialByMemberId(member.getId()) > 0)
            throw new BusinessException(TrialException.ONGOING_TRIAL_EXISTS);

        Trial trial = trialRepository.findByIdWithTrialMembers(command.trialId())
                .orElseThrow(() -> new BusinessException(TrialException.UNEXISTS_TRIAL));

        trial.join(member, command.nickname());

        outboxRepository.save(
                Outbox.create(
                        Trial.AGGREGATE_TYPE,
                        trial.getId().toString(),
                        TrialJoinedEvent.EVENT_TYPE,
                        JsonUtils.writeValueAsString(new TrialJoinedEvent(trial.getId(), command.nickname()))));
    }

}
