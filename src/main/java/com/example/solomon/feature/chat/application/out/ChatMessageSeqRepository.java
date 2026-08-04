package com.example.solomon.feature.chat.application.out;

public interface ChatMessageSeqRepository {

    public Long incr(String trialId);

    public void fillBack(String trialId, String seq);

}