package com.gamer.fowever.tabletopservice.service.character;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hand-curated 2014 catalog for choices the SRD feed does not cover (charged
 * under design-v1-fixes #48/#50 and alpha round 2 #66): the full PHB background
 * list and each class's official 2014 subclass options (PHB + XGtE + Tasha's +
 * SCAG), indexed like SRD records and carrying the class's starting level.
 * Names/indices only — no rulebook text is embedded. The SRD example archetype
 * keeps its existing index so current characters and quick-builds stay
 * compatible.
 */
@Component
public class ChargenCatalog {

    public record BackgroundRef(String index, String name) {
    }

    public record SubclassRef(String classIndex, String index, String name, int level) {
    }

    public record SubclassFeatureRef(String subclassIndex, int level, String feature) {
    }

    private static final List<BackgroundRef> BACKGROUNDS = List.of(
            new BackgroundRef("acolyte", "Acolyte"),
            new BackgroundRef("charlatan", "Charlatan"),
            new BackgroundRef("criminal", "Criminal"),
            new BackgroundRef("entertainer", "Entertainer"),
            new BackgroundRef("folk-hero", "Folk Hero"),
            new BackgroundRef("guild-artisan", "Guild Artisan"),
            new BackgroundRef("hermit", "Hermit"),
            new BackgroundRef("noble", "Noble"),
            new BackgroundRef("outlander", "Outlander"),
            new BackgroundRef("sage", "Sage"),
            new BackgroundRef("sailor", "Sailor"),
            new BackgroundRef("soldier", "Soldier"),
            new BackgroundRef("urchin", "Urchin"));

