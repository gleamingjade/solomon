package com.example.solomon.feature.trial.application.port.in.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.solomon.common.app.dto.exception.AppException;
import com.example.solomon.feature.member.application.in.usecase.dto.exception.MemberException;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.member.domain.repository.MemberRepository;
import com.example.solomon.feature.trial.application.port.in.usecase.dto.CreateTrialCommand;
import com.example.solomon.feature.trial.application.port.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.Stage;
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
            throw new AppException(MemberException.UNEXISTS_MEMBER);
        });

        if (trialRepository.countUnTerminatedTrialByMemberId(member.getId(),
                Stage.TERMINATED) > 0) {
            throw new AppException(TrialException.ONGOING_TRIAL_EXISTS);
        }

        return trialRepository.save(Trial.create(member, command.issueTitle(),
                command.nickname())).getId().toString();
    }

}