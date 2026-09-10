package com.gamer.fowever.tabletopfunctionaltest.dice;

import com.gamer.fowever.tabletopapi.EventType;
import com.gamer.fowever.tabletopapi.dto.SessionEventDto;
import com.gamer.fowever.tabletopfunctionaltest.FunctionalTestBase;
import com.gamer.fowever.tabletopfunctionaltest.support.Api;
import com.gamer.fowever.tabletopfunctionaltest.support.FunctionalStomp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import tools.jackson.databind.JsonNode;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Dice journeys through the real HTTP + STOMP stack: public rolls return the full
 * result, private rolls hide values on the shared topic and deliver the full roll to
 * the GM's user queue.
 */
class DiceJourneyIT extends FunctionalTestBase {

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
    void publicRollReturnsFullResultAndPrivateRollIsGmOnly() throws Exception {
        String gmJwt = registerVerifyLogin("d1gm");
        String playerJwt = registerVerifyLogin("d1pip");
        SessionHandle session = createSession(gmJwt, "Dice Room");
        joinSession(playerJwt, session.inviteCode());

        JsonNode publicRoll = roll(playerJwt, session.id(), "2d6+3", "Perception", false);
        assertThat(publicRoll.get("type").asText()).isEqualTo("DICE");
        assertThat(publicRoll.at("/payload/expression").asText()).isEqualTo("2d6+3");
        assertThat(publicRoll.at("/payload/hidden").asBoolean()).isFalse();
        assertThat(publicRoll.at("/payload/rolls").size()).isEqualTo(2);
        assertThat(publicRoll.at("/payload/total").isNumber()).isTrue();
        assertThat(publicRoll.at("/payload/rolledBy/username").asText()).isEqualTo("d1pip");
        assertThat(publicRoll.get("id").isNumber()).isTrue();

        String outsiderJwt = registerVerifyLogin("d1out");
        Api.Response stranger = Api.post(baseUrl() + "/api/sessions/" + session.id() + "/roll", outsiderJwt,
                Api.body(Map.of("expression", "d20", "privateRoll", true)));
        assertThat(stranger.status()).isEqualTo(403);

        JsonNode privateRoll = roll(gmJwt, session.id(), "d20", "Stealth", true);
        assertThat(privateRoll.at("/payload/hidden").asBoolean()).isTrue();
        assertThat(privateRoll.at("/id").isNull()).isTrue();
    }

