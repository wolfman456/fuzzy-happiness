package com.gamer.fowever.tabletopapi.dto;

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
}