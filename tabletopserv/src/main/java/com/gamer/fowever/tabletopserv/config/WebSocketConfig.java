package com.gamer.fowever.tabletopserv.config;

import com.gamer.fowever.tabletopserv.security.StompAuthChannelInterceptor;
import com.gamer.fowever.tabletopserv.security.TokenHandshakeHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Arrays;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final TokenHandshakeHandler handshakeHandler;
    private final StompAuthChannelInterceptor authChannelInterceptor;
    private final String[] allowedOrigins;

    public WebSocketConfig(TokenHandshakeHandler handshakeHandler,
                           StompAuthChannelInterceptor authChannelInterceptor,
                           @Value("${tabletopserv.cors.allowed-origins}") String allowedOrigins) {
        this.handshakeHandler = handshakeHandler;
        this.authChannelInterceptor = authChannelInterceptor;
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .setHandshakeHandler(handshakeHandler);
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOrigins)
                .setHandshakeHandler(handshakeHandler)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.executor(new SyncTaskExecutor()).interceptors(authChannelInterceptor);
    }
}