package com.gamer.fowever.tabletopservice.service;

import com.gamer.fowever.tabletopservice.domain.BattleMap;
import com.gamer.fowever.tabletopapi.EventType;
import com.gamer.fowever.tabletopservice.domain.Game;
import com.gamer.fowever.tabletopservice.domain.GameSession;
import com.gamer.fowever.tabletopservice.domain.InitiativeEntry;
import com.gamer.fowever.tabletopservice.domain.MapToken;
import com.gamer.fowever.tabletopservice.domain.Participant;
import com.gamer.fowever.tabletopapi.Role;
import com.gamer.fowever.tabletopservice.domain.SessionEvent;
import com.gamer.fowever.tabletopapi.SessionStatus;
import com.gamer.fowever.tabletopapi.TokenCategory;
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
    private InitiativeEntryRepository initiativeRepository;
    @Mock
    private ParticipantRepository participantRepository;
    @Mock
    private GameSessionRepository sessionRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SessionEventRepository eventRepository;
    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private final List<Participant> savedParticipants = new ArrayList<>();
    private final List<MapToken> savedTokens = new ArrayList<>();
    private final List<SessionEvent> savedEvents = new ArrayList<>();
    private final List<InitiativeEntry> savedInitiative = new ArrayList<>();
    private BattleMap savedMap;

    private BattleMapService service;

    @BeforeEach
    void setUp() {
        service = new BattleMapService(mapRepository, tokenRepository, initiativeRepository,
                participantRepository, sessionRepository, userRepository, eventRepository,
                messagingTemplate, objectMapper);
        savedParticipants.clear();
        savedTokens.clear();
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
}