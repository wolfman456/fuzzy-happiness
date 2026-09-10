package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.ScoreSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Options for the random quick-build ("surprise me"): the server assembles a
 * legal random draft. A nonzero {@code seed} makes the build deterministic for
 * tests; zero uses the secure RNG.
 */
public record GenerateCharacterRequest(
        String name,
        ScoreSource scoreSource,
        @Min(1) @Max(3) Integer startingLevel,
        Integer seed) {

    public ScoreSource scoreSourceOrDefault() {
        return scoreSource != null ? scoreSource : ScoreSource.STANDARD_ARRAY;
    }

    public int startingLevelOrDefault() {
        return startingLevel != null ? startingLevel : 1;
    }

    public int seedOrDefault() {
        return seed != null && seed != 0 ? seed : 0;
    }
}