package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Place a catalog object on a battle map (design-draft-v2 §6, Slice 3b).
 * {@code objectIndex} references the {@code MapCatalog}; the footprint and the
 * default blocking flag come from the catalog unless {@code blocksMovement} is
 * supplied. {@code rotation} is a visual quarter-turn.
 */
public record AddObjectRequest(
        @NotNull @Min(0) Integer objectIndex,
        @NotNull @Min(0) Integer x,
        @NotNull @Min(0) Integer y,
        @Min(0) Integer rotation,
        Boolean blocksMovement) {
}