    private static final Map<String, List<SubclassRef>> SUBCLASSES = subclasses(
            entry("barbarian", 3,
                    new SubclassRef("barbarian", "berserker", "Berserker", 3),
                    new SubclassRef("barbarian", "totem-warrior", "Totem Warrior", 3),
                    new SubclassRef("barbarian", "ancestral-guardian", "Ancestral Guardian", 3),
                    new SubclassRef("barbarian", "storm-herald", "Storm Herald", 3),
                    new SubclassRef("barbarian", "zealot", "Zealot", 3),
                    new SubclassRef("barbarian", "beast", "Path of the Beast", 3),
                    new SubclassRef("barbarian", "wild-magic", "Path of Wild Magic", 3),
                    new SubclassRef("barbarian", "battlerager", "Path of the Battlerager", 3)),
            entry("bard", 3,
                    new SubclassRef("bard", "lore", "College of Lore", 3),
                    new SubclassRef("bard", "valor", "College of Valor", 3),
                    new SubclassRef("bard", "glamour", "College of Glamour", 3),
                    new SubclassRef("bard", "swords", "College of Swords", 3),
                    new SubclassRef("bard", "whispers", "College of Whispers", 3),
                    new SubclassRef("bard", "eloquence", "College of Eloquence", 3),
                    new SubclassRef("bard", "creation", "College of Creation", 3)),
            entry("cleric", 1,
                    new SubclassRef("cleric", "life", "Life Domain", 1),
                    new SubclassRef("cleric", "knowledge", "Knowledge Domain", 1),
                    new SubclassRef("cleric", "light", "Light Domain", 1),
                    new SubclassRef("cleric", "nature", "Nature Domain", 1),
                    new SubclassRef("cleric", "tempest", "Tempest Domain", 1),
                    new SubclassRef("cleric", "trickery", "Trickery Domain", 1),
                    new SubclassRef("cleric", "war", "War Domain", 1),
                    new SubclassRef("cleric", "arcana", "Arcana Domain", 1),
                    new SubclassRef("cleric", "forge", "Forge Domain", 1),
                    new SubclassRef("cleric", "grave", "Grave Domain", 1),
                    new SubclassRef("cleric", "order", "Order Domain", 1),
                    new SubclassRef("cleric", "peace", "Peace Domain", 1),
                    new SubclassRef("cleric", "twilight", "Twilight Domain", 1)),
            entry("druid", 2,
                    new SubclassRef("druid", "land", "Circle of the Land", 2),
                    new SubclassRef("druid", "moon", "Circle of the Moon", 2),
                    new SubclassRef("druid", "dreams", "Circle of Dreams", 2),
                    new SubclassRef("druid", "shepherd", "Circle of the Shepherd", 2),
                    new SubclassRef("druid", "spores", "Circle of Spores", 2),
                    new SubclassRef("druid", "stars", "Circle of Stars", 2),
                    new SubclassRef("druid", "wildfire", "Circle of Wildfire", 2)),
            entry("fighter", 3,
                    new SubclassRef("fighter", "champion", "Champion", 3),
                    new SubclassRef("fighter", "battle-master", "Battle Master", 3),
                    new SubclassRef("fighter", "eldritch-knight", "Eldritch Knight", 3),
                    new SubclassRef("fighter", "arcane-archer", "Arcane Archer", 3),
                    new SubclassRef("fighter", "cavalier", "Cavalier", 3),
                    new SubclassRef("fighter", "samurai", "Samurai", 3),
                    new SubclassRef("fighter", "psi-warrior", "Psi Warrior", 3),
                    new SubclassRef("fighter", "rune-knight", "Rune Knight", 3),
                    new SubclassRef("fighter", "purple-dragon-knight", "Purple Dragon Knight", 3)),
            entry("monk", 3,
                    new SubclassRef("monk", "open-hand", "Way of the Open Hand", 3),
                    new SubclassRef("monk", "shadow", "Way of Shadow", 3),
                    new SubclassRef("monk", "four-elements", "Way of the Four Elements", 3),
                    new SubclassRef("monk", "kensei", "Way of the Kensei", 3),
                    new SubclassRef("monk", "drunken-master", "Way of the Drunken Master", 3),
                    new SubclassRef("monk", "sun-soul", "Way of the Sun Soul", 3),
                    new SubclassRef("monk", "mercy", "Way of Mercy", 3),
                    new SubclassRef("monk", "astral-self", "Way of the Astral Self", 3)),
            entry("paladin", 3,
                    new SubclassRef("paladin", "devotion", "Oath of Devotion", 3),
                    new SubclassRef("paladin", "ancients", "Oath of the Ancients", 3),
                    new SubclassRef("paladin", "vengeance", "Oath of Vengeance", 3),
                    new SubclassRef("paladin", "conquest", "Oath of Conquest", 3),
                    new SubclassRef("paladin", "redemption", "Oath of Redemption", 3),
                    new SubclassRef("paladin", "glory", "Oath of Glory", 3),
                    new SubclassRef("paladin", "watchers", "Oath of the Watchers", 3),
                    new SubclassRef("paladin", "crown", "Oath of the Crown", 3)),
            entry("ranger", 3,
                    new SubclassRef("ranger", "hunter", "Hunter", 3),
                    new SubclassRef("ranger", "beast-master", "Beast Master", 3),
                    new SubclassRef("ranger", "gloom-stalker", "Gloom Stalker", 3),
                    new SubclassRef("ranger", "horizon-walker", "Horizon Walker", 3),
                    new SubclassRef("ranger", "monster-slayer", "Monster Slayer", 3),
                    new SubclassRef("ranger", "fey-wanderer", "Fey Wanderer", 3),
                    new SubclassRef("ranger", "swarmkeeper", "Swarmkeeper", 3)),
            entry("rogue", 3,
                    new SubclassRef("rogue", "thief", "Thief", 3),
                    new SubclassRef("rogue", "assassin", "Assassin", 3),
                    new SubclassRef("rogue", "arcane-trickster", "Arcane Trickster", 3),
                    new SubclassRef("rogue", "inquisitive", "Inquisitive", 3),
                    new SubclassRef("rogue", "mastermind", "Mastermind", 3),
                    new SubclassRef("rogue", "scout", "Scout", 3),
                    new SubclassRef("rogue", "swashbuckler", "Swashbuckler", 3),
                    new SubclassRef("rogue", "phantom", "Phantom", 3),
                    new SubclassRef("rogue", "soulknife", "Soulknife", 3)),
            entry("sorcerer", 1,
                    new SubclassRef("sorcerer", "draconic", "Draconic Bloodline", 1),
                    new SubclassRef("sorcerer", "wild-magic", "Wild Magic", 1),
                    new SubclassRef("sorcerer", "storm", "Storm Sorcery", 1),
                    new SubclassRef("sorcerer", "divine-soul", "Divine Soul", 1),
                    new SubclassRef("sorcerer", "shadow-magic", "Shadow Magic", 1),
                    new SubclassRef("sorcerer", "aberrant-mind", "Aberrant Mind", 1),
                    new SubclassRef("sorcerer", "clockwork-soul", "Clockwork Soul", 1)),
            entry("warlock", 1,
                    new SubclassRef("warlock", "fiend", "The Fiend", 1),
                    new SubclassRef("warlock", "archfey", "The Archfey", 1),
                    new SubclassRef("warlock", "great-old-one", "The Great Old One", 1),
                    new SubclassRef("warlock", "celestial", "The Celestial", 1),
                    new SubclassRef("warlock", "hexblade", "The Hexblade", 1),
                    new SubclassRef("warlock", "undying", "The Undying", 1),
                    new SubclassRef("warlock", "genie", "The Genie", 1)),
            entry("wizard", 2,
                    new SubclassRef("wizard", "evocation", "School of Evocation", 2),
                    new SubclassRef("wizard", "abjuration", "School of Abjuration", 2),
                    new SubclassRef("wizard", "conjuration", "School of Conjuration", 2),
                    new SubclassRef("wizard", "divination", "School of Divination", 2),
                    new SubclassRef("wizard", "enchantment", "School of Enchantment", 2),
                    new SubclassRef("wizard", "illusion", "School of Illusion", 2),
                    new SubclassRef("wizard", "necromancy", "School of Necromancy", 2),
                    new SubclassRef("wizard", "transmutation", "School of Transmutation", 2),
                    new SubclassRef("wizard", "bladesinging", "Bladesinging", 2),
                    new SubclassRef("wizard", "war-magic", "War Magic", 2),
                    new SubclassRef("wizard", "scribes", "Order of Scribes", 2)));

