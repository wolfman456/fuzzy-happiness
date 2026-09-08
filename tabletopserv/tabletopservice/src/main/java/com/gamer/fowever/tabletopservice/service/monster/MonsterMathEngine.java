package com.gamer.fowever.tabletopservice.service.monster;

import com.gamer.fowever.tabletopapi.MonsterEdition;
import com.gamer.fowever.tabletopapi.MonsterRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Deterministic CR-driven statblock math (§9b). Given a Challenge Rating and a combat role, every
 * number is computed from the official "Monster Statistics by Challenge Rating" curve (DMG) — the
 * engine never rolls, never calls upstream, and never delegates numeric choices to an LLM. Roles
 * shift the baselines per the §9b table while XP stays tied to CR.
 */
public final class MonsterMathEngine {

    private MonsterMathEngine() {
    }

    public record CrBaseline(double cr, String label, int proficiency, int ac, int hpLow, int hpHigh,
                             int attackBonus, int dprLow, int dprHigh, int saveDc, int xp) {

        public int hp() {
            return (hpLow + hpHigh) / 2;
        }

        public int dpr() {
            return (dprLow + dprHigh) / 2;
        }
    }

    public record Statblock(String name, String crLabel, int proficiencyBonus, int armorClass,
                            int hitPoints, int attackBonus, int saveDc, int damagePerRound, int xp,
                            MonsterEdition edition, MonsterRole role, String size, String type,
                            String alignment, int speedFeet, int strength, int dexterity, int constitution,
                            int intelligence, int wisdom, int charisma, String description,
                            List<MonsterAction> actions, List<String> traits) {

        public record MonsterAction(String name, String description) {
        }
    }

    /** Official "Monster Statistics by Challenge Rating" (DMG). CR is the rank index. */
    private static final List<CrBaseline> BASELINES = List.of(
            baseline(0, "0", 2, 13, 1, 6, 3, 0, 1, 13, 10),
            baseline(0.125, "1/8", 2, 13, 7, 35, 3, 1, 2, 13, 25),
            baseline(0.25, "1/4", 2, 13, 36, 49, 3, 3, 4, 13, 50),
            baseline(0.5, "1/2", 2, 13, 50, 70, 3, 5, 6, 13, 100),
            baseline(1, "1", 2, 13, 71, 85, 3, 7, 10, 13, 200),
            baseline(2, "2", 2, 13, 86, 100, 3, 11, 14, 13, 450),
            baseline(3, "3", 2, 13, 101, 115, 4, 15, 20, 13, 700),
            baseline(4, "4", 2, 14, 116, 130, 5, 21, 26, 14, 1100),
            baseline(5, "5", 3, 15, 131, 145, 6, 27, 32, 15, 1800),
            baseline(6, "6", 3, 15, 146, 160, 6, 33, 38, 15, 2300),
            baseline(7, "7", 3, 15, 161, 175, 6, 39, 44, 15, 2900),
            baseline(8, "8", 3, 16, 176, 190, 7, 45, 50, 16, 3900),
            baseline(9, "9", 4, 16, 191, 205, 7, 51, 56, 16, 5000),
            baseline(10, "10", 4, 17, 206, 220, 7, 57, 62, 16, 5900),
            baseline(11, "11", 4, 17, 221, 235, 8, 63, 68, 17, 7200),
            baseline(12, "12", 4, 17, 236, 250, 8, 69, 74, 17, 8400),
            baseline(13, "13", 5, 18, 251, 265, 8, 75, 80, 18, 10000),
            baseline(14, "14", 5, 18, 266, 280, 9, 81, 86, 18, 11500),
            baseline(15, "15", 5, 18, 281, 295, 9, 87, 92, 18, 13000),
            baseline(16, "16", 5, 18, 296, 310, 9, 93, 98, 18, 15000),
            baseline(17, "17", 6, 19, 311, 325, 10, 99, 104, 19, 18000),
            baseline(18, "18", 6, 19, 326, 340, 10, 105, 110, 19, 20000),
            baseline(19, "19", 6, 19, 341, 355, 10, 111, 116, 19, 22000),
            baseline(20, "20", 6, 19, 356, 400, 11, 117, 122, 19, 25000),
            baseline(21, "21", 7, 19, 401, 445, 11, 123, 128, 19, 33000),
            baseline(22, "22", 7, 19, 446, 490, 11, 129, 134, 19, 41000),
            baseline(23, "23", 7, 19, 491, 535, 12, 135, 140, 19, 50000),
            baseline(24, "24", 7, 19, 536, 580, 12, 141, 146, 19, 62000),
            baseline(25, "25", 8, 19, 581, 625, 12, 147, 152, 19, 75000),
            baseline(26, "26", 8, 19, 626, 670, 13, 153, 158, 19, 90000),
            baseline(27, "27", 8, 19, 671, 715, 13, 159, 164, 19, 105000),
            baseline(28, "28", 8, 19, 716, 760, 13, 165, 170, 19, 120000),
            baseline(29, "29", 9, 19, 761, 805, 14, 171, 176, 19, 135000),
            baseline(30, "30", 9, 19, 806, 850, 14, 177, 182, 19, 155000));