    @Test
    void privateRollHidesValuesFromOtherClientsAndDeliversFullResultToGmQueue() throws Exception {
        String gmJwt = registerVerifyLogin("d2gm");
        String playerJwt = registerVerifyLogin("d2pip");
        SessionHandle session = createSession(gmJwt, "Secret Dice");
        joinSession(playerJwt, session.inviteCode());

        WebSocketStompClient client = FunctionalStomp.client();
        StompSession gm = connect(client, gmJwt);
        StompSession player = connect(client, playerJwt);

        Map<String, Map<String, Object>> gmHiddenByRoll = new ConcurrentHashMap<>();
        Map<String, Map<String, Object>> playerHiddenByRoll = new ConcurrentHashMap<>();
        Map<String, Map<String, Object>> gmFullByRoll = new ConcurrentHashMap<>();
        CountDownLatch gmHiddenLatch = new CountDownLatch(1);
        CountDownLatch playerHiddenLatch = new CountDownLatch(1);
        CountDownLatch gmFullLatch = new CountDownLatch(1);

        gm.subscribe("/topic/sessions/" + session.id(), diceTopicHandler(gmHiddenByRoll, gmHiddenLatch));
        player.subscribe("/topic/sessions/" + session.id(), diceTopicHandler(playerHiddenByRoll, playerHiddenLatch));
        gm.subscribe("/user/queue/dice", topicAwareFrameHandler((headers, payload) -> {
            Map<String, Object> dice = (Map<String, Object>) ((SessionEventDto) payload).payload();
            gmFullByRoll.put((String) dice.get("rollId"), dice);
            gmFullLatch.countDown();
        }));
        gm.subscribe("/app/sessions/" + session.id(), topicAwareFrameHandler((headers, payload) -> {
        }));

        Map<String, Object> full = null;
        Map<String, Object> hidden = null;
        Map<String, Object> playerHidden = null;
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (full == null && System.currentTimeMillis() < deadline) {
            roll(gmJwt, session.id(), "d100", null, true);
            if (gmHiddenLatch.await(1, TimeUnit.SECONDS)) {
                hidden = gmHiddenByRoll.values().stream().findFirst().orElse(null);
            }
            playerHiddenLatch.await(1, TimeUnit.SECONDS);
            if (gmFullLatch.await(1, TimeUnit.SECONDS)) {
                full = gmFullByRoll.values().stream().findFirst().orElse(null);
            }
            for (Map.Entry<String, Map<String, Object>> entry : gmHiddenByRoll.entrySet()) {
                Map<String, Object> gmFullOfRoll = gmFullByRoll.get(entry.getKey());
                Map<String, Object> playerHiddenOfRoll = playerHiddenByRoll.get(entry.getKey());
                if (gmFullOfRoll != null && playerHiddenOfRoll != null) {
                    hidden = entry.getValue();
                    playerHidden = playerHiddenOfRoll;
                    full = gmFullOfRoll;
                    break;
                }
            }
        }

        assertThat(full).as("private roll delivered to the GM user queue").isNotNull();
        assertThat(hidden).as("GM topic received the hidden roll").isNotNull();
        assertThat(hidden.get("hidden")).isEqualTo(Boolean.TRUE);
        assertThat(hidden).doesNotContainKey("rolls");
        assertThat(hidden).doesNotContainKey("total");
        assertThat(playerHidden).as("player topic received the hidden roll").isNotNull();
        assertThat(playerHidden.get("rollId")).isEqualTo(hidden.get("rollId"));
        assertThat(full.get("hidden")).isEqualTo(Boolean.TRUE);
        assertThat(full).containsKey("rolls");
        assertThat(full).containsKey("total");
        assertThat(full.get("rollId")).isEqualTo(hidden.get("rollId"));
        assertThat(((java.util.List<?>) full.get("rolls"))).hasSize(1);
        assertThat((Integer) full.get("total")).isBetween(1, 100);
    }

    @Test
    void rollRejectsNonMembersAndBadExpressions() throws Exception {
        String gmJwt = registerVerifyLogin("d3gm");
        String playerJwt = registerVerifyLogin("d3pip");
        SessionHandle session = createSession(gmJwt, "Strict Table");
        joinSession(playerJwt, session.inviteCode());

        Api.Response unauthenticated = Api.post(baseUrl() + "/api/sessions/" + session.id() + "/roll", null,
                Api.body(Map.of("expression", "d20")));
        assertThat(unauthenticated.status()).isEqualTo(401);

        Api.Response badExpression = Api.post(baseUrl() + "/api/sessions/" + session.id() + "/roll", playerJwt,
                Api.body(Map.of("expression", "ninja")));
        assertThat(badExpression.status()).isEqualTo(400);
    }

    private JsonNode roll(String jwt, long sessionId, String expression, String label, Boolean privateRoll) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("expression", expression);
        if (label != null) {
            payload.put("label", label);
        }
        if (privateRoll != null) {
            payload.put("privateRoll", privateRoll);
        }
        Api.Response response = Api.post(baseUrl() + "/api/sessions/" + sessionId + "/roll", jwt, Api.body(payload));
        Api.requireStatus(response, 200, "roll " + expression);
        return Api.json(response.body());
    }

    private StompSession connect(WebSocketStompClient client, String jwt) throws Exception {
        StompSession session = FunctionalStomp.connect(client, baseUrl(), jwt);
        openSessions.add(session);
        return session;
    }

    private static StompFrameHandler diceTopicHandler(
            Map<String, Map<String, Object>> captureByRoll, CountDownLatch latch) {
        return topicAwareFrameHandler((headers, payload) -> {
            SessionEventDto event = (SessionEventDto) payload;
            if (event.type() == EventType.DICE) {
                Map<String, Object> dice = (Map<String, Object>) event.payload();
                captureByRoll.put((String) dice.get("rollId"), dice);
                latch.countDown();
            }
        });
    }

    private static StompFrameHandler topicAwareFrameHandler(FunctionalStomp.FrameConsumer consumer) {
        return new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return SessionEventDto.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                consumer.accept(headers, payload);
            }
        };
    }
}