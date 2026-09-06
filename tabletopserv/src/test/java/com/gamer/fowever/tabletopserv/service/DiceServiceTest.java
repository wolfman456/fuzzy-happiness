package com.gamer.fowever.tabletopserv.service;

import com.gamer.fowever.tabletopserv.domain.EventType;
import com.gamer.fowever.tabletopserv.domain.Game;
import com.gamer.fowever.tabletopserv.domain.GameSession;
import com.gamer.fowever.tabletopserv.domain.Participant;
import com.gamer.fowever.tabletopserv.domain.Role;
import com.gamer.fowever.tabletopserv.domain.SessionEvent;
import com.gamer.fowever.tabletopserv.domain.SessionStatus;
import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.RollRequest;
import com.gamer.fowever.tabletopserv.dto.SessionEventDto;
import com.gamer.fowever.tabletopserv.repository.GameSessionRepository;
import com.gamer.fowever.tabletopserv.repository.ParticipantRepository;
import com.gamer.fowever.tabletopserv.repository.SessionEventRepository;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import com.gamer.fowever.tabletopserv.support.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DiceServiceTest {

    private static final Long SESSION_ID = 1L;

    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private GameSessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionEventRepository eventRepository;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private final List<SessionEvent> savedEvents = new ArrayList<>();
    private final List<Participant> savedParticipants = new ArrayList<>();

    private DiceService service;

    @BeforeEach
    void setUp() {
        service = new DiceService(participantRepository, sessionRepository, userRepository,
                eventRepository, objectMapper);
        savedEvents.clear();
        savedParticipants.clear();
    }

    @Test
    void publicRollPersistsEventAndReturnsFullResult() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        savedParticipants.add(new Participant(session, gm, Role.GM));

        DiceService.DiceRoll roll = service.roll(gm, SESSION_ID,
                new RollRequest("2d6+3", "Perception check", false));

        verify(eventRepository).save(any(SessionEvent.class));
        assertThat(roll.isHidden()).isFalse();
        assertThat(roll.event()).isSameAs(roll.topicEvent());
        assertThat(roll.event().type()).isEqualTo(EventType.DICE);
        JsonNode payload = json(roll.event().payload());
        assertThat(payload.get("expression").asText()).isEqualTo("2d6+3");
        assertThat(payload.get("label").asText()).isEqualTo("Perception check");
        assertThat(payload.get("hidden").asBoolean()).isFalse();
        assertThat(payload.get("rolls")).hasSize(2);
        assertThat(payload.get("total").asInt()).isBetween(2 + 3, 12 + 3);
        assertThat(payload.get("rollId").asText()).isNotBlank();
        assertThat(payload.get("rolledBy").get("username").asText()).isEqualTo("aria");
    }

    @Test
    void privateRollBroadcastsHiddenFrameAndDeliversFullOnlyToGm() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        savedParticipants.add(new Participant(session, gm, Role.GM));

        DiceService.DiceRoll roll = service.roll(gm, SESSION_ID,
                new RollRequest("d20", "Stealth", true));

        verify(eventRepository, never()).save(any(SessionEvent.class));
        assertThat(roll.isHidden()).isTrue();
        JsonNode full = json(roll.event().payload());
        JsonNode topic = json(roll.topicEvent().payload());
        assertThat(full.get("hidden").asBoolean()).isTrue();
        assertThat(full.get("rolls")).isNotNull();
        assertThat(full.get("total").asInt()).isBetween(1, 20);
        assertThat(topic.get("hidden").asBoolean()).isTrue();
        assertThat(topic.has("rolls")).isFalse();
        assertThat(topic.has("total")).isFalse();
        assertThat(full.get("rollId").asText()).isEqualTo(topic.get("rollId").asText());
    }

    @Test
    void privateRollRequiresGm() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        savedParticipants.add(new Participant(session, gm, Role.GM));
        savedParticipants.add(new Participant(session, player, Role.PLAYER));
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> service.roll(player, SESSION_ID,
                new RollRequest("d20", null, true)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only the GM can roll in private");
    }

    @Test
    void publicRollAllowsPlayers() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        savedParticipants.add(new Participant(session, gm, Role.GM));
        savedParticipants.add(new Participant(session, player, Role.PLAYER));
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));

        DiceService.DiceRoll roll = service.roll(player, SESSION_ID, new RollRequest("d20", null, false));

        JsonNode payload = json(roll.event().payload());
        assertThat(payload.get("rolledBy").get("username").asText()).isEqualTo("ivo");
        assertThat(payload.get("hidden").asBoolean()).isFalse();
    }

    @Test
    void nonMemberCannotRoll() {
        User gm = user(1L, "aria");
        User stranger = user(9L, "zed");
        GameSession session = session(gm);
        coreStubs(gm, session);
        savedParticipants.add(new Participant(session, gm, Role.GM));
        when(userRepository.findById(9L)).thenReturn(Optional.of(stranger));

        assertThatThrownBy(() -> service.roll(stranger, SESSION_ID, new RollRequest("d20", null, false)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not a participant");
    }

    @Test
    void invalidExpressionIsRejected() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        savedParticipants.add(new Participant(session, gm, Role.GM));

        assertThatThrownBy(() -> service.roll(gm, SESSION_ID, new RollRequest("ninja", null, false)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unsupported dice expression");
    }

    private JsonNode json(Object payload) {
        return (JsonNode) payload;
    }

    private User user(Long id, String username) {
        User user = new User(username, "Display " + username, username + "@example.com",
                LocalDate.of(1990, 1, 1), "hash");
        user.setId(id);
        user.setEmailVerified(true);
        return user;
    }

    private GameSession session(User creator) {
        GameSession session = new GameSession();
        session.setId(SESSION_ID);
        session.setName("Grumm's Revenge");
        session.setInviteCode("ABC234");
        session.setGame(new Game("dnd-5e", "D&D 5e", "{}"));
        session.setCreatedBy(creator);
        session.setStatus(SessionStatus.OPEN);
        return session;
    }

    private void coreStubs(User gm, GameSession session) {
        when(userRepository.findById(gm.getId())).thenReturn(Optional.of(gm));
        when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        when(participantRepository.existsBySessionIdAndUserId(anyLong(), anyLong()))
                .thenAnswer(invocation -> savedParticipants.stream()
                        .anyMatch(p -> p.getUser().getId().equals(invocation.getArgument(1))));
        when(participantRepository.findBySessionIdAndUserId(anyLong(), anyLong()))
                .thenAnswer(invocation -> savedParticipants.stream()
                        .filter(p -> p.getUser().getId().equals(invocation.getArgument(1)))
                        .findFirst());
        when(eventRepository.save(any(SessionEvent.class))).thenAnswer(invocation -> {
            SessionEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId((long) savedEvents.size() + 1);
            }
            savedEvents.add(event);
            return event;
        });
    }
}