    private static CrBaseline baseline(double cr, String label, int proficiency, int ac,
                                       int hpLow, int hpHigh, int attackBonus, int dprLow,
                                       int dprHigh, int saveDc, int xp) {
        return new CrBaseline(cr, label, proficiency, ac, hpLow, hpHigh, attackBonus,
                dprLow, dprHigh, saveDc, xp);
    }

    private static final List<String> CR_LABELS = BASELINES.stream()
            .map(CrBaseline::label)
            .toList();

    /** Keyword→role hints for the §9b Auto role (concept/name driven selection). */
    private static final List<KeywordRole> AUTO_KEYWORDS = List.of(
            new KeywordRole(List.of("guard", "wall", "protect", "warden", "sentinel"), MonsterRole.DEFENDER),
            new KeywordRole(List.of("brute", "ram", "hulk", "ogre", "heavy"), MonsterRole.BRUTE),
            new KeywordRole(List.of("lurk", "stalk", "ambush", "prowl", "shadow"), MonsterRole.LURKER),
            new KeywordRole(List.of("swarm", "horde", "cloud", "nest"), MonsterRole.SWARM),
            new KeywordRole(List.of("bombard", "artiller", "cannon", "sniper", "bolt"), MonsterRole.ARTILLERY),
            new KeywordRole(List.of("support", "heal", "herald", "convert", "bless"), MonsterRole.SUPPORT),
            new KeywordRole(List.of("weave", "control", "beguil", "gaze", "enchant"), MonsterRole.CONTROLLER),
            new KeywordRole(List.of("skim", "dart", "skirmish", "rider", "scout", "runner"), MonsterRole.SKIRMISHER));

    private record KeywordRole(List<String> keywords, MonsterRole role) {
    }

    public static MonsterRole resolveRole(String concept, String name) {
        String haystack = (concept == null ? "" : concept + " ") + (name == null ? "" : name);
        String lowered = haystack.toLowerCase(Locale.ROOT);
        for (KeywordRole keywordRole : AUTO_KEYWORDS) {
            for (String keyword : keywordRole.keywords()) {
                if (lowered.contains(keyword)) {
                    return keywordRole.role();
                }
            }
        }
        return MonsterRole.BALANCED;
    }

    public static Optional<CrBaseline> baselineFor(String crLabel) {
        return BASELINES.stream().filter(b -> b.label().equals(crLabel)).findFirst();
    }

    public static List<String> challengeRatings() {
        return CR_LABELS;
    }

    /** Classic size ladder by challenge rating (CR < 1 Small, ≤ 8 Medium, ≤ 18 Large, else Huge). */
    public static String sizeFor(String crLabel) {
        return baselineFor(crLabel)
                .map(b -> b.cr() < 1 ? "Small" : b.cr() <= 8 ? "Medium" : b.cr() <= 18 ? "Large" : "Huge")
                .orElse("Medium");
    }

    public static Statblock generate(String name, String crLabel, MonsterRole role,
                                     MonsterEdition edition) {
        CrBaseline base = baselineFor(crLabel)
                .orElseThrow(() -> new IllegalArgumentException("unsupported challenge rating: " + crLabel));
        RoleMath math = roleMath(role, base);
        AbilityProfile abilities = AbilityProfile.forRole(role, base);
        String resolvedName = resolveName(name, base, role);
        List<String> traits = traits(role, base);
        List<Statblock.MonsterAction> actions = actions(role, base, math, abilities);
        String description = description(role, resolvedName, base, edition);
        return new Statblock(resolvedName, base.label(), base.proficiency(), math.ac(),
                math.hp(), math.attackBonus(), math.saveDc(), math.dpr(), base.xp(),
                edition, role, sizeFor(crLabel), roleType(role), "unaligned", speedFor(role),
                abilities.strength(), abilities.dexterity(), abilities.constitution(),
                abilities.intelligence(), abilities.wisdom(), abilities.charisma(),
                description, actions, traits);
    }

    private record RoleMath(int ac, int hp, int attackBonus, int saveDc, int dpr) {
    }

