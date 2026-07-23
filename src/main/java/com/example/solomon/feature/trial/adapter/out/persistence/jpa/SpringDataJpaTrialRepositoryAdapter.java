package com.example.solomon.feature.trial.adapter.out.persistence.jpa;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.example.solomon.feature.trial.application.port.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.Stage;
import com.example.solomon.feature.trial.domain.entity.Trial;
import com.example.solomon.feature.trial.domain.entity.TrialMember;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SpringDataJpaTrialRepositoryAdapter implements TrialRepository {

    private final SpringDataJpaTrialRepository jpaTrialRepository;
    private final SpringDataJpaTrialMemberRepository jpaTrialMemberRepository;

    @Override
    public Trial save(Trial trial) {
        return jpaTrialRepository.save(trial);
    }

    @Override
    public Optional<Trial> findById(UUID id) {
        return jpaTrialRepository.findById(id);
    }

    @Override
    public long countUnTerminatedTrialByMemberId(Long memberId, Stage stage) {
        return jpaTrialRepository.countUnTerminatedTrialByMemberId(memberId, stage);
    }

    @Override
    public Optional<Trial> findByIdWithTrialMembers(UUID id) {
        return jpaTrialRepository.findByIdWithTrialMembers(id);
    }

    @Override
    public List<TrialMember> findAllTrialMembersByTrialId(UUID trialId) {
        return jpaTrialMemberRepository.findAllByTrialId(trialId);

    }

}