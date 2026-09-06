package com.gamer.fowever.tabletopserv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RollRequest(
        @NotBlank(message = "Dice expression is required") String expression,
        @Size(max = 120, message = "Roll label must be at most 120 characters") String label,
        Boolean privateRoll) {
}