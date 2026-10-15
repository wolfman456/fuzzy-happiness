package com.gamer.fowever.tabletopapi.dto;

/**
 * A placeable environment item from the hand-curated {@code MapCatalog}
 * (design-draft-v2 §6, Slice 3b). Carries the default footprint
 * ({@code width}×{@code height} squares) and whether the object blocks movement
 * by default; the GM may override {@code blocksMovement} per placed instance.
 */
public record MapObjectTypeDto(int index, String name, String color,
                               int width, int height, boolean blocksMovement) {
}
