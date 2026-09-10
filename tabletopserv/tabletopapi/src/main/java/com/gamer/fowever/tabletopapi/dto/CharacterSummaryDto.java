package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.ScoreSource;

/** Lightweight row for the "my characters" list. */
public record CharacterSummaryDto(
        Long id,
        String name,
        int level,
        ScoreSource scoreSource,
        String raceIndex,
        String classIndex,
        String subclassIndex,
        String backgroundIndex,
        int hitPoints,
        int armorClass) {
}