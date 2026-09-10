package com.gamer.fowever.tabletopservice.service.character;

import com.gamer.fowever.tabletopapi.ScoreSource;
import com.gamer.fowever.tabletopapi.dto.CharacterDraftDto;
import com.gamer.fowever.tabletopapi.dto.CharacterSheetDto;
import com.gamer.fowever.tabletopapi.dto.CharacterSummaryDto;
import com.gamer.fowever.tabletopapi.dto.CompileResult;
import com.gamer.fowever.tabletopapi.dto.GenerateCharacterRequest;
import com.gamer.fowever.tabletopapi.support.ApiException;
import com.gamer.fowever.tabletopservice.domain.Character;
import com.gamer.fowever.tabletopservice.domain.Dnd5eCharacter;
import com.gamer.fowever.tabletopservice.domain.User;
import com.gamer.fowever.tabletopservice.repository.CharacterRepository;
import com.gamer.fowever.tabletopservice.service.SrdClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

/**
 * Draft → compile → finalize for D&D 5e characters (§8). {@code compile} is a
 * pure, idempotent validation + derivation step against the SRD allow-lists;
 * {@code generate} assembles a legal random quick-build; {@code create}
 * persists a validated sheet.
 */
@Service
public class CharacterService {

    private static final List<String> ABILITIES = List.of("str", "dex", "con", "int", "wis", "cha");

    private final SrdClient srd;
    private final CharacterRepository characterRepository;
    private final ObjectMapper objectMapper;

    public CharacterService(SrdClient srd, CharacterRepository characterRepository, ObjectMapper objectMapper) {
        this.srd = srd;
        this.characterRepository = characterRepository;
        this.objectMapper = objectMapper;
    }

    private record SrdFacts(Set<String> races, Set<String> classes, Set<String> backgrounds,
                            Set<String> skills, Set<String> equipment,
                            JsonNode race, JsonNode classRecord, JsonNode classLevels, JsonNode classSpells,
                            List<EquipmentFact> equipmentFacts) {
    }

    private record EquipmentFact(String index, boolean armor, boolean shield, int baseAc, boolean dexBonus) {
    }

    private record CastingRow(int cantrips, Map<Integer, Integer> slots) {
    }

    public CompileResult compile(User actor, CharacterDraftDto draft) {
        List<String> violations = new ArrayList<>();
        SrdFacts facts = loadFacts(draft, violations);
        if (violations.isEmpty()) {
            validate(draft, facts, violations);
        }
        if (!violations.isEmpty()) {
            return CompileResult.invalid(violations);
        }
        return CompileResult.ok(derive(draft, facts));
    }

    public CompileResult generate(User actor, GenerateCharacterRequest request) {
        Random random = request.seedOrDefault() == 0 ? new SecureRandom() : new Random(request.seedOrDefault());

        JsonNode races = srd.list("races", Map.of());
        JsonNode classes = srd.list("classes", Map.of());
        JsonNode backgrounds = srd.list("backgrounds", Map.of());
        JsonNode skillsList = srd.list("skills", Map.of());
        JsonNode equipmentList = srd.list("equipment", Map.of());

        String raceIndex = randomElement(races, random);
        String classIndex = randomElement(classes, random);
        String backgroundIndex = randomElement(backgrounds, random);

        JsonNode race = srd.detail("races", raceIndex);
        JsonNode classRecord = srd.detail("classes", classIndex);
        JsonNode background = srd.detail("backgrounds", backgroundIndex);
        JsonNode classLevels = srd.subresource("classes", classIndex, "levels", Map.of());
        boolean caster = isCaster(classRecord);
        JsonNode classSpells = caster ? srd.subresource("classes", classIndex, "spells", Map.of()) : null;

        int level = request.startingLevelOrDefault();
        ScoreSource source = request.scoreSourceOrDefault();

        Map<String, Integer> base = rollBaseScores(source, random);
        Map<String, Integer> finalScores = applyRacialBonuses(base, race);

        Set<String> offered = offeredClassSkills(classRecord);
        int classCap = classChoiceCap(classRecord);
        List<String> skillPicks = randomSkills(offered, skillsList, classCap, random);

        String subclass = randomSubclass(classRecord, level, random);
        List<String> spells = caster ? randomSpells(classSpells, classLevels, level, random) : List.of();
        List<String> equipment = randomEquipment(equipmentList, background, random);

        CharacterDraftDto draft = new CharacterDraftDto(
                isBlank(request.name()) ? "Surprise Me" : request.name().trim(),
                finalScores.get("str"), finalScores.get("dex"), finalScores.get("con"),
                finalScores.get("int"), finalScores.get("wis"), finalScores.get("cha"),
                source, level,
                raceIndex, classIndex, subclass, backgroundIndex,
                new HashSet<>(skillPicks), new HashSet<>(spells), new HashSet<>(equipment));
        return compile(actor, draft);
    }

