package com.gamer.fowever.tabletopserv.dto;

import com.gamer.fowever.tabletopserv.domain.BattleMap;
import com.gamer.fowever.tabletopserv.domain.InitiativeEntry;
import com.gamer.fowever.tabletopserv.domain.MapToken;

import java.util.List;

public record BattleMapDto(
        Long id,
        Long sessionId,
        String name,
        int width,
        int height,
        int squareFeet,
        Long currentTurnTokenId,
        int initiativeIndex,
        List<InitiativeEntryDto> initiative,
        List<MapTokenDto> tokens) {

    public static BattleMapDto from(BattleMap map, List<MapToken> tokens, List<InitiativeEntry> initiative) {
        return new BattleMapDto(
                map.getId(),
                map.getSession().getId(),
                map.getName(),
                map.getWidth(),
                map.getHeight(),
                map.getSquareFeet(),
                map.getCurrentTurnTokenId(),
                map.getInitiativeIndex(),
                initiative.stream().map(InitiativeEntryDto::from).toList(),
                tokens.stream().map(MapTokenDto::from).toList());
    }
}