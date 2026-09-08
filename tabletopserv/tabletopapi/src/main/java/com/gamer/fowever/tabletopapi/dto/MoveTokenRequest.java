package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.constraints.Min;

public record MoveTokenRequest(@Min(0) int x, @Min(0) int y) {
}