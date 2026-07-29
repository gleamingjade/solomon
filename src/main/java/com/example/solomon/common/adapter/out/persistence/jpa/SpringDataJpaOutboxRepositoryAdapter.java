package com.example.solomon.common.adapter.out.persistence.jpa;

import org.springframework.stereotype.Repository;

import com.example.solomon.common.application.out.OutboxRepository;
import com.example.solomon.common.domain.entity.Outbox;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SpringDataJpaOutboxRepositoryAdapter implements OutboxRepository {

    private final SpringDataJpaOutboxRepository jpaOutboxRepository;

    @Override
    public Outbox save(Outbox outbox) {
        return jpaOutboxRepository.save(outbox);
    }

}