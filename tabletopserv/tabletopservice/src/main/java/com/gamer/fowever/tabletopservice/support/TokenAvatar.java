package com.gamer.fowever.tabletopservice.support;

import com.gamer.fowever.tabletopapi.Sex;
import com.gamer.fowever.tabletopapi.dto.CharacterAppearance;

/**
 * The avatar coordinates of a battle-map token's linked player, resolved from
 * that player's character (design-draft-v2 §5). Used to decorate
 * {@code MapTokenDto} so the map can render the character's portrait; null
 * fields mean the player has no character and the token keeps its colour disc.
 */
public record TokenAvatar(String raceIndex, Sex sex, CharacterAppearance appearance) {
}
