package com.example.solomon.feature.chat.application.in.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.example.solomon.feature.chat.application.in.usecase.dto.CreateChatMessageCommand;
import com.example.solomon.feature.trial.application.port.out.TrialRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotifyTrialCreatedUsecase {

    private final TrialRepository trialRepository;

    private final CreateChatMessageUsecase createChatMessageUsecase;

    public void execute(UUID trialId) {
        String nickname = trialRepository.findAllTrialMembersByTrialId(trialId).get(0).getNickname();

        createChatMessageUsecase.execute(
                new CreateChatMessageCommand(trialId, null, nickname + "came into play."));
    }

}
