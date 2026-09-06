package com.gamer.fowever.tabletopserv.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.gamer.fowever.tabletopserv.domain.EventType;
import com.gamer.fowever.tabletopserv.domain.Game;
import com.gamer.fowever.tabletopserv.domain.GameSession;
import com.gamer.fowever.tabletopserv.domain.Participant;
import com.gamer.fowever.tabletopserv.domain.Role;
import com.gamer.fowever.tabletopserv.domain.SessionEvent;
import com.gamer.fowever.tabletopserv.domain.SessionStatus;
import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.ChatPayload;
import com.gamer.fowever.tabletopserv.dto.ParticipantSummary;
import com.gamer.fowever.tabletopserv.dto.PresencePayload;
import com.gamer.fowever.tabletopserv.dto.SessionEventDto;
import com.gamer.fowever.tabletopserv.dto.SessionSummary;
import com.gamer.fowever.tabletopserv.dto.UserSummary;
import com.gamer.fowever.tabletopserv.repository.GameRepository;
import com.gamer.fowever.tabletopserv.repository.GameSessionRepository;
import com.gamer.fowever.tabletopserv.repository.ParticipantRepository;
import com.gamer.fowever.tabletopserv.repository.SessionEventRepository;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import com.gamer.fowever.tabletopserv.support.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Service
public class SessionService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    static final String INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int INVITE_LENGTH = 6;
    private static final int MAX_INVITE_ATTEMPTS = 100;
    private static final int MAX_RECENT_EVENTS = 50;
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final GameRepository gameRepository;
    private final GameSessionRepository sessionRepository;
    private final ParticipantRepository participantRepository;
    private final SessionEventRepository eventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public SessionService(GameRepository gameRepository,
                          GameSessionRepository sessionRepository,
                          ParticipantRepository participantRepository,
                          SessionEventRepository eventRepository,
                          UserRepository userRepository,
                          ObjectMapper objectMapper) {
        this.gameRepository = gameRepository;
        this.sessionRepository = sessionRepository;
        this.participantRepository = participantRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionSummary createSession(User creator, String name, String gameSlug) {
        User managedCreator = managedUser(creator);
        Game game = gameRepository.findBySlug(gameSlug)
                .orElseThrow(() -> ApiException.badRequest("Unknown game: " + gameSlug));

        GameSession session = new GameSession();
        session.setName(name.trim());
        session.setInviteCode(generateUniqueInviteCode());
        session.setGame(game);
        session.setCreatedBy(managedCreator);
        session.setStatus(SessionStatus.OPEN);
        sessionRepository.save(session);

        Participant gm = new Participant(session, managedCreator, Role.GM);
        participantRepository.save(gm);

        recordPresenceEvent(session, managedCreator, "joined");
        return getSnapshot(session.getId());
    }

    @Transactional
    public SessionSummary joinSession(User user, String inviteCode) {
        User managedUser = managedUser(user);
        GameSession session = sessionRepository.findByInviteCode(inviteCode.trim())
                .orElseThrow(() -> ApiException.notFound("No session found for invite code " + inviteCode));
        if (session.getStatus() == SessionStatus.CLOSED) {
            throw ApiException.forbidden("This session has already ended");
        }
        if (participantRepository.existsBySessionIdAndUserId(session.getId(), managedUser.getId())) {
            return getSnapshot(session.getId());
        }

        participantRepository.save(new Participant(session, managedUser, Role.PLAYER));
        recordPresenceEvent(session, managedUser, "joined");
        return getSnapshot(session.getId());
    }

    @Transactional
    public void leaveSession(User user, Long sessionId) {
        User managedUser = managedUser(user);
        GameSession session = managedSession(sessionId);
        if (!participantRepository.existsBySessionIdAndUserId(session.getId(), managedUser.getId())) {
            throw ApiException.forbidden("You are not a participant of this session");
        }
        Participant participant = participantRepository.findBySessionIdAndUserId(session.getId(), managedUser.getId()).orElseThrow();
        participantRepository.delete(participant);

        recordPresenceEvent(session, managedUser, "left");
        if (participantRepository.findBySessionId(session.getId()).isEmpty()) {
            session.setStatus(SessionStatus.CLOSED);
        }
    }

    @Transactional(readOnly = true)
    public SessionSummary getSnapshot(Long sessionId) {
        return toSummary(managedSession(sessionId));
    }

    @Transactional
    public SessionEventDto addChatMessage(User sender, Long sessionId, String text) {
        User managedSender = managedUser(sender);
        GameSession session = managedSession(sessionId);
        if (!participantRepository.existsBySessionIdAndUserId(session.getId(), managedSender.getId())) {
            throw ApiException.forbidden("Only participants can send messages in this session");
        }
        SessionEvent event = new SessionEvent(session, EventType.CHAT, serialize(
                new ChatPayload(UserSummary.from(managedSender), text.trim(), now())));
        eventRepository.save(event);
        return toEventDto(event);
    }

    @Transactional
    public SessionEventDto recordPresence(GameSession session, User user, String action) {
        SessionEvent event = new SessionEvent(session, EventType.PRESENCE, serialize(
                new PresencePayload(UserSummary.from(user), action, now())));
        eventRepository.save(event);
        return toEventDto(event);
    }

    private void recordPresenceEvent(GameSession session, User user, String action) {
        recordPresence(session, user, action);
    }

    private String generateUniqueInviteCode() {
        for (int attempt = 0; attempt < MAX_INVITE_ATTEMPTS; attempt++) {
            String code = generateInviteCode();
            if (!sessionRepository.existsByInviteCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Could not allocate a unique invite code");
    }

    private String generateInviteCode() {
        StringBuilder builder = new StringBuilder(INVITE_LENGTH);
        for (int i = 0; i < INVITE_LENGTH; i++) {
            builder.append(INVITE_ALPHABET.charAt(SECURE_RANDOM.nextInt(INVITE_ALPHABET.length())));
        }
        return builder.toString();
    }

    private GameSession managedSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found: " + sessionId));
    }

    private User managedUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> ApiException.notFound("User not found: " + user.getId()));
    }

    private SessionSummary toSummary(GameSession session) {
        List<ParticipantSummary> participants = participantRepository.findBySessionId(session.getId()).stream()
                .map(ParticipantSummary::from)
                .toList();
        List<SessionEventDto> recentEvents = eventRepository.findTop50BySessionIdOrderByIdDesc(session.getId()).stream()
                .map(this::toEventDto)
                .sorted(Comparator.comparing(SessionEventDto::id))
                .toList();
        return new SessionSummary(
                session.getId(),
                session.getName(),
                session.getInviteCode(),
                session.getGame().getSlug(),
                session.getGame().getDisplayName(),
                session.getStatus(),
                UserSummary.from(session.getCreatedBy()),
                participants,
                recentEvents);
    }

    private SessionEventDto toEventDto(SessionEvent event) {
        JsonNode payload = null;
        if (event.getPayload() != null && !event.getPayload().isBlank()) {
            try {
                payload = objectMapper.readTree(event.getPayload());
            } catch (JacksonException ignored) {
                payload = null;
            }
        }
        return new SessionEventDto(event.getId(), event.getType(), payload, event.getCreatedAt());
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize event payload", e);
        }
    }

    private String now() {
        return LocalDateTime.now().format(TIMESTAMP);
    }
}