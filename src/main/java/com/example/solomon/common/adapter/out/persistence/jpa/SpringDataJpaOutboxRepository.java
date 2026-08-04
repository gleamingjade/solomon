package com.example.solomon.common.adapter.out.persistence.jpa;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.solomon.common.domain.entity.jpa.Outbox;

public interface SpringDataJpaOutboxRepository extends JpaRepository<Outbox, UUID> {
}