    @Transactional
    public CharacterSheetDto create(User actor, CharacterDraftDto draft) {
        CompileResult result = compile(actor, draft);
        if (!result.valid()) {
            throw ApiException.badRequest("character is not legal: " + String.join("; ", result.violations()));
        }
        CharacterSheetDto sheet = result.sheet();
        Dnd5eCharacter entity = new Dnd5eCharacter();
        entity.setOwner(actor);
        entity.setName(sheet.name());
        entity.setGameVersion("2014");
        entity.setStrength(sheet.strength());
        entity.setDexterity(sheet.dexterity());
        entity.setConstitution(sheet.constitution());
        entity.setIntelligence(sheet.intelligence());
        entity.setWisdom(sheet.wisdom());
        entity.setCharisma(sheet.charisma());
        entity.setScoreSource(sheet.scoreSource());
        entity.setLevel(sheet.level());
        entity.setRaceIndex(sheet.raceIndex());
        entity.setClassIndex(sheet.classIndex());
        entity.setSubclassIndex(sheet.subclassIndex());
        entity.setBackgroundIndex(sheet.backgroundIndex());
        entity.setSkillPickIndexes(new HashSet<>(sheet.skillPicks()));
        entity.setSpellIndexes(new HashSet<>(sheet.spellIndexes()));
        entity.setFeatureIndexes(new HashSet<>(sheet.featureIndexes()));
        entity.setEquipmentIndexes(new HashSet<>(sheet.equipmentIndexes()));
        entity.setHitPoints(sheet.hitPoints());
        entity.setArmorClass(sheet.armorClass());
        entity.setSpeedFeet(sheet.speedFeet());
        entity.setProficiencyBonus(sheet.proficiencyBonus());
        entity.setSheetSnapshot(toJson(sheet));
        Dnd5eCharacter saved = (Dnd5eCharacter) characterRepository.saveAndFlush(entity);
        return withId(saved.getId(), sheet);
    }

