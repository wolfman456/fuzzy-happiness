package com.gamer.fowever.tabletopfunctionaltest.support;

import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.TimeUnit;

/**
 * STOMP plumbing shared by functional journeys: a real WebSocket client mirroring the
 * configuration used by the service's own unit tests (Jackson message converter).
 */
public final class FunctionalStomp {

    private FunctionalStomp() {
    }

    public static WebSocketStompClient client() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        client.setInboundMessageSizeLimit(64 * 1024);
        return client;
    }

    public static String wsUrl(String baseUrl, String jwt) {
        return baseUrl.replaceFirst("^http", "ws") + "/ws?token=" + jwt;
    }

    public static StompSession connect(WebSocketStompClient client, String baseUrl, String jwt) throws Exception {
        return client.connectAsync(wsUrl(baseUrl, jwt), new StompSessionHandlerAdapter() {
        }).get(10, TimeUnit.SECONDS);
    }

    public static StompFrameHandler frameHandler(Class<?> payloadType, FrameConsumer consumer) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return payloadType;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                consumer.accept(headers, payload);
            }
        };
    }

    @FunctionalInterface
    public interface FrameConsumer {
        void accept(StompHeaders headers, Object payload);
    }
}