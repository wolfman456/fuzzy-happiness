package com.gamer.fowever.tabletopserv.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendRequest(
        @NotBlank(message = "Username or email is required")
        String identifier
) {
}