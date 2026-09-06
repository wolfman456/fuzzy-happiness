package com.gamer.fowever.tabletopserv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateTokenRequest(
        String name,
        String color,
        @Min(1) @Max(240) Integer speedFeet) {
}