package com.example.solomon.common.adapter.out.persistence;

import com.example.solomon.common.adapter.out.persistence.jpa.SpringDataJpaOutboxRepository;
import org.springframework.stereotype.Repository;

import com.example.solomon.common.application.out.OutboxRepository;
import com.example.solomon.common.domain.entity.jpa.Outbox;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class OutboxRepositoryAdapter implements OutboxRepository {

    private final SpringDataJpaOutboxRepository jpaOutboxRepository;

    @Override
    public Outbox save(Outbox outbox) {
        return jpaOutboxRepository.save(outbox);
    }

}