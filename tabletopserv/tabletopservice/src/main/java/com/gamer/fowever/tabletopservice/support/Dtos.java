package com.gamer.fowever.tabletopservice.support;

import com.gamer.fowever.tabletopapi.dto.BattleMapDto;
import com.gamer.fowever.tabletopapi.dto.GameSummary;
import com.gamer.fowever.tabletopapi.dto.InitiativeEntryDto;
import com.gamer.fowever.tabletopapi.dto.MapTileDto;
import com.gamer.fowever.tabletopapi.dto.MapTokenDto;
import com.gamer.fowever.tabletopapi.dto.ParticipantSummary;
import com.gamer.fowever.tabletopapi.dto.UserSummary;
import com.gamer.fowever.tabletopservice.domain.BattleMap;
import com.gamer.fowever.tabletopservice.domain.BattleMapTile;
import com.gamer.fowever.tabletopservice.domain.Game;
import com.gamer.fowever.tabletopservice.domain.InitiativeEntry;
import com.gamer.fowever.tabletopservice.domain.MapToken;
import com.gamer.fowever.tabletopservice.domain.Participant;
import com.gamer.fowever.tabletopservice.domain.User;

import java.util.List;
import java.util.Map;

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

    public static MapTokenDto mapToken(MapToken token, Map<Long, TokenAvatar> avatarsByUserId) {
        TokenAvatar avatar = token.getLinkedUserId() == null
                ? null
                : avatarsByUserId.get(token.getLinkedUserId());
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
                token.getLinkedUserId(),
                avatar == null ? null : avatar.raceIndex(),
                avatar == null ? null : avatar.sex(),
                avatar == null ? null : avatar.appearance());
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

    public static List<MapTokenDto> mapTokens(List<MapToken> tokens, Map<Long, TokenAvatar> avatarsByUserId) {
        return tokens.stream().map(token -> mapToken(token, avatarsByUserId)).toList();
    }

    public static MapTileDto mapTile(BattleMapTile tile) {
        return new MapTileDto(tile.getPosX(), tile.getPosY(), tile.getTileIndex());
    }

    public static List<MapTileDto> mapTiles(List<BattleMapTile> tiles) {
        return tiles.stream().map(Dtos::mapTile).toList();
    }

    public static BattleMapDto battleMap(BattleMap map, List<MapToken> tokens, List<InitiativeEntry> initiative,
                                         Map<Long, TokenAvatar> avatarsByUserId, List<BattleMapTile> tiles) {
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
                mapTokens(tokens, avatarsByUserId),
                mapTiles(tiles));
    }
}