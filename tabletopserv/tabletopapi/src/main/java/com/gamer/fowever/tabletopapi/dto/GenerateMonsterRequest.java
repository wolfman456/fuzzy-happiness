package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.MonsterEdition;
import com.gamer.fowever.tabletopapi.MonsterRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record GenerateMonsterRequest(
        @NotBlank String cr,
        MonsterRole role,
        MonsterEdition edition,
        String name,
        String concept,
        @Min(0) @Max(Integer.MAX_VALUE) Integer seed) {

    public MonsterRole roleOrAuto() {
        return role != null ? role : MonsterRole.AUTO;
    }

    public MonsterEdition editionOrDefault() {
        return edition != null ? edition : MonsterEdition.SRD_2014;
    }

    public int seedOrDefault() {
        return seed != null ? seed : 0;
    }
}