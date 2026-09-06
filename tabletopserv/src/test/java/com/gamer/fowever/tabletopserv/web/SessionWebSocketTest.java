package com.gamer.fowever.tabletopserv.web;

import com.gamer.fowever.tabletopserv.domain.EventType;
import com.gamer.fowever.tabletopserv.domain.Game;
import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.ChatMessage;
import com.gamer.fowever.tabletopserv.dto.SessionEventDto;
import com.gamer.fowever.tabletopserv.dto.SessionSummary;
import com.gamer.fowever.tabletopserv.repository.GameRepository;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import com.gamer.fowever.tabletopserv.security.JwtService;
import com.gamer.fowever.tabletopserv.service.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SessionWebSocketTest {

    @LocalServerPort
    private int port;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private SessionService sessionService;

    private final java.util.ArrayList<StompSession> openSessions = new java.util.ArrayList<>();

    @AfterEach
    void tearDown() {
        openSessions.forEach(session -> {
            try {
                session.disconnect();
            } catch (RuntimeException ignored) {
                // connection already closed
            }
        });
        openSessions.clear();
    }

    @Test
    void subscriberReceivesSnapshotThenLiveChat() throws Exception {
        User gm = createUser("wallygm");
        User player = createUser("pip");
        sessionService.createSession(gm, "The Fabled Tower", "dnd-5e");
        var created = sessionService.createSession(gm, "Grumm's Tower", "dnd-5e");
        sessionService.joinSession(player, created.inviteCode());

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        client.setInboundMessageSizeLimit(64 * 1024);
        StompSession session = connect(client, player);

        CountDownLatch snapshotLatch = new CountDownLatch(1);
        CountDownLatch chatLatch = new CountDownLatch(1);
        AtomicReference<String> snapshotName = new AtomicReference<>();
        AtomicReference<String> chatText = new AtomicReference<>();
        AtomicReference<EventType> chatType = new AtomicReference<>();

        session.subscribe("/topic/sessions/" + created.id(), frameHandler(SessionEventDto.class, (headers, payload) -> {
            SessionEventDto event = (SessionEventDto) payload;
            if (event.type() == EventType.CHAT) {
                chatType.set(event.type());
                chatText.set(((Map<String, Object>) event.payload()).get("text").toString());
                chatLatch.countDown();
            }
        }));
        session.subscribe("/app/sessions/" + created.id(), frameHandler(SessionSummary.class, (headers, payload) -> {
            snapshotName.set(((SessionSummary) payload).name());
            snapshotLatch.countDown();
        }));

        session.send("/app/sessions/" + created.id() + "/chat", new ChatMessage("Hello table!"));

        assertThat(snapshotLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(snapshotName.get()).isEqualTo("Grumm's Tower");
        assertThat(chatLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(chatType.get()).isEqualTo(EventType.CHAT);
        assertThat(chatText.get()).isEqualTo("Hello table!");
    }

    @Test
    void snapshotListsParticipantsIncludingThoseWhoJoinedViaRest() throws Exception {
        User gm = createUser("duadgm");
        User player = createUser("tibo");
        var created = sessionService.createSession(gm, "Shared Table", "dnd-5e");
        sessionService.joinSession(player, created.inviteCode());

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompSession session = connect(client, gm);

        CountDownLatch snapshotLatch = new CountDownLatch(1);
        AtomicReference<SessionSummary> received = new AtomicReference<>();
        session.subscribe("/app/sessions/" + created.id(), frameHandler(SessionSummary.class, (headers, payload) -> {
            received.set((SessionSummary) payload);
            snapshotLatch.countDown();
        }));

        assertThat(snapshotLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(received.get().name()).isEqualTo("Shared Table");
        assertThat(received.get().participants()).hasSize(2);
    }

    @Test
    void nonMemberSubscriptionIsRejected() throws Exception {
        User gm = createUser("gaurdgm");
        var created = sessionService.createSession(gm, "Private Room", "dnd-5e");
        User outsider = createUser("eave");

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());

        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        StompSession session = client.connectAsync(stompUrl(outsider), new StompSessionHandlerAdapter() {
            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                String message = firstNativeHeader(headers, "message");
                if (message != null && message.contains("not a participant")) {
                    errorMessage.set(message);
                    errorLatch.countDown();
                }
            }

            @Override
            public void handleException(StompSession sess, StompCommand command, StompHeaders headers,
                                        byte[] payload, Throwable exception) {
                if (exception != null && exception.getMessage() != null
                        && exception.getMessage().contains("not a participant")) {
                    errorMessage.set(exception.getMessage());
                    errorLatch.countDown();
                }
            }
        }).get(10, TimeUnit.SECONDS);
        openSessions.add(session);

        session.subscribe("/topic/sessions/" + created.id(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return byte[].class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
            }
        });

        assertThat(errorLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(errorMessage.get()).contains("not a participant");
    }

    private StompSession connect(WebSocketStompClient client, User user) throws Exception {
        StompSession session = client.connectAsync(stompUrl(user), new StompSessionHandlerAdapter() {}).get(10, TimeUnit.SECONDS);
        openSessions.add(session);
        return session;
    }

    private String firstNativeHeader(StompHeaders headers, String name) {
        String value = headers.getFirst(name);
        return value;
    }

    private String stompUrl(User user) {
        return "ws://localhost:" + port + "/ws?token=" + jwtService.generateToken(user);
    }

    private StompFrameHandler frameHandler(Class<?> payloadType, FrameConsumer consumer) {
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

    private User createUser(String username) {
        User user = new User(username, "Display " + username, username + "@example.com",
                LocalDate.of(1990, 1, 1), "hash");
        user.setEmailVerified(true);
        userRepository.save(user);
        gameRepository.findBySlug("dnd-5e").orElseGet(() ->
                gameRepository.save(new Game("dnd-5e", "D&D 5e", "{}")));
        return user;
    }

    @FunctionalInterface
    private interface FrameConsumer {
        void accept(StompHeaders headers, Object payload);
    }
}