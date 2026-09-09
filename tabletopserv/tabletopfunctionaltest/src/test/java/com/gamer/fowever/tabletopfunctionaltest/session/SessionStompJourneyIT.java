package com.gamer.fowever.tabletopfunctionaltest.session;

import com.gamer.fowever.tabletopapi.EventType;
import com.gamer.fowever.tabletopapi.dto.ChatMessage;
import com.gamer.fowever.tabletopapi.dto.SessionEventDto;
import com.gamer.fowever.tabletopapi.dto.SessionSummary;
import com.gamer.fowever.tabletopfunctionaltest.FunctionalTestBase;
import com.gamer.fowever.tabletopfunctionaltest.support.FunctionalStomp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live STOMP journeys: a real WebSocket connection to the packaged backend, verifying
 * the session snapshot subscription, live chat broadcast, presence, and the
 * participant gate on subscriptions.
 */
class SessionStompJourneyIT extends FunctionalTestBase {

    private final java.util.ArrayList<StompSession> openSessions = new java.util.ArrayList<>();

    @AfterEach
    void tearDown() {
        openSessions.forEach(session -> {
            try {
                session.disconnect();
            } catch (RuntimeException ignored) {
                // already closed
            }
        });
        openSessions.clear();
    }

    @Test
    void subscriberReceivesSnapshotThenLiveChat() throws Exception {
        String gmJwt = registerVerifyLogin("s1gm");
        String playerJwt = registerVerifyLogin("s1pip");
        SessionHandle created = createSession(gmJwt, "Grumm's Tower");
        joinSession(playerJwt, created.inviteCode());

        WebSocketStompClient client = FunctionalStomp.client();
        StompSession player = connect(client, playerJwt);

        CountDownLatch snapshotLatch = new CountDownLatch(1);
        CountDownLatch chatLatch = new CountDownLatch(1);
        AtomicReference<String> snapshotName = new AtomicReference<>();
        AtomicReference<String> chatText = new AtomicReference<>();
        AtomicReference<EventType> chatType = new AtomicReference<>();

        player.subscribe("/topic/sessions/" + created.id(), FunctionalStomp.frameHandler(SessionEventDto.class,
                (headers, payload) -> {
                    SessionEventDto event = (SessionEventDto) payload;
                    if (event.type() == EventType.CHAT) {
                        chatType.set(event.type());
                        chatText.set(((Map<String, Object>) event.payload()).get("text").toString());
                        chatLatch.countDown();
                    }
                }));
        player.subscribe("/app/sessions/" + created.id(), FunctionalStomp.frameHandler(SessionSummary.class,
                (headers, payload) -> {
                    snapshotName.set(((SessionSummary) payload).name());
                    snapshotLatch.countDown();
                }));

        player.send("/app/sessions/" + created.id() + "/chat", new ChatMessage("Hello table!"));

        assertThat(snapshotLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(snapshotName.get()).isEqualTo("Grumm's Tower");
        assertThat(chatLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(chatType.get()).isEqualTo(EventType.CHAT);
        assertThat(chatText.get()).isEqualTo("Hello table!");
    }

    @Test
    void nonMemberSubscriptionIsRejected() throws Exception {
        String gmJwt = registerVerifyLogin("s2gm");
        SessionHandle created = createSession(gmJwt, "Private Room");
        String outsiderJwt = registerVerifyLogin("s2out");

        WebSocketStompClient client = FunctionalStomp.client();
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        StompSession outsider = client.connectAsync(FunctionalStomp.wsUrl(baseUrl(), outsiderJwt),
                        new StompSessionHandlerAdapter() {
                            @Override
                            public void handleFrame(StompHeaders headers, Object payload) {
                                String message = headers.getFirst("message");
                                if (message != null && message.contains("not a participant")) {
                                    errorMessage.set(message);
                                    errorLatch.countDown();
                                }
                            }

                            @Override
                            public void handleException(StompSession session, StompCommand command,
                                                        StompHeaders headers, byte[] payload, Throwable exception) {
                                if (exception != null && exception.getMessage() != null
                                        && exception.getMessage().contains("not a participant")) {
                                    errorMessage.set(exception.getMessage());
                                    errorLatch.countDown();
                                }
                            }
                        })
                .get(10, TimeUnit.SECONDS);
        openSessions.add(outsider);

        outsider.subscribe("/topic/sessions/" + created.id(), new StompFrameHandler() {
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

    @Test
    void snapshotListsRestJoinedParticipants() throws Exception {
        String gmJwt = registerVerifyLogin("s3gm");
        String playerJwt = registerVerifyLogin("s3pip");
        SessionHandle created = createSession(gmJwt, "Shared Table");
        joinSession(playerJwt, created.inviteCode());

        WebSocketStompClient client = FunctionalStomp.client();
        StompSession gm = connect(client, gmJwt);

        CountDownLatch snapshotLatch = new CountDownLatch(1);
        AtomicReference<SessionSummary> received = new AtomicReference<>();
        gm.subscribe("/app/sessions/" + created.id(), FunctionalStomp.frameHandler(SessionSummary.class,
                (headers, payload) -> {
                    received.set((SessionSummary) payload);
                    snapshotLatch.countDown();
                }));

        assertThat(snapshotLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(received.get().name()).isEqualTo("Shared Table");
        assertThat(received.get().participants()).hasSize(2);
        assertThat(received.get().recentEvents().stream()
                .map(SessionEventDto::type).toList()).contains(EventType.PRESENCE);
    }

    private StompSession connect(WebSocketStompClient client, String jwt) throws Exception {
        StompSession session = FunctionalStomp.connect(client, baseUrl(), jwt);
        openSessions.add(session);
        return session;
    }
}