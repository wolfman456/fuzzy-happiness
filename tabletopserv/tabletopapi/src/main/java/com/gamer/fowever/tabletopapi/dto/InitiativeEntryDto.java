package com.gamer.fowever.tabletopapi.dto;

public record InitiativeEntryDto(
        Long id,
        String label,
        Long tokenId,
        String tokenName,
        int score) {
}