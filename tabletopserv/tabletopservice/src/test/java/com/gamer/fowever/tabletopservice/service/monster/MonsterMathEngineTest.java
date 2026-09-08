package com.gamer.fowever.tabletopservice.service.monster;

import com.gamer.fowever.tabletopapi.MonsterEdition;
import com.gamer.fowever.tabletopapi.MonsterRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MonsterMathEngineTest {

    @Test
    void challengeRatingsCoversFractionalAndIntegerCr() {
        assertThat(MonsterMathEngine.challengeRatings())
                .contains("0", "1/8", "1/4", "1/2", "1", "5", "20", "30")
                .hasSize(34);
    }

    @Test
    void baselineForResolvesFractionalLabel() {
        Optional<MonsterMathEngine.CrBaseline> baseline = MonsterMathEngine.baselineFor("1/2");
        assertThat(baseline).isPresent();
        assertThat(baseline.get().cr()).isEqualTo(0.5);
        assertThat(baseline.get().xp()).isEqualTo(100);
        assertThat(baseline.get().proficiency()).isEqualTo(2);
    }

    @Test
    void baselineForRejectsUnknownLabel() {
        assertThat(MonsterMathEngine.baselineFor("17/8")).isEmpty();
    }

    @Test
    void generateIsDeterministicForSameInputs() {
        MonsterMathEngine.Statblock first = MonsterMathEngine.generate(
                "Gravetusk", "7", MonsterRole.BRUTE, MonsterEdition.SRD_2014);
        MonsterMathEngine.Statblock second = MonsterMathEngine.generate(
                "Gravetusk", "7", MonsterRole.BRUTE, MonsterEdition.SRD_2014);

        assertThat(first).isEqualTo(second);
    }

    @Test
    void balancedRoleUsesOfficialBaseline() {
        MonsterMathEngine.Statblock statblock = MonsterMathEngine.generate(
                null, "5", MonsterRole.BALANCED, MonsterEdition.SRD_2014);

        assertThat(statblock.crLabel()).isEqualTo("5");
        assertThat(statblock.proficiencyBonus()).isEqualTo(3);
        assertThat(statblock.armorClass()).isEqualTo(15);
        assertThat(statblock.hitPoints()).isEqualTo((131 + 145) / 2);
        assertThat(statblock.attackBonus()).isEqualTo(6);
        assertThat(statblock.saveDc()).isEqualTo(15);
        assertThat(statblock.damagePerRound()).isEqualTo((27 + 32) / 2);
        assertThat(statblock.xp()).isEqualTo(1800);
        assertThat(statblock.role()).isEqualTo(MonsterRole.BALANCED);
        assertThat(statblock.size()).isEqualTo("Medium");
    }

    @Test
    void roleMathShiftsApplyPerSection9b() {
        MonsterMathEngine.Statblock brute = MonsterMathEngine.generate(
                null, "8", MonsterRole.BRUTE, MonsterEdition.SRD_2014);
        MonsterMathEngine.Statblock defender = MonsterMathEngine.generate(
                null, "8", MonsterRole.DEFENDER, MonsterEdition.SRD_2014);
        MonsterMathEngine.Statblock artillery = MonsterMathEngine.generate(
                null, "8", MonsterRole.ARTILLERY, MonsterEdition.SRD_2014);
        MonsterMathEngine.Statblock lattice = MonsterMathEngine.generate(
                null, "8", MonsterRole.BALANCED, MonsterEdition.SRD_2014);

        assertThat(brute.armorClass()).isEqualTo(lattice.armorClass() - 1);
        assertThat(brute.hitPoints()).isEqualTo(pct(lattice.hitPoints(), 125));
        assertThat(defender.armorClass()).isEqualTo(lattice.armorClass() + 2);
        assertThat(defender.hitPoints()).isEqualTo(pct(lattice.hitPoints(), 110));
        assertThat(defender.damagePerRound()).isEqualTo(pct(lattice.damagePerRound(), 85));
        assertThat(artillery.armorClass()).isEqualTo(lattice.armorClass() - 2);
        assertThat(artillery.hitPoints()).isEqualTo(pct(lattice.hitPoints(), 90));
    }

    @Test
    void roleMathRaisesSaveDcForController() {
        MonsterMathEngine.Statblock controller = MonsterMathEngine.generate(
                null, "10", MonsterRole.CONTROLLER, MonsterEdition.SRD_2014);

        assertThat(controller.saveDc()).isEqualTo(17);
    }

    @Test
    void sizeLadderFollowsClassicTiers() {
        assertThat(MonsterMathEngine.sizeFor("0")).isEqualTo("Small");
        assertThat(MonsterMathEngine.sizeFor("1/2")).isEqualTo("Small");
        assertThat(MonsterMathEngine.sizeFor("1")).isEqualTo("Medium");
        assertThat(MonsterMathEngine.sizeFor("8")).isEqualTo("Medium");
        assertThat(MonsterMathEngine.sizeFor("9")).isEqualTo("Large");
        assertThat(MonsterMathEngine.sizeFor("18")).isEqualTo("Large");
        assertThat(MonsterMathEngine.sizeFor("19")).isEqualTo("Huge");
        assertThat(MonsterMathEngine.sizeFor("30")).isEqualTo("Huge");
    }

    @Test
    void resolveRolePicksRemoveKeywordsOverDefault() {
        assertThat(MonsterMathEngine.resolveRole("a lurking horror", null)).isEqualTo(MonsterRole.LURKER);
        assertThat(MonsterMathEngine.resolveRole(null, "Swarm of Vines")).isEqualTo(MonsterRole.SWARM);
        assertThat(MonsterMathEngine.resolveRole("ogre heavy", null)).isEqualTo(MonsterRole.BRUTE);
        assertThat(MonsterMathEngine.resolveRole("arcane weaver", null)).isEqualTo(MonsterRole.CONTROLLER);
        assertThat(MonsterMathEngine.resolveRole("interesting concept", null)).isEqualTo(MonsterRole.BALANCED);
    }

    @Test
    void generationRejectsUnsupportedCr() {
        assertThatThrownBy(() -> MonsterMathEngine.generate(
                null, "17/8", MonsterRole.BALANCED, MonsterEdition.SRD_2014))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported challenge rating: 17/8");
    }

    @Test
    void autoRoleFallsThroughToBalancedMath() {
        MonsterMathEngine.Statblock statblock = MonsterMathEngine.generate(
                null, "4", MonsterRole.AUTO, MonsterEdition.SRD_2014);

        assertThat(statblock.armorClass()).isEqualTo(14);
        assertThat(statblock.hitPoints()).isEqualTo((116 + 130) / 2);
        assertThat(statblock.attackBonus()).isEqualTo(5);
    }

    @Test
    void statblockCarriesTemplatedActionsAndTraits() {
        MonsterMathEngine.Statblock statblock = MonsterMathEngine.generate(
                null, "5", MonsterRole.BALANCED, MonsterEdition.SRD_2014);

        assertThat(statblock.traits()).anyMatch(trait -> trait.contains("Proficiency bonus +3"));
        assertThat(statblock.actions()).extracting("name").contains("Multiattack", "Claw");
    }

    @Test
    void multiattackAppearsFromCrFiveForBalancedRole() {
        MonsterMathEngine.Statblock statblock = MonsterMathEngine.generate(
                null, "4", MonsterRole.BALANCED, MonsterEdition.SRD_2014);

        assertThat(statblock.actions()).extracting("name").doesNotContain("Multiattack");
        assertThat(statblock.actions()).extracting("name").contains("Claw");
        assertThat(statblock.actions().getFirst().name()).isEqualTo("Claw");
    }

    @Test
    void editionFlowsIntoDescription() {
        MonsterMathEngine.Statblock statblock = MonsterMathEngine.generate(
                null, "3", MonsterRole.BALANCED, MonsterEdition.SRD_2024);

        assertThat(statblock.edition()).isEqualTo(MonsterEdition.SRD_2024);
        assertThat(statblock.description()).contains("2024");
    }

    @Test
    void generateUsesProvidedNameWhenPresent() {
        MonsterMathEngine.Statblock statblock = MonsterMathEngine.generate(
                "Gravetusk", "3", MonsterRole.BRUTE, MonsterEdition.SRD_2014);

        assertThat(statblock.name()).isEqualTo("Gravetusk");
    }

    private static int pct(int value, int percent) {
        return Math.max(1, (value * percent) / 100);
    }
}