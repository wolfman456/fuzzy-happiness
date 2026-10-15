package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * A single painted grid square (design-draft-v2 §6, Slice 3a). {@code tileIndex}
 * references a palette entry from {@link MapTileTypeDto}; a {@code null} index
 * means "erase this square".
 */
public record MapTileDto(
        @NotNull @Min(0) Integer x,
        @NotNull @Min(0) Integer y,
        @Min(0) Integer tileIndex) {
}
