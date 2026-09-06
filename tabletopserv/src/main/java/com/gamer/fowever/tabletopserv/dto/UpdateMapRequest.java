package com.gamer.fowever.tabletopserv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record UpdateMapRequest(
        @Size(max = 120) String name,
        @Min(1) @Max(200) Integer width,
        @Min(1) @Max(200) Integer height) {
}