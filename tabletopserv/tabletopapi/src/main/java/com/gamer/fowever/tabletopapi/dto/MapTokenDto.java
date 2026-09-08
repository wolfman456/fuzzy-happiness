package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.TokenCategory;

public record MapTokenDto(
        Long id,
        String name,
        TokenCategory category,
        String color,
        int speedFeet,
        int posX,
        int posY,
        int movedFeet,
        Long linkedParticipantId,
        Long linkedUserId) {
}