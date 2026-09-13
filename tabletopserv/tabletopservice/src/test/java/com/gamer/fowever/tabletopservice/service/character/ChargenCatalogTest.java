package com.gamer.fowever.tabletopservice.service.character;

import com.gamer.fowever.tabletopservice.service.character.ChargenCatalog.BackgroundRef;
import com.gamer.fowever.tabletopservice.service.character.ChargenCatalog.SubclassRef;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ChargenCatalogTest {

    private final ChargenCatalog catalog = new ChargenCatalog();

    @Test
    void backgroundsCoverTheThirteenPhbOptions() {
        assertThat(catalog.backgrounds()).hasSize(13);
        assertThat(catalog.backgroundIndexes()).containsExactlyInAnyOrder(
                "acolyte", "charlatan", "criminal", "entertainer", "folk-hero", "guild-artisan",
                "hermit", "noble", "outlander", "sage", "sailor", "soldier", "urchin");
    }

    @Test
    void backgroundIndexesAreUnique() {
        assertThat(catalog.backgroundIndexes())
                .hasSize(new HashSet<>(catalog.backgroundIndexes()).size());
    }

    @Test
    void subclassesCoverEveryClass() {
        Set<String> classes = new HashSet<>();
        for (SubclassRef ref : catalog.subclasses()) {
            classes.add(ref.classIndex());
        }
        assertThat(classes).containsExactlyInAnyOrder(
                "barbarian", "bard", "cleric", "druid", "fighter", "monk", "paladin",
                "ranger", "rogue", "sorcerer", "warlock", "wizard");
    }

    @Test
    void subclassPairsAreUnique() {
        Set<String> pairs = new HashSet<>();
        for (SubclassRef ref : catalog.subclasses()) {
            pairs.add(ref.classIndex() + "/" + ref.index());
        }
        assertThat(pairs).hasSize(catalog.subclasses().size());
    }

    @Test
    void subclassLevelsFollowThePhb() {
        assertThat(catalog.subclassOf("cleric", "light").level()).isEqualTo(1);
        assertThat(catalog.subclassOf("sorcerer", "wild-magic").level()).isEqualTo(1);
        assertThat(catalog.subclassOf("warlock", "archfey").level()).isEqualTo(1);
        assertThat(catalog.subclassOf("wizard", "evocation").level()).isEqualTo(2);
        assertThat(catalog.subclassOf("druid", "moon").level()).isEqualTo(2);
        assertThat(catalog.subclassOf("fighter", "battle-master").level()).isEqualTo(3);
        assertThat(catalog.subclassOf("paladin", "vengeance").level()).isEqualTo(3);
        assertThat(catalog.subclassOf("rogue", "arcane-trickster").level()).isEqualTo(3);
        assertThat(catalog.subclassOf("barbarian", "totem-warrior").level()).isEqualTo(3);
    }

    @Test
    void srdArchetypesKeepTheirCanonicalIndex() {
        assertThat(catalog.subclassOf("barbarian", "berserker")).isNotNull();
        assertThat(catalog.subclassOf("bard", "lore")).isNotNull();
        assertThat(catalog.subclassOf("cleric", "life")).isNotNull();
        assertThat(catalog.subclassOf("druid", "land")).isNotNull();
        assertThat(catalog.subclassOf("fighter", "champion")).isNotNull();
        assertThat(catalog.subclassOf("monk", "open-hand")).isNotNull();
        assertThat(catalog.subclassOf("paladin", "devotion")).isNotNull();
        assertThat(catalog.subclassOf("ranger", "hunter")).isNotNull();
        assertThat(catalog.subclassOf("rogue", "thief")).isNotNull();
        assertThat(catalog.subclassOf("sorcerer", "draconic")).isNotNull();
        assertThat(catalog.subclassOf("warlock", "fiend")).isNotNull();
        assertThat(catalog.subclassOf("wizard", "evocation")).isNotNull();
    }

    @Test
    void subclassCatalogCoversAllOfficial2014Options() {
        assertThat(catalog.subclassesFor("barbarian")).hasSize(8)
                .contains("berserker", "totem-warrior", "ancestral-guardian", "storm-herald",
                        "zealot", "beast", "wild-magic", "battlerager");
        assertThat(catalog.subclassesFor("bard")).hasSize(7);
        assertThat(catalog.subclassesFor("cleric")).hasSize(13)
                .contains("arcana", "forge", "grave", "order", "peace", "twilight");
        assertThat(catalog.subclassesFor("druid")).hasSize(7);
        assertThat(catalog.subclassesFor("fighter")).hasSize(9)
                .contains("arcane-archer", "cavalier", "samurai", "psi-warrior", "rune-knight",
                        "purple-dragon-knight");
        assertThat(catalog.subclassesFor("monk")).hasSize(8);
        assertThat(catalog.subclassesFor("paladin")).hasSize(8);
        assertThat(catalog.subclassesFor("ranger")).hasSize(7);
        assertThat(catalog.subclassesFor("rogue")).hasSize(9);
        assertThat(catalog.subclassesFor("sorcerer")).hasSize(7);
        assertThat(catalog.subclassesFor("warlock")).hasSize(7);
        assertThat(catalog.subclassesFor("wizard")).hasSize(11)
                .contains("bladesinging", "war-magic", "scribes");
    }

    @Test
    void curatorSubclassLevelsMatchOfficialUnlockLevels() {
        assertThat(catalog.subclasses()).allSatisfy(ref -> {
            int level = switch (ref.classIndex()) {
                case "cleric", "sorcerer", "warlock" -> 1;
                case "druid", "wizard" -> 2;
                default -> 3;
            };
            assertThat(ref.level()).as(ref.classIndex() + "/" + ref.index()).isEqualTo(level);
        });
    }

    @Test
    void subclassesForAndSubclassOfBehave() {
        assertThat(catalog.subclassesFor("cleric")).contains("life", "light", "knowledge");
        assertThat(catalog.subclassesFor("not-a-class")).isEmpty();
        assertThat(catalog.subclassOf("cleric", "nope")).isNull();
        assertThat(catalog.subclassOf("not-a-class", "life")).isNull();
        assertThat(catalog.subclasses()).extracting("index").contains("light", "moon", "evocation");
    }

    @Test
    void backgroundRefsCarryNames() {
        assertThat(catalog.backgrounds()).extracting(BackgroundRef::index).contains("noble");
        assertThat(catalog.backgrounds()).extracting(BackgroundRef::name)
                .contains("Noble", "Folk Hero", "Guild Artisan");
    }
}