    /**
     * Curated subclass features for archetypes the SRD feed does not cover (only the 12 SRD
     * example archetypes expose a `subclasses/{id}/levels` payload). Names only — no rulebook
     * text. Each entry is (subclass index, unlock level, feature index) so the compiled sheet
     * can merge features up to the starting level, mirroring the SRD subclass-level shape.
     */
    private static final Map<String, List<SubclassFeatureRef>> SUBCLASS_FEATURES = featureMap(
            entry("barbarian", "berserker", f(3, "frenzy"), f(6, "mindless-rage"), f(10, "intimidating-presence"), f(14, "retaliation")),
            entry("barbarian", "totem-warrior", f(3, "spirit-seeker"), f(3, "totem-spirit"), f(6, "aspect-of-the-beast"), f(10, "spirit-walker"), f(14, "totemic-attunement")),
            entry("barbarian", "ancestral-guardian", f(3, "ancestral-protectors"), f(6, "spirit-shield"), f(10, "consult-the-spirits"), f(14, "vengeful-ancestors")),
            entry("barbarian", "storm-herald", f(3, "storm-aura"), f(6, "storm-soul"), f(10, "shielding-storm"), f(14, "raging-storm")),
            entry("barbarian", "zealot", f(3, "divine-fury"), f(6, "zealous-focus"), f(10, "zealous-presence"), f(14, "rage-beyond-death")),
            entry("barbarian", "beast", f(3, "form-of-the-beast"), f(6, "bestial-soul"), f(10, "infectious-fury"), f(14, "call-the-hunt")),
            entry("barbarian", "wild-magic", f(3, "wild-surge"), f(3, "wild-magic"), f(6, "bolstering-magic"), f(10, "unstable-backlash"), f(14, "controlled-surge")),
            entry("barbarian", "battlerager", f(3, "battlerager-armor"), f(6, "reckless-abandon"), f(10, "battlerager-charge"), f(14, "spiked-retribution")),
            entry("bard", "lore", f(3, "bonus-proficiencies"), f(3, "cutting-words"), f(6, "additional-magical-secrets"), f(14, "peerless-skill")),
            entry("bard", "valor", f(3, "bonus-proficiencies"), f(3, "combat-inspiration"), f(6, "extra-attack"), f(14, "battle-magic")),
            entry("bard", "glamour", f(3, "mantle-of-inspiration"), f(3, "enthralling-performance"), f(6, "mantle-of-majesty"), f(14, "unbreakable-majesty")),
            entry("bard", "swords", f(3, "bonus-proficiencies"), f(3, "fighting-style"), f(3, "blade-flourish"), f(6, "extra-attack"), f(14, "master-flourish")),
            entry("bard", "whispers", f(3, "psychic-blades"), f(3, "words-of-terror"), f(6, "mantle-of-whispers"), f(14, "shadow-lore")),
            entry("bard", "eloquence", f(3, "silver-tongue"), f(3, "unsettling-words"), f(6, "unfailing-inspiration"), f(6, "universal-speech"), f(14, "infectious-inspiration")),
            entry("bard", "creation", f(3, "mote-of-potential"), f(3, "performance-of-creation"), f(6, "animating-performance"), f(14, "creative-crescendo")),
            entry("cleric", "knowledge", f(1, "blessings-of-knowledge"), f(2, "knowledge-of-the-ages"), f(6, "read-thoughts"), f(8, "potent-spellcasting"), f(17, "visions-of-the-past")),
            entry("cleric", "life", f(1, "disciple-of-life"), f(2, "preserve-life"), f(6, "blessed-healer"), f(8, "divine-strike"), f(17, "supreme-healing")),
            entry("cleric", "light", f(1, "bonus-cantrip"), f(1, "warding-flare"), f(2, "radiance-of-the-dawn"), f(6, "improved-flare"), f(8, "potent-spellcasting"), f(17, "corona-of-light")),
            entry("cleric", "nature", f(1, "acolyte-of-nature"), f(1, "bonus-proficiency"), f(2, "charm-animals-and-plants"), f(6, "dampen-elements"), f(8, "divine-strike"), f(17, "master-of-nature")),
            entry("cleric", "tempest", f(1, "bonus-proficiencies"), f(1, "wrath-of-the-storm"), f(2, "destructive-wrath"), f(6, "thunderbolt-strike"), f(8, "divine-strike"), f(17, "stormborn")),
            entry("cleric", "trickery", f(1, "blessing-of-the-trickster"), f(2, "invoke-duplicity"), f(6, "cloak-of-shadows"), f(8, "divine-strike"), f(17, "improved-duplicity")),
            entry("cleric", "war", f(1, "bonus-proficiencies"), f(1, "war-priest"), f(2, "guided-strike"), f(6, "war-gods-blessing"), f(8, "divine-strike"), f(17, "avatar-of-battle")),
            entry("cleric", "arcana", f(1, "arcana-initiate"), f(2, "arcane-abjuration"), f(6, "spell-breaker"), f(8, "potent-spellcasting"), f(17, "arcane-mastery")),
            entry("cleric", "forge", f(1, "blessing-of-the-forge"), f(2, "artisans-blessing"), f(6, "soul-of-the-forge"), f(8, "divine-strike"), f(17, "saint-of-forge-and-fire")),
            entry("cleric", "grave", f(1, "circle-of-mortality"), f(1, "eyes-of-the-grave"), f(2, "path-to-the-grave"), f(6, "sentinel-at-deaths-door"), f(8, "potent-spellcasting"), f(17, "keeper-of-souls")),
            entry("cleric", "order", f(1, "voice-of-authority"), f(2, "orders-demand"), f(6, "embodiment-of-the-law"), f(8, "divine-strike"), f(17, "orders-wrath")),
            entry("cleric", "peace", f(1, "emboldening-bond"), f(2, "peaceful-vestments"), f(6, "protective-bond"), f(8, "potent-spellcasting"), f(17, "expansive-bond")),
            entry("cleric", "twilight", f(1, "eyes-of-night"), f(1, "vigilant-blessing"), f(2, "twilight-sanctuary"), f(6, "steps-of-night"), f(8, "divine-strike"), f(17, "twilight-shroud")),
            entry("druid", "land", f(2, "bonus-cantrip"), f(2, "natural-recovery"), f(6, "lands-stride"), f(10, "natures-ward"), f(14, "natures-sanctuary")),
            entry("druid", "moon", f(2, "combat-wild-shape"), f(2, "circle-forms"), f(6, "primal-strike"), f(10, "elemental-wild-shape"), f(14, "thousand-forms")),
            entry("druid", "dreams", f(2, "balm-of-the-summer-court"), f(6, "hearth-of-moonlight-and-shadow"), f(10, "hidden-paths"), f(14, "walker-of-dreams")),
            entry("druid", "shepherd", f(2, "speech-of-the-woods"), f(2, "spirit-totem"), f(6, "mighty-summoner"), f(10, "guardian-spirit"), f(14, "faithful-summons")),
            entry("druid", "spores", f(2, "halo-of-spores"), f(2, "symbiotic-entity"), f(6, "fungal-infestation"), f(10, "spreading-spores"), f(14, "fungal-body")),
            entry("druid", "stars", f(2, "star-map"), f(2, "starry-form"), f(6, "cosmic-omen"), f(10, "twinkling-constellations"), f(14, "full-of-stars")),
            entry("druid", "wildfire", f(2, "summon-wildfire-spirit"), f(2, "enhanced-bond"), f(6, "cauterizing-flames"), f(10, "blazing-revival"), f(14, "combustible")),
            entry("fighter", "champion", f(3, "improved-critical"), f(7, "remarkable-athlete"), f(10, "additional-fighting-style"), f(15, "superior-critical"), f(18, "survivor")),
            entry("fighter", "battle-master", f(3, "combat-superiority"), f(3, "student-of-war"), f(7, "know-your-enemy"), f(10, "improved-combat-superiority"), f(15, "relentless")),
            entry("fighter", "eldritch-knight", f(3, "spellcasting"), f(3, "weapon-bond"), f(7, "war-magic"), f(10, "eldritch-strike"), f(15, "arcane-charge"), f(18, "improved-war-magic")),
            entry("fighter", "arcane-archer", f(3, "arcane-archer-lore"), f(3, "arcane-shot"), f(7, "magic-arrow"), f(7, "curving-shot"), f(15, "ever-ready-shot")),
            entry("fighter", "cavalier", f(3, "bonus-proficiency"), f(3, "unwavering-mark"), f(7, "warding-maneuver"), f(10, "hold-the-line"), f(15, "ferocious-charger")),
            entry("fighter", "samurai", f(3, "bonus-proficiency"), f(3, "fighting-spirit"), f(7, "elegant-courtier"), f(10, "rapid-strike"), f(15, "strength-before-death")),
            entry("fighter", "psi-warrior", f(3, "psionic-power"), f(3, "psionic-strike"), f(7, "telekinetic-adept"), f(10, "bulwark-of-force"), f(15, "telekinetic-mastery"), f(18, "psionic-protection")),
            entry("fighter", "rune-knight", f(3, "bonus-proficiencies"), f(3, "rune-carver"), f(3, "giant-might"), f(7, "runic-shield"), f(10, "great-stature"), f(15, "master-of-runes"), f(18, "runic-juggernaut")),
            entry("fighter", "purple-dragon-knight", f(3, "rallying-cry"), f(7, "royal-envoy"), f(10, "inspiring-surge"), f(15, "bulwark")),
            entry("monk", "open-hand", f(3, "open-hand-technique"), f(6, "wholeness-of-body"), f(11, "tranquility"), f(17, "quivering-palm")),
            entry("monk", "shadow", f(3, "shadow-arts"), f(6, "shadow-step"), f(11, "cloak-of-shadows"), f(17, "opportunist")),
            entry("monk", "four-elements", f(3, "disciple-of-the-elements"), f(3, "elemental-attunement"), f(6, "elemental-disciplines"), f(11, "elemental-disciplines"), f(17, "elemental-disciplines")),
            entry("monk", "kensei", f(3, "path-of-the-kensei"), f(3, "agile-parry"), f(3, "kenseis-shot"), f(6, "way-of-the-brush"), f(6, "deft-strike"), f(11, "sharpen-the-blade"), f(17, "unerring-accuracy"), f(17, "one-with-the-blade")),
            entry("monk", "drunken-master", f(3, "drunken-technique"), f(6, "tipsy-sway"), f(11, "drunkards-luck"), f(17, "intoxicated-frenzy")),
            entry("monk", "sun-soul", f(3, "radiant-sun-bolt"), f(3, "searing-arc-strike"), f(6, "searing-sunburst"), f(11, "sun-shield"), f(17, "one-with-the-sun")),
            entry("monk", "mercy", f(3, "implements-of-mercy"), f(3, "hand-of-healing"), f(3, "hand-of-harm"), f(6, "physicians-touch"), f(6, "flurry-of-healing-and-harm"), f(11, "hand-of-ultimate-mercy")),
            entry("monk", "astral-self", f(3, "arms-of-the-astral-self"), f(6, "visage-of-the-astral-self"), f(11, "body-of-the-astral-self"), f(17, "awakened-astral-self")),
            entry("paladin", "devotion", f(3, "sacred-oath"), f(3, "channel-divinity"), f(7, "aura-of-devotion"), f(15, "purity-of-spirit"), f(20, "holy-nimbus")),
            entry("paladin", "ancients", f(3, "sacred-oath"), f(3, "channel-divinity"), f(7, "aura-of-warding"), f(15, "undying-sentinel"), f(20, "elder-champion")),
            entry("paladin", "vengeance", f(3, "sacred-oath"), f(3, "channel-divinity"), f(7, "relentless-avenger"), f(15, "soul-of-vengeance"), f(20, "avenging-angel")),
            entry("paladin", "conquest", f(3, "sacred-oath"), f(3, "channel-divinity"), f(7, "aura-of-conquest"), f(15, "scornful-rebuke"), f(20, "invincible-conqueror")),
            entry("paladin", "redemption", f(3, "sacred-oath"), f(3, "channel-divinity"), f(7, "aura-of-the-guardian"), f(15, "protective-spirit"), f(20, "emissary-of-peace")),
            entry("paladin", "glory", f(3, "sacred-oath"), f(3, "channel-divinity"), f(7, "aura-of-alacrity"), f(15, "glorious-defense"), f(20, "living-legend")),
            entry("paladin", "watchers", f(3, "sacred-oath"), f(3, "channel-divinity"), f(7, "aura-of-the-sentinel"), f(15, "vigilant-rebuke"), f(20, "mortal-enemies")),
            entry("paladin", "crown", f(3, "sacred-oath"), f(3, "channel-divinity"), f(7, "divine-allegiance"), f(15, "unyielding-spirit"), f(20, "exalted-champion")),
            entry("ranger", "hunter", f(3, "hunters-prey"), f(7, "defensive-tactics"), f(11, "multiattack"), f(15, "superior-hunters-defense")),
            entry("ranger", "beast-master", f(3, "rangers-companion"), f(5, "extra-attack"), f(7, "exceptional-training"), f(11, "bestial-fury"), f(15, "share-spells")),
            entry("ranger", "gloom-stalker", f(3, "dread-ambusher"), f(3, "umbral-sight"), f(7, "iron-mind"), f(11, "stalkers-flurry"), f(15, "shadowy-dodge")),
            entry("ranger", "horizon-walker", f(3, "detect-portal"), f(3, "planar-warrior"), f(7, "ethereal-step"), f(11, "distant-strike"), f(15, "spectral-defense")),
            entry("ranger", "monster-slayer", f(3, "hunters-sense"), f(3, "slayers-prey"), f(7, "supernatural-defense"), f(11, "magic-users-nemesis"), f(15, "slayers-counter")),
            entry("ranger", "fey-wanderer", f(3, "dreadful-strikes"), f(3, "fey-wanderer-magic"), f(3, "otherworldly-glamour"), f(7, "beguiling-twist"), f(11, "fey-reinforcements"), f(15, "misty-wanderer")),
            entry("ranger", "swarmkeeper", f(3, "swarmkeeper-magic"), f(3, "gathered-swarm"), f(7, "writhing-tide"), f(11, "swarming-dispersal"), f(15, "swarm-shield")),
            entry("rogue", "thief", f(3, "fast-hands"), f(3, "second-story-work"), f(9, "supreme-sneak"), f(13, "use-magic-device"), f(17, "thiefs-reflexes")),
            entry("rogue", "assassin", f(3, "assassinate"), f(3, "bonus-proficiencies"), f(9, "infiltration-expertise"), f(13, "impostor"), f(17, "death-strike")),
            entry("rogue", "arcane-trickster", f(3, "spellcasting"), f(3, "mage-hand-legerdemain"), f(9, "magical-ambush"), f(13, "versatile-trickster"), f(17, "spell-thief")),
            entry("rogue", "inquisitive", f(3, "ear-for-deceit"), f(3, "eye-for-detail"), f(3, "insightful-fighting"), f(9, "steady-eye"), f(13, "unerring-eye"), f(17, "eye-for-weakness")),
            entry("rogue", "mastermind", f(3, "master-of-intrigue"), f(3, "master-of-tactics"), f(9, "insightful-manipulator"), f(13, "misdirection"), f(17, "soul-of-deceit")),
            entry("rogue", "scout", f(3, "skirmisher"), f(3, "survivalist"), f(9, "superior-mobility"), f(13, "ambush-master"), f(17, "sudden-strike")),
            entry("rogue", "swashbuckler", f(3, "fancy-footwork"), f(3, "rakish-audacity"), f(9, "panache"), f(13, "elegant-maneuver"), f(17, "master-duelist")),
            entry("rogue", "phantom", f(3, "whispers-of-the-dead"), f(3, "wails-from-the-grave"), f(9, "tokens-of-the-departed"), f(13, "ghost-walk"), f(17, "deaths-friend")),
            entry("rogue", "soulknife", f(3, "psionic-power"), f(3, "psychic-blades"), f(3, "steady-mind"), f(9, "psi-bolstered-knack"), f(13, "psychic-teleportation"), f(17, "rend-mind")),
            entry("sorcerer", "draconic", f(1, "draconic-resilience"), f(6, "elemental-affinity"), f(14, "dragon-wings"), f(18, "draconic-presence")),
            entry("sorcerer", "wild-magic", f(1, "wild-magic-surge"), f(1, "tides-of-chaos"), f(6, "bend-luck"), f(14, "controlled-chaos"), f(18, "spell-bombardment")),
            entry("sorcerer", "storm", f(1, "tempestuous-magic"), f(6, "heart-of-the-storm"), f(6, "storm-guide"), f(14, "storms-fury"), f(18, "wind-soul")),
            entry("sorcerer", "divine-soul", f(1, "divine-magic"), f(1, "favored-by-the-gods"), f(6, "empowered-healing"), f(14, "otherworldly-wings"), f(18, "unearthly-recovery")),
            entry("sorcerer", "shadow-magic", f(1, "eyes-of-the-dark"), f(1, "strength-of-the-grave"), f(6, "hound-of-ill-omen"), f(14, "shadow-walk"), f(18, "umbral-form")),
            entry("sorcerer", "aberrant-mind", f(1, "psionic-spells"), f(1, "telepathic-speech"), f(6, "psychic-defenses"), f(14, "revelation-in-flesh"), f(18, "warping-implosion")),
            entry("sorcerer", "clockwork-soul", f(1, "clockwork-magic"), f(1, "restore-balance"), f(6, "bastion-of-law"), f(14, "trance-of-order"), f(18, "clockwork-cavalcade")),
            entry("warlock", "fiend", f(1, "dark-ones-blessing"), f(6, "dark-ones-own-luck"), f(10, "fiendish-resilience"), f(14, "hurl-through-hell")),
            entry("warlock", "archfey", f(1, "fey-presence"), f(6, "misty-escape"), f(10, "beguiling-defenses"), f(14, "dark-delirium")),
            entry("warlock", "great-old-one", f(1, "awakened-mind"), f(6, "entropic-ward"), f(10, "thought-shield"), f(14, "create-thrall")),
            entry("warlock", "celestial", f(1, "bonus-cantrips"), f(1, "healing-light"), f(6, "radiant-soul"), f(10, "celestial-resilience"), f(14, "searing-vengeance")),
            entry("warlock", "hexblade", f(1, "hexblades-curse"), f(1, "hex-warrior"), f(6, "accursed-specter"), f(10, "armor-of-hexes"), f(14, "master-of-hexes")),
            entry("warlock", "undying", f(1, "among-the-dead"), f(6, "defy-death"), f(10, "undying-nature"), f(14, "indestructible-life")),
            entry("warlock", "genie", f(1, "genies-vessel"), f(1, "genies-wrath"), f(6, "elemental-gift"), f(10, "sanctuary-vessel"), f(14, "limited-wish")),
            entry("wizard", "evocation", f(2, "evocation-savant"), f(2, "sculpt-spells"), f(6, "potent-cantrip"), f(10, "empowered-evocation"), f(14, "overchannel")),
            entry("wizard", "abjuration", f(2, "arcane-ward"), f(2, "abjuration-savant"), f(6, "projected-ward"), f(10, "improved-abjuration"), f(14, "spell-resistance")),
            entry("wizard", "conjuration", f(2, "conjuration-savant"), f(2, "minor-conjuration"), f(6, "benign-transposition"), f(10, "focused-conjuration"), f(14, "durable-summons")),
            entry("wizard", "divination", f(2, "divination-savant"), f(2, "portent"), f(6, "expert-divination"), f(10, "the-third-eye"), f(14, "greater-portent")),
            entry("wizard", "enchantment", f(2, "enchantment-savant"), f(2, "hypnotic-gaze"), f(6, "instinctive-charm"), f(10, "split-enchantment"), f(14, "alter-memories")),
            entry("wizard", "illusion", f(2, "illusion-savant"), f(2, "improved-minor-illusion"), f(6, "malleable-illusions"), f(10, "illusory-self"), f(14, "illusory-reality")),
            entry("wizard", "necromancy", f(2, "necromancy-savant"), f(2, "grim-harvest"), f(6, "undead-thralls"), f(10, "inured-to-undeath"), f(14, "command-undead")),
            entry("wizard", "transmutation", f(2, "transmutation-savant"), f(2, "minor-alchemy"), f(6, "transmuters-stone"), f(10, "shapechanger"), f(14, "master-transmuter")),
            entry("wizard", "bladesinging", f(2, "training-in-war-and-song"), f(2, "bladesong"), f(6, "extra-attack"), f(10, "song-of-defense"), f(14, "song-of-victory")),
            entry("wizard", "war-magic", f(2, "arcane-deflection"), f(2, "tactical-wit"), f(6, "power-surge"), f(10, "durable-magic"), f(14, "deflecting-shroud")),
            entry("wizard", "scribes", f(2, "wizardly-quill"), f(2, "awakened-spellbook"), f(6, "manifest-mind"), f(10, "master-scrivener"), f(14, "one-with-the-word")));

