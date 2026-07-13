package com.example.solomon.feature.chat.adapter.in.kafka;

import java.util.UUID;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.example.solomon.common.infra.messaging.kafka.DebeziumEnvelope;
import com.example.solomon.feature.chat.application.in.usecase.AllocateChatServerUsecase;
import com.example.solomon.feature.chat.application.in.usecase.CreateChatMessageUsecase;
import com.example.solomon.feature.chat.application.in.usecase.dto.CreateChatMessageCommand;
import com.example.solomon.feature.trial.application.port.out.TrialRepository;
import com.example.solomon.feature.trial.domain.entity.TrialMember;

import lombok.RequiredArgsConstructor;

// 왜냐하면 adapter는 "어디에서 들어오는가"를 표현하는 계층이기 때문입니다.

/**
 * 음. 우리 서비스는 1번에 1채팅방이기 때문에
 * 일단 채팅방 생성되면 redis에 한 채팅방을 할당해서 맵핑함
 * 채팅방 입장하면 그 채팅방에만 입장하게 해서 유저들이 같은 채팅 서버로 모이게 하고 그 채팅방 id로 구독
 * 채팅오면 저장하고 바로 그 방에 쏘면 됨
 * 
 * hash로 생성할 때 정해버리면
 * 나중에 채팅서버 추가되도 새 유저가 같은 곳으로 편입되고
 * 채팅 서버 추가 이후에 생성된 건 새로 되겠지
 * 
 * 그니까 처음 채팅방에 접속할 때, 구독 요청 인터셉터로 잡아서 만약에 없으면 redis에 넣어야 하고 있으면 좋고
 */

@Component
@RequiredArgsConstructor
public class TrialCreatedConsumer {

    private final TrialRepository trialRepository;

    private final AllocateChatServerUsecase allocateChatServerUsecase;

    private final CreateChatMessageUsecase createChatMessageUsecase;

    @KafkaListener(topics = "cdc-mysql.localdb.trial-created", containerFactory = "trialCreatedConsumerFactory")
    public void consume(DebeziumEnvelope envelope) {
        UUID trialId = UUID.fromString(envelope.payload().after().get("id").asText());

        TrialMember creator = trialRepository.findAllTrialMembersByTrialId(trialId).get(0);

        allocateChatServerUsecase.allocate();

        // 이건 채팅방 입장 시 인터셉터에서 해야할 듯.
        // createChatMessageUsecase.execute(
        //         new CreateChatMessageCommand(
        //                 trialId,
        //                 null,
        //                 creator.getNickname() + "came into play."));
    }

}
