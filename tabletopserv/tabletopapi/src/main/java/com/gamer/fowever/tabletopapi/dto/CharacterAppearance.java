package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Cosmetic appearance for a character's avatar (design-draft-v2.md §5).
 *
 * <p>The tints are hue values in {@code [0, 359]} and optional: a null tone means
 * "use the renderer's default for the chosen race/sex". {@code hairStyle} is an
 * optional lowercase slug naming a curated hair-part asset (e.g. {@code "short"},
 * {@code "ponytail"}); a null/blank value falls back to the race/sex default.
 * Race features (ears/horns/tusks/tail) are derived from the race, not stored here.
 */
public record CharacterAppearance(
        @Min(0) @Max(359) Integer skinTone,
        @Min(0) @Max(359) Integer hairTone,
        String hairStyle) {

    public CharacterAppearance {
        hairStyle = hairStyle == null || hairStyle.isBlank() ? null : hairStyle.trim();
    }
}
