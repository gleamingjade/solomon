package com.example.solomon.feature.chat.adapter.websocket.config;

import com.example.solomon.feature.chat.adapter.in.websocket.AuthenticationRequiredHandshakeInterceptor;
import com.example.solomon.feature.chat.adapter.in.websocket.ChatWebSocketSessionTracker;
import com.example.solomon.feature.chat.adapter.in.websocket.SecurityContextIntegrationHandShakeHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${SERVER_ID}")
    private String serverId;

    @Value("${FRONT_END_ORIGIN}")
    private String frontEndOrigin;

    private final SecurityContextIntegrationHandShakeHandler handshakeHandler;

    private final AuthenticationRequiredHandshakeInterceptor authenticationRequiredHandshakeInterceptor;

    private final ChatWebSocketSessionTracker chatWebSocketSessionTracker;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.enableSimpleBroker("/topic", "/queue");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-" + serverId)
                .setAllowedOriginPatterns(frontEndOrigin)
                .setHandshakeHandler(handshakeHandler)
                .addInterceptors(authenticationRequiredHandshakeInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.addDecoratorFactory(chatWebSocketSessionTracker);
    }

}
