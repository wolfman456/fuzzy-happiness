package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.ScoreSource;

/**
 * The six pre-racial base scores produced by a server-side roll, laid out in
 * {@code str/dex/con/int/wis/cha} order. These are base scores — the race's
 * ability bonuses are applied later when a race is chosen.
 */
public record RollScoresResult(
        ScoreSource scoreSource,
        int strength,
        int dexterity,
        int constitution,
        int intelligence,
        int wisdom,
        int charisma) {
}