package com.gamer.fowever.tabletopservice.service;

import com.gamer.fowever.tabletopservice.domain.BattleMap;
import com.gamer.fowever.tabletopservice.domain.BattleMapObject;
import com.gamer.fowever.tabletopapi.EventType;
import com.gamer.fowever.tabletopservice.domain.Dnd5eCharacter;
import com.gamer.fowever.tabletopservice.domain.Game;
import com.gamer.fowever.tabletopservice.domain.GameSession;
import com.gamer.fowever.tabletopservice.domain.InitiativeEntry;
import com.gamer.fowever.tabletopservice.domain.MapToken;
import com.gamer.fowever.tabletopservice.domain.Participant;
import com.gamer.fowever.tabletopapi.Role;
import com.gamer.fowever.tabletopservice.domain.SessionEvent;
import com.gamer.fowever.tabletopapi.SessionStatus;
import com.gamer.fowever.tabletopapi.Sex;
import com.gamer.fowever.tabletopapi.TokenCategory;
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
import com.gamer.fowever.tabletopapi.dto.MapObjectDto;
import com.gamer.fowever.tabletopapi.dto.MapTileDto;
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
import com.gamer.fowever.tabletopservice.repository.CharacterRepository;
import com.gamer.fowever.tabletopservice.repository.GameSessionRepository;
import com.gamer.fowever.tabletopservice.repository.InitiativeEntryRepository;
import com.gamer.fowever.tabletopservice.repository.MapTokenRepository;
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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BattleMapServiceTest {

    private static final Long SESSION_ID = 1L;

    @Mock
    private BattleMapRepository mapRepository;
    @Mock
    private MapTokenRepository tokenRepository;
    @Mock
    private com.gamer.fowever.tabletopservice.repository.BattleMapTileRepository tileRepository;
    @Mock
    private BattleMapObjectRepository objectRepository;
    @Mock
    private InitiativeEntryRepository initiativeRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private CharacterRepository characterRepository;
    @Mock
    private GameSessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionEventRepository eventRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private com.gamer.fowever.tabletopservice.service.map.MapCatalog mapCatalog;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private final List<Participant> savedParticipants = new ArrayList<>();
    private final List<MapToken> savedTokens = new ArrayList<>();
    private final List<com.gamer.fowever.tabletopservice.domain.BattleMapTile> savedTiles = new ArrayList<>();
    private final List<BattleMapObject> savedObjects = new ArrayList<>();
    private final List<SessionEvent> savedEvents = new ArrayList<>();
    private final List<InitiativeEntry> savedInitiative = new ArrayList<>();
    private BattleMap savedMap;

    private BattleMapService service;

    @BeforeEach
    void setUp() {
        service = new BattleMapService(mapRepository, tokenRepository, tileRepository, objectRepository,
                initiativeRepository, participantRepository, characterRepository, sessionRepository,
                userRepository, eventRepository, messagingTemplate, objectMapper, mapCatalog);
        savedParticipants.clear();
        savedTokens.clear();
        savedTiles.clear();
        savedObjects.clear();
        savedEvents.clear();
        savedInitiative.clear();
        savedMap = null;
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
        if (session.getId() != null) {
            when(sessionRepository.findById(session.getId())).thenReturn(Optional.of(session));
        }
        when(mapRepository.save(any(BattleMap.class))).thenAnswer(invocation -> {
            BattleMap map = invocation.getArgument(0);
            if (map.getId() == null) {
                map.setId(10L);
            }
            savedMap = map;
            return map;
        });
        when(mapRepository.findBySessionId(SESSION_ID))
                .thenAnswer(invocation -> Optional.ofNullable(savedMap));
        when(tokenRepository.save(any(MapToken.class))).thenAnswer(invocation -> {
            MapToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId((long) savedTokens.size() + 1);
            }
            if (!savedTokens.contains(token)) {
                savedTokens.add(token);
            }
            return token;
        });
        when(tokenRepository.findByMapIdOrderByIdAsc(anyLong()))
                .thenAnswer(invocation -> new ArrayList<>(savedTokens));
        when(tokenRepository.findByIdAndMapId(anyLong(), anyLong()))
                .thenAnswer(invocation -> savedTokens.stream()
                        .filter(token -> token.getId().equals(invocation.getArgument(0)))
                        .findFirst());
        org.mockito.Mockito.doAnswer(invocation -> {
            savedTokens.remove(invocation.getArgument(0));
            return null;
        }).when(tokenRepository).delete(any(MapToken.class));
        when(tileRepository.saveAll(anyIterable())).thenAnswer(invocation -> {
            java.util.List<com.gamer.fowever.tabletopservice.domain.BattleMapTile> tiles =
                    new ArrayList<>();
            ((Iterable<com.gamer.fowever.tabletopservice.domain.BattleMapTile>) invocation.getArgument(0))
                    .forEach(tiles::add);
            for (com.gamer.fowever.tabletopservice.domain.BattleMapTile tile : tiles) {
                if (tile.getId() == null) {
                    tile.setId((long) (savedTiles.size() + 1));
                }
                if (!savedTiles.contains(tile)) {
                    savedTiles.add(tile);
                }
            }
            return tiles;
        });
        when(tileRepository.findByMapId(anyLong()))
                .thenAnswer(invocation -> new ArrayList<>(savedTiles));
        when(tileRepository.findByMapIdOrderByPosYAscPosXAsc(anyLong()))
                .thenAnswer(invocation -> new ArrayList<>(savedTiles));
        org.mockito.Mockito.doAnswer(invocation -> {
            savedTiles.remove(invocation.getArgument(0));
            return null;
        }).when(tileRepository).delete(any(com.gamer.fowever.tabletopservice.domain.BattleMapTile.class));
        when(objectRepository.save(any(BattleMapObject.class))).thenAnswer(invocation -> {
            BattleMapObject object = invocation.getArgument(0);
            if (object.getId() == null) {
                object.setId((long) (savedObjects.size() + 1));
            }
            if (!savedObjects.contains(object)) {
                savedObjects.add(object);
            }
            return object;
        });
        when(objectRepository.findByMapIdOrderByIdAsc(anyLong()))
                .thenAnswer(invocation -> new ArrayList<>(savedObjects));
        when(objectRepository.findByIdAndMapId(anyLong(), anyLong()))
                .thenAnswer(invocation -> savedObjects.stream()
                        .filter(object -> object.getId().equals(invocation.getArgument(0)))
                        .findFirst());
        org.mockito.Mockito.doAnswer(invocation -> {
            savedObjects.remove(invocation.getArgument(0));
            return null;
        }).when(objectRepository).delete(any(BattleMapObject.class));
        when(mapCatalog.hasTile(anyInt())).thenReturn(true);
        when(participantRepository.findBySessionIdAndUserId(eq(SESSION_ID), anyLong()))
                .thenAnswer(invocation -> savedParticipants.stream()
                        .filter(p -> p.getUser().getId().equals(invocation.getArgument(1)))
                        .findFirst());
        when(participantRepository.existsBySessionIdAndUserId(eq(SESSION_ID), anyLong()))
                .thenAnswer(invocation -> savedParticipants.stream()
                        .anyMatch(p -> p.getUser().getId().equals(invocation.getArgument(1))));
        when(participantRepository.findBySessionId(SESSION_ID))
                .thenAnswer(invocation -> new ArrayList<>(savedParticipants));
        when(eventRepository.save(any(SessionEvent.class))).thenAnswer(invocation -> {
            SessionEvent event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId((long) savedEvents.size() + 1);
            }
            savedEvents.add(event);
            return event;
        });
        when(initiativeRepository.save(any(InitiativeEntry.class))).thenAnswer(invocation -> {
            InitiativeEntry entry = invocation.getArgument(0);
            if (entry.getId() == null) {
                entry.setId((long) savedInitiative.size() + 1);
            }
            if (!savedInitiative.contains(entry)) {
                savedInitiative.add(entry);
            }
            return entry;
        });
        when(initiativeRepository.findByBattleMapIdOrderByScoreDescIdAsc(anyLong()))
                .thenAnswer(invocation -> new ArrayList<>(savedInitiative.stream()
                        .sorted(java.util.Comparator
                                .comparingInt(InitiativeEntry::getScore).reversed()
                                .thenComparing(InitiativeEntry::getId))
                        .toList()));
        when(initiativeRepository.findByIdAndBattleMapId(anyLong(), anyLong()))
                .thenAnswer(invocation -> savedInitiative.stream()
                        .filter(entry -> entry.getId().equals(invocation.getArgument(0)))
                        .findFirst());
        org.mockito.Mockito.doAnswer(invocation -> {
            savedInitiative.remove(invocation.getArgument(0));
            return null;
        }).when(initiativeRepository).delete(any(InitiativeEntry.class));
        org.mockito.Mockito.doAnswer(invocation -> {
            ((List<?>) invocation.getArgument(0)).forEach(savedInitiative::remove);
            return null;
        }).when(initiativeRepository).deleteAll(any(Iterable.class));
    }

    private void withGm(User gm, GameSession session) {
        savedParticipants.add(new Participant(session, gm, Role.GM));
    }

    private void withPlayer(User player, GameSession session) {
        savedParticipants.add(new Participant(session, player, Role.PLAYER));
    }

    private void withSpectator(User spectator, GameSession session) {
        savedParticipants.add(new Participant(session, spectator, Role.SPECTATOR));
    }

    private BattleMap storedMap(GameSession session) {
        BattleMap map = new BattleMap(session, "Dungeon", 24, 18, 10);
        map.setId(10L);
        savedMap = map;
        return map;
    }

    private MapToken storedToken(String name, TokenCategory category, String color, int speedFeet,
                                 int x, int y, Long linkedParticipantId, Long linkedUserId) {
        MapToken token = new MapToken(savedMap, name, category, color, speedFeet, x, y,
                linkedParticipantId, linkedUserId);
        token.setId((long) savedTokens.size() + 1);
        savedTokens.add(token);
        return token;
    }

    @Test
    void getMapThrowsNotFoundWhenNoMapExists() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        when(userRepository.findById(1L)).thenReturn(Optional.of(gm));

        assertThatThrownBy(() -> service.getMap(SESSION_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No battle map");
    }

    @Test
    void createMapPlacesPlayerTokenForEachNonSpectatorParticipant() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        User spectator = user(3L, "ted");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        withPlayer(player, session);
        withSpectator(spectator, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        when(userRepository.findById(3L)).thenReturn(Optional.of(spectator));

        BattleMapDto dto = service.createMap(gm, SESSION_ID, new CreateMapRequest("Dungeon", null, null));

        assertThat(dto.name()).isEqualTo("Dungeon");
        assertThat(dto.width()).isEqualTo(24);
        assertThat(dto.height()).isEqualTo(18);
        assertThat(dto.squareFeet()).isEqualTo(10);
        assertThat(dto.tokens()).hasSize(2);
        assertThat(dto.tokens()).extracting(MapTokenDto::category).containsOnly(TokenCategory.PLAYER);
        assertThat(dto.tokens()).extracting(MapTokenDto::speedFeet).containsOnly(30);
        assertThat(dto.tokens()).extracting(MapTokenDto::linkedUserId)
                .containsExactlyInAnyOrder(1L, 2L);
        dto.tokens().forEach(token -> {
            assertThat(token.posX()).isLessThan(24);
            assertThat(token.posY()).isLessThan(18);
        });
        verify(messagingTemplate).convertAndSend(eq("/topic/sessions/" + SESSION_ID), any(SessionEventDto.class));
        assertThat(savedEvents).extracting(SessionEvent::getType).containsExactly(EventType.TABLE);
    }

    @Test
    void createMapDecoratesPlayerTokenWithLinkedCharacterAvatar() throws Exception {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        String appearance = objectMapper.writeValueAsString(new CharacterAppearance(120, 300, "ponytail"));
        when(characterRepository.findFirstByOwnerIdOrderByIdAsc(2L))
                .thenReturn(Optional.of(character(player, "dwarf", Sex.FEMALE, appearance)));

        BattleMapDto dto = service.createMap(gm, SESSION_ID, new CreateMapRequest("Dungeon", null, null));

        MapTokenDto playerToken = dto.tokens().stream()
                .filter(token -> player.getId().equals(token.linkedUserId()))
                .findFirst()
                .orElseThrow();
        assertThat(playerToken.raceIndex()).isEqualTo("dwarf");
        assertThat(playerToken.sex()).isEqualTo(Sex.FEMALE);
        assertThat(playerToken.appearance().skinTone()).isEqualTo(120);
        assertThat(playerToken.appearance().hairTone()).isEqualTo(300);
        assertThat(playerToken.appearance().hairStyle()).isEqualTo("ponytail");

        MapTokenDto gmToken = dto.tokens().stream()
                .filter(token -> gm.getId().equals(token.linkedUserId()))
                .findFirst()
                .orElseThrow();
        assertThat(gmToken.raceIndex()).isNull();
        assertThat(gmToken.sex()).isNull();
        assertThat(gmToken.appearance()).isNull();
    }

    @Test
    void playerTokenAvatarKeepsRaceAndSexWhenAppearanceIsUnparseable() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        when(characterRepository.findFirstByOwnerIdOrderByIdAsc(2L))
                .thenReturn(Optional.of(character(player, "elf", Sex.MALE, "{not-json")));

        BattleMapDto dto = service.createMap(gm, SESSION_ID, new CreateMapRequest("Dungeon", null, null));

        MapTokenDto playerToken = dto.tokens().stream()
                .filter(token -> player.getId().equals(token.linkedUserId()))
                .findFirst()
                .orElseThrow();
        assertThat(playerToken.raceIndex()).isEqualTo("elf");
        assertThat(playerToken.sex()).isEqualTo(Sex.MALE);
        assertThat(playerToken.appearance()).isNull();
    }

    @Test
    void getMapResolvesAvatarsForStoredPlayerTokens() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        storedMap(session);
        storedToken("aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, null, 1L);
        storedToken("ivo", TokenCategory.PLAYER, "#3b82f6", 30, 3, 1, null, 2L);
        storedToken("Goblin", TokenCategory.MONSTER_NPC, "#22c55e", 30, 5, 5, null, null);
        when(characterRepository.findFirstByOwnerIdOrderByIdAsc(2L))
                .thenReturn(Optional.of(character(player, "half-orc", Sex.MALE, null)));

        BattleMapDto dto = service.getMap(SESSION_ID);

        MapTokenDto playerToken = dto.tokens().stream()
                .filter(token -> player.getId().equals(token.linkedUserId()))
                .findFirst()
                .orElseThrow();
        assertThat(playerToken.raceIndex()).isEqualTo("half-orc");
        assertThat(playerToken.sex()).isEqualTo(Sex.MALE);
        assertThat(playerToken.appearance()).isNull();

        MapTokenDto monsterToken = dto.tokens().stream()
                .filter(token -> token.category() == TokenCategory.MONSTER_NPC)
                .findFirst()
                .orElseThrow();
        assertThat(monsterToken.raceIndex()).isNull();
        assertThat(monsterToken.sex()).isNull();
        assertThat(monsterToken.appearance()).isNull();
    }

    private Dnd5eCharacter character(User owner, String raceIndex, Sex sex, String appearanceJson) {
        Dnd5eCharacter character = new Dnd5eCharacter();
        character.setId(100L);
        character.setOwner(owner);
        character.setRaceIndex(raceIndex);
        character.setSex(sex);
        character.setAppearance(appearanceJson);
        return character;
    }

    @Test
    void createMapIsIdempotentWhenMapAlreadyExists() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        BattleMap map = new BattleMap(session, "Old", 24, 18, 10);
        map.setId(10L);
        savedMap = map;

        BattleMapDto dto = service.createMap(gm, SESSION_ID, new CreateMapRequest("New", null, null));

        assertThat(dto.name()).isEqualTo("Old");
        assertThat(savedTokens).isEmpty();
        verify(tokenRepository, never()).save(any());
    }

    @Test
    void createMapRequiresGmRole() {
        User player = user(2L, "ivo");
        GameSession session = session(user(1L, "aria"));
        coreStubs(user(1L, "aria"), session);
        withGm(user(1L, "aria"), session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));

        assertThatThrownBy(() -> service.createMap(player, SESSION_ID, new CreateMapRequest("Dungeon", null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only the GM");
        verify(mapRepository, never()).save(any());
    }

    @Test
    void createMapValidatesSize() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);

        assertThatThrownBy(() -> service.createMap(gm, SESSION_ID, new CreateMapRequest("Big", 0, 18)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Map size");
    }

    @Test
    void addTokenDefaultsToMonsterCategoryWithThirtyFeetSpeed() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        BattleMapDto dto = service.addToken(gm, SESSION_ID, new AddTokenRequest("Goblin", null, null, null, null, null));

        MapTokenDto token = dto.tokens().getFirst();
        assertThat(token.name()).isEqualTo("Goblin");
        assertThat(token.category()).isEqualTo(TokenCategory.MONSTER_NPC);
        assertThat(token.speedFeet()).isEqualTo(30);
        assertThat(token.posX()).isZero();
        assertThat(token.posY()).isZero();
        assertThat(token.movedFeet()).isZero();
    }

    @Test
    void addTokenRejectsOutOfBoundsSquare() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        assertThatThrownBy(() -> service.addToken(gm, SESSION_ID,
                new AddTokenRequest("Goblin", TokenCategory.MONSTER_NPC, null, null, 24, 5)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("outside the map");
    }

    @Test
    void addTokenRejectsInvalidSpeed() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        assertThatThrownBy(() -> service.addToken(gm, SESSION_ID,
                new AddTokenRequest("Goblin", TokenCategory.MONSTER_NPC, null, 1, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Speed must be between");
    }

    @Test
    void updateTokenChangesNameColorAndSpeed() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Goblin", TokenCategory.MONSTER_NPC, "#ef4444", 30, 1, 1, null, null);

        BattleMapDto dto = service.updateToken(gm, SESSION_ID, 1L,
                new UpdateTokenRequest("Kobold King", "#22c55e", 60));

        MapTokenDto token = dto.tokens().getFirst();
        assertThat(token.name()).isEqualTo("Kobold King");
        assertThat(token.color()).isEqualTo("#22c55e");
        assertThat(token.speedFeet()).isEqualTo(60);
    }

    @Test
    void updateTokenRejectsBlankName() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Goblin", TokenCategory.MONSTER_NPC, "#ef4444", 30, 1, 1, null, null);

        assertThatThrownBy(() -> service.updateToken(gm, SESSION_ID, 1L, new UpdateTokenRequest(" ", null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("must not be blank");
    }

    @Test
    void updateMapResizesAndClipsOutOfBoundsTokens() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Goblin", TokenCategory.MONSTER_NPC, "#ef4444", 30, 20, 17, null, null);

        BattleMapDto dto = service.updateMap(gm, SESSION_ID, new UpdateMapRequest(null, 10, 10));

        assertThat(dto.width()).isEqualTo(10);
        assertThat(dto.height()).isEqualTo(10);
        assertThat(dto.tokens().getFirst().posX()).isEqualTo(9);
        assertThat(dto.tokens().getFirst().posY()).isEqualTo(9);
    }

    @Test
    void updateMapRenamesAndIsIdempotentForUnchangedValues() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        BattleMapDto renamed = service.updateMap(gm, SESSION_ID, new UpdateMapRequest("Tower of Grumm", null, null));
        assertThat(renamed.name()).isEqualTo("Tower of Grumm");

        int eventsBefore = savedEvents.size();
        service.updateMap(gm, SESSION_ID, new UpdateMapRequest("Tower of Grumm", 24, 18));
        assertThat(savedEvents).hasSize(eventsBefore);
    }

    @Test
    void updateMapRejectsNonGm() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        storedMap(session);

        assertThatThrownBy(() -> service.updateMap(player, SESSION_ID, new UpdateMapRequest(null, 30, 30)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only the GM");
    }

    @Test
    void updateMapRejectsOutOfRangeSizes() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        assertThatThrownBy(() -> service.updateMap(gm, SESSION_ID, new UpdateMapRequest(null, 300, 5)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("between 1 and 200");
    }

    @Test
    void removeTokenDeletesAndClearsCurrentTurn() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        savedMap.setCurrentTurnTokenId(1L);
        storedToken("Goblin", TokenCategory.MONSTER_NPC, "#ef4444", 30, 1, 1, null, null);

        BattleMapDto dto = service.removeToken(gm, SESSION_ID, 1L);

        assertThat(dto.tokens()).isEmpty();
        assertThat(dto.currentTurnTokenId()).isNull();
    }

    @Test
    void moveTokenWithinBudgetUpdatesPositionAndMovedFeet() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);

        BattleMapDto dto = service.moveToken(gm, SESSION_ID, 1L, new MoveTokenRequest(3, 1));

        MapTokenDto token = dto.tokens().getFirst();
        assertThat(token.posX()).isEqualTo(3);
        assertThat(token.posY()).isEqualTo(1);
        assertThat(token.movedFeet()).isEqualTo(20);
    }

    @Test
    void moveTokenUsesChebyshevDistanceForDiagonals() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 60, 1, 1, 2L, 1L);

        BattleMapDto dto = service.moveToken(gm, SESSION_ID, 1L, new MoveTokenRequest(4, 4));

        assertThat(dto.tokens().getFirst().movedFeet()).isEqualTo(30);
    }

    @Test
    void moveTokenRejectsMoveBeyondRemainingBudget() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        MapToken token = storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);
        token.setMovedFeet(20);

        assertThatThrownBy(() -> service.moveToken(gm, SESSION_ID, 1L, new MoveTokenRequest(5, 1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("exceeds the 10 ft remaining");
        assertThat(savedTokens.getFirst().getPosX()).isEqualTo(1);
    }

    @Test
    void moveTokenRejectsOutOfBoundsDestination() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);

        assertThatThrownBy(() -> service.moveToken(gm, SESSION_ID, 1L, new MoveTokenRequest(24, 0)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("outside the map");
    }

    @Test
    void moveTokenRejectsPlayerMovingAnotherUsersToken() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        storedMap(session);
        storedToken("Other", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 3L, 3L);

        assertThatThrownBy(() -> service.moveToken(player, SESSION_ID, 1L, new MoveTokenRequest(2, 1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("only move your own token");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void moveTokenAllowsPlayerMovingTheirOwnToken() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        storedMap(session);
        storedToken("Ivo", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 2L);

        BattleMapDto dto = service.moveToken(player, SESSION_ID, 1L, new MoveTokenRequest(2, 1));

        assertThat(dto.tokens().getFirst().posX()).isEqualTo(2);
    }

    @Test
    void moveTokenRejectsNonParticipant() {
        User gm = user(1L, "aria");
        User outsider = user(9L, "stranger");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        when(userRepository.findById(9L)).thenReturn(Optional.of(outsider));
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);

        assertThatThrownBy(() -> service.moveToken(outsider, SESSION_ID, 1L, new MoveTokenRequest(2, 1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not a participant");
    }

    @Test
    void placeTokenSetsPositionWithoutSpendingMovement() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);

        BattleMapDto dto = service.placeToken(gm, SESSION_ID, 1L, new MoveTokenRequest(5, 4));

        MapTokenDto token = dto.tokens().getFirst();
        assertThat(token.posX()).isEqualTo(5);
        assertThat(token.posY()).isEqualTo(4);
        assertThat(token.movedFeet()).isZero();
    }

    @Test
    void placeTokenAllowsPlayerToPlaceTheirOwnToken() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        storedMap(session);
        storedToken("Ivo", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 2L);

        BattleMapDto dto = service.placeToken(player, SESSION_ID, 1L, new MoveTokenRequest(3, 3));

        assertThat(dto.tokens().getFirst().posX()).isEqualTo(3);
    }

    @Test
    void placeTokenRejectsPlayerPlacingAnotherUsersToken() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        storedMap(session);
        storedToken("Other", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 3L, 3L);

        assertThatThrownBy(() -> service.placeToken(player, SESSION_ID, 1L, new MoveTokenRequest(2, 1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("only place your own token");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void placeTokenRejectsPlacingOnceCombatHasStarted() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        savedMap.setInitiativeIndex(0);
        storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);

        assertThatThrownBy(() -> service.placeToken(gm, SESSION_ID, 1L, new MoveTokenRequest(5, 4)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("use move instead");
        verify(eventRepository, never()).save(any());
    }

    @Test
    void placeTokenRejectsOutOfBounds() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);

        assertThatThrownBy(() -> service.placeToken(gm, SESSION_ID, 1L, new MoveTokenRequest(24, 0)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("outside the map");
    }

    @Test
    void moveTokenToSameSquareDoesNotPersistEvent() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);

        BattleMapDto dto = service.moveToken(gm, SESSION_ID, 1L, new MoveTokenRequest(1, 1));

        assertThat(dto.tokens().getFirst().movedFeet()).isZero();
        verify(eventRepository, never()).save(any());
        verify(messagingTemplate, never())
                .convertAndSend(anyString(), any(SessionEventDto.class));
    }

    @Test
    void turnStartResetsBudgetAndSetsCurrentTurn() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        MapToken token = storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);
        token.setMovedFeet(20);

        BattleMapDto dto = service.turnCommand(gm, SESSION_ID, new TurnCommandRequest(TurnAction.START, 1L));

        assertThat(dto.currentTurnTokenId()).isEqualTo(1L);
        assertThat(dto.tokens().getFirst().movedFeet()).isZero();
    }

    @Test
    void turnEndClearsCurrentTurn() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        savedMap.setCurrentTurnTokenId(1L);
        storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);

        BattleMapDto dto = service.turnCommand(gm, SESSION_ID, new TurnCommandRequest(TurnAction.END, null));

        assertThat(dto.currentTurnTokenId()).isNull();
    }

    @Test
    void turnNewRoundResetsAllBudgets() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        savedMap.setCurrentTurnTokenId(2L);
        MapToken first = storedToken("Aria", TokenCategory.PLAYER, "#ef4444", 30, 1, 1, 2L, 1L);
        first.setMovedFeet(20);
        MapToken second = storedToken("Goblin", TokenCategory.MONSTER_NPC, "#ef4444", 30, 2, 2, null, null);
        second.setMovedFeet(30);

        BattleMapDto dto = service.turnCommand(gm, SESSION_ID, new TurnCommandRequest(TurnAction.NEW_ROUND, null));

        assertThat(dto.tokens()).extracting(MapTokenDto::movedFeet).containsOnly(0);
        assertThat(dto.currentTurnTokenId()).isNull();
    }

    @Test
    void turnCommandRequiresGm() {
        User player = user(2L, "ivo");
        GameSession session = session(user(1L, "aria"));
        coreStubs(user(1L, "aria"), session);
        withGm(user(1L, "aria"), session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        storedMap(session);

        assertThatThrownBy(() -> service.turnCommand(player, SESSION_ID, new TurnCommandRequest(TurnAction.END, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only the GM");
    }

    @Test
    void turnStartRejectsMissingToken() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        assertThatThrownBy(() -> service.turnCommand(gm, SESSION_ID, new TurnCommandRequest(TurnAction.START, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("tokenId is required");
    }

    @Test
    void setInitiativeReplacesEntriesAndAutoRollsMissingScores() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        MapToken token = storedToken("Goblin", TokenCategory.MONSTER_NPC, "#ef4444", 30, 2, 2, null, null);
        storedToken("Aria", TokenCategory.PLAYER, "#3b82f6", 30, 1, 1, 2L, 1L);

        InitiativeRequest request = new InitiativeRequest(List.of(
                new InitiativeEntryRequest("Goblin", null, 15),
                new InitiativeEntryRequest(null, token.getId(), null),
                new InitiativeEntryRequest("Aria", null, 99),
                new InitiativeEntryRequest("Bob", null, 99)));

        BattleMapDto dto = service.setInitiative(gm, SESSION_ID, request);

        assertThat(dto.initiative()).hasSize(4);
        assertThat(dto.initiative().get(0).label()).isEqualTo("Aria");
        assertThat(dto.initiative().get(1).label()).isEqualTo("Bob");
        InitiativeEntryDto autoRolled = dto.initiative().stream()
                .filter(entry -> entry.tokenName() != null).findFirst().orElseThrow();
        assertThat(autoRolled.tokenName()).isEqualTo("Goblin");
        assertThat(autoRolled.score()).isBetween(1, 20);
        assertThat(dto.initiative().stream().map(InitiativeEntryDto::label))
                .containsExactlyInAnyOrder("Aria", "Bob", "Goblin", null);
        assertThat(dto.initiativeIndex()).isEqualTo(-1);
    }

    @Test
    void setInitiativeRejectsEntryWithoutLabelOrToken() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        MapToken goblin = storedToken("Goblin", TokenCategory.MONSTER_NPC, "#ef4444", 30, 2, 2, null, null);

        InitiativeRequest request = new InitiativeRequest(
                List.of(new InitiativeEntryRequest("Goblin", goblin.getId(), null)));

        assertThatThrownBy(() -> service.setInitiative(gm, SESSION_ID, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("label or a tokenId");
    }

    @Test
    void setInitiativeRejectsEntryWithNeitherLabelNorToken() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        InitiativeRequest request = new InitiativeRequest(
                List.of(new InitiativeEntryRequest(null, null, null)));

        assertThatThrownBy(() -> service.setInitiative(gm, SESSION_ID, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("label or a tokenId");
    }

    @Test
    void setInitiativeRejectsUnknownToken() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        InitiativeRequest request = new InitiativeRequest(
                List.of(new InitiativeEntryRequest(null, 999L, null)));

        assertThatThrownBy(() -> service.setInitiative(gm, SESSION_ID, request))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Map token not found");
    }

    @Test
    void setInitiativeRequiresGm() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        withPlayer(player, session);
        when(userRepository.findById(2L)).thenReturn(Optional.of(player));
        storedMap(session);

        assertThatThrownBy(() ->
                service.setInitiative(player, SESSION_ID, new InitiativeRequest(List.of())))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only the GM");
    }

    @Test
    void nextInitiativeAdvancesAndActivatesTheToken() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        MapToken goblin = storedToken("Goblin", TokenCategory.MONSTER_NPC, "#ef4444", 30, 2, 2, null, null);
        goblin.setMovedFeet(20);

        InitiativeRequest request = new InitiativeRequest(List.of(
                new InitiativeEntryRequest(null, goblin.getId(), 12),
                new InitiativeEntryRequest("Orc", null, 5)));
        service.setInitiative(gm, SESSION_ID, request);

        BattleMapDto dto = service.nextInitiative(gm, SESSION_ID);

        assertThat(dto.initiativeIndex()).isEqualTo(0);
        assertThat(dto.currentTurnTokenId()).isEqualTo(goblin.getId());
        assertThat(dto.tokens()).filteredOn(t -> t.id().equals(goblin.getId()))
                .singleElement()
                .extracting(MapTokenDto::movedFeet)
                .isEqualTo(0);
    }

    @Test
    void nextInitiativeWrapsAround() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        InitiativeRequest request = new InitiativeRequest(List.of(
                new InitiativeEntryRequest("Aria", null, 10),
                new InitiativeEntryRequest("Bob", null, 5)));
        service.setInitiative(gm, SESSION_ID, request);
        savedMap.setInitiativeIndex(1);

        BattleMapDto dto = service.nextInitiative(gm, SESSION_ID);

        assertThat(dto.initiativeIndex()).isEqualTo(0);
        assertThat(dto.currentTurnTokenId()).isNull();
    }

    @Test
    void nextInitiativeRejectsEmptyOrder() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        assertThatThrownBy(() -> service.nextInitiative(gm, SESSION_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("no initiative order");
    }

    @Test
    void rerollInitiativeAssignsANewScore() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        InitiativeRequest request = new InitiativeRequest(
                List.of(new InitiativeEntryRequest("Orc", null, 15)));
        service.setInitiative(gm, SESSION_ID, request);
        InitiativeEntry entry = savedInitiative.get(0);

        BattleMapDto dto = service.rerollInitiative(gm, SESSION_ID, entry.getId());

        assertThat(dto.initiative()).singleElement().extracting(InitiativeEntryDto::score)
                .isNotNull();
        assertThat(savedInitiative).singleElement().extracting(InitiativeEntry::getScore)
                .isEqualTo(dto.initiative().get(0).score());
    }

    @Test
    void removeInitiativeEntryClampsTheIndex() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        InitiativeRequest request = new InitiativeRequest(List.of(
                new InitiativeEntryRequest("Aria", null, 10),
                new InitiativeEntryRequest("Bob", null, 5)));
        service.setInitiative(gm, SESSION_ID, request);
        savedMap.setInitiativeIndex(1);

        BattleMapDto dto = service.removeInitiativeEntry(gm, SESSION_ID, savedInitiative.get(0).getId());

        assertThat(dto.initiative()).hasSize(1);
        assertThat(dto.initiativeIndex()).isEqualTo(0);
    }

    @Test
    void removeLastInitiativeEntryResetsIndex() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        InitiativeRequest request = new InitiativeRequest(
                List.of(new InitiativeEntryRequest("Orc", null, 5)));
        service.setInitiative(gm, SESSION_ID, request);
        savedMap.setInitiativeIndex(0);

        BattleMapDto dto = service.removeInitiativeEntry(gm, SESSION_ID, savedInitiative.get(0).getId());

        assertThat(dto.initiative()).isEmpty();
        assertThat(dto.initiativeIndex()).isEqualTo(-1);
    }

    @Test
    void getCatalogExposesPalette() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        when(mapCatalog.tiles()).thenReturn(List.of(
                new com.gamer.fowever.tabletopservice.service.map.MapCatalog.TileType(0, "Stone Floor", "#9ca3af"),
                new com.gamer.fowever.tabletopservice.service.map.MapCatalog.TileType(1, "Grass", "#4ade80")));

        MapCatalogDto catalog = service.getCatalog(SESSION_ID);

        assertThat(catalog.tiles()).hasSize(2);
        assertThat(catalog.tiles().get(0).index()).isZero();
        assertThat(catalog.tiles().get(0).name()).isEqualTo("Stone Floor");
        assertThat(catalog.tiles().get(0).color()).isEqualTo("#9ca3af");
        assertThat(catalog.tiles().get(1).name()).isEqualTo("Grass");
    }

    @Test
    void getCatalogThrowsWhenSessionMissing() {
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getCatalog(SESSION_ID))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Session not found");
    }

    @Test
    void paintTilesStoresPaintedSquares() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        BattleMapDto dto = service.paintTiles(gm, SESSION_ID, new PaintTilesRequest(List.of(
                new MapTileDto(2, 3, 0), new MapTileDto(4, 5, 7))));

        assertThat(dto.tiles()).hasSize(2);
        assertThat(dto.tiles()).extracting(MapTileDto::x).containsExactly(2, 4);
        assertThat(dto.tiles()).extracting(MapTileDto::tileIndex).containsExactly(0, 7);
    }

    @Test
    void paintTilesOverwritesExistingSquare() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        service.paintTiles(gm, SESSION_ID, new PaintTilesRequest(List.of(new MapTileDto(0, 0, 0))));
        BattleMapDto dto = service.paintTiles(gm, SESSION_ID,
                new PaintTilesRequest(List.of(new MapTileDto(0, 0, 5))));

        assertThat(dto.tiles()).hasSize(1);
        assertThat(dto.tiles().get(0).tileIndex()).isEqualTo(5);
    }

    @Test
    void paintTilesWithNullIndexErasesSquare() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        service.paintTiles(gm, SESSION_ID, new PaintTilesRequest(List.of(new MapTileDto(0, 0, 0))));
        BattleMapDto dto = service.paintTiles(gm, SESSION_ID,
                new PaintTilesRequest(java.util.Arrays.asList(new MapTileDto(0, 0, null))));

        assertThat(dto.tiles()).isEmpty();
    }

    @Test
    void paintTilesRejectsUnknownTileIndex() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        when(mapCatalog.hasTile(99)).thenReturn(false);

        assertThatThrownBy(() -> service.paintTiles(gm, SESSION_ID,
                new PaintTilesRequest(List.of(new MapTileDto(0, 0, 99)))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown tile index");
    }

    @Test
    void paintTilesRejectsOutOfBoundsSquare() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        assertThatThrownBy(() -> service.paintTiles(gm, SESSION_ID,
                new PaintTilesRequest(List.of(new MapTileDto(99, 0, 0)))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("outside the map");
    }

    @Test
    void paintTilesRequiresGm() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(player, session);
        withGm(gm, session);
        withPlayer(player, session);
        storedMap(session);

        assertThatThrownBy(() -> service.paintTiles(player, SESSION_ID,
                new PaintTilesRequest(List.of(new MapTileDto(0, 0, 0)))))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only the GM");
    }

    @Test
    void paintTilesWithEmptyBatchIsNoOp() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        BattleMapDto dto = service.paintTiles(gm, SESSION_ID, new PaintTilesRequest(List.of()));

        assertThat(dto.tiles()).isEmpty();
        verify(eventRepository, never()).save(any(SessionEvent.class));
    }

    @Test
    void resizingMapPrunesOutOfBoundsTiles() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        service.paintTiles(gm, SESSION_ID, new PaintTilesRequest(List.of(
                new MapTileDto(5, 5, 0), new MapTileDto(2, 2, 1))));
        BattleMapDto dto = service.updateMap(gm, SESSION_ID, new UpdateMapRequest(null, 3, 3));

        assertThat(dto.tiles()).hasSize(1);
        assertThat(dto.tiles().get(0).x()).isEqualTo(2);
        assertThat(dto.tiles().get(0).y()).isEqualTo(2);
    }

    @Test
    void initiativeEntryMustBelongToTheMap() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);

        assertThatThrownBy(() -> service.rerollInitiative(gm, SESSION_ID, 999L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Initiative entry not found");
    }

    private void stubObjectType(int index, String name, int width, int height, boolean blocksMovement) {
        when(mapCatalog.object(index)).thenReturn(java.util.Optional.of(
                new com.gamer.fowever.tabletopservice.service.map.MapCatalog.ObjectType(
                        index, name, "#6b7280", width, height, blocksMovement)));
        when(mapCatalog.hasObject(index)).thenReturn(true);
    }

    @Test
    void getCatalogExposesObjectDefs() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        when(mapCatalog.objects()).thenReturn(List.of(
                new com.gamer.fowever.tabletopservice.service.map.MapCatalog.ObjectType(
                        0, "Wall", "#6b7280", 1, 1, true)));

        MapCatalogDto catalog = service.getCatalog(SESSION_ID);

        assertThat(catalog.objects()).hasSize(1);
        assertThat(catalog.objects().get(0).name()).isEqualTo("Wall");
        assertThat(catalog.objects().get(0).blocksMovement()).isTrue();
    }

    @Test
    void addObjectStoresCatalogFootprintAndDefaultBlocking() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        stubObjectType(4, "Table", 2, 2, true);

        BattleMapDto dto = service.addObject(gm, SESSION_ID, new AddObjectRequest(4, 3, 5, null, null));

        assertThat(dto.objects()).hasSize(1);
        MapObjectDto object = dto.objects().get(0);
        assertThat(object.objectIndex()).isEqualTo(4);
        assertThat(object.x()).isEqualTo(3);
        assertThat(object.y()).isEqualTo(5);
        assertThat(object.w()).isEqualTo(2);
        assertThat(object.h()).isEqualTo(2);
        assertThat(object.rotation()).isZero();
        assertThat(object.blocksMovement()).isTrue();
    }

    @Test
    void addObjectHonorsBlocksMovementOverride() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        stubObjectType(0, "Wall", 1, 1, true);

        BattleMapDto dto = service.addObject(gm, SESSION_ID, new AddObjectRequest(0, 0, 0, 90, false));

        assertThat(dto.objects().get(0).blocksMovement()).isFalse();
        assertThat(dto.objects().get(0).rotation()).isEqualTo(90);
    }

    @Test
    void addObjectRejectsUnknownObjectIndex() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        when(mapCatalog.object(99)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.addObject(gm, SESSION_ID, new AddObjectRequest(99, 0, 0, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Unknown object index");
    }

    @Test
    void addObjectRejectsFootprintOutOfBounds() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        stubObjectType(4, "Table", 2, 2, true);

        assertThatThrownBy(() -> service.addObject(gm, SESSION_ID, new AddObjectRequest(4, 23, 17, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("footprint is outside the map");
    }

    @Test
    void addObjectRequiresGm() {
        User gm = user(1L, "aria");
        User player = user(2L, "ivo");
        GameSession session = session(gm);
        coreStubs(player, session);
        withGm(gm, session);
        withPlayer(player, session);
        storedMap(session);

        assertThatThrownBy(() -> service.addObject(player, SESSION_ID, new AddObjectRequest(0, 0, 0, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Only the GM");
    }

    @Test
    void updateObjectMovesRotatesAndTogglesBlocking() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        stubObjectType(0, "Wall", 1, 1, true);
        BattleMapDto created = service.addObject(gm, SESSION_ID, new AddObjectRequest(0, 0, 0, null, null));
        Long objectId = created.objects().get(0).id();

        BattleMapDto dto = service.updateObject(gm, SESSION_ID, objectId,
                new UpdateObjectRequest(5, 6, 180, false));

        MapObjectDto object = dto.objects().get(0);
        assertThat(object.x()).isEqualTo(5);
        assertThat(object.y()).isEqualTo(6);
        assertThat(object.rotation()).isEqualTo(180);
        assertThat(object.blocksMovement()).isFalse();
    }

    @Test
    void updateObjectRejectsFootprintOutOfBounds() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        stubObjectType(4, "Table", 2, 2, true);
        BattleMapDto created = service.addObject(gm, SESSION_ID, new AddObjectRequest(4, 0, 0, null, null));
        Long objectId = created.objects().get(0).id();

        assertThatThrownBy(() -> service.updateObject(gm, SESSION_ID, objectId,
                new UpdateObjectRequest(30, 0, null, null)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("footprint is outside the map");
    }

    @Test
    void removeObjectDeletesIt() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        stubObjectType(0, "Wall", 1, 1, true);
        BattleMapDto created = service.addObject(gm, SESSION_ID, new AddObjectRequest(0, 1, 1, null, null));

        BattleMapDto dto = service.removeObject(gm, SESSION_ID, created.objects().get(0).id());

        assertThat(dto.objects()).isEmpty();
    }

    @Test
    void resizingMapPrunesOutOfBoundsObjects() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        stubObjectType(0, "Wall", 1, 1, true);
        service.addObject(gm, SESSION_ID, new AddObjectRequest(0, 5, 5, null, null));
        service.addObject(gm, SESSION_ID, new AddObjectRequest(0, 2, 2, null, null));

        BattleMapDto dto = service.updateMap(gm, SESSION_ID, new UpdateMapRequest(null, 3, 3));

        assertThat(dto.objects()).hasSize(1);
        assertThat(dto.objects().get(0).x()).isEqualTo(2);
    }

    @Test
    void moveTokenRejectsBlockedDestination() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#3b82f6", 30, 0, 0, null, 1L);
        stubObjectType(0, "Wall", 1, 1, true);
        service.addObject(gm, SESSION_ID, new AddObjectRequest(0, 3, 0, null, null));

        assertThatThrownBy(() -> service.moveToken(gm, SESSION_ID, 1L, new MoveTokenRequest(3, 0)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("blocked by an object");
    }

    @Test
    void moveTokenRejectsPathBlockedByWall() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#3b82f6", 30, 0, 0, null, 1L);
        stubObjectType(0, "Wall", 1, 18, true);
        service.addObject(gm, SESSION_ID, new AddObjectRequest(0, 1, 0, null, null));

        assertThatThrownBy(() -> service.moveToken(gm, SESSION_ID, 1L, new MoveTokenRequest(2, 0)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("No clear path");
    }

    @Test
    void placeTokenRejectsBlockedSquare() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        storedToken("Aria", TokenCategory.PLAYER, "#3b82f6", 30, 0, 0, null, 1L);
        stubObjectType(0, "Wall", 1, 1, true);
        service.addObject(gm, SESSION_ID, new AddObjectRequest(0, 4, 4, null, null));

        assertThatThrownBy(() -> service.placeToken(gm, SESSION_ID, 1L, new MoveTokenRequest(4, 4)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("blocked by an object");
    }

    @Test
    void addTokenRejectsBlockedSquare() {
        User gm = user(1L, "aria");
        GameSession session = session(gm);
        coreStubs(gm, session);
        withGm(gm, session);
        storedMap(session);
        stubObjectType(0, "Wall", 1, 1, true);
        service.addObject(gm, SESSION_ID, new AddObjectRequest(0, 2, 2, null, null));

        assertThatThrownBy(() -> service.addToken(gm, SESSION_ID,
                new AddTokenRequest("Goblin", TokenCategory.MONSTER_NPC, "#ef4444", 30, 2, 2)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("blocked by an object");
    }
}