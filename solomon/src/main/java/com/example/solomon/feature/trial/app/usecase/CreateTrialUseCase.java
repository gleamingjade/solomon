package com.example.solomon.feature.trial.app.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.solomon.common.app.dto.exception.AppException;
import com.example.solomon.feature.member.app.dto.exception.MemberException;
import com.example.solomon.feature.member.domain.entity.Member;
import com.example.solomon.feature.member.domain.repository.MemberRepository;
import com.example.solomon.feature.trial.app.dto.CreateTrialCommand;
import com.example.solomon.feature.trial.app.dto.exception.TrialException;
import com.example.solomon.feature.trial.domain.entity.Stage;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.repository.TrialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateTrialUseCase {

    private final MemberRepository memberRepository;

    private final TrialRepository trialRepository;

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