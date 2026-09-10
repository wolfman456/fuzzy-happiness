package com.gamer.fowever.tabletopapi;

/**
 * How a character's base ability scores were produced. Validation restores the
 * pre-racial base scores (final score minus the race's ability bonuses) before
 * applying the source's constraints.
 */
public enum ScoreSource {
    /** The 2014 PHB standard array: 15, 14, 13, 12, 10, 8. */
    STANDARD_ARRAY,
    /** 27-point buy with the PHB cost table (scores 8-15). */
    POINT_BUY,
    /** 4d6-drop-lowest rolled six times, server-side. */
    FOUR_D6_DROP_LOWEST,
    /** House rule: six d20s rolled server-side, unassigned. */
    HOUSE_RULE_D20
}