package com.example.solomon.feature.chat.app.usecase;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.example.solomon.feature.chat.app.dto.CreateChatMessageCommand;
import com.example.solomon.feature.chat.domain.entity.ChatMessage;
import com.example.solomon.feature.chat.domain.repository.ChatMessageRepository;
import com.example.solomon.feature.chat.infra.messaging.event.ChatMessageCreatedEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateChatMessageUsecase {

    private final ApplicationEventPublisher eventPublisher;

    private final ChatMessageRepository chatMessageRepository;

    public void execute(CreateChatMessageCommand command) {
        chatMessageRepository
                .save(ChatMessage.create(command.trialId(), command.memberId(), command.content()));

        // @formatter:off
        // If the application layer, which contains business logic, depends on the infrastructure layer,
        // such as WebSocketPublisher, the application logic becomes coupled to infrastructure concerns,
        // losing focus on the business logic itself.
        //
        // To prevent this kind of situation, raise an event and let the infrastructure layer depend on the event
        // instead of the application layer depending on infrastructure concerns.
        //
        // I'm going to use Spring's ApplicationEventPublisher.
        // One concern is that the application layer becomes coupled to the framework.
        // However, I think this is acceptable since framework changes are rarely required in practice.
        // Since I'm using JPA, I decided to use JPA entities as domain objects instead of keeping a pure POJO-based domain model.
        // @formatter:on

        // 음.. 근데 상식적으로 생각하자. 메시지가 발생하면 cdc로 읽고 있는데 굳이 애플리케이션 이벤트를 써야 하냐? 
        eventPublisher.publishEvent(new ChatMessageCreatedEvent());
    }

}
