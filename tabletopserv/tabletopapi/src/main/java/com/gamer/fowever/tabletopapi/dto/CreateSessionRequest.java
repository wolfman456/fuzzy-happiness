package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
        @NotBlank(message = "Session name is required")
        @Size(max = 100, message = "Session name must be at most 100 characters")
        String name,
        @NotBlank(message = "Game slug is required")
        String gameSlug
) {
}