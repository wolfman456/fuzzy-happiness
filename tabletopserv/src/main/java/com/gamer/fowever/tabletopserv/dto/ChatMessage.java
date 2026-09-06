package com.gamer.fowever.tabletopserv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatMessage(
        @NotBlank(message = "Message must not be empty")
        @Size(max = 2000, message = "Message must be at most 2000 characters")
        String text
) {
}