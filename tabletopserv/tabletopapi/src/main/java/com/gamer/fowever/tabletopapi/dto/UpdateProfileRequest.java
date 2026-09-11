package com.gamer.fowever.tabletopapi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "Name is required")
        String displayName,
        @NotBlank(message = "Real name is required")
        @Size(max = 150, message = "Real name must be 150 characters or fewer")
        String realName
) {
}