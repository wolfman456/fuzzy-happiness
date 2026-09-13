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

    private static Map<String, List<SubclassRef>> subclasses(Entry... entries) {
        Map<String, List<SubclassRef>> map = new LinkedHashMap<>();
        for (Entry entry : entries) {
            List<SubclassRef> refs = java.util.Arrays.stream(entry.refs()).toList();
            map.put(entry.classIndex(), List.copyOf(refs));
        }
        return Map.copyOf(map);
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
}