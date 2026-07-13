package com.example.solomon.feature.chat.domain.repository;

public interface ChatMessageSeqRepository {

    public Long incr(String trialId);
    
}