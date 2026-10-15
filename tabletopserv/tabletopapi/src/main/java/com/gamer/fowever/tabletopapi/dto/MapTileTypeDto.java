package com.gamer.fowever.tabletopapi.dto;

/**
 * A paintable terrain swatch from the hand-curated {@code MapCatalog}
 * (design-draft-v2 §6, Slice 3a). Names/colours only — no rulebook text, the
 * {@code ChargenCatalog} precedent.
 */
public record MapTileTypeDto(int index, String name, String color) {
}
