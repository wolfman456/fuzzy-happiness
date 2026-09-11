package com.gamer.fowever.tabletopservice.service.character;

import com.gamer.fowever.tabletopapi.ScoreSource;
import com.gamer.fowever.tabletopservice.service.character.ChargenRules.ArmorPiece;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChargenRulesTest {

    @Test
    void abilityModifierFollowsIntegerDivision() {
        assertThat(ChargenRules.abilityModifier(1)).isEqualTo(-5);
        assertThat(ChargenRules.abilityModifier(10)).isEqualTo(0);
        assertThat(ChargenRules.abilityModifier(11)).isEqualTo(0);
        assertThat(ChargenRules.abilityModifier(12)).isEqualTo(1);
        assertThat(ChargenRules.abilityModifier(20)).isEqualTo(5);
    }

    @Test
    void pointBuyCostMatchesPhbTable() {
        assertThat(ChargenRules.pointBuyCost(8)).isEqualTo(0);
        assertThat(ChargenRules.pointBuyCost(13)).isEqualTo(5);
        assertThat(ChargenRules.pointBuyCost(15)).isEqualTo(9);
        assertThat(ChargenRules.pointBuyCost(16)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void standardArrayRecognizesExactPermutationOnly() {
        Map<String, Integer> valid = Map.of(
                "str", 15, "dex", 13, "con", 14, "int", 12, "wis", 10, "cha", 8);
        Map<String, Integer> duplicated = Map.of(
                "str", 15, "dex", 15, "con", 14, "int", 12, "wis", 10, "cha", 8);
        Map<String, Integer> outOfRange = Map.of(
                "str", 15, "dex", 14, "con", 13, "int", 12, "wis", 10, "cha", 3);

        assertThat(ChargenRules.isLegalBaseScoreSet(ScoreSource.STANDARD_ARRAY, valid)).isTrue();
        assertThat(ChargenRules.isLegalBaseScoreSet(ScoreSource.STANDARD_ARRAY, duplicated)).isFalse();
        assertThat(ChargenRules.isLegalBaseScoreSet(ScoreSource.STANDARD_ARRAY, outOfRange)).isFalse();
    }

    @Test
    void pointBuyRespectsBudgetOfTwentySeven() {
        Map<String, Integer> atBudget = Map.of(
                "str", 15, "dex", 15, "con", 15, "int", 8, "wis", 8, "cha", 8);
        Map<String, Integer> overBudget = Map.of(
                "str", 15, "dex", 15, "con", 15, "int", 15, "wis", 8, "cha", 8);
        Map<String, Integer> belowMinimum = Map.of(
                "str", 7, "dex", 8, "con", 8, "int", 8, "wis", 8, "cha", 8);

        assertThat(ChargenRules.isLegalBaseScoreSet(ScoreSource.POINT_BUY, atBudget)).isTrue();
        assertThat(ChargenRules.isLegalBaseScoreSet(ScoreSource.POINT_BUY, overBudget)).isFalse();
        assertThat(ChargenRules.isLegalBaseScoreSet(ScoreSource.POINT_BUY, belowMinimum)).isFalse();
    }

    @Test
    void rolledSourcesAcceptWideBands() {
        Map<String, Integer> fourD6 = Map.of(
                "str", 18, "dex", 3, "con", 12, "int", 9, "wis", 10, "cha", 5);
        Map<String, Integer> house = Map.of(
                "str", 1, "dex", 30, "con", 15, "int", 2, "wis", 20, "cha", 4);
        Map<String, Integer> tooHigh = Map.of(
                "str", 19, "dex", 8, "con", 8, "int", 8, "wis", 8, "cha", 8);

        assertThat(ChargenRules.isLegalBaseScoreSet(ScoreSource.FOUR_D6_DROP_LOWEST, fourD6)).isTrue();
        assertThat(ChargenRules.isLegalBaseScoreSet(ScoreSource.FOUR_D6_DROP_LOWEST, tooHigh)).isFalse();
        assertThat(ChargenRules.isLegalBaseScoreSet(ScoreSource.HOUSE_RULE_D20, house)).isTrue();
    }

    @Test
    void startingGoldBudgetFollowsPhbWealthByClass() {
        assertThat(ChargenRules.startingGoldClassBudget("monk")).isEqualTo(12);
        assertThat(ChargenRules.startingGoldClassBudget("druid")).isEqualTo(50);
        assertThat(ChargenRules.startingGoldClassBudget("sorcerer")).isEqualTo(75);
        assertThat(ChargenRules.startingGoldClassBudget("rogue")).isEqualTo(100);
        assertThat(ChargenRules.startingGoldClassBudget("fighter")).isEqualTo(125);
        assertThat(ChargenRules.startingGoldClassBudget("unknown")).isEqualTo(100);
    }

    @Test
    void equipmentGoldCostConvertsUnitsAndRoundsSubGoldToZero() {
        assertThat(ChargenRules.equipmentGoldCostGp(75, "gp")).isEqualTo(75);
        assertThat(ChargenRules.equipmentGoldCostGp(2, "sp")).isZero();
        assertThat(ChargenRules.equipmentGoldCostGp(15, "sp")).isEqualTo(1);
        assertThat(ChargenRules.equipmentGoldCostGp(300, "cp")).isEqualTo(3);
        assertThat(ChargenRules.equipmentGoldCostGp(5, "pp")).isEqualTo(50);
        assertThat(ChargenRules.equipmentGoldCostGp(1, "")).isEqualTo(1);
    }

    @Test
    void hitPointsScaleByLevel() {
        assertThat(ChargenRules.hitPoints(1, 8, 2)).isEqualTo(10);
        assertThat(ChargenRules.hitPoints(2, 8, 2)).isEqualTo(17);
        assertThat(ChargenRules.hitPoints(1, 12, 0)).isEqualTo(12);
        assertThat(ChargenRules.hitPoints(1, 8, -2)).isEqualTo(6);
        assertThat(ChargenRules.hitPoints(3, 6, 1)).isEqualTo(17);
    }

    @Test
    void armorClassPicksBestNonShieldArmorAndAddsShield() {
        int dex = 3;
        assertThat(ChargenRules.armorClass(dex, List.of(), false)).isEqualTo(13);
        assertThat(ChargenRules.armorClass(dex, List.of(new ArmorPiece(11, true)), false)).isEqualTo(14);
        assertThat(ChargenRules.armorClass(dex, List.of(new ArmorPiece(11, true)), true)).isEqualTo(16);
        assertThat(ChargenRules.armorClass(dex, List.of(new ArmorPiece(14, false)), true)).isEqualTo(16);
        assertThat(ChargenRules.armorClass(dex, List.of(new ArmorPiece(11, true), new ArmorPiece(14, false)), false))
                .isEqualTo(14);
    }
}