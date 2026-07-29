package com.example.solomon.common.application.out;

import com.example.solomon.common.domain.entity.Outbox;

public interface OutboxRepository {

    Outbox save(Outbox outbox);

}