package com.gamer.fowever.tabletopapi.dto;

import com.gamer.fowever.tabletopapi.TokenCategory;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AddTokenRequest(
        @NotBlank String name,
        TokenCategory category,
        String color,
        @Min(1) @Max(240) Integer speedFeet,
        @Min(0) Integer x,
        @Min(0) Integer y) {
}