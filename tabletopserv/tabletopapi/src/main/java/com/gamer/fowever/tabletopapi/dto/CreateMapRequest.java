package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record CreateMapRequest(
        @NotBlank String name,
        @Min(1) @Max(200) Integer width,
        @Min(1) @Max(200) Integer height) {
}