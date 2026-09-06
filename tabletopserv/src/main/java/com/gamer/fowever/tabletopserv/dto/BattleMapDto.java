package com.gamer.fowever.tabletopserv.dto;

import com.gamer.fowever.tabletopserv.domain.BattleMap;
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
        List<MapTokenDto> tokens) {

    public static BattleMapDto from(BattleMap map, List<MapToken> tokens) {
        return new BattleMapDto(
                map.getId(),
                map.getSession().getId(),
                map.getName(),
                map.getWidth(),
                map.getHeight(),
                map.getSquareFeet(),
                map.getCurrentTurnTokenId(),
                tokens.stream().map(MapTokenDto::from).toList());
    }
}