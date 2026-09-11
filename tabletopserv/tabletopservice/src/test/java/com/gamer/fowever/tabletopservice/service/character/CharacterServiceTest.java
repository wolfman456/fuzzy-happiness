package com.gamer.fowever.tabletopservice.service.character;

import com.gamer.fowever.tabletopapi.ScoreSource;
import com.gamer.fowever.tabletopapi.dto.CharacterDraftDto;
import com.gamer.fowever.tabletopapi.dto.CharacterSheetDto;
import com.gamer.fowever.tabletopapi.dto.CharacterSummaryDto;
import com.gamer.fowever.tabletopapi.dto.CompileResult;
import com.gamer.fowever.tabletopapi.dto.GenerateCharacterRequest;
import com.gamer.fowever.tabletopapi.dto.RollScoresRequest;
import com.gamer.fowever.tabletopapi.dto.RollScoresResult;
import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.domain.Character;
import com.gamer.fowever.tabletopservice.domain.Dnd5eCharacter;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.repository.CharacterRepository;
import com.gamer.fowever.tabletopservice.service.SrdClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CharacterServiceTest {

    private static final String RACES = "{\"results\":[{\"index\":\"dwarf\"}]}";
    private static final String CLASSES = "{\"results\":[{\"index\":\"cleric\"}]}";
    private static final String BACKGROUNDS = "{\"results\":[{\"index\":\"acolyte\"}]}";
    private static final String SKILLS = "{\"results\":[{\"index\":\"medicine\"},{\"index\":\"religion\"}]}";
    private static final String EQUIPMENT = "{\"results\":[{\"index\":\"leather-armor\"},{\"index\":\"shield\"},{\"index\":\"club\"}]}";
    private static final String DWARF = "{\"index\":\"dwarf\",\"name\":\"Dwarf\",\"speed\":25,"
            + "\"ability_bonuses\":[{\"ability_score\":{\"index\":\"con\"},\"bonus\":2}]}";
    private static final String CLERIC = "{\"index\":\"cleric\",\"name\":\"Cleric\",\"hit_die\":8,"
            + "\"subclass_level\":1,\"spellcasting\":{},\"saving_throws\":[{\"index\":\"wis\"},{\"index\":\"cha\"}],"
            + "\"proficiency_choices\":[{\"choose\":2,\"from\":{\"options\":["
            + "{\"item\":{\"index\":\"skill-medicine\"}},{\"item\":{\"index\":\"skill-religion\"}}]}}],"
            + "\"subclasses\":[{\"index\":\"life\"}]}";
    private static final String CLERIC_LEVELS = "[{\"level\":1,\"prof_bonus\":2,\"features\":[{\"index\":\"spellcasting\"}],"
            + "\"spellcasting\":{\"cantrips_known\":3,\"spell_slots_level_1\":2}},"
            + "{\"level\":2,\"prof_bonus\":2,\"features\":[{\"index\":\"channel-divinity\"}],"
            + "\"spellcasting\":{\"cantrips_known\":3,\"spell_slots_level_1\":3}}]";
    private static final String CLERIC_SPELLS = "{\"count\":2,\"results\":["
            + "{\"index\":\"sacred-flame\",\"level\":0},{\"index\":\"cure-wounds\",\"level\":1}]}";
    private static final String LEATHER = "{\"index\":\"leather-armor\",\"equipment_category\":{\"index\":\"armor\"},"
            + "\"armor_class\":{\"base\":11,\"dex_bonus\":true},"
            + "\"cost\":{\"quantity\":10,\"unit\":\"gp\"}}";
    private static final String SHIELD = "{\"index\":\"shield\",\"equipment_category\":{\"index\":\"armor\"},"
            + "\"armor_class\":{\"base\":2,\"dex_bonus\":false},"
            + "\"cost\":{\"quantity\":10,\"unit\":\"gp\"}}";
    private static final String CLUB = "{\"index\":\"club\",\"equipment_category\":{\"index\":\"weapon\"},"
            + "\"cost\":{\"quantity\":1,\"unit\":\"sp\"}}";

    @Mock
    private SrdClient srd;
    @Mock
    private CharacterRepository characterRepository;

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    private CharacterService service;

    @BeforeEach
    void setUp() {
        service = new CharacterService(srd, characterRepository, objectMapper);
    }

    @Test
    void compileDerivesLegalStandardArraySheet() {
        stubCommonCatalog();
        CharacterDraftDto draft = legalDraft();

        CompileResult result = service.compile(user(5L), draft);

        assertThat(result.valid()).isTrue();
        CharacterSheetDto sheet = result.sheet();
        assertThat(sheet.name()).isEqualTo("Tordek");
        assertThat(sheet.level()).isEqualTo(1);
        assertThat(sheet.raceIndex()).isEqualTo("dwarf");
        assertThat(sheet.classIndex()).isEqualTo("cleric");
        assertThat(sheet.subclassIndex()).isEqualTo("life");
        assertThat(sheet.backgroundIndex()).isEqualTo("acolyte");
        assertThat(sheet.proficiencyBonus()).isEqualTo(2);
        assertThat(sheet.hitPoints()).isEqualTo(10);
        assertThat(sheet.armorClass()).isEqualTo(15);
        assertThat(sheet.speedFeet()).isEqualTo(25);
        assertThat(sheet.savingThrows()).containsExactly("cha", "wis");
        assertThat(sheet.classSkills()).containsExactly("medicine", "religion");
        assertThat(sheet.skillPicks()).containsExactly("medicine", "religion");
        assertThat(sheet.spellIndexes()).containsExactly("cure-wounds", "sacred-flame");
        assertThat(sheet.spellSlots()).containsEntry(0, 3).containsEntry(1, 2);
        assertThat(sheet.featureIndexes()).contains("spellcasting");
        assertThat(sheet.sheetSnapshot()).isNotNull();
        assertThat(sheet.startingGoldGp()).isEqualTo(125);
        assertThat(sheet.spentGoldGp()).isEqualTo(20);

        verify(srd).subresource("classes", "cleric", "levels", Map.of());
    }

    @Test
    void compileRejectsIllegalScoreSet() {
        stubCommonCatalog();
        CharacterDraftDto draft = draftBuilder(legalDraft()).strength(18).build();

        CompileResult result = service.compile(user(5L), draft);

        assertThat(result.valid()).isFalse();
        assertThat(result.sheet()).isNull();
        assertThat(result.violations()).anyMatch(v -> v.contains("scores"));
    }

    @Test
    void compileRejectsScoresThatAlreadyIncludeTheRaceBonus() {
        stubCommonCatalog();
        // A revise-from-quick-build draft carries the FINAL sheet scores (con 15 + dwarf +2 = 17);
        // the backend subtracts the bonus again, so this must be rejected rather than double-counted.
        CharacterDraftDto draft = draftBuilder(legalDraft()).constitution(17).build();

        CompileResult result = service.compile(user(5L), draft);

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("scores"));
    }

    @Test
    void compileRejectsUnknownRace() {
        stubCommonCatalog();

        CompileResult result = service.compile(user(5L), draftBuilder(legalDraft()).raceIndex("drow").build());

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("drow"));
    }

    @Test
    void compileRejectsSpellNotOnClassList() {
        stubCommonCatalog();
        CharacterDraftDto draft = draftBuilder(legalDraft())
                .spellIndexes(Set.of("fireball", "sacred-flame")).build();

        CompileResult result = service.compile(user(5L), draft);

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("fireball"));
    }

    @Test
    void compileRejectsUnknownEquipment() {
        stubCommonCatalog();
        CharacterDraftDto draft = draftBuilder(legalDraft()).equipmentIndexes(Set.of("vorpal-sword")).build();

        CompileResult result = service.compile(user(5L), draft);

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("vorpal-sword"));
    }

    @Test
    void compileDerivesClassStartingGoldAndSpend() {
        stubCommonCatalog();

        CompileResult result = service.compile(user(5L), legalDraft());

        assertThat(result.valid()).isTrue();
        assertThat(result.sheet().startingGoldGp()).isEqualTo(125);
        assertThat(result.sheet().spentGoldGp()).isEqualTo(20);
    }

    @Test
    void compileRejectsEquipmentOverStartingGoldBudget() {
        stubCommonCatalog();
        when(srd.detail("equipment", "shield")).thenReturn(objectMapper.readTree(
                "{\"index\":\"shield\",\"equipment_category\":{\"index\":\"armor\"},"
                        + "\"cost\":{\"quantity\":2000,\"unit\":\"gp\"}}"));
        CharacterDraftDto draft = draftBuilder(legalDraft()).equipmentIndexes(Set.of("shield")).build();

        CompileResult result = service.compile(user(5L), draft);

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("exceeds"));
    }

    @Test
    void rollScoresProducesBaseScoresWithinRange() {
        RollScoresResult result = service.rollScores(new RollScoresRequest(ScoreSource.FOUR_D6_DROP_LOWEST, 7));

        assertThat(result.scoreSource()).isEqualTo(ScoreSource.FOUR_D6_DROP_LOWEST);
        assertThat(result.strength()).isBetween(3, 18);
        assertThat(result.dexterity()).isBetween(3, 18);
        assertThat(result.constitution()).isBetween(3, 18);
        assertThat(result.intelligence()).isBetween(3, 18);
        assertThat(result.wisdom()).isBetween(3, 18);
        assertThat(result.charisma()).isBetween(3, 18);
    }

    @Test
    void rollScoresCoversHouseRuleRange() {
        RollScoresResult result = service.rollScores(new RollScoresRequest(ScoreSource.HOUSE_RULE_D20, 4));

        assertThat(result.strength()).isBetween(1, 30);
        assertThat(result.dexterity()).isBetween(1, 30);
        assertThat(result.constitution()).isBetween(1, 30);
        assertThat(result.intelligence()).isBetween(1, 30);
        assertThat(result.wisdom()).isBetween(1, 30);
        assertThat(result.charisma()).isBetween(1, 30);
    }

    @Test
    void rollScoresRejectsAssignedSources() {
        assertThatThrownBy(() -> service.rollScores(new RollScoresRequest(ScoreSource.STANDARD_ARRAY, 1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not a rolled score method");
        assertThatThrownBy(() -> service.rollScores(new RollScoresRequest(ScoreSource.POINT_BUY, 1)))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not a rolled score method");
    }

    @Test
    void compileRejectsSubclassOfAnotherClass() {
        stubCommonCatalog();
        CharacterDraftDto draft = draftBuilder(legalDraft()).subclassIndex("berserker").build();

        CompileResult result = service.compile(user(5L), draft);

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("berserker"));
    }

    @Test
    void compileToleratesUnknownRaceWithoutNpe() {
        stubCommonCatalog();
        CharacterDraftDto draft = draftBuilder(legalDraft())
                .raceIndex("drow")
                .strength(15).dexterity(14).constitution(13).intelligence(12).wisdom(10).charisma(8)
                .classIndex("cleric").subclassIndex("life").backgroundIndex("acolyte").build();

        CompileResult result = service.compile(user(5L), draft);

        assertThat(result.valid()).isFalse();
        assertThat(result.violations()).anyMatch(v -> v.contains("valid race"));
        assertThat(result.violations()).noneMatch(v -> v.contains("scores"));
    }

    @Test
    void createPersistsValidatedSheet() {
        stubCommonCatalog();
        when(characterRepository.saveAndFlush(any(Character.class)))
                .thenAnswer(invocation -> {
                    Dnd5eCharacter entity = invocation.getArgument(0);
                    entity.setId(11L);
                    return entity;
                });

        CharacterSheetDto sheet = service.create(user(5L), legalDraft());

        assertThat(sheet.id()).isEqualTo(11L);
        ArgumentCaptor<Dnd5eCharacter> captor = ArgumentCaptor.forClass(Dnd5eCharacter.class);
        verify(characterRepository).saveAndFlush(captor.capture());
        Dnd5eCharacter saved = captor.getValue();
        assertThat(saved.getOwner().getId()).isEqualTo(5L);
        assertThat(saved.getName()).isEqualTo("Tordek");
        assertThat(saved.getRaceIndex()).isEqualTo("dwarf");
        assertThat(saved.getClassIndex()).isEqualTo("cleric");
        assertThat(saved.getSubclassIndex()).isEqualTo("life");
        assertThat(saved.getBackgroundIndex()).isEqualTo("acolyte");
        assertThat(saved.getHitPoints()).isEqualTo(10);
        assertThat(saved.getArmorClass()).isEqualTo(15);
        assertThat(saved.getSpeedFeet()).isEqualTo(25);
        assertThat(saved.getEquipmentIndexes()).contains("leather-armor", "shield");
        assertThat(saved.getSheetSnapshot()).contains("\"name\":\"Tordek\"");
    }

    @Test
    void createRejectsIllegalDraft() {
        stubCommonCatalog();
        CharacterDraftDto draft = draftBuilder(legalDraft()).strength(18).build();

        assertThatThrownBy(() -> service.create(user(5L), draft))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not legal");
        verify(characterRepository, never()).saveAndFlush(any());
    }

    @Test
    void mineMapsSummaries() {
        Dnd5eCharacter one = savedCharacter(1L, "Tordek");
        Dnd5eCharacter two = savedCharacter(2L, "Bronn");
        when(characterRepository.findByOwnerIdOrderByIdDesc(5L)).thenReturn(List.of(two, one));

        List<CharacterSummaryDto> result = service.mine(user(5L));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).name()).isEqualTo("Bronn");
        assertThat(result.get(1).id()).isEqualTo(1L);
    }

    @Test
    void getReturnsSnapshotRePairedWithId() {
        CharacterSheetDto sheet = new CharacterSheetDto(
                null, "Tordek", 1, ScoreSource.STANDARD_ARRAY, "dwarf", "cleric", "life", "acolyte",
                15, 14, 13, 12, 10, 8, 2, 10, 13, 25,
                List.of("cha", "wis"),
                List.of("skill-medicine", "skill-religion"),
                List.of("skill-medicine", "skill-religion"),
                List.of("skill-medicine", "skill-religion"),
                List.of("cure-wounds", "sacred-flame"),
                Map.of(0, 3, 1, 2),
                List.of("spellcasting"),
                List.of("leather-armor", "shield"),
                125, 25,
                null);
        Dnd5eCharacter entity = savedCharacter(7L, "Tordek");
        entity.setSheetSnapshot(objectMapper.writeValueAsString(sheet));
        when(characterRepository.findByIdAndOwnerId(7L, 5L)).thenReturn(Optional.of(entity));

        CharacterSheetDto result = service.get(user(5L), 7L);

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.name()).isEqualTo("Tordek");
        assertThat(result.spellSlots()).containsEntry(1, 2);
        assertThat(result.startingGoldGp()).isEqualTo(125);
        assertThat(result.spentGoldGp()).isEqualTo(25);
    }

    @Test
    void getRejectsForeignCharacter() {
        when(characterRepository.findByIdAndOwnerId(9L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(user(5L), 9L))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void generateProducesLegalSheetWithSeededRandom() {
        when(srd.list("races", Map.of())).thenReturn(objectMapper.readTree(RACES));
        when(srd.list("classes", Map.of())).thenReturn(objectMapper.readTree(CLASSES));
        when(srd.list("backgrounds", Map.of())).thenReturn(objectMapper.readTree(BACKGROUNDS));
        when(srd.list("skills", Map.of())).thenReturn(objectMapper.readTree(SKILLS));
        when(srd.list("equipment", Map.of())).thenReturn(objectMapper.readTree(EQUIPMENT));

        when(srd.detail("races", "dwarf")).thenReturn(objectMapper.readTree(DWARF));
        when(srd.detail("classes", "cleric")).thenReturn(objectMapper.readTree(CLERIC));
        when(srd.detail("backgrounds", "acolyte")).thenReturn(objectMapper.readTree(
                "{\"index\":\"acolyte\",\"name\":\"Acolyte\",\"starting_equipment\":[]}"));
        when(srd.subresource("classes", "cleric", "levels", Map.of())).thenReturn(objectMapper.readTree(CLERIC_LEVELS));
        when(srd.subresource("classes", "cleric", "spells", Map.of())).thenReturn(objectMapper.readTree(CLERIC_SPELLS));
        stubEquipmentDetails();

        CompileResult result = service.generate(user(5L),
                new GenerateCharacterRequest("Surprise", ScoreSource.STANDARD_ARRAY, 1, 7));

        assertThat(result.valid()).isTrue();
        CharacterSheetDto sheet = result.sheet();
        assertThat(sheet.raceIndex()).isEqualTo("dwarf");
        assertThat(sheet.classIndex()).isEqualTo("cleric");
        assertThat(sheet.backgroundIndex()).isEqualTo("acolyte");
        assertThat(sheet.strength()).isBetween(3, 18);
        assertThat(sheet.dexterity()).isBetween(3, 18);
        assertThat(sheet.constitution()).isBetween(3, 18);
        assertThat(sheet.intelligence()).isBetween(3, 18);
        assertThat(sheet.wisdom()).isBetween(3, 18);
        assertThat(sheet.charisma()).isBetween(3, 18);
    }

    @Test
    void generateShowsSomethingWhenNameBlank() {
        when(srd.list("races", Map.of())).thenReturn(objectMapper.readTree(RACES));
        when(srd.list("classes", Map.of())).thenReturn(objectMapper.readTree(CLASSES));
        when(srd.list("backgrounds", Map.of())).thenReturn(objectMapper.readTree(BACKGROUNDS));
        when(srd.list("skills", Map.of())).thenReturn(objectMapper.readTree(SKILLS));
        when(srd.list("equipment", Map.of())).thenReturn(objectMapper.readTree(EQUIPMENT));
        when(srd.detail("races", "dwarf")).thenReturn(objectMapper.readTree(DWARF));
        when(srd.detail("classes", "cleric")).thenReturn(objectMapper.readTree(CLERIC));
        when(srd.detail("backgrounds", "acolyte")).thenReturn(objectMapper.readTree(
                "{\"index\":\"acolyte\"}"));
        when(srd.subresource("classes", "cleric", "levels", Map.of())).thenReturn(objectMapper.readTree(CLERIC_LEVELS));
        when(srd.subresource("classes", "cleric", "spells", Map.of())).thenReturn(objectMapper.readTree(CLERIC_SPELLS));
        stubEquipmentDetails();

        CompileResult result = service.generate(user(5L), new GenerateCharacterRequest("", null, 1, 3));

        assertThat(result.valid()).isTrue();
        assertThat(result.sheet().name()).isEqualTo("Surprise Me");
    }

    private void stubEquipmentDetails() {
        when(srd.detail("equipment", "leather-armor")).thenReturn(objectMapper.readTree(LEATHER));
        when(srd.detail("equipment", "shield")).thenReturn(objectMapper.readTree(SHIELD));
        when(srd.detail("equipment", "club")).thenReturn(objectMapper.readTree(CLUB));
        when(srd.detail("equipment", "dagger")).thenReturn(objectMapper.readTree(
                "{\"index\":\"dagger\",\"equipment_category\":{\"index\":\"weapon\"}}"));
        when(srd.detail("equipment", "staff")).thenReturn(objectMapper.readTree(
                "{\"index\":\"staff\",\"equipment_category\":{\"index\":\"weapon\"}}"));
        when(srd.detail("equipment", "light-crossbow")).thenReturn(objectMapper.readTree(
                "{\"index\":\"light-crossbow\",\"equipment_category\":{\"index\":\"weapon\"}}"));
    }

    private void stubCommonCatalog() {
        stubCatalog();
        when(srd.detail("races", "dwarf")).thenReturn(objectMapper.readTree(DWARF));
        when(srd.detail("classes", "cleric")).thenReturn(objectMapper.readTree(CLERIC));
        when(srd.detail("backgrounds", "acolyte")).thenReturn(objectMapper.readTree(
                "{\"index\":\"acolyte\",\"name\":\"Acolyte\"}"));
        when(srd.subresource("classes", "cleric", "levels", Map.of())).thenReturn(objectMapper.readTree(CLERIC_LEVELS));
        when(srd.subresource("classes", "cleric", "spells", Map.of())).thenReturn(objectMapper.readTree(CLERIC_SPELLS));
        when(srd.detail("equipment", "leather-armor")).thenReturn(objectMapper.readTree(LEATHER));
        when(srd.detail("equipment", "shield")).thenReturn(objectMapper.readTree(SHIELD));
        when(srd.detail("equipment", "club")).thenReturn(objectMapper.readTree(CLUB));
    }

    private void stubCatalog() {
        when(srd.list("races", Map.of())).thenReturn(objectMapper.readTree(RACES));
        when(srd.list("classes", Map.of())).thenReturn(objectMapper.readTree(CLASSES));
        when(srd.list("backgrounds", Map.of())).thenReturn(objectMapper.readTree(BACKGROUNDS));
        when(srd.list("skills", Map.of())).thenReturn(objectMapper.readTree(SKILLS));
        when(srd.list("equipment", Map.of())).thenReturn(objectMapper.readTree(EQUIPMENT));
    }

    private CharacterDraftDto legalDraft() {
        return new CharacterDraftDto(
                "Tordek", 15, 14, 15, 12, 10, 8, ScoreSource.STANDARD_ARRAY, 1,
                "dwarf", "cleric", "life", "acolyte",
                Set.of("skill-medicine", "skill-religion"),
                Set.of("sacred-flame", "cure-wounds"),
                Set.of("leather-armor", "shield"));
    }

    private DraftBuilder draftBuilder(CharacterDraftDto template) {
        return new DraftBuilder(template);
    }

    private static final class DraftBuilder {
        private String name;
        private int strength;
        private int dexterity;
        private int constitution;
        private int intelligence;
        private int wisdom;
        private int charisma;
        private ScoreSource scoreSource;
        private int startingLevel;
        private String raceIndex;
        private String classIndex;
        private String subclassIndex;
        private String backgroundIndex;
        private Set<String> skillPickIndexes;
        private Set<String> spellIndexes;
        private Set<String> equipmentIndexes;

        DraftBuilder(CharacterDraftDto d) {
            this.name = d.name();
            this.strength = d.strength();
            this.dexterity = d.dexterity();
            this.constitution = d.constitution();
            this.intelligence = d.intelligence();
            this.wisdom = d.wisdom();
            this.charisma = d.charisma();
            this.scoreSource = d.scoreSource();
            this.startingLevel = d.startingLevel();
            this.raceIndex = d.raceIndex();
            this.classIndex = d.classIndex();
            this.subclassIndex = d.subclassIndex();
            this.backgroundIndex = d.backgroundIndex();
            this.skillPickIndexes = d.skillPickIndexes();
            this.spellIndexes = d.spellIndexes();
            this.equipmentIndexes = d.equipmentIndexes();
        }

        DraftBuilder strength(int v) { this.strength = v; return this; }
        DraftBuilder dexterity(int v) { this.dexterity = v; return this; }
        DraftBuilder constitution(int v) { this.constitution = v; return this; }
        DraftBuilder intelligence(int v) { this.intelligence = v; return this; }
        DraftBuilder wisdom(int v) { this.wisdom = v; return this; }
        DraftBuilder charisma(int v) { this.charisma = v; return this; }
        DraftBuilder startingLevel(int v) { this.startingLevel = v; return this; }
        DraftBuilder raceIndex(String v) { this.raceIndex = v; return this; }
        DraftBuilder classIndex(String v) { this.classIndex = v; return this; }
        DraftBuilder subclassIndex(String v) { this.subclassIndex = v; return this; }
        DraftBuilder backgroundIndex(String v) { this.backgroundIndex = v; return this; }
        DraftBuilder skillPickIndexes(Set<String> v) { this.skillPickIndexes = v; return this; }
        DraftBuilder spellIndexes(Set<String> v) { this.spellIndexes = v; return this; }
        DraftBuilder equipmentIndexes(Set<String> v) { this.equipmentIndexes = v; return this; }

        CharacterDraftDto build() {
            return new CharacterDraftDto(name, strength, dexterity, constitution, intelligence, wisdom,
                    charisma, scoreSource, startingLevel, raceIndex, classIndex, subclassIndex,
                    backgroundIndex, skillPickIndexes, spellIndexes, equipmentIndexes);
        }
    }

    private Dnd5eCharacter savedCharacter(Long id, String name) {
        Dnd5eCharacter entity = new Dnd5eCharacter();
        entity.setId(id);
        entity.setOwner(user(5L));
        entity.setName(name);
        entity.setLevel(1);
        entity.setScoreSource(ScoreSource.STANDARD_ARRAY);
        entity.setRaceIndex("dwarf");
        entity.setClassIndex("cleric");
        entity.setSubclassIndex("life");
        entity.setBackgroundIndex("acolyte");
        entity.setHitPoints(10);
        entity.setArmorClass(13);
        return entity;
    }

    private static User user(Long id) {
        User user = new User("aria", "Aria", "aria@example.com", LocalDate.of(1990, 1, 15), "hash");
        user.setId(id);
        return user;
    }
}