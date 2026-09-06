package com.gamer.fowever.tabletopserv.dto;

import com.gamer.fowever.tabletopserv.domain.MapToken;
import com.gamer.fowever.tabletopserv.domain.TokenCategory;

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

    public static MapTokenDto from(MapToken token) {
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
}