package com.gamer.fowever.tabletopservice.web;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.gamer.fowever.tabletopapi.EventType;
import com.gamer.fowever.tabletopservice.domain.Game;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopapi.dto.SessionEventDto;
import com.gamer.fowever.tabletopservice.repository.GameRepository;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import com.gamer.fowever.tabletopservice.security.JwtService;
import com.gamer.fowever.tabletopservice.service.SessionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class DiceFlowTest {

    @LocalServerPort
    private int port;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private ObjectMapper objectMapper;

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
    void rollEndpointIsRestrictedToMembers() throws Exception {
        User gm = createUser("rollgm");
        var created = sessionService.createSession(gm, "Dice Room", "dnd-5e");
        User player = createUser("rollpip");
        sessionService.joinSession(player, created.inviteCode());

        mvc.perform(post("/api/sessions/" + created.id() + "/roll")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"d20\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(post("/api/sessions/" + created.id() + "/roll")
                        .header("Authorization", "Bearer " + jwtService.generateToken(player))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"ninja\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void publicRollReturnsFullResultAndPrivateRollIsGmOnly() throws Exception {
        User gm = createUser("pubgm");
        var created = sessionService.createSession(gm, "Dice Room", "dnd-5e");
        User player = createUser("pubpip");
        sessionService.joinSession(player, created.inviteCode());
        String gmToken = jwtService.generateToken(gm);
        String playerToken = jwtService.generateToken(player);

        String publicBody = mvc.perform(post("/api/sessions/" + created.id() + "/roll")
                        .header("Authorization", "Bearer " + playerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"2d6+3\",\"label\":\"Perception\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DICE"))
                .andExpect(jsonPath("$.payload.expression").value("2d6+3"))
                .andExpect(jsonPath("$.payload.hidden").value(false))
                .andExpect(jsonPath("$.payload.total").isNumber())
                .andReturn().getResponse().getContentAsString();
        JsonNode publicRoll = objectMapper.readTree(publicBody);
        assertThat(publicRoll.at("/payload/rolls").size()).isEqualTo(2);
        assertThat(publicRoll.at("/payload/rolledBy/username").asText()).isEqualTo("pubpip");
        assertThat(publicRoll.at("/id").isNumber()).isTrue();

        User stranger = createUser("onlooker");
        mvc.perform(post("/api/sessions/" + created.id() + "/roll")
                        .header("Authorization", "Bearer " + jwtService.generateToken(stranger))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"d20\",\"privateRoll\":true}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not a participant")));

        String privateBody = mvc.perform(post("/api/sessions/" + created.id() + "/roll")
                        .header("Authorization", "Bearer " + gmToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expression\":\"d20\",\"label\":\"Stealth\",\"privateRoll\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload.hidden").value(true))
                .andExpect(jsonPath("$.payload.rolls").isArray())
                .andExpect(jsonPath("$.payload.total").isNumber())
                .andReturn().getResponse().getContentAsString();
        JsonNode privateRoll = objectMapper.readTree(privateBody);
        assertThat(privateRoll.at("/payload/rolledBy/username").asText()).isEqualTo("pubgm");
        assertThat(privateRoll.at("/id").isNull()).isTrue();
    }

    @Test
    void privateRollHidesValuesFromOtherClientsAndDeliversFullResultToGmQueue() throws Exception {
        User gm = createUser("secgm");
        var created = sessionService.createSession(gm, "Secret Dice", "dnd-5e");
        User player = createUser("secpip");
        sessionService.joinSession(player, created.inviteCode());

        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        client.setInboundMessageSizeLimit(64 * 1024);

        StompSession gmSession = connect(client, gm);
        StompSession playerSession = connect(client, player);

        CountDownLatch gmHiddenLatch = new CountDownLatch(1);
        CountDownLatch gmFullLatch = new CountDownLatch(1);
        CountDownLatch playerHiddenLatch = new CountDownLatch(1);
        AtomicReference<Map<String, Object>> gmHiddenTopic = new AtomicReference<>();
        AtomicReference<Map<String, Object>> playerHiddenTopic = new AtomicReference<>();
        AtomicReference<Map<String, Object>> gmFullQueue = new AtomicReference<>();

        gmSession.subscribe("/topic/sessions/" + created.id(), frameHandler((headers, payload) -> {
            SessionEventDto event = (SessionEventDto) payload;
            if (event.type() == EventType.DICE) {
                gmHiddenTopic.set((Map<String, Object>) event.payload());
                gmHiddenLatch.countDown();
            }
        }));
        gmSession.subscribe("/user/queue/dice", frameHandler((headers, payload) -> {
            gmFullQueue.set((Map<String, Object>) ((SessionEventDto) payload).payload());
            gmFullLatch.countDown();
        }));
        playerSession.subscribe("/topic/sessions/" + created.id(), frameHandler((headers, payload) -> {
            SessionEventDto event = (SessionEventDto) payload;
            if (event.type() == EventType.DICE) {
                playerHiddenTopic.set((Map<String, Object>) event.payload());
                playerHiddenLatch.countDown();
            }
        }));
        gmSession.subscribe("/app/sessions/" + created.id(), frameHandler((headers, payload) -> {
        }));

        Map<String, Object> hidden = null;
        Map<String, Object> full = null;
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(10);
        while (full == null && System.currentTimeMillis() < deadline) {
            mvc.perform(post("/api/sessions/" + created.id() + "/roll")
                            .header("Authorization", "Bearer " + jwtService.generateToken(gm))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"expression\":\"d100\",\"privateRoll\":true}"))
                    .andExpect(status().isOk());
            if (gmHiddenLatch.await(1, TimeUnit.SECONDS)) {
                hidden = gmHiddenTopic.get();
            }
            playerHiddenLatch.await(1, TimeUnit.SECONDS);
            if (gmFullLatch.await(1, TimeUnit.SECONDS)) {
                full = gmFullQueue.get();
            }
        }

        assertThat(full).as("private roll delivered to the GM user queue").isNotNull();
        assertThat(hidden.get("hidden")).isEqualTo(Boolean.TRUE);
        assertThat(hidden).doesNotContainKey("rolls");
        assertThat(hidden).doesNotContainKey("total");
        assertThat(playerHiddenTopic.get().get("rollId")).isEqualTo(hidden.get("rollId"));
        assertThat(full.get("hidden")).isEqualTo(Boolean.TRUE);
        assertThat(full).containsKey("rolls");
        assertThat(full).containsKey("total");
        assertThat(full.get("rollId")).isEqualTo(hidden.get("rollId"));
        assertThat(((java.util.List<?>) full.get("rolls"))).hasSize(1);
        assertThat((Integer) full.get("total")).isBetween(1, 100);
    }

    private StompSession connect(WebSocketStompClient client, User user) throws Exception {
        StompSession session = client
                .connectAsync("ws://localhost:" + port + "/ws?token=" + jwtService.generateToken(user),
                        new StompSessionHandlerAdapter() {
                        })
                .get(10, TimeUnit.SECONDS);
        openSessions.add(session);
        return session;
    }

    private StompFrameHandler frameHandler(FrameConsumer consumer) {
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