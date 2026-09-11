package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.ScoreSource;
import tools.jackson.databind.JsonNode;

import java.util.List;
import java.util.Map;

/**
 * A compiled, validated 5e sheet: SRD {@code index} references, final scores and
 * every derived value. {@code sheetSnapshot} carries the same sheet as JSON so
 * the client can render it without replaying rule data.
 */
public record CharacterSheetDto(
        Long id,
        String name,
        int level,
        ScoreSource scoreSource,
        String raceIndex,
        String classIndex,
        String subclassIndex,
        String backgroundIndex,
        int strength,
        int dexterity,
        int constitution,
        int intelligence,
        int wisdom,
        int charisma,
        int proficiencyBonus,
        int hitPoints,
        int armorClass,
        int speedFeet,
        List<String> savingThrows,
        List<String> classSkills,
        List<String> backgroundSkills,
        List<String> skillPicks,
        List<String> spellIndexes,
        Map<Integer, Integer> spellSlots,
        List<String> featureIndexes,
        List<String> equipmentIndexes,
        int startingGoldGp,
        int spentGoldGp,
        JsonNode sheetSnapshot) {
}