    @Transactional(readOnly = true)
    public List<CharacterSummaryDto> mine(User actor) {
        return characterRepository.findByOwnerIdOrderByIdDesc(actor.getId()).stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public CharacterSheetDto get(User actor, Long id) {
        Character found = characterRepository.findByIdAndOwnerId(id, actor.getId())
                .orElseThrow(() -> ApiException.notFound("character not found"));
        Dnd5eCharacter entity = dnd5e(found);
        return withId(entity.getId(), fromJson(entity.getSheetSnapshot()));
    }

    private SrdFacts loadFacts(CharacterDraftDto draft, List<String> violations) {
        Set<String> races = indexSet(srd.list("races", Map.of()));
        Set<String> classes = indexSet(srd.list("classes", Map.of()));
        Set<String> backgrounds = indexSet(srd.list("backgrounds", Map.of()));
        Set<String> skills = normalizeSet(indexSet(srd.list("skills", Map.of())));
        Set<String> equipment = indexSet(srd.list("equipment", Map.of()));

        requireIn(races, "race", draft.raceIndex(), violations);
        requireIn(classes, "class", draft.classIndex(), violations);
        requireIn(backgrounds, "background", draft.backgroundIndex(), violations);

        JsonNode race = isPresent(races, draft.raceIndex()) ? srd.detail("races", draft.raceIndex()) : null;
        JsonNode classRecord = isPresent(classes, draft.classIndex()) ? srd.detail("classes", draft.classIndex()) : null;
        JsonNode classLevels = classRecord != null
                ? srd.subresource("classes", draft.classIndex(), "levels", Map.of()) : null;
        JsonNode classSpells = isCaster(classRecord)
                ? srd.subresource("classes", draft.classIndex(), "spells", Map.of()) : null;

        List<EquipmentFact> equipmentFacts = new ArrayList<>();
        for (String index : draft.equipmentIndexes()) {
            if (equipment.contains(index)) {
                equipmentFacts.add(toEquipmentFact(srd.detail("equipment", index)));
            }
        }
        return new SrdFacts(races, classes, backgrounds, skills, equipment,
                race, classRecord, classLevels, classSpells, equipmentFacts);
    }

    private void validate(CharacterDraftDto draft, SrdFacts facts, List<String> violations) {
        validateScores(draft, facts, violations);
        validateSubclass(draft, facts, violations);
        validateSkills(draft, facts, violations);
        validateSpells(draft, facts, violations);
        validateEquipment(draft, facts, violations);
    }

    private void validateScores(CharacterDraftDto draft, SrdFacts facts, List<String> violations) {
        Map<String, Integer> base = subtractRacialBonuses(scoresOf(draft), facts.race());
        if (!ChargenRules.isLegalBaseScoreSet(draft.scoreSource(), base)) {
            violations.add("scores: not a valid " + draft.scoreSource() + " set once race bonuses are removed");
        }
    }

    private void validateSubclass(CharacterDraftDto draft, SrdFacts facts, List<String> violations) {
        String subclass = draft.subclassIndex();
        if (isBlank(subclass)) {
            return;
        }
        JsonNode classRecord = facts.classRecord();
        if (classRecord == null) {
            return;
        }
        Set<String> subclasses = indexSetOf(classRecord, "subclasses");
        if (!subclasses.contains(subclass)) {
            violations.add("subclassIndex: '" + subclass + "' is not a subclass of '" + draft.classIndex() + "'");
            return;
        }
        int required = subclassLevel(classRecord);
        if (draft.startingLevel() < required) {
            violations.add("subclassIndex: '" + subclass + "' requires level " + required);
        }
    }

    private void validateSkills(CharacterDraftDto draft, SrdFacts facts, List<String> violations) {
        Set<String> picks = normalizeSet(draft.skillPickIndexes());
        if (picks.isEmpty()) {
            return;
        }
        Set<String> known = facts.skills();
        Set<String> offered = offeredClassSkills(facts.classRecord());
        List<String> outside = new ArrayList<>();
        for (String pick : picks) {
            if (!known.contains(pick)) {
                violations.add("skillPickIndexes: '" + pick + "' is not a known SRD skill");
            } else if (!offered.contains(pick)) {
                outside.add(pick);
            }
        }
        int cap = classChoiceCap(facts.classRecord()) + 2;
        if (picks.size() > cap) {
            violations.add("skillPickIndexes: no more than " + cap + " skill proficiencies for this class/background");
        }
        if (outside.size() > 2) {
            violations.add("skillPickIndexes: at most 2 skill picks may come from the background (outside the class list)");
        }
    }

    private void validateSpells(CharacterDraftDto draft, SrdFacts facts, List<String> violations) {
        Set<String> picks = normalizeSet(draft.spellIndexes());
        if (picks.isEmpty()) {
            return;
        }
        if (!isCaster(facts.classRecord())) {
            violations.add("spellIndexes: this class cannot cast spells");
            return;
        }
        JsonNode classSpells = facts.classSpells();
        Set<String> allowed = indexSet(classSpells);
        Map<String, Integer> byIndex = new HashMap<>();
        if (classSpells != null) {
            for (JsonNode spell : classSpells.path("results")) {
                byIndex.put(spell.path("index").asText(), spell.path("level").asInt());
            }
        }
        CastingRow row = castingRow(facts.classLevels(), draft.startingLevel());
        int maxSlot = row == null ? 0 : row.slots().keySet().stream().mapToInt(Integer::intValue).max().orElse(0);
        int cantripsKnown = row == null ? 0 : row.cantrips();
        int cantrips = 0;
        int leveledCount = 0;
        for (String pick : picks) {
            if (!allowed.contains(pick)) {
                violations.add("spellIndexes: '" + pick + "' is not on the " + draft.classIndex() + " spell list");
                continue;
            }
            int level = byIndex.getOrDefault(pick, maxSlot);
            if (level == 0) {
                cantrips++;
            } else {
                leveledCount++;
                if (level > maxSlot) {
                    violations.add("spellIndexes: '" + pick + "' needs spell slots above level " + maxSlot);
                }
            }
        }
        if (cantrips > cantripsKnown) {
            violations.add("spellIndexes: no more than " + cantripsKnown + " cantrips at " + draft.startingLevel() + " level");
        }
        if (leveledCount > 0 && maxSlot == 0) {
            violations.add("spellIndexes: this class has no spell slots at " + draft.startingLevel() + " level");
        }
    }

    private void validateEquipment(CharacterDraftDto draft, SrdFacts facts, List<String> violations) {
        for (String index : draft.equipmentIndexes()) {
            if (isBlank(index)) {
                continue;
            }
            if (!facts.equipment().contains(index)) {
                violations.add("equipmentIndexes: '" + index + "' is not a known SRD equipment item");
            }
        }
    }

    private CharacterSheetDto derive(CharacterDraftDto draft, SrdFacts facts) {
        Map<String, Integer> scores = scoresOf(draft);
        int level = draft.startingLevel();
        int conMod = ChargenRules.abilityModifier(scores.get("con"));
        int dexMod = ChargenRules.abilityModifier(scores.get("dex"));
        int hitDie = facts.classRecord() != null && facts.classRecord().hasNonNull("hit_die")
                ? facts.classRecord().get("hit_die").asInt() : 8;
        int hitPoints = ChargenRules.hitPoints(level, hitDie, conMod);
        int speed = facts.race() != null && facts.race().hasNonNull("speed")
                ? facts.race().get("speed").asInt(30) : 30;

        List<String> savingThrows = sortedIndexes(facts.classRecord(), "saving_throws");
        Set<String> offered = offeredClassSkills(facts.classRecord());
        List<String> classSkills = new ArrayList<>(offered);
        Collections.sort(classSkills);
        List<String> skillPicks = sortAndStrip(draft.skillPickIndexes());
        List<String> backgroundSkills = skillPicks.stream().filter(pick -> !offered.contains(pick)).toList();

        List<ChargenRules.ArmorPiece> armors = facts.equipmentFacts().stream()
                .filter(EquipmentFact::armor)
                .map(fact -> new ChargenRules.ArmorPiece(fact.baseAc(), fact.dexBonus()))
                .toList();
        boolean shield = facts.equipmentFacts().stream().anyMatch(EquipmentFact::shield);
        int armorClass = ChargenRules.armorClass(dexMod, armors, shield);

        int proficiencyBonus = profBonusAt(facts.classLevels(), level);
        Map<Integer, Integer> spellSlots = spellSlotsAt(facts.classLevels(), level);
        List<String> features = featuresUpTo(facts.classLevels(), level);
        List<String> spells = sortAndStrip(draft.spellIndexes());
        List<String> equipment = sortedNonBlank(draft.equipmentIndexes());

        CharacterSheetDto core = new CharacterSheetDto(
                null, draft.name(), level, draft.scoreSource(),
                draft.raceIndex(), draft.classIndex(), draft.subclassIndex(), draft.backgroundIndex(),
                scores.get("str"), scores.get("dex"), scores.get("con"),
                scores.get("int"), scores.get("wis"), scores.get("cha"),
                proficiencyBonus, hitPoints, armorClass, speed,
                savingThrows, classSkills, backgroundSkills, skillPicks, spells, spellSlots,
                features, equipment, null);
        JsonNode snapshot = toNode(toJson(core));
        return new CharacterSheetDto(
                null, draft.name(), level, draft.scoreSource(),
                draft.raceIndex(), draft.classIndex(), draft.subclassIndex(), draft.backgroundIndex(),
                scores.get("str"), scores.get("dex"), scores.get("con"),
                scores.get("int"), scores.get("wis"), scores.get("cha"),
                proficiencyBonus, hitPoints, armorClass, speed,
                savingThrows, classSkills, backgroundSkills, skillPicks, spells, spellSlots,
                features, equipment, snapshot);
    }

    private CharacterSheetDto withId(Long id, CharacterSheetDto sheet) {
        return new CharacterSheetDto(
                id, sheet.name(), sheet.level(), sheet.scoreSource(),
                sheet.raceIndex(), sheet.classIndex(), sheet.subclassIndex(), sheet.backgroundIndex(),
                sheet.strength(), sheet.dexterity(), sheet.constitution(),
                sheet.intelligence(), sheet.wisdom(), sheet.charisma(),
                sheet.proficiencyBonus(), sheet.hitPoints(), sheet.armorClass(), sheet.speedFeet(),
                sheet.savingThrows(), sheet.classSkills(), sheet.backgroundSkills(), sheet.skillPicks(),
                sheet.spellIndexes(), sheet.spellSlots(), sheet.featureIndexes(), sheet.equipmentIndexes(),
                sheet.sheetSnapshot());
    }

    private CharacterSummaryDto toSummary(Character character) {
        Dnd5eCharacter entity = dnd5e(character);
        return new CharacterSummaryDto(
                entity.getId(), entity.getName(), entity.getLevel(), entity.getScoreSource(),
                entity.getRaceIndex(), entity.getClassIndex(), entity.getSubclassIndex(),
                entity.getBackgroundIndex(), entity.getHitPoints(), entity.getArmorClass());
    }

    private Dnd5eCharacter dnd5e(Character character) {
        if (character instanceof Dnd5eCharacter entity) {
            return entity;
        }
        throw ApiException.notFound("unsupported character kind");
    }

    private void requireIn(Set<String> allowList, String label, String index, List<String> violations) {
        if (!isPresent(allowList, index)) {
            violations.add("must choose a valid " + label + " (unknown index: '"
                    + (isBlank(index) ? "" : index) + "')");
        }
    }

    private EquipmentFact toEquipmentFact(JsonNode detail) {
        String index = detail.path("index").asText();
        boolean armor = "armor".equals(detail.path("equipment_category").path("index").asText());
        boolean shield = "shield".equals(index);
        JsonNode ac = detail.get("armor_class");
        int baseAc = ac != null ? ac.path("base").asInt(0) : 0;
        boolean dexBonus = ac != null && ac.path("dex_bonus").asBoolean(false);
        return new EquipmentFact(index, armor, shield, baseAc, dexBonus);
    }

    private Map<String, Integer> rollBaseScores(ScoreSource source, Random random) {
        List<Integer> values = new ArrayList<>();
        if (source == ScoreSource.STANDARD_ARRAY) {
            values.addAll(List.of(15, 14, 13, 12, 10, 8));
        } else if (source == ScoreSource.FOUR_D6_DROP_LOWEST) {
            for (int i = 0; i < 6; i++) {
                List<Integer> rolls = new ArrayList<>();
                for (int r = 0; r < 4; r++) {
                    rolls.add(random.nextInt(6) + 1);
                }
                rolls.sort(Integer::compareTo);
                values.add(rolls.get(1) + rolls.get(2) + rolls.get(3));
            }
        } else if (source == ScoreSource.HOUSE_RULE_D20) {
            for (int i = 0; i < 6; i++) {
                values.add(random.nextInt(20) + 1);
            }
        } else {
            List<Integer> attempt;
            do {
                attempt = new ArrayList<>();
                for (int i = 0; i < 6; i++) {
                    attempt.add(8 + random.nextInt(8));
                }
            } while (attempt.stream().mapToInt(ChargenRules::pointBuyCost).sum() > 27);
            values.addAll(attempt);
        }
        Collections.shuffle(values, random);
        Map<String, Integer> scores = new LinkedHashMap<>();
        for (int i = 0; i < ABILITIES.size(); i++) {
            scores.put(ABILITIES.get(i), values.get(i));
        }
        return scores;
    }

    private Map<String, Integer> applyRacialBonuses(Map<String, Integer> base, JsonNode race) {
        Map<String, Integer> scores = new LinkedHashMap<>(base);
        JsonNode bonuses = race == null ? null : race.get("ability_bonuses");
        if (bonuses != null && bonuses.isArray()) {
            for (JsonNode entry : bonuses) {
                String ability = entry.path("ability_score").path("index").asText();
                int bonus = entry.path("bonus").asInt(0);
                scores.computeIfPresent(ability, (key, value) -> value + bonus);
            }
        }
        return scores;
    }

    private Map<String, Integer> subtractRacialBonuses(Map<String, Integer> finalScores, JsonNode race) {
        Map<String, Integer> base = new LinkedHashMap<>(finalScores);
        JsonNode bonuses = race == null ? null : race.get("ability_bonuses");
        if (bonuses != null && bonuses.isArray()) {
            for (JsonNode entry : bonuses) {
                String ability = entry.path("ability_score").path("index").asText();
                int bonus = entry.path("bonus").asInt(0);
                base.computeIfPresent(ability, (key, value) -> value - bonus);
            }
        }
        return base;
    }

    private List<String> randomSkills(Set<String> offered, JsonNode skillsList, int classCap, Random random) {
        List<String> classSkills = new ArrayList<>(offered);
        Collections.shuffle(classSkills, random);
        int take = Math.min(classCap, classSkills.size());
        List<String> picks = new ArrayList<>(classSkills.subList(0, take));
        List<String> others = new ArrayList<>(normalizeSet(indexSet(skillsList)));
        others.removeAll(picks);
        Collections.shuffle(others, random);
        int budget = classCap + 2;
        for (int i = 0; i < others.size() && picks.size() < budget; i++) {
            picks.add(others.get(i));
        }
        return picks;
    }

    private String randomSubclass(JsonNode classRecord, int level, Random random) {
        if (classRecord == null || level < subclassLevel(classRecord)) {
            return null;
        }
        List<String> subclasses = sortedIndexes(classRecord, "subclasses");
        if (subclasses.isEmpty()) {
            return null;
        }
        return subclasses.get(random.nextInt(subclasses.size()));
    }

    private List<String> randomSpells(JsonNode classSpells, JsonNode classLevels, int level, Random random) {
        CastingRow row = castingRow(classLevels, level);
        int cantripsKnown = row == null ? 0 : row.cantrips();
        Map<Integer, Integer> slots = row == null ? Map.of() : row.slots();
        int maxSlot = slots.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

        Map<Integer, List<String>> byLevel = new HashMap<>();
        for (JsonNode spell : classSpells.path("results")) {
            byLevel.computeIfAbsent(spell.path("level").asInt(), key -> new ArrayList<>())
                    .add(spell.path("index").asText());
        }
        List<String> picks = new ArrayList<>();
        List<String> cantrips = byLevel.getOrDefault(0, new ArrayList<>());
        Collections.shuffle(cantrips, random);
        for (int i = 0; i < Math.min(cantripsKnown, cantrips.size()); i++) {
            picks.add(cantrips.get(i));
        }
        for (int spellLevel = 1; spellLevel <= maxSlot; spellLevel++) {
            List<String> pool = byLevel.getOrDefault(spellLevel, new ArrayList<>());
            Collections.shuffle(pool, random);
            int allowed = slots.getOrDefault(spellLevel, 0);
            for (int i = 0; i < Math.min(allowed, pool.size()); i++) {
                picks.add(pool.get(i));
            }
        }
        return picks;
    }

    private List<String> randomEquipment(JsonNode equipmentList, JsonNode background, Random random) {
        Set<String> all = indexSet(equipmentList);
        LinkedHashSet<String> kit = new LinkedHashSet<>();

        JsonNode starting = background == null ? null : background.get("starting_equipment");
        if (starting != null && starting.isArray()) {
            for (JsonNode entry : starting) {
                String index = entry.path("equipment").path("index").asText();
                if (all.contains(index) && kit.size() < 3) {
                    kit.add(index);
                }
                if (kit.size() >= 3) {
                    break;
                }
            }
        }
        List<String> starters = new ArrayList<>(List.of("dagger", "staff", "leather-armor", "shield", "light-crossbow"));
        starters.retainAll(all);
        Collections.shuffle(starters, random);
        for (int i = 0; i < Math.min(2, starters.size()) && kit.size() < 6; i++) {
            kit.add(starters.get(i));
        }
        return new ArrayList<>(kit);
    }

    private String randomElement(JsonNode list, Random random) {
        List<String> indexes = new ArrayList<>(indexSet(list));
        indexes.removeIf(String::isBlank);
        if (indexes.isEmpty()) {
            throw ApiException.badGateway("rules data unavailable");
        }
        return indexes.get(random.nextInt(indexes.size()));
    }

    private CastingRow castingRow(JsonNode classLevels, int level) {
        if (classLevels == null || !classLevels.isArray()) {
            return null;
        }
        for (JsonNode row : classLevels) {
            if (row.path("level").asInt() == level) {
                JsonNode casting = row.get("spellcasting");
                if (casting == null || casting.isNull()) {
                    return null;
                }
                Map<Integer, Integer> slots = new LinkedHashMap<>();
                int cantrips = casting.path("cantrips_known").asInt(0);
                if (cantrips > 0) {
                    slots.put(0, cantrips);
                }
                for (int i = 1; i <= 9; i++) {
                    int count = casting.path("spell_slots_level_" + i).asInt(0);
                    if (count > 0) {
                        slots.put(i, count);
                    }
                }
                return new CastingRow(cantrips, slots);
            }
        }
        return null;
    }

    private int profBonusAt(JsonNode classLevels, int level) {
        int[] found = {2};
        if (classLevels != null && classLevels.isArray()) {
            classLevels.forEach(row -> {
                if (row.path("level").asInt() == level) {
                    found[0] = row.path("prof_bonus").asInt(2);
                }
            });
        }
        return found[0];
    }

    private List<String> featuresUpTo(JsonNode classLevels, int level) {
        Set<String> features = new TreeSet<>();
        if (classLevels != null && classLevels.isArray()) {
            classLevels.forEach(row -> {
                if (row.path("level").asInt() <= level) {
                    row.path("features").forEach(feature -> features.add(feature.path("index").asText()));
                }
            });
        }
        return new ArrayList<>(features);
    }

    private Map<Integer, Integer> spellSlotsAt(JsonNode classLevels, int level) {
        CastingRow row = castingRow(classLevels, level);
        return row == null ? Map.of() : row.slots();
    }

    private boolean isCaster(JsonNode classRecord) {
        return classRecord != null && classRecord.path("spellcasting").isObject();
    }

    private int subclassLevel(JsonNode classRecord) {
        JsonNode levelNode = classRecord.get("subclass_level");
        if (levelNode == null || levelNode.isNull()) {
            return 1;
        }
        return levelNode.asInt(1);
    }

    private int classChoiceCap(JsonNode classRecord) {
        if (classRecord == null) {
            return 2;
        }
        int cap = 0;
        JsonNode choices = classRecord.get("proficiency_choices");
        if (choices != null && choices.isArray()) {
            for (JsonNode choice : choices) {
                cap += choice.path("choose").asInt(0);
            }
        }
        return Math.max(1, cap);
    }

    private Set<String> offeredClassSkills(JsonNode classRecord) {
        Set<String> offered = new HashSet<>();
        if (classRecord == null) {
            return offered;
        }
        JsonNode choices = classRecord.get("proficiency_choices");
        if (choices != null && choices.isArray()) {
            for (JsonNode choice : choices) {
                JsonNode from = choice.get("from");
                if (from == null) {
                    continue;
                }
                from.path("options").forEach(option -> {
                    String index = option.path("item").path("index").asText();
                    if (!index.isBlank()) {
                        offered.add(stripSkillPrefix(index));
                    }
                });
            }
        }
        return offered;
    }

    private Set<String> indexSet(JsonNode list) {
        Set<String> set = new HashSet<>();
        if (list != null) {
            list.path("results").forEach(node -> {
                String index = node.path("index").asText();
                if (!index.isBlank()) {
                    set.add(index);
                }
            });
        }
        return set;
    }

    private Set<String> indexSetOf(JsonNode object, String field) {
        Set<String> set = new HashSet<>();
        if (object != null) {
            object.path(field).forEach(node -> set.add(node.path("index").asText()));
        }
        return set;
    }

    private List<String> sortedIndexes(JsonNode object, String field) {
        Set<String> set = indexSetOf(object, field);
        List<String> list = new ArrayList<>(set);
        Collections.sort(list);
        return list;
    }

    private Set<String> normalizeSet(Set<String> values) {
        Set<String> normalized = new HashSet<>();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            normalized.add(stripSkillPrefix(value.trim()));
        }
        return normalized;
    }

