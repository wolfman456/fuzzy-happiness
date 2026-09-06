package com.gamer.fowever.tabletopserv.dto;

import jakarta.validation.constraints.NotBlank;

public record JoinSessionRequest(
        @NotBlank(message = "Invite code is required")
        String inviteCode
) {
}