package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.ScoreSource;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

/**
 * Client-assembled character choices (SRD {@code index} references plus scores).
 * The server compiles this into a legal sheet; nothing here is trusted.
 */
public record CharacterDraftDto(
        @NotBlank String name,
        @Min(1) @Max(30) int strength,
        @Min(1) @Max(30) int dexterity,
        @Min(1) @Max(30) int constitution,
        @Min(1) @Max(30) int intelligence,
        @Min(1) @Max(30) int wisdom,
        @Min(1) @Max(30) int charisma,
        @NotNull ScoreSource scoreSource,
        @Min(1) @Max(3) int startingLevel,
        String raceIndex,
        String classIndex,
        String subclassIndex,
        String backgroundIndex,
        Set<String> skillPickIndexes,
        Set<String> spellIndexes,
        Set<String> equipmentIndexes) {

    public CharacterDraftDto {
        skillPickIndexes = skillPickIndexes == null ? new HashSet<>() : skillPickIndexes;
        spellIndexes = spellIndexes == null ? new HashSet<>() : spellIndexes;
        equipmentIndexes = equipmentIndexes == null ? new HashSet<>() : equipmentIndexes;
    }
}