    private static Map<String, List<SubclassRef>> subclasses(Entry... entries) {
        Map<String, List<SubclassRef>> map = new LinkedHashMap<>();
        for (Entry entry : entries) {
            List<SubclassRef> refs = java.util.Arrays.stream(entry.refs()).toList();
            map.put(entry.classIndex(), List.copyOf(refs));
        }
        return Map.copyOf(map);
    }

    private static Map<String, List<SubclassFeatureRef>> featureMap(FeatureEntry... entries) {
        Map<String, List<SubclassFeatureRef>> map = new LinkedHashMap<>();
        for (FeatureEntry entry : entries) {
            List<SubclassFeatureRef> refs = java.util.Arrays.stream(entry.refs()).toList();
            map.put(entry.classIndex() + "/" + entry.subclassIndex(), List.copyOf(refs));
            boolean declared = SUBCLASSES.getOrDefault(entry.classIndex(), List.of()).stream()
                    .anyMatch(ref -> ref.index().equals(entry.subclassIndex()));
            if (!declared) {
                throw new IllegalArgumentException("subclass feature entry not in catalog: " + entry.subclassIndex());
            }
        }
        return Map.copyOf(map);
    }

    private static SubclassFeatureRef f(int level, String feature) {
        return new SubclassFeatureRef(null, level, feature);
    }

