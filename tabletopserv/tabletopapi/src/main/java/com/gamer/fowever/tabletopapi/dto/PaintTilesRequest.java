package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Bulk tile paint for a battle map (design-draft-v2 §6, Slice 3a). Each entry
 * sets (or erases, when {@code tileIndex} is null) one square; the whole batch
 * is applied atomically by {@code PUT /api/sessions/{id}/map/tiles}.
 */
public record PaintTilesRequest(@NotNull List<@Valid MapTileDto> tiles) {
}
