package com.gamer.fowever.tabletopserv.dto;

import com.gamer.fowever.tabletopserv.domain.InitiativeEntry;

public record InitiativeEntryDto(
        Long id,
        String label,
        Long tokenId,
        String tokenName,
        int score) {

    public static InitiativeEntryDto from(InitiativeEntry entry) {
        return new InitiativeEntryDto(
                entry.getId(),
                entry.getLabel(),
                entry.getToken() != null ? entry.getToken().getId() : null,
                entry.getToken() != null ? entry.getToken().getName() : null,
                entry.getScore());
    }
}