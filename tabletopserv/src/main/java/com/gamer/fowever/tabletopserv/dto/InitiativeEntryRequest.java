package com.gamer.fowever.tabletopserv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record InitiativeEntryRequest(
        @Size(max = 120, message = "Initiative label must be at most 120 characters") String label,
        Long tokenId,
        @Min(value = 1, message = "Initiative score must be at least 1")
        @Max(value = 999, message = "Initiative score must be at most 999")
        Integer score) {
}