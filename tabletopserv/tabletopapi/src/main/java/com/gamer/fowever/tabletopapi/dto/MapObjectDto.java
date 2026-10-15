package com.gamer.fowever.tabletopapi.dto;

/**
 * A placed environment item on a battle map (design-draft-v2 §6, Slice 3b).
 * The footprint is an axis-aligned {@code w}×{@code h} block of squares anchored
 * at {@code (x, y)}; {@code blocksMovement} marks those squares impassable.
 * {@code rotation} is a visual quarter-turn (0/90/180/270) and does not change
 * the blocking footprint (MVP).
 */
public record MapObjectDto(Long id, int objectIndex, int x, int y,
                           int w, int h, int rotation, boolean blocksMovement) {
}
