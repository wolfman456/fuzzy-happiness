package com.gamer.fowever.tabletopservice.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.gamer.fowever.tabletopapi.EventType;
import com.gamer.fowever.tabletopapi.Role;
import com.gamer.fowever.tabletopapi.TokenCategory;
import com.gamer.fowever.tabletopservice.domain.BattleMap;
import com.gamer.fowever.tabletopservice.domain.GameSession;
import com.gamer.fowever.tabletopservice.domain.InitiativeEntry;
import com.gamer.fowever.tabletopservice.domain.MapToken;
import com.gamer.fowever.tabletopservice.domain.Participant;
import com.gamer.fowever.tabletopservice.domain.SessionEvent;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopapi.dto.AddTokenRequest;
import com.gamer.fowever.tabletopapi.dto.BattleMapDto;
import com.gamer.fowever.tabletopapi.dto.CreateMapRequest;
import com.gamer.fowever.tabletopapi.dto.InitiativeEntryDto;
import com.gamer.fowever.tabletopapi.dto.InitiativeEntryRequest;
import com.gamer.fowever.tabletopapi.dto.InitiativeRequest;
import com.gamer.fowever.tabletopapi.dto.MapTokenDto;
import com.gamer.fowever.tabletopapi.dto.MoveTokenRequest;
import com.gamer.fowever.tabletopapi.dto.SessionEventDto;
import com.gamer.fowever.tabletopapi.dto.TurnAction;
import com.gamer.fowever.tabletopapi.dto.TurnCommandRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateMapRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateTokenRequest;
import com.gamer.fowever.tabletopservice.repository.BattleMapRepository;
import com.gamer.fowever.tabletopservice.repository.GameSessionRepository;
import com.gamer.fowever.tabletopservice.support.Dtos;
import com.gamer.fowever.tabletopservice.repository.InitiativeEntryRepository;
import com.gamer.fowever.tabletopservice.repository.MapTokenRepository;
import com.gamer.fowever.tabletopservice.repository.ParticipantRepository;
import com.gamer.fowever.tabletopservice.repository.SessionEventRepository;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import com.gamer.fowever.tabletopapi.support.ApiException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class BattleMapService {

    private static final int DEFAULT_WIDTH = 24;
    private static final int DEFAULT_HEIGHT = 18;
    private static final int DEFAULT_SQUARE_FEET = 10;
    private static final int DEFAULT_SPEED_FEET = 30;
    private static final int MIN_SPEED_FEET = 5;
    private static final int MAX_SPEED_FEET = 240;
    private static final int MAX_INITIATIVE_ENTRIES = 30;
    private static final int INITIATIVE_DIE_SIDES = 20;
    private static final int MIN_INITIATIVE_SCORE = 1;
    private static final int MAX_INITIATIVE_SCORE = 999;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOPIC = "/topic/sessions/%d";
    private static final List<String> COLORS = List.of(
            "#ef4444", "#f97316", "#eab308", "#22c55e",
            "#3b82f6", "#8b5cf6", "#ec4899", "#14b8a6");

    private final BattleMapRepository mapRepository;
    private final MapTokenRepository tokenRepository;
    private final InitiativeEntryRepository initiativeRepository;
    private final ParticipantRepository participantRepository;
    private final GameSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SessionEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    public BattleMapService(BattleMapRepository mapRepository,
                            MapTokenRepository tokenRepository,
                            InitiativeEntryRepository initiativeRepository,
                            ParticipantRepository participantRepository,
                            GameSessionRepository sessionRepository,
                            UserRepository userRepository,
                            SessionEventRepository eventRepository,
                            SimpMessagingTemplate messagingTemplate,
                            ObjectMapper objectMapper) {
        this.mapRepository = mapRepository;
        this.tokenRepository = tokenRepository;
        this.initiativeRepository = initiativeRepository;
        this.participantRepository = participantRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public BattleMapDto getMap(Long sessionId) {
        managedSession(sessionId);
        return toDto(managedMap(sessionId));
    }

    @Transactional
    public BattleMapDto createMap(User actor, Long sessionId, CreateMapRequest request) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap existing = mapRepository.findBySessionId(sessionId).orElse(null);
        if (existing != null) {
            return toDto(existing);
        }
        int width = request.width() != null ? request.width() : DEFAULT_WIDTH;
        int height = request.height() != null ? request.height() : DEFAULT_HEIGHT;
        validateMapSize(width, height);

        BattleMap map = mapRepository.save(
                new BattleMap(session, request.name().trim(), width, height, DEFAULT_SQUARE_FEET));

        List<Participant> participants = participantRepository.findBySessionId(sessionId);
        int index = 0;
        for (Participant participant : participants) {
            if (participant.getRole() == Role.SPECTATOR) {
                continue;
            }
            User user = participant.getUser();
            tokenRepository.save(new MapToken(
                    map,
                    displayNameOf(user),
                    TokenCategory.PLAYER,
                    COLORS.get(index % COLORS.size()),
                    DEFAULT_SPEED_FEET,
                    Math.min(index * 2 + 1, width - 1),
                    Math.min(index * 2 + 1, height - 1),
                    participant.getId(),
                    user.getId()));
            index++;
        }
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto updateMap(User actor, Long sessionId, UpdateMapRequest request) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);

        boolean changed = false;
        if (request.width() != null && request.height() != null) {
            validateMapSize(request.width(), request.height());
        } else if (request.width() != null) {
            validateMapSize(request.width(), map.getHeight());
        } else if (request.height() != null) {
            validateMapSize(map.getWidth(), request.height());
        }
        if (request.name() != null && !request.name().isBlank() && !map.getName().equals(request.name())) {
            map.setName(request.name());
            changed = true;
        }
        if (request.width() != null && request.width() != map.getWidth()) {
            map.setWidth(request.width());
            changed = true;
        }
        if (request.height() != null && request.height() != map.getHeight()) {
            map.setHeight(request.height());
            changed = true;
        }
        if (!changed) {
            return toDto(map);
        }
        mapRepository.save(map);

        tokenRepository.findByMapIdOrderByIdAsc(map.getId()).forEach(token -> {
            boolean dirty = false;
            if (token.getPosX() >= map.getWidth()) {
                token.setPosX(Math.max(0, map.getWidth() - 1));
                dirty = true;
            }
            if (token.getPosY() >= map.getHeight()) {
                token.setPosY(Math.max(0, map.getHeight() - 1));
                dirty = true;
            }
            if (dirty) {
                tokenRepository.save(token);
            }
        });
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto addToken(User actor, Long sessionId, AddTokenRequest request) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);

        int x = request.x() != null ? request.x() : 0;
        int y = request.y() != null ? request.y() : 0;
        requireInBounds(map, x, y);
        int speedFeet = request.speedFeet() != null ? request.speedFeet() : DEFAULT_SPEED_FEET;
        validateSpeed(speedFeet);
        TokenCategory category = request.category() != null ? request.category() : TokenCategory.MONSTER_NPC;

        tokenRepository.save(new MapToken(
                map,
                request.name().trim(),
                category,
                request.color() != null ? request.color() : COLORS.get(tokenRepository.findByMapIdOrderByIdAsc(map.getId()).size() % COLORS.size()),
                speedFeet,
                x,
                y,
                null,
                null));
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto updateToken(User actor, Long sessionId, Long tokenId, UpdateTokenRequest request) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);
        MapToken token = managedToken(tokenId, map.getId());

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw ApiException.badRequest("Token name must not be blank");
            }
            token.setName(request.name().trim());
        }
        if (request.color() != null) {
            token.setColor(request.color());
        }
        if (request.speedFeet() != null) {
            validateSpeed(request.speedFeet());
            token.setSpeedFeet(request.speedFeet());
        }
        tokenRepository.save(token);
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto removeToken(User actor, Long sessionId, Long tokenId) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);
        MapToken token = managedToken(tokenId, map.getId());

        if (map.getCurrentTurnTokenId() != null && map.getCurrentTurnTokenId().equals(tokenId)) {
            map.setCurrentTurnTokenId(null);
            mapRepository.save(map);
        }
        tokenRepository.delete(token);
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto moveToken(User actor, Long sessionId, Long tokenId, MoveTokenRequest request) {
        GameSession session = managedSession(sessionId);
        User managedActor = managedUser(actor);
        requireMember(session, managedActor);
        BattleMap map = managedMap(sessionId);
        MapToken token = managedToken(tokenId, map.getId());
        requireInBounds(map, request.x(), request.y());

        int squares = Math.max(Math.abs(request.x() - token.getPosX()), Math.abs(request.y() - token.getPosY()));
        if (squares == 0) {
            return toDto(map);
        }
        if (!isGm(session, managedActor) && !isOwner(token, managedActor)) {
            throw ApiException.forbidden("You can only move your own token");
        }

        int requiredFeet = squares * map.getSquareFeet();
        int remainingFeet = token.getSpeedFeet() - token.getMovedFeet();
        if (requiredFeet > remainingFeet) {
            throw ApiException.badRequest("Movement of " + requiredFeet
                    + " ft exceeds the " + remainingFeet + " ft remaining this turn");
        }

        token.setPosX(request.x());
        token.setPosY(request.y());
        token.setMovedFeet(token.getMovedFeet() + requiredFeet);
        tokenRepository.save(token);
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto turnCommand(User actor, Long sessionId, TurnCommandRequest request) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);

        if (request.action() == null) {
            throw ApiException.badRequest("Turn action is required");
        }
        switch (request.action()) {
            case START -> {
                if (request.tokenId() == null) {
                    throw ApiException.badRequest("tokenId is required to start a turn");
                }
                MapToken token = managedToken(request.tokenId(), map.getId());
                token.setMovedFeet(0);
                tokenRepository.save(token);
                map.setCurrentTurnTokenId(token.getId());
                mapRepository.save(map);
            }
            case END -> {
                map.setCurrentTurnTokenId(null);
                mapRepository.save(map);
            }
            case NEW_ROUND -> {
                tokenRepository.findByMapIdOrderByIdAsc(map.getId())
                        .forEach(token -> {
                            token.setMovedFeet(0);
                            tokenRepository.save(token);
                        });
                map.setCurrentTurnTokenId(null);
                mapRepository.save(map);
            }
        }
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto setInitiative(User actor, Long sessionId, InitiativeRequest request) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);

        List<InitiativeEntryRequest> entries = request.entries();
        if (entries.size() > MAX_INITIATIVE_ENTRIES) {
            throw ApiException.badRequest("At most " + MAX_INITIATIVE_ENTRIES + " initiative entries");
        }

        initiativeRepository.deleteAll(orderedInitiative(map.getId()));

        for (InitiativeEntryRequest entry : entries) {
            boolean hasLabel = entry.label() != null && !entry.label().isBlank();
            boolean hasToken = entry.tokenId() != null;
            if (hasLabel == hasToken) {
                throw ApiException.badRequest("Each initiative entry needs a label or a tokenId, but not both");
            }
            MapToken token = hasToken ? managedToken(entry.tokenId(), map.getId()) : null;
            int score = entry.score() != null ? entry.score() : autoRollInitiative();
            validateInitiativeScore(score);
            initiativeRepository.save(
                    new InitiativeEntry(map, hasLabel ? entry.label().trim() : null, token, score));
        }
        map.setInitiativeIndex(-1);
        mapRepository.save(map);
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto rerollInitiative(User actor, Long sessionId, Long entryId) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);
        InitiativeEntry entry = managedInitiativeEntry(entryId, map.getId());
        entry.setScore(autoRollInitiative());
        initiativeRepository.save(entry);
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto nextInitiative(User actor, Long sessionId) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);
        List<InitiativeEntry> entries = orderedInitiative(map.getId());
        if (entries.isEmpty()) {
            throw ApiException.badRequest("There is no initiative order yet");
        }
        int index = (map.getInitiativeIndex() + 1) % entries.size();
        map.setInitiativeIndex(index);

        InitiativeEntry current = entries.get(index);
        if (current.getToken() != null) {
            MapToken token = current.getToken();
            token.setMovedFeet(0);
            tokenRepository.save(token);
            map.setCurrentTurnTokenId(token.getId());
        }
        mapRepository.save(map);
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto removeInitiativeEntry(User actor, Long sessionId, Long entryId) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);
        InitiativeEntry entry = managedInitiativeEntry(entryId, map.getId());
        initiativeRepository.delete(entry);

        List<InitiativeEntry> remaining = orderedInitiative(map.getId());
        if (remaining.isEmpty()) {
            map.setInitiativeIndex(-1);
        } else if (map.getInitiativeIndex() >= remaining.size()) {
            map.setInitiativeIndex(remaining.size() - 1);
        }
        mapRepository.save(map);
        return broadcastAndPersist(map);
    }

    private String displayNameOf(User user) {
        return user.getDisplayName() != null && !user.getDisplayName().isBlank()
                ? user.getDisplayName()
                : user.getUsername();
    }

    private boolean isOwner(MapToken token, User actor) {
        return token.getLinkedUserId() != null && token.getLinkedUserId().equals(actor.getId());
    }

    private boolean isGm(GameSession session, User user) {
        return participantRepository.findBySessionIdAndUserId(session.getId(), user.getId())
                .filter(participant -> participant.getRole() == Role.GM)
                .isPresent();
    }

    private void requireMember(GameSession session, User user) {
        if (!participantRepository.existsBySessionIdAndUserId(session.getId(), user.getId())) {
            throw ApiException.forbidden("You are not a participant of this session");
        }
    }

    private void requireGm(User actor, GameSession session) {
        User managedActor = managedUser(actor);
        Participant participant = participantRepository.findBySessionIdAndUserId(session.getId(), managedActor.getId())
                .orElseThrow(() -> ApiException.forbidden("You are not a participant of this session"));
        if (participant.getRole() != Role.GM) {
            throw ApiException.forbidden("Only the GM can manage the battle map");
        }
    }

    private void validateMapSize(int width, int height) {
        if (width < 1 || width > 200 || height < 1 || height > 200) {
            throw ApiException.badRequest("Map size must be between 1 and 200 squares per side");
        }
    }

    private void validateSpeed(int speedFeet) {
        if (speedFeet < MIN_SPEED_FEET || speedFeet > MAX_SPEED_FEET) {
            throw ApiException.badRequest("Speed must be between " + MIN_SPEED_FEET + " and " + MAX_SPEED_FEET + " feet");
        }
    }

    private int autoRollInitiative() {
        return RANDOM.nextInt(INITIATIVE_DIE_SIDES) + 1;
    }

    private void validateInitiativeScore(int score) {
        if (score < MIN_INITIATIVE_SCORE || score > MAX_INITIATIVE_SCORE) {
            throw ApiException.badRequest("Initiative score must be between "
                    + MIN_INITIATIVE_SCORE + " and " + MAX_INITIATIVE_SCORE);
        }
    }

    private void requireInBounds(BattleMap map, int x, int y) {
        if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight()) {
            throw ApiException.badRequest("Square is outside the map (" + x + ", " + y + ")");
        }
    }

    private BattleMapDto broadcastAndPersist(BattleMap map) {
        BattleMapDto dto = toDto(map);
        SessionEvent event = new SessionEvent(map.getSession(), EventType.TABLE, serialize(dto));
        eventRepository.save(event);
        messagingTemplate.convertAndSend(TOPIC.formatted(map.getSession().getId()),
                new SessionEventDto(event.getId(), EventType.TABLE, toJson(dto), event.getCreatedAt()));
        return dto;
    }

    private BattleMapDto toDto(BattleMap map) {
        return Dtos.battleMap(map,
                tokenRepository.findByMapIdOrderByIdAsc(map.getId()),
                orderedInitiative(map.getId()));
    }

    private List<InitiativeEntry> orderedInitiative(Long battleMapId) {
        return initiativeRepository.findByBattleMapIdOrderByScoreDescIdAsc(battleMapId);
    }

    private InitiativeEntry managedInitiativeEntry(Long entryId, Long battleMapId) {
        return initiativeRepository.findByIdAndBattleMapId(entryId, battleMapId)
                .orElseThrow(() -> ApiException.notFound("Initiative entry not found: " + entryId));
    }

    private GameSession managedSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> ApiException.notFound("Session not found: " + sessionId));
    }

    private User managedUser(User user) {
        return userRepository.findById(user.getId())
                .orElseThrow(() -> ApiException.notFound("User not found: " + user.getId()));
    }

    private BattleMap managedMap(Long sessionId) {
        return mapRepository.findBySessionId(sessionId)
                .orElseThrow(() -> ApiException.notFound("No battle map for this session yet"));
    }

    private MapToken managedToken(Long tokenId, Long mapId) {
        return tokenRepository.findByIdAndMapId(tokenId, mapId)
                .orElseThrow(() -> ApiException.notFound("Map token not found: " + tokenId));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize map state", e);
        }
    }

    private JsonNode toJson(Object value) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(value));
        } catch (JacksonException e) {
            throw new IllegalStateException("Failed to serialize map state", e);
        }
    }
}