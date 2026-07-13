package com.example.solomon.feature.chat.application.in.usecase;

import org.springframework.stereotype.Service;

import com.example.solomon.feature.chat.application.out.ChatServerMappingRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AllocateChatServerUsecase {

    private final ChatServerMappingRepository chatServerMappingRepository;

    public void allocate() {
        // 현재 서버 개수 조회
        // 해쉬로 할당
    }

}
