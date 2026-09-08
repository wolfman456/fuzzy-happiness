package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.MonsterEdition;
import com.gamer.fowever.tabletopapi.MonsterRole;

import java.time.Instant;
import java.util.List;

public record MonsterDto(
        Long id,
        String name,
        String cr,
        int xp,
        MonsterEdition edition,
        MonsterRole role,
        int proficiencyBonus,
        int armorClass,
        int hitPoints,
        String size,
        String type,
        String alignment,
        int speedFeet,
        int strength,
        int dexterity,
        int constitution,
        int intelligence,
        int wisdom,
        int charisma,
        int attackBonus,
        int saveDc,
        int damagePerRound,
        String description,
        List<MonsterAction> actions,
        List<String> traits,
        Instant createdAt) {

    public record MonsterAction(String name, String description) {
    }
}