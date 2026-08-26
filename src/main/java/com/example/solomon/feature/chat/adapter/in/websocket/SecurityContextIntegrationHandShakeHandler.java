package com.example.solomon.feature.chat.adapter.in.websocket;

import com.example.solomon.common.adapter.in.web.security.SessionMemberHolder;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class SecurityContextIntegrationHandShakeHandler extends DefaultHandshakeHandler {

    @Override
    protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                       Map<String, Object> attributes) {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return principal instanceof SessionMemberHolder holder ? holder.getSessionMember() : null;
    }

}
