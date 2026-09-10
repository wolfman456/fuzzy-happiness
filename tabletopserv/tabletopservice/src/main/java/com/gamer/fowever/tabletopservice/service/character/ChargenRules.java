package com.gamer.fowever.tabletopservice.service.character;

import com.gamer.fowever.tabletopapi.ScoreSource;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Pure, deterministic 2014-PHB character math: ability modifiers, score-source
 * legality (against the pre-racial base scores), hit points and armor class.
 * No SRD access here — everything is a plain function of its inputs.
 */
public final class ChargenRules {

    public static final int STANDARD_ARRAY_MIN = 15;
    public static final int STANDARD_ARRAY_MAX = 8;

    private ChargenRules() {
    }

    public static int abilityModifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }

    /** PHB 27-point-buy cost table (p.13); out-of-range scores are unaffordable. */
    public static int pointBuyCost(int score) {
        return switch (score) {
            case 8 -> 0;
            case 9 -> 1;
            case 10 -> 2;
            case 11 -> 3;
            case 12 -> 4;
            case 13 -> 5;
            case 14 -> 7;
            case 15 -> 9;
            default -> Integer.MAX_VALUE;
        };
    }

    /**
     * Checks that {@code base} (the six ability scores with the race's bonuses
     * removed) is producible by the given score source.
     *
     * @param base ability -> base score map for str/dex/con/int/wis/cha
     */
    public static boolean isLegalBaseScoreSet(ScoreSource source, Map<String, Integer> base) {
        List<Integer> values = List.of("str", "dex", "con", "int", "wis", "cha").stream()
                .map(base::get).toList();
        return switch (source) {
            case STANDARD_ARRAY -> {
                List<Integer> allowed = List.copyOf(values);
                yield allowed.containsAll(List.of(15, 14, 13, 12, 10, 8))
                        && values.stream().distinct().count() == 6
                        && values.stream().allMatch(v -> v >= 8 && v <= 15);
            }
            case POINT_BUY -> values.stream().allMatch(v -> v >= 8 && v <= 15)
                    && values.stream().mapToInt(ChargenRules::pointBuyCost).sum() <= 27;
            case FOUR_D6_DROP_LOWEST -> values.stream().allMatch(v -> v >= 3 && v <= 18);
            case HOUSE_RULE_D20 -> values.stream().allMatch(v -> v >= 1 && v <= 30);
        };
    }

    /**
     * Level 1 = max hit die + CON mod; each further level = fixed PHB hit-die
     * value (half the die, rounded down, +1) + CON mod. Result is at least one
     * point per level.
     */
    public static int hitPoints(int level, int hitDie, int constitutionModifier) {
        int levelOne = hitDie + constitutionModifier;
        int perLevel = hitDie / 2 + 1 + constitutionModifier;
        return Math.max(level, levelOne + perLevel * (level - 1));
    }

    /**
     * AC = 10 + DEX modifier, replaced by the best worn non-shield armor's
     * (base + DEX if that armor allows it) and +2 for an equipped shield.
     */
    public static int armorClass(int dexterityModifier, Collection<ArmorPiece> armors, boolean shieldEquipped) {
        int ac = 10 + dexterityModifier;
        int armorAc = ac;
        for (ArmorPiece piece : armors) {
            armorAc = Math.max(armorAc, piece.baseAc() + (piece.dexBonus() ? dexterityModifier : 0));
        }
        return Math.max(ac, armorAc) + (shieldEquipped ? 2 : 0);
    }

    public record ArmorPiece(int baseAc, boolean dexBonus) {
    }
}