    private List<String> sortAndStrip(Set<String> values) {
        List<String> list = new ArrayList<>(normalizeSet(values));
        Collections.sort(list);
        return list;
    }

    private List<String> sortedNonBlank(Set<String> values) {
        List<String> list = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                list.add(value.trim());
            }
        }
        Collections.sort(list);
        return list;
    }

    private String stripSkillPrefix(String index) {
        return index.startsWith("skill-") ? index.substring("skill-".length()) : index;
    }

    private Map<String, Integer> scoresOf(CharacterDraftDto draft) {
        Map<String, Integer> scores = new LinkedHashMap<>();
        scores.put("str", draft.strength());
        scores.put("dex", draft.dexterity());
        scores.put("con", draft.constitution());
        scores.put("int", draft.intelligence());
        scores.put("wis", draft.wisdom());
        scores.put("cha", draft.charisma());
        return scores;
    }

    private boolean isPresent(Set<String> allowList, String index) {
        return index != null && !index.isBlank() && allowList.contains(index);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String toJson(CharacterSheetDto sheet) {
        try {
            return objectMapper.writeValueAsString(sheet);
        } catch (JacksonException ex) {
            throw ApiException.badGateway("sheet serialization failed");
        }
    }

    private JsonNode toNode(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException ex) {
            throw ApiException.badGateway("sheet parse failed");
        }
    }

    private CharacterSheetDto fromJson(String json) {
        try {
            return objectMapper.readValue(json, CharacterSheetDto.class);
        } catch (JacksonException ex) {
            throw ApiException.badGateway("sheet snapshot is invalid");
        }
    }
}