package com.gamer.fowever.tabletopapi;

/**
 * A character's sex, chosen on the wizard's Appearance step. Cosmetic only —
 * 5e does not gate any chargen choice on it — and required, binary
 * {@code MALE}/{@code FEMALE} (design-draft-v2.md §5).
 */
public enum Sex {
    MALE,
    FEMALE
}
