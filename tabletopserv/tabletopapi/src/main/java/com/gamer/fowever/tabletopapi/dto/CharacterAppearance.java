package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Cosmetic appearance tints for a character's avatar (design-draft-v2.md §5).
 * Both tones are hue values in {@code [0, 359]} and optional: a null tone means
 * "use the renderer's default for the chosen race/sex".
 */
public record CharacterAppearance(
        @Min(0) @Max(359) Integer skinTone,
        @Min(0) @Max(359) Integer hairTone) {
}
