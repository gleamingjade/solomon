package com.example.solomon.feature.chat.adapter.in.websocket;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ChatWebSocketSessionTracker implements WebSocketHandlerDecoratorFactory {

    private final Map<String, WebSocketSession> sessionsByMemberId = new ConcurrentHashMap<>();

    private final SimpMessagingTemplate simpMessagingTemplate;

    @Override
    public WebSocketHandler decorate(WebSocketHandler handler) {
        return new WebSocketHandlerDecorator(handler) {

            @Override
            public void afterConnectionEstablished(WebSocketSession session) throws Exception {
                Principal principal = session.getPrincipal();

                if (principal != null) {
                    WebSocketSession previous = sessionsByMemberId.put(principal.getName(), session);

                    if (previous != null) {
                        kick(principal.getName(), previous);
                    }
                }

                super.afterConnectionEstablished(session);
            }

            @Override
            public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
                Principal principal = session.getPrincipal();

                if (principal != null) {
                    sessionsByMemberId.remove(principal.getName(), session);
                }

                super.afterConnectionClosed(session, closeStatus);
            }
        };
    }

    private void kick(String memberId, WebSocketSession previous) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headerAccessor.setSessionId(previous.getId());
        headerAccessor.setLeaveMutable(true);

        simpMessagingTemplate.convertAndSendToUser(
                memberId,
                "/queue/session-replaced",
                Map.of("reason", "REPLACED_BY_NEW_CONNECTION"),
                headerAccessor.getMessageHeaders());

        if (previous.isOpen()) {
            try {
                previous.close(CloseStatus.POLICY_VIOLATION);
            } catch (Exception ignored) {
            }
        }
    }

}
