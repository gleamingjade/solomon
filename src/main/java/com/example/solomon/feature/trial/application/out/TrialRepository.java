package com.example.solomon.feature.trial.application.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.entity.TrialMember;


public interface TrialRepository {

    public Trial save(Trial trial);

    public Optional<Trial> findById(UUID id);

    public long countUnTerminatedTrialByMemberId(Long memberId);

    public Optional<Trial> findByIdWithTrialMembers(UUID id);

    public List<TrialMember> findAllTrialMembersByTrialId(UUID trialId);

}
