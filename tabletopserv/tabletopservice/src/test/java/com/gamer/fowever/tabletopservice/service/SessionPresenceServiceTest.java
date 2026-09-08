package com.gamer.fowever.tabletopservice.service;

import com.gamer.fowever.tabletopapi.EventType;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopapi.dto.SessionEventDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SessionPresenceServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final JsonMapper objectMapper = JsonMapper.builder().build();
    private SessionPresenceService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new SessionPresenceService(messagingTemplate, objectMapper);
        user = new User("aria", "Aria", "aria@example.com", LocalDate.of(1990, 1, 1), "hash");
        user.setId(1L);
        user.setEmailVerified(true);
    }

    @Test
    void joinedBroadcastsPresenceAndTracksConnection() {
        service.joined(5L, user, "simp-1");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/sessions/5"), captor.capture());
        SessionEventDto event = (SessionEventDto) captor.getValue();
        assertThat(event.type()).isEqualTo(EventType.PRESENCE);
        JsonNode payload = (JsonNode) event.payload();
        assertThat(payload.get("action").asText()).isEqualTo("joined");
        assertThat(payload.get("sender").get("username").asText()).isEqualTo("aria");
        assertThat(service.presentForTesting("simp-1")).containsKey(5L);
    }

    @Test
    void leftBroadcastsPresenceAndForgetsConnection() {
        service.joined(5L, user, "simp-1");
        org.mockito.Mockito.reset(messagingTemplate);

        service.left("simp-1");

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/sessions/5"), captor.capture());
        SessionEventDto event = (SessionEventDto) captor.getValue();
        assertThat(event.type()).isEqualTo(EventType.PRESENCE);
        JsonNode payload = (JsonNode) event.payload();
        assertThat(payload.get("action").asText()).isEqualTo("left");
        assertThat(service.presentForTesting("simp-1")).isNull();
    }

    @Test
    void leftForUnknownSessionDoesNothing() {
        service.left("never-seen");

        verifyNoInteractions(messagingTemplate);
    }
}