package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.ScoreSource;
import jakarta.validation.constraints.NotNull;

/**
 * Options for a server-side ability-score roll. Only the rolled sources
 * ({@link ScoreSource#FOUR_D6_DROP_LOWEST}, {@link ScoreSource#HOUSE_RULE_D20})
 * are accepted; the rewarded methods (standard array, point buy) are assigned,
 * never rolled.
 *
 * @param scoreSource the rolled method to use
 * @param seed        nonzero for deterministic rolls (tests); zero uses the secure RNG
 */
public record RollScoresRequest(
        @NotNull ScoreSource scoreSource,
        Integer seed) {

    public int seedOrDefault() {
        return seed != null && seed != 0 ? seed : 0;
    }
}