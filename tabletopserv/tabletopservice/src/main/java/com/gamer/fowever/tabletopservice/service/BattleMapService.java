package com.gamer.fowever.tabletopservice.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.gamer.fowever.tabletopapi.EventType;
import com.gamer.fowever.tabletopapi.Role;
import com.gamer.fowever.tabletopapi.TokenCategory;
import com.gamer.fowever.tabletopservice.domain.BattleMap;
import com.gamer.fowever.tabletopservice.domain.BattleMapObject;
import com.gamer.fowever.tabletopservice.domain.BattleMapTile;
import com.gamer.fowever.tabletopservice.domain.Dnd5eCharacter;
import com.gamer.fowever.tabletopservice.domain.GameSession;
import com.gamer.fowever.tabletopservice.domain.InitiativeEntry;
import com.gamer.fowever.tabletopservice.domain.MapToken;
import com.gamer.fowever.tabletopservice.domain.Participant;
import com.gamer.fowever.tabletopservice.domain.SessionEvent;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopapi.dto.AddObjectRequest;
import com.gamer.fowever.tabletopapi.dto.AddTokenRequest;
import com.gamer.fowever.tabletopapi.dto.BattleMapDto;
import com.gamer.fowever.tabletopapi.dto.CharacterAppearance;
import com.gamer.fowever.tabletopapi.dto.CreateMapRequest;
import com.gamer.fowever.tabletopapi.dto.InitiativeEntryDto;
import com.gamer.fowever.tabletopapi.dto.InitiativeEntryRequest;
import com.gamer.fowever.tabletopapi.dto.InitiativeRequest;
import com.gamer.fowever.tabletopapi.dto.MapCatalogDto;
import com.gamer.fowever.tabletopapi.dto.MapObjectTypeDto;
import com.gamer.fowever.tabletopapi.dto.MapTileDto;
import com.gamer.fowever.tabletopapi.dto.MapTileTypeDto;
import com.gamer.fowever.tabletopapi.dto.MapTokenDto;
import com.gamer.fowever.tabletopapi.dto.MoveTokenRequest;
import com.gamer.fowever.tabletopapi.dto.PaintTilesRequest;
import com.gamer.fowever.tabletopapi.dto.SessionEventDto;
import com.gamer.fowever.tabletopapi.dto.TurnAction;
import com.gamer.fowever.tabletopapi.dto.TurnCommandRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateMapRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateObjectRequest;
import com.gamer.fowever.tabletopapi.dto.UpdateTokenRequest;
import com.gamer.fowever.tabletopservice.repository.BattleMapObjectRepository;
import com.gamer.fowever.tabletopservice.repository.BattleMapRepository;
import com.gamer.fowever.tabletopservice.repository.BattleMapTileRepository;
import com.gamer.fowever.tabletopservice.repository.CharacterRepository;
import com.gamer.fowever.tabletopservice.repository.GameSessionRepository;
import com.gamer.fowever.tabletopservice.support.Dtos;
import com.gamer.fowever.tabletopservice.support.TokenAvatar;
import com.gamer.fowever.tabletopservice.repository.InitiativeEntryRepository;
import com.gamer.fowever.tabletopservice.repository.MapTokenRepository;
import com.gamer.fowever.tabletopservice.repository.ParticipantRepository;
import com.gamer.fowever.tabletopservice.repository.SessionEventRepository;
import com.gamer.fowever.tabletopservice.repository.UserRepository;
import com.gamer.fowever.tabletopservice.service.map.MapCatalog;
import com.gamer.fowever.tabletopapi.support.ApiException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
    private static final int INITIATIVE_NOT_STARTED = -1;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TOPIC = "/topic/sessions/%d";
    private static final List<String> COLORS = List.of(
            "#ef4444", "#f97316", "#eab308", "#22c55e",
            "#3b82f6", "#8b5cf6", "#ec4899", "#14b8a6");

    private final BattleMapRepository mapRepository;
    private final MapTokenRepository tokenRepository;
    private final BattleMapTileRepository tileRepository;
    private final BattleMapObjectRepository objectRepository;
    private final InitiativeEntryRepository initiativeRepository;
    private final ParticipantRepository participantRepository;
    private final CharacterRepository characterRepository;
    private final GameSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final SessionEventRepository eventRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final MapCatalog mapCatalog;

    public BattleMapService(BattleMapRepository mapRepository,
                            MapTokenRepository tokenRepository,
                            BattleMapTileRepository tileRepository,
                            BattleMapObjectRepository objectRepository,
                            InitiativeEntryRepository initiativeRepository,
                            ParticipantRepository participantRepository,
                            CharacterRepository characterRepository,
                            GameSessionRepository sessionRepository,
                            UserRepository userRepository,
                            SessionEventRepository eventRepository,
                            SimpMessagingTemplate messagingTemplate,
                            ObjectMapper objectMapper,
                            MapCatalog mapCatalog) {
        this.mapRepository = mapRepository;
        this.tokenRepository = tokenRepository;
        this.tileRepository = tileRepository;
        this.objectRepository = objectRepository;
        this.initiativeRepository = initiativeRepository;
        this.participantRepository = participantRepository;
        this.characterRepository = characterRepository;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper = objectMapper;
        this.mapCatalog = mapCatalog;
    }

    @Transactional(readOnly = true)
    public BattleMapDto getMap(Long sessionId) {
        managedSession(sessionId);
        return toDto(managedMap(sessionId));
    }

    @Transactional(readOnly = true)
    public MapCatalogDto getCatalog(Long sessionId) {
        managedSession(sessionId);
        List<MapTileTypeDto> tiles = mapCatalog.tiles().stream()
                .map(tile -> new MapTileTypeDto(tile.index(), tile.name(), tile.color()))
                .toList();
        List<MapObjectTypeDto> objects = mapCatalog.objects().stream()
                .map(object -> new MapObjectTypeDto(object.index(), object.name(), object.color(),
                        object.width(), object.height(), object.blocksMovement()))
                .toList();
        return new MapCatalogDto(tiles, objects);
    }

    @Transactional
    public BattleMapDto paintTiles(User actor, Long sessionId, PaintTilesRequest request) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);

        List<MapTileDto> tiles = request.tiles();
        if (tiles.isEmpty()) {
            return toDto(map);
        }
        Map<String, BattleMapTile> existing = tileRepository.findByMapId(map.getId()).stream()
                .collect(Collectors.toMap(tile -> cellKey(tile.getPosX(), tile.getPosY()), tile -> tile));
        for (MapTileDto tile : tiles) {
            requireInBounds(map, tile.x(), tile.y());
            String key = cellKey(tile.x(), tile.y());
            if (tile.tileIndex() == null) {
                BattleMapTile erased = existing.remove(key);
                if (erased != null) {
                    tileRepository.delete(erased);
                }
                continue;
            }
            if (!mapCatalog.hasTile(tile.tileIndex())) {
                throw ApiException.badRequest("Unknown tile index: " + tile.tileIndex());
            }
            BattleMapTile present = existing.get(key);
            if (present == null) {
                existing.put(key, new BattleMapTile(map, tile.x(), tile.y(), tile.tileIndex()));
            } else {
                present.setTileIndex(tile.tileIndex());
            }
        }
        tileRepository.saveAll(existing.values());
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto addObject(User actor, Long sessionId, AddObjectRequest request) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);

        MapCatalog.ObjectType type = mapCatalog.object(request.objectIndex())
                .orElseThrow(() -> ApiException.badRequest("Unknown object index: " + request.objectIndex()));
        int rotation = normalizeRotation(request.rotation());
        boolean blocksMovement = request.blocksMovement() != null ? request.blocksMovement() : type.blocksMovement();
        requireFootprintInBounds(map, request.x(), request.y(), type.width(), type.height());

        objectRepository.save(new BattleMapObject(map, request.objectIndex(), request.x(), request.y(),
                type.width(), type.height(), rotation, blocksMovement));
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto updateObject(User actor, Long sessionId, Long objectId, UpdateObjectRequest request) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);
        BattleMapObject object = managedObject(objectId, map.getId());

        int x = request.x() != null ? request.x() : object.getPosX();
        int y = request.y() != null ? request.y() : object.getPosY();
        requireFootprintInBounds(map, x, y, object.getWidth(), object.getHeight());
        object.setPosX(x);
        object.setPosY(y);
        if (request.rotation() != null) {
            object.setRotation(normalizeRotation(request.rotation()));
        }
        if (request.blocksMovement() != null) {
            object.setBlocksMovement(request.blocksMovement());
        }
        objectRepository.save(object);
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto removeObject(User actor, Long sessionId, Long objectId) {
        GameSession session = managedSession(sessionId);
        requireGm(actor, session);
        BattleMap map = managedMap(sessionId);
        objectRepository.delete(managedObject(objectId, map.getId()));
        return broadcastAndPersist(map);
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
        tileRepository.findByMapId(map.getId()).forEach(tile -> {
            if (tile.getPosX() >= map.getWidth() || tile.getPosY() >= map.getHeight()) {
                tileRepository.delete(tile);
            }
        });
        objectRepository.findByMapIdOrderByIdAsc(map.getId()).forEach(object -> {
            if (object.getPosX() + object.getWidth() > map.getWidth()
                    || object.getPosY() + object.getHeight() > map.getHeight()) {
                objectRepository.delete(object);
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
        requireOpenSquare(map, x, y);
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

        Set<String> blocked = blockedSquares(map);
        if (blocked.contains(cellKey(request.x(), request.y()))) {
            throw ApiException.badRequest("Square is blocked by an object (" + request.x() + ", " + request.y() + ")");
        }
        if (!pathReachable(map, token.getPosX(), token.getPosY(), request.x(), request.y(),
                remainingFeet / map.getSquareFeet(), blocked)) {
            throw ApiException.badRequest("No clear path to (" + request.x() + ", " + request.y() + ")");
        }

        token.setPosX(request.x());
        token.setPosY(request.y());
        token.setMovedFeet(token.getMovedFeet() + requiredFeet);
        tokenRepository.save(token);
        return broadcastAndPersist(map);
    }

    @Transactional
    public BattleMapDto placeToken(User actor, Long sessionId, Long tokenId, MoveTokenRequest request) {
        GameSession session = managedSession(sessionId);
        User managedActor = managedUser(actor);
        requireMember(session, managedActor);
        BattleMap map = managedMap(sessionId);
        MapToken token = managedToken(tokenId, map.getId());
        requireInBounds(map, request.x(), request.y());
        requireOpenSquare(map, request.x(), request.y());

        if (map.getInitiativeIndex() != INITIATIVE_NOT_STARTED) {
            throw ApiException.badRequest("The map is in combat — use move instead");
        }
        if (!isGm(session, managedActor) && !isOwner(token, managedActor)) {
            throw ApiException.forbidden("You can only place your own token");
        }
        if (request.x() == token.getPosX() && request.y() == token.getPosY()) {
            return toDto(map);
        }

        token.setPosX(request.x());
        token.setPosY(request.y());
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

    private void requireFootprintInBounds(BattleMap map, int x, int y, int width, int height) {
        if (x < 0 || y < 0 || x + width > map.getWidth() || y + height > map.getHeight()) {
            throw ApiException.badRequest("Object footprint is outside the map ("
                    + x + ", " + y + " + " + width + "×" + height + ")");
        }
    }

    private static int normalizeRotation(Integer rotation) {
        if (rotation == null) {
            return 0;
        }
        return Math.floorMod(rotation, 360) / 90 * 90;
    }

    private void requireOpenSquare(BattleMap map, int x, int y) {
        if (blockedSquares(map).contains(cellKey(x, y))) {
            throw ApiException.badRequest("Square is blocked by an object (" + x + ", " + y + ")");
        }
    }

    /**
     * Expands every movement-blocking object into the set of squares its
     * axis-aligned footprint covers (clamped to the map). Objects never block
     * off-map squares.
     */
    private Set<String> blockedSquares(BattleMap map) {
        Set<String> blocked = new HashSet<>();
        for (BattleMapObject object : objectRepository.findByMapIdOrderByIdAsc(map.getId())) {
            if (!object.isBlocksMovement()) {
                continue;
            }
            for (int x = object.getPosX(); x < object.getPosX() + object.getWidth(); x++) {
                for (int y = object.getPosY(); y < object.getPosY() + object.getHeight(); y++) {
                    if (x >= 0 && y >= 0 && x < map.getWidth() && y < map.getHeight()) {
                        blocked.add(cellKey(x, y));
                    }
                }
            }
        }
        return blocked;
    }

    /**
     * Breadth-first search over the 8 surrounding squares (each step costs one
     * square), refusing to enter blocked or off-map squares. A diagonal step is
     * only allowed when at least one of its two orthogonal neighbours is open,
     * so a token cannot cut through a solid corner.
     */
    private boolean pathReachable(BattleMap map, int fromX, int fromY, int toX, int toY,
                                  int maxSteps, Set<String> blocked) {
        if (maxSteps < 0) {
            return false;
        }
        if (fromX == toX && fromY == toY) {
            return true;
        }
        boolean[][] visited = new boolean[map.getWidth()][map.getHeight()];
        visited[fromX][fromY] = true;
        List<int[]> frontier = new ArrayList<>();
        frontier.add(new int[] {fromX, fromY});
        int depth = 0;
        while (!frontier.isEmpty() && depth < maxSteps) {
            List<int[]> next = new ArrayList<>();
            for (int[] cell : frontier) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        if (dx == 0 && dy == 0) {
                            continue;
                        }
                        int x = cell[0] + dx;
                        int y = cell[1] + dy;
                        if (x < 0 || y < 0 || x >= map.getWidth() || y >= map.getHeight()) {
                            continue;
                        }
                        if (visited[x][y] || blocked.contains(cellKey(x, y))) {
                            continue;
                        }
                        if (dx != 0 && dy != 0
                                && blocked.contains(cellKey(cell[0] + dx, cell[1]))
                                && blocked.contains(cellKey(cell[0], cell[1] + dy))) {
                            continue;
                        }
                        if (x == toX && y == toY) {
                            return true;
                        }
                        visited[x][y] = true;
                        next.add(new int[] {x, y});
                    }
                }
            }
            frontier = next;
            depth++;
        }
        return false;
    }

    private BattleMapObject managedObject(Long objectId, Long mapId) {
        return objectRepository.findByIdAndMapId(objectId, mapId)
                .orElseThrow(() -> ApiException.notFound("Map object not found: " + objectId));
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
        List<MapToken> tokens = tokenRepository.findByMapIdOrderByIdAsc(map.getId());
        return Dtos.battleMap(map, tokens, orderedInitiative(map.getId()), resolveAvatars(tokens),
                tileRepository.findByMapIdOrderByPosYAscPosXAsc(map.getId()),
                objectRepository.findByMapIdOrderByIdAsc(map.getId()));
    }

    private static String cellKey(int x, int y) {
        return x + "," + y;
    }

    /**
     * Resolves the avatar of each linked player's earliest-created character so
     * player tokens can render a portrait instead of the colour disc
     * (design-draft-v2 §5). Users without a character are simply absent from the
     * result and their tokens keep the colour disc.
     */
    private Map<Long, TokenAvatar> resolveAvatars(List<MapToken> tokens) {
        Set<Long> userIds = tokens.stream()
                .map(MapToken::getLinkedUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, TokenAvatar> avatars = new HashMap<>();
        for (Long userId : userIds) {
            characterRepository.findFirstByOwnerIdOrderByIdAsc(userId)
                    .filter(Dnd5eCharacter.class::isInstance)
                    .map(Dnd5eCharacter.class::cast)
                    .ifPresent(character -> avatars.put(userId, new TokenAvatar(
                            character.getRaceIndex(),
                            character.getSex(),
                            parseAppearance(character.getAppearance()))));
        }
        return avatars;
    }

    private CharacterAppearance parseAppearance(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, CharacterAppearance.class);
        } catch (JacksonException e) {
            return null;
        }
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