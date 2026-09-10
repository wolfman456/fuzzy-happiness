package com.gamer.fowever.tabletopservice.support;

import com.gamer.fowever.tabletopapi.dto.BattleMapDto;
import com.gamer.fowever.tabletopapi.dto.GameSummary;
import com.gamer.fowever.tabletopapi.dto.InitiativeEntryDto;
import com.gamer.fowever.tabletopapi.dto.MapTokenDto;
import com.gamer.fowever.tabletopapi.dto.ParticipantSummary;
import com.gamer.fowever.tabletopapi.dto.UserSummary;
import com.gamer.fowever.tabletopservice.domain.BattleMap;
import com.gamer.fowever.tabletopservice.domain.Game;
import com.gamer.fowever.tabletopservice.domain.InitiativeEntry;
import com.gamer.fowever.tabletopservice.domain.MapToken;
import com.gamer.fowever.tabletopservice.domain.Participant;
import com.gamer.fowever.tabletopservice.domain.User;

import java.util.List;

public final class Dtos {

    private Dtos() {
    }

    public static UserSummary userSummary(User user) {
        return new UserSummary(user.getId(), user.getUsername(), user.getDisplayName(), user.getRealName(),
                user.getEmail(), user.getAuthRole(), user.isEmailVerified());
    }

    public static GameSummary gameSummary(Game game) {
        return new GameSummary(game.getSlug(), game.getDisplayName(), game.getSheetSchema());
    }

    public static ParticipantSummary participantSummary(Participant participant) {
        return new ParticipantSummary(userSummary(participant.getUser()), participant.getRole(), participant.getJoinedAt());
    }

    public static MapTokenDto mapToken(MapToken token) {
        return new MapTokenDto(
                token.getId(),
                token.getName(),
                token.getCategory(),
                token.getColor(),
                token.getSpeedFeet(),
                token.getPosX(),
                token.getPosY(),
                token.getMovedFeet(),
                token.getLinkedParticipantId(),
                token.getLinkedUserId());
    }

    public static InitiativeEntryDto initiativeEntry(InitiativeEntry entry) {
        return new InitiativeEntryDto(
                entry.getId(),
                entry.getLabel(),
                entry.getToken() != null ? entry.getToken().getId() : null,
                entry.getToken() != null ? entry.getToken().getName() : null,
                entry.getScore());
    }

    public static List<InitiativeEntryDto> initiativeEntries(List<InitiativeEntry> entries) {
        return entries.stream().map(Dtos::initiativeEntry).toList();
    }

    public static List<MapTokenDto> mapTokens(List<MapToken> tokens) {
        return tokens.stream().map(Dtos::mapToken).toList();
    }

    public static BattleMapDto battleMap(BattleMap map, List<MapToken> tokens, List<InitiativeEntry> initiative) {
        return new BattleMapDto(
                map.getId(),
                map.getSession().getId(),
                map.getName(),
                map.getWidth(),
                map.getHeight(),
                map.getSquareFeet(),
                map.getCurrentTurnTokenId(),
                map.getInitiativeIndex(),
                initiativeEntries(initiative),
                mapTokens(tokens));
    }
}