package com.example.solomon.feature.chat.application.out;

import java.util.UUID;

public interface ChatMessageSeqRepository {

    Long next(UUID trialId);

}