    private static FeatureEntry entry(String classIndex, String subclassIndex, SubclassFeatureRef... refs) {
        SubclassFeatureRef[] bound = new SubclassFeatureRef[refs.length];
        for (int i = 0; i < refs.length; i++) {
            bound[i] = new SubclassFeatureRef(subclassIndex, refs[i].level(), refs[i].feature());
        }
        return new FeatureEntry(classIndex, subclassIndex, bound);
    }

    private static Entry entry(String classIndex, int level, SubclassRef... refs) {
        for (SubclassRef ref : refs) {
            if (!ref.classIndex().equals(classIndex)) {
                throw new IllegalArgumentException("subclass classIndex mismatch for " + ref.index());
            }
        }
        return new Entry(classIndex, refs);
    }

    private record Entry(String classIndex, SubclassRef[] refs) {
    }

    private record FeatureEntry(String classIndex, String subclassIndex, SubclassFeatureRef[] refs) {
    }

    public List<BackgroundRef> backgrounds() {
        return BACKGROUNDS;
    }

    public Set<String> backgroundIndexes() {
        Set<String> indexes = new LinkedHashSet<>();
        for (BackgroundRef background : BACKGROUNDS) {
            indexes.add(background.index());
        }
        return indexes;
    }

    public List<SubclassRef> subclasses() {
        return SUBCLASSES.values().stream().flatMap(List::stream).toList();
    }

    public Set<String> subclassesFor(String classIndex) {
        Set<String> indexes = new LinkedHashSet<>();
        for (SubclassRef ref : SUBCLASSES.getOrDefault(classIndex, List.of())) {
            indexes.add(ref.index());
        }
        return indexes;
    }

    public SubclassRef subclassOf(String classIndex, String subclassIndex) {
        return SUBCLASSES.getOrDefault(classIndex, List.of()).stream()
                .filter(ref -> ref.index().equals(subclassIndex))
                .findFirst()
                .orElse(null);
    }

    /**
     * Curated subclass features for {@code classIndex}/{@code subclassIndex}
     * (levels included as declared in {@link #SUBCLASS_FEATURES}), or empty when
     * the subclass is not curated.
     */
    public List<SubclassFeatureRef> subclassFeatures(String classIndex, String subclassIndex) {
        return SUBCLASS_FEATURES.getOrDefault(classIndex + "/" + subclassIndex, List.of());
    }
}