    private static RoleMath roleMath(MonsterRole role, CrBaseline base) {
        int ac = base.ac();
        int hp = base.hp();
        int dpr = base.dpr();
        int saveDc = base.saveDc();
        switch (role) {
            case BRUTE -> {
                ac -= 1;
                hp = percent(base.hp(), 125);
                dpr = base.dpr();
            }
            case DEFENDER -> {
                ac += 2;
                hp = percent(base.hp(), 110);
                dpr = percent(base.dpr(), 85);
            }
            case ARTILLERY -> {
                ac -= 2;
                hp = percent(base.hp(), 90);
                dpr = percent(base.dpr(), 95);
            }
            case CONTROLLER -> {
                saveDc += 1;
                dpr = percent(base.dpr(), 70);
            }
            case LURKER -> {
                hp = percent(base.hp(), 80);
                dpr = percent(base.dpr(), 120);
            }
            case SUPPORT -> dpr = percent(base.dpr(), 60);
            case SWARM -> hp = percent(base.hp(), 125);
            case BALANCED, SKIRMISHER, AUTO -> {
                // raw baseline (skirmisher trades a defensive trait for mobility)
            }
        }
        return new RoleMath(ac, hp, base.attackBonus(), saveDc, dpr);
    }

    private static int percent(int value, int percent) {
        return Math.max(1, (value * percent) / 100);
    }

    private static String roleType(MonsterRole role) {
        return switch (role) {
            case BRUTE -> "giant";
            case SWARM -> "swarm of tiny beasts";
            case LURKER -> "shadow";
            case ARTILLERY -> "construct";
            default -> "monstrosity";
        };
    }

    private static int speedFor(MonsterRole role) {
        return switch (role) {
            case LURKER, SKIRMISHER -> 40;
            case SWARM -> 20;
            case BRUTE, DEFENDER -> 30;
            default -> 30;
        };
    }

    private record AbilityProfile(int strength, int dexterity, int constitution,
                                  int intelligence, int wisdom, int charisma) {

        static AbilityProfile forRole(MonsterRole role, CrBaseline base) {
            int lift = Math.min(6, Math.max(0, (int) base.cr() / 4));
            switch (role) {
                case BRUTE -> {
                    return new AbilityProfile(16 + lift, 8, 16 + lift, 5, 8, 6);
                }
                case DEFENDER -> {
                    return new AbilityProfile(14 + lift, 10, 16 + lift, 8, 12, 8);
                }
                case SKIRMISHER -> {
                    return new AbilityProfile(10, 16 + lift, 13, 10, 12, 10);
                }
                case ARTILLERY -> {
                    return new AbilityProfile(8, 14, 12, 16 + lift, 10, 8);
                }
                case CONTROLLER -> {
                    return new AbilityProfile(8, 10, 12, 15 + lift, 16 + lift, 12);
                }
                case LURKER -> {
                    return new AbilityProfile(12, 17 + lift, 12, 12, 13, 10);
                }
                case SUPPORT -> {
                    return new AbilityProfile(8, 12, 12, 14 + lift, 16 + lift, 16 + lift);
                }
                case SWARM -> {
                    return new AbilityProfile(10, 12, 12 + lift, 4, 8, 4);
                }
                case BALANCED, AUTO -> {
                    return new AbilityProfile(14 + lift, 12 + lift, 14 + lift, 10, 12, 9);
                }
            }
            return new AbilityProfile(14, 12, 14, 10, 12, 9);
        }
    }

    private static String resolveName(String name, CrBaseline base, MonsterRole role) {
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return switch (role) {
            case BRUTE -> "Raging " + base.label() + " Brute";
            case DEFENDER -> base.label() + " Wall Guardian";
            case SKIRMISHER -> "Rapid " + base.label() + " Skirmisher";
            case ARTILLERY -> base.label() + " Bone Artillerist";
            case CONTROLLER -> base.label() + " Mind Weaver";
            case LURKER -> "Stalking " + base.label() + " Lurker";
            case SUPPORT -> base.label() + " Healing Herald";
            case SWARM -> base.label() + " Devouring Swarm";
            default -> base.label() + " Balanced Monstrosity";
        };
    }

    private static List<String> traits(MonsterRole role, CrBaseline base) {
        List<String> traits = new ArrayList<>();
        traits.add("Proficiency bonus +" + base.proficiency());
        switch (role) {
            case BRUTE -> traits.add("Hardy Frame. The brute's thick body shrugs off blows; its HP is 125% of baseline for its challenge rating.");
            case DEFENDER -> traits.add("Protective Hostility. As a reaction when an ally within 5 ft. is attacked, the defender imposes disadvantage on the attack roll.");
            case SKIRMISHER -> traits.add("Nimble Escape. The skirmisher can take the Disengage or Hide action as a bonus action.");
            case ARTILLERY -> traits.add("Artillery Range. Ranged attacks ignore the usual long-range penalty and it can escape grapples as a bonus action.");
            case CONTROLLER -> traits.add("Area Control. Effects that restrain, slow, or blind have their save DC increased by 1.");
            case LURKER -> traits.add("Opening Strike. The lurker's first attack on a hidden or surprised target deals +50% damage.");
            case SUPPORT -> traits.add("Battlefield Support. It heals, buffs, or enables allies instead of maximizing its own damage (output is 60% of baseline).");
            case SWARM -> traits.add("Swarm Resilience. The swarm halves damage from bludgeoning, piercing, and slashing; its damage halves below half HP.");
            case BALANCED, AUTO -> {
                // no role trait
            }
        }
        return List.copyOf(traits);
    }

