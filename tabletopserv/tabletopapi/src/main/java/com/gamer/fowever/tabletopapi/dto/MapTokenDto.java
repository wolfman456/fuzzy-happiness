package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.Sex;
import com.gamer.fowever.tabletopapi.TokenCategory;

/**
 * A token on the battle map. Player tokens are linked to a user
 * ({@code linkedUserId}); for those the token also carries the linked player's
 * character avatar ({@code raceIndex}/{@code sex}/{@code appearance}) so the map
 * can render the character's portrait instead of the colour disc
 * (design-draft-v2 §5). Monster/NPC tokens leave those null.
 */
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
        Long linkedUserId,
        String raceIndex,
        Sex sex,
        CharacterAppearance appearance) {
}
