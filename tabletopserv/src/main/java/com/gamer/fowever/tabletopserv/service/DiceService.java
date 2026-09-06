package com.gamer.fowever.tabletopserv.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.gamer.fowever.tabletopserv.domain.EventType;
import com.gamer.fowever.tabletopserv.domain.GameSession;
import com.gamer.fowever.tabletopserv.domain.SessionEvent;
import com.gamer.fowever.tabletopserv.domain.User;
import com.gamer.fowever.tabletopserv.dto.RollRequest;
import com.gamer.fowever.tabletopserv.dto.SessionEventDto;
import com.gamer.fowever.tabletopserv.dto.UserSummary;
import com.gamer.fowever.tabletopserv.repository.GameSessionRepository;
import com.gamer.fowever.tabletopserv.repository.ParticipantRepository;
import com.gamer.fowever.tabletopserv.repository.SessionEventRepository;
import com.gamer.fowever.tabletopserv.repository.UserRepository;
import com.gamer.fowever.tabletopserv.support.ApiException;
import com.gamer.fowever.tabletopserv.support.DiceExpressionParser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DiceService {

    private final ParticipantRepository participantRepository;
    private final GameSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SessionEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private static final SecureRandom RANDOM = new SecureRandom();

    public DiceService(ParticipantRepository participantRepository,
                       GameSessionRepository sessionRepository,
                       UserRepository userRepository,
                       SessionEventRepository eventRepository,
                       ObjectMapper objectMapper) {
        this.participantRepository = participantRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public DiceRoll roll(User actor, Long sessionId, RollRequest request) {
        User managedActor = managedUser(actor);
        GameSession session = managedSession(sessionId);
        requireMember(session, managedActor);

        DiceExpressionParser.ParsedDice dice = DiceExpressionParser.parse(request.expression());
        boolean hidden = Boolean.TRUE.equals(request.privateRoll());
        if (hidden) {
            requireGm(session, managedActor);
        }

        List<Integer> rolls = new ArrayList<>(dice.count());
        for (int i = 0; i < dice.count(); i++) {
            rolls.add(RANDOM.nextInt(dice.sides()) + 1);
        }
        int total = rolls.stream().mapToInt(Integer::intValue).sum() + dice.modifier();
        String rollId = UUID.randomUUID().toString();
        String label = request.label() == null || request.label().isBlank() ? null : request.label().trim();

        Map<String, Object> full = payload(rollId, managedActor, dice.normalized(), label, rolls, total, hidden);
        if (hidden) {
            Map<String, Object> publicFrame = payload(rollId, managedActor, dice.normalized(), label, null, null, true);
            SessionEventDto privateEvent = new SessionEventDto(null, EventType.DICE, toJson(full), now());
            SessionEventDto topicEvent = new SessionEventDto(null, EventType.DICE, toJson(publicFrame), now());
            return new DiceRoll(privateEvent, topicEvent);
        }

        SessionEvent event = new SessionEvent(session, EventType.DICE, serialize(full));
        eventRepository.save(event);
        SessionEventDto dto = new SessionEventDto(event.getId(), EventType.DICE, toJson(full), event.getCreatedAt());
        return new DiceRoll(dto, dto);
    }

    private Map<String, Object> payload(String rollId, User roller, String expression, String label,
                                        List<Integer> rolls, Integer total, boolean hidden) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rollId", rollId);
        payload.put("rolledBy", UserSummary.from(roller));
        payload.put("expression", expression);
        if (label != null) {
            payload.put("label", label);
        }
        if (rolls != null) {
            payload.put("rolls", rolls);
            payload.put("total", total);
        }
        payload.put("hidden", hidden);
        return payload;
    }

    public record DiceRoll(SessionEventDto event, SessionEventDto topicEvent) {

        public boolean isHidden() {
            return topicEvent != event;
        }
    }

    private void requireMember(GameSession session, User user) {
        if (!participantRepository.existsBySessionIdAndUserId(session.getId(), user.getId())) {
            throw ApiException.forbidden("You are not a participant of this session");
        }
    }

    private void requireGm(GameSession session, User user) {
        participantRepository.findBySessionIdAndUserId(session.getId(), user.getId())
                .filter(participant -> participant.getRole() == com.gamer.fowever.tabletopserv.domain.Role.GM)
                .orElseThrow(() -> ApiException.forbidden("Only the GM can roll in private"));
    }

    private GameSession managedSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found: " + sessionId));
    }

    private User managedUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> ApiException.notFound("User not found: " + user.getId()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize dice roll", e);
        }
    }

    private JsonNode toJson(Object value) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(value));
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize dice roll", e);
        }
    }
}