    private static List<Statblock.MonsterAction> actions(MonsterRole role, CrBaseline base,
                                                         RoleMath math, AbilityProfile abilities) {
        int attacks = role == MonsterRole.BRUTE || role == MonsterRole.LURKER ? 1
                : role == MonsterRole.SWARM ? 4 : base.cr() >= 5 ? 2 : 1;
        int perHit = attacks == 0 ? math.dpr() : Math.max(1, math.dpr() / attacks);
        List<Statblock.MonsterAction> actions = new ArrayList<>();
        if (attacks > 1) {
            actions.add(new Statblock.MonsterAction("Multiattack",
                    "The " + resolveName(null, base, role) + " makes " + attacks + " attacks."));
        }
        actions.add(new Statblock.MonsterAction(attackName(role, base, abilities),
                attackDescription(role, base, math, perHit, abilities)));
        if (role == MonsterRole.ARTILLERY) {
            actions.add(new Statblock.MonsterAction("Bombard",
                    "Ranged attack; on a hit the target and each creature within 5 ft. take damage."));
        }
        if (role == MonsterRole.CONTROLLER) {
            actions.add(new Statblock.MonsterAction("Beguiling Gaze",
                    "Each creature in a 30 ft. cone must succeed on a Charisma saving throw or be restrained until the end of its next turn (DC " + math.saveDc() + ")."));
        }
        if (role == MonsterRole.SUPPORT) {
            actions.add(new Statblock.MonsterAction("Renewing Touch",
                    "An ally within 5 ft. regains hit points equal to the damage-per-round budget."));
        }
        if (role == MonsterRole.SWARM) {
            actions.add(new Statblock.MonsterAction("Engulf",
                    "A Medium or smaller creature in the swarm's space is engulfed: restrained and takes the swarm's damage at the start of each of its turns."));
        }
        return List.copyOf(actions);
    }

    private static String attackName(MonsterRole role, CrBaseline base, AbilityProfile abilities) {
        return switch (role) {
            case BRUTE -> "Slam";
            case SKIRMISHER -> "Darting Claw";
            case ARTILLERY -> "Bolt";
            case CONTROLLER -> "Mind Lash";
            case LURKER -> "Ambush Strike";
            case SUPPORT -> "Warding Touch";
            case SWARM -> "Bites";
            default -> base.cr() >= 3 ? "Claw" : "Bite";
        };
    }

    private static String attackDescription(MonsterRole role, CrBaseline base, RoleMath math,
                                            int perHit, AbilityProfile abilities) {
        int abilityMod = switch (role) {
            case SKIRMISHER, ARTILLERY, LURKER, SWARM -> abilityMod(abilities.dexterity());
            default -> abilityMod(abilities.strength());
        };
        int toHit = math.attackBonus();
        String damage = perHit + " (" + diceFor(perHit) + " + " + abilityMod + ")";
        return "Melee or ranged weapon attack, +" + toHit + " to hit, one target. Hit: " + damage + " " + damageType(role);
    }

    private static String diceFor(int average) {
        // Convert average damage to an approximate number of d6 (a d6 averages 3.5).
        int dice = Math.max(1, (int) Math.round(average / 3.5));
        return dice + "d6";
    }

    private static String damageType(MonsterRole role) {
        return switch (role) {
            case BRUTE -> "bludgeoning";
            case SKIRMISHER -> "slashing";
            case ARTILLERY -> "force";
            case CONTROLLER -> "psychic";
            case LURKER, SWARM -> "piercing";
            default -> "bludgeoning";
        };
    }

    private static String description(MonsterRole role, String name, CrBaseline base,
                                      MonsterEdition edition) {
        String ruleSet = edition == MonsterEdition.SRD_2024 ? "2024" : "2014";
        return "A " + role.name().toLowerCase().replace('_', ' ') + " of challenge rating "
                + base.label() + ". " + name + " (" + roleType(role) + ") stands " + sizeFor(base.label())
                + ", unaligned, with the classic " + ruleSet + " statblock layout.";
    }

    private static int abilityMod(int score) {
        return (score - 10) / 2;
    }
}