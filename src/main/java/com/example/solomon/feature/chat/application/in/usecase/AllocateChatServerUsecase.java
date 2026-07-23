package com.example.solomon.feature.chat.application.in.usecase;

import org.springframework.stereotype.Service;

import com.example.solomon.feature.chat.application.out.ChatMessageSeqRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AllocateChatServerUsecase {

    private final ChatMessageSeqRepository chatMessageSeqRepository;

    public void allocate() {
        // 현재 서버 개수 조회
        // 해쉬로 할당
    }

}
