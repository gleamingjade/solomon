package com.example.solomon.feature.trial.application.in.usecase;

import com.example.solomon.feature.member.application.out.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.solomon.common.domain.entity.exception.ApplicationException;
import com.example.solomon.feature.member.domain.exception.MemberException;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.trial.application.in.usecase.dto.CreateTrialCommand;
import com.example.solomon.feature.trial.application.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.exception.TrialException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateTrialUseCase {

    private final TrialRepository trialRepository;

    private final MemberRepository memberRepository;

    @Transactional
    public String execute(CreateTrialCommand command) {
        Member member = memberRepository.findById(command.memberId()).orElseThrow(() -> {
            throw new ApplicationException(MemberException.UNEXISTS_MEMBER);
        });

        if (trialRepository.countUnTerminatedTrialByMemberId(member.getId()) > 0) {
            throw new ApplicationException(TrialException.ONGOING_TRIAL_EXISTS);
        }

        return trialRepository.save(Trial.create(member, command.issueTitle(),
                command.nickname())).getId().toString();
    }

}