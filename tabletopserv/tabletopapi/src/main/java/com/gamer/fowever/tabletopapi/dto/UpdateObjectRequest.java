package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.constraints.Min;

/**
 * Patch a placed battle-map object (design-draft-v2 §6, Slice 3b). Only the
 * supplied fields change; {@code null} leaves a field untouched. Move and
 * rotate are visual/positional — the catalog footprint ({@code w}/{@code h}) is
 * fixed for the object's lifetime.
 */
public record UpdateObjectRequest(
        @Min(0) Integer x,
        @Min(0) Integer y,
        @Min(0) Integer rotation,
        Boolean blocksMovement) {
}
