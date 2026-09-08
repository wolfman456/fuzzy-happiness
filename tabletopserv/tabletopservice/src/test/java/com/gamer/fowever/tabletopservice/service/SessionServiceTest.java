package com.gamer.fowever.tabletopservice.service;

import com.gamer.fowever.tabletopapi.EventType;
import com.gamer.fowever.tabletopservice.domain.Game;
import com.gamer.fowever.tabletopservice.domain.GameSession;
import com.gamer.fowever.tabletopservice.domain.Participant;
import com.gamer.fowever.tabletopapi.Role;
import com.gamer.fowever.tabletopservice.domain.SessionEvent;
import com.gamer.fowever.tabletopapi.SessionStatus;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopapi.dto.SessionEventDto;
import com.gamer.fowever.tabletopapi.dto.SessionSummary;
import com.gamer.fowever.tabletopservice.repository.GameRepository;
import com.gamer.fowever.tabletopservice.repository.GameSessionRepository;
import com.gamer.fowever.tabletopservice.repository.ParticipantRepository;
import com.gamer.fowever.tabletopservice.repository.SessionEventRepository;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import com.gamer.fowever.tabletopapi.support.ApiException;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceTest {

    private static final Long SESSION_ID = 1L;
    private static final String INVITE = "ABC234";

    @Mock
    private GameRepository gameRepository;
    @Mock
    private GameSessionRepository sessionRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private SessionEventRepository eventRepository;
    @Mock
    private UserRepository userRepository;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private final List<Participant> savedParticipants = new ArrayList<>();
    private final List<SessionEvent> savedEvents = new ArrayList<>();

    private SessionService service;

    @BeforeEach
    void setUp() {
        service = new SessionService(gameRepository, sessionRepository, participantRepository,
                eventRepository, userRepository, objectMapper);
        savedParticipants.clear();
        savedEvents.clear();
    }

    private User user(Long id, String username) {
        User user = new User(username, "Display " + username, username + "@example.com",
                LocalDate.of(1990, 1, 1), "hash");
        user.setId(id);
        user.setEmailVerified(true);
        return user;
    }

    private Game game() {
        return new Game("dnd-5e", "D&D 5e", "{}");
    }

    private GameSession session(User creator) {
        GameSession session = new GameSession();
        session.setId(SESSION_ID);
        session.setName("Grumm's Revenge");
        session.setInviteCode(INVITE);
        session.setGame(game());
        session.setCreatedBy(creator);
        session.setStatus(SessionStatus.OPEN);
        return session;
    }

    private void coreStubs(User creator, GameSession session) {
        when(userRepository.findById(creator.getId())).thenReturn(Optional.of(creator));
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(gameRepository.findBySlug("dnd-5e")).thenReturn(Optional.of(session.getGame()));
        when(sessionRepository.existsByInviteCode(anyString())).thenReturn(false);
        when(sessionRepository.save(any(GameSession.class))).thenAnswer(invocation -> {
            GameSession saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(SESSION_ID);
            }
            return saved;
        });
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> {
            Participant participant = invocation.getArgument(0);
            if (participant.getId() == null) {
                participant.setId((long) savedParticipants.size() + 1);
            }
            savedParticipants.add(participant);
            return participant;
        });
        when(eventRepository.save(any(SessionEvent.class))).thenAnswer(invocation -> {
            SessionEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId((long) savedEvents.size() + 1);
            }
            savedEvents.add(event);
            return event;
        });
        when(participantRepository.findBySessionId(SESSION_ID))
                .thenAnswer(invocation -> new ArrayList<>(savedParticipants));
        when(participantRepository.findBySessionIdAndUserId(eq(SESSION_ID), anyLong()))
                .thenAnswer(invocation -> savedParticipants.stream()
                        .filter(p -> p.getUser().getId().equals(invocation.getArgument(1)))
                        .findFirst());
        when(participantRepository.existsBySessionIdAndUserId(eq(SESSION_ID), anyLong()))
                .thenAnswer(invocation -> savedParticipants.stream()
                        .anyMatch(p -> p.getUser().getId().equals(invocation.getArgument(1))));
        when(eventRepository.findTop50BySessionIdOrderByIdDesc(SESSION_ID))
                .thenAnswer(invocation -> new ArrayList<>(savedEvents));
    }

    @Test
    void createSessionPromotesCreatorToGmAndRecordsPresence() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        coreStubs(creator, session);

        SessionSummary summary = service.createSession(creator, "  Grumm's Revenge  ", "dnd-5e");

        assertThat(summary.id()).isEqualTo(SESSION_ID);
        assertThat(summary.name()).isEqualTo("Grumm's Revenge");
        assertThat(summary.inviteCode()).matches("[A-HJ-NP-Z2-9]{6}");
        assertThat(summary.status()).isEqualTo(SessionStatus.OPEN);
        assertThat(summary.gameSlug()).isEqualTo("dnd-5e");
        assertThat(summary.createdBy().username()).isEqualTo("aria");
        assertThat(summary.participants()).hasSize(1);
        assertThat(summary.participants().getFirst().role()).isEqualTo(Role.GM);
        assertThat(summary.participants().getFirst().user().username()).isEqualTo("aria");
        assertThat(summary.recentEvents()).extracting(SessionEventDto::type).containsExactly(EventType.PRESENCE);
        verify(sessionRepository).save(any());
        verify(participantRepository).save(any());
    }

    @Test
    void createSessionRejectsUnknownGame() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(gameRepository.findBySlug("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSession(creator, "Room", "nope"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown game");
        verify(sessionRepository, never()).save(any());
    }

    @Test
    void createSessionRetriesInviteCodeOnCollision() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(gameRepository.findBySlug("dnd-5e")).thenReturn(Optional.of(session.getGame()));
        when(sessionRepository.existsByInviteCode(anyString())).thenReturn(true, false);
        when(sessionRepository.save(any(GameSession.class))).thenAnswer(invocation -> {
            GameSession saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(SESSION_ID);
            }
            return saved;
        });
        when(participantRepository.save(any(Participant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepository.save(any(SessionEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.findBySessionId(SESSION_ID)).thenReturn(List.of());
        when(eventRepository.findTop50BySessionIdOrderByIdDesc(SESSION_ID)).thenReturn(List.of());
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        SessionSummary summary = service.createSession(creator, "Room", "dnd-5e");

        verify(sessionRepository, times(2)).existsByInviteCode(anyString());
        assertThat(summary.inviteCode()).matches("[A-HJ-NP-Z2-9]{6}");
    }

    @Test
    void createSessionFailsWhenInviteCodesExhausted() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(gameRepository.findBySlug("dnd-5e")).thenReturn(Optional.of(session.getGame()));
        when(sessionRepository.existsByInviteCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> service.createSession(creator, "Room", "dnd-5e"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void joinSessionAddsPlayerAndRecordsPresence() {
        User creator = user(1L, "aria");
        User ivo = user(2L, "ivo");
        GameSession session = session(creator);
        coreStubs(creator, session);
        when(sessionRepository.findByInviteCode(INVITE)).thenReturn(Optional.of(session));
        when(userRepository.findById(2L)).thenReturn(Optional.of(ivo));
        savedParticipants.add(new Participant(session, creator, Role.GM));

        SessionSummary summary = service.joinSession(ivo, " " + INVITE + " ");

        assertThat(summary.participants()).extracting("role")
                .containsExactlyInAnyOrder(Role.GM, Role.PLAYER);
        assertThat(summary.participants()).extracting(p -> p.user().username())
                .containsExactlyInAnyOrder("aria", "ivo");
        assertThat(summary.recentEvents()).extracting(SessionEventDto::type)
                .contains(EventType.PRESENCE);
    }

    @Test
    void joinSessionIsIdempotentForExistingParticipant() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        coreStubs(creator, session);
        when(sessionRepository.findByInviteCode(INVITE)).thenReturn(Optional.of(session));
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        savedParticipants.add(new Participant(session, creator, Role.GM));

        SessionSummary summary = service.joinSession(creator, INVITE);

        assertThat(summary.participants()).hasSize(1);
        verify(participantRepository, never()).save(any());
    }

    @Test
    void joinSessionRejectsClosedSession() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        session.setStatus(SessionStatus.CLOSED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        when(sessionRepository.findByInviteCode(INVITE)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.joinSession(creator, INVITE))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("ended");
    }

    @Test
    void joinSessionRejectsUnknownInviteCode() {
        User user = user(1L, "aria");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(sessionRepository.findByInviteCode("NOPE12")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.joinSession(user, "NOPE12"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No session found");
    }

    @Test
    void leaveSessionRemovesParticipantAndEmitsLeft() {
        User creator = user(1L, "aria");
        User ivo = user(2L, "ivo");
        GameSession session = session(creator);
        coreStubs(creator, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(ivo));
        Participant player = new Participant(session, ivo, Role.PLAYER);
        player.setId(2L);
        savedParticipants.add(new Participant(session, creator, Role.GM));
        savedParticipants.add(player);
        doAnswer(invocation -> {
            savedParticipants.remove(invocation.getArgument(0));
            return null;
        }).when(participantRepository).delete(any(Participant.class));

        service.leaveSession(ivo, SESSION_ID);

        assertThat(savedParticipants).extracting(p -> p.getUser().getId()).doesNotContain(2L);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.OPEN);
        assertThat(savedEvents).extracting(SessionEvent::getType).last().isEqualTo(EventType.PRESENCE);
    }

    @Test
    void leaveSessionClosesSessionWhenLastParticipantLeaves() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        coreStubs(creator, session);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        savedParticipants.add(new Participant(session, creator, Role.GM));
        doAnswer(invocation -> {
            savedParticipants.remove(invocation.getArgument(0));
            return null;
        }).when(participantRepository).delete(any(Participant.class));

        service.leaveSession(creator, SESSION_ID);

        assertThat(session.getStatus()).isEqualTo(SessionStatus.CLOSED);
        assertThat(savedParticipants).isEmpty();
    }

    @Test
    void leaveSessionRejectsNonParticipant() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        coreStubs(creator, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "ivo")));

        assertThatThrownBy(() -> service.leaveSession(user(2L, "ivo"), SESSION_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not a participant");
    }

    @Test
    void getSnapshotThrowsNotFoundForUnknownSession() {
        when(sessionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSnapshot(99L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void addChatMessagePersistsEventForParticipant() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        coreStubs(creator, session);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
        savedParticipants.add(new Participant(session, creator, Role.GM));

        SessionEventDto event = service.addChatMessage(creator, SESSION_ID, "  Hi all!  ");

        assertThat(event.type()).isEqualTo(EventType.CHAT);
        JsonNode payload = (JsonNode) event.payload();
        assertThat(payload.get("text").asText()).isEqualTo("Hi all!");
        assertThat(payload.get("sender").get("username").asText()).isEqualTo("aria");
        verify(eventRepository).save(any());
    }

    @Test
    void addChatMessageRejectsNonParticipant() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        coreStubs(creator, session);
        User ivo = user(2L, "ivo");
        when(userRepository.findById(2L)).thenReturn(Optional.of(ivo));

        assertThatThrownBy(() -> service.addChatMessage(ivo, SESSION_ID, "hello"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only participants");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void recordPresencePersistsPresenceEvent() {
        User creator = user(1L, "aria");
        GameSession session = session(creator);
        coreStubs(creator, session);
        when(userRepository.findById(1L)).thenReturn(Optional.of(creator));

        SessionEventDto event = service.recordPresence(session, creator, "joined");

        assertThat(event.type()).isEqualTo(EventType.PRESENCE);
        JsonNode payload = (JsonNode) event.payload();
        assertThat(payload.get("action").asText()).isEqualTo("